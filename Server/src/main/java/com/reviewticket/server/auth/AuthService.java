package com.reviewticket.server.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.reviewticket.server.config.ReviewTicketProperties;
import com.reviewticket.server.domain.PendingSignup;
import com.reviewticket.server.domain.Role;
import com.reviewticket.server.domain.User;
import com.reviewticket.server.mail.VerificationMailer;
import com.reviewticket.server.repository.PendingSignupRepository;
import com.reviewticket.server.repository.UserRepository;

/**
 * 가입, 이메일 인증, 로그인.
 *
 * 핵심 정책 — 이메일 인증이 끝나기 전에는 회원이 만들어지지 않는다.
 * 가입 요청은 {@link PendingSignup} 에 대기하고, 메일 링크를 눌러 인증이
 * 확인된 시점에 {@link User} 로 옮긴다.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    /** 재발송 최소 간격. 없으면 남의 메일함에 무한정 보낼 수 있다. */
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private static final int MAX_DISPLAY_NAME_LENGTH = 32;

    private final UserRepository users;
    private final PendingSignupRepository pendings;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final VerificationMailer mailer;
    private final ReviewTicketProperties properties;

    public AuthService(UserRepository users, PendingSignupRepository pendings,
            PasswordEncoder passwordEncoder, JwtProvider jwtProvider,
            VerificationMailer mailer, ReviewTicketProperties properties) {
        this.users = users;
        this.pendings = pendings;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.mailer = mailer;
        this.properties = properties;
    }

    // ---------- 중복 검사 ----------
    // 두 표를 함께 본다. 대기 중인 이메일·닉네임도 예약된 것으로 취급한다.

    public boolean emailTaken(String email) {
        String normalized = normalizeEmail(email);
        return users.existsByEmail(normalized) || pendings.existsByEmail(normalized);
    }

    public boolean displayNameTaken(String displayName) {
        String trimmed = displayName.trim();
        return users.existsByDisplayName(trimmed) || pendings.existsByDisplayName(trimmed);
    }

    // ---------- 가입 요청 ----------

    /**
     * 가입 정보를 검증해 대기 표에 넣고 인증 메일을 보낸다.
     * 이 시점에 회원은 만들어지지 않는다.
     *
     * 같은 이메일로 다시 요청하면 옛 대기 건을 지우고 새로 만든다 —
     * 오타를 고쳐 다시 시도하는 흐름을 막지 않기 위함이다.
     */
    @Transactional
    public void requestSignUp(String rawEmail, String rawPassword, Role role, String rawDisplayName) {
        String email = normalizeEmail(rawEmail);
        String displayName = rawDisplayName == null ? "" : rawDisplayName.trim();

        if (displayName.isEmpty()) {
            throw new IllegalArgumentException(
                    role == Role.OWNER ? "가게 이름을 입력해 주세요" : "닉네임을 입력해 주세요");
        }
        if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException("이름은 " + MAX_DISPLAY_NAME_LENGTH + "자 이하여야 합니다");
        }
        PasswordPolicy.require(rawPassword);

        // 이미 가입을 마친 회원과의 충돌은 무조건 거부한다.
        if (users.existsByEmail(email)) {
            throw new ConflictException("이미 가입된 이메일입니다");
        }
        if (users.existsByDisplayName(displayName)) {
            throw new ConflictException(
                    role == Role.OWNER ? "이미 쓰이고 있는 가게 이름입니다" : "이미 쓰이고 있는 닉네임입니다");
        }

        // 대기 중인 건과의 충돌: 같은 이메일이면 본인의 재시도로 보고 덮어쓴다.
        pendings.findByEmail(email).ifPresent(pendings::delete);

        // 다른 사람이 같은 이름으로 대기 중이면 거부한다.
        if (pendings.existsByDisplayName(displayName)) {
            throw new ConflictException(
                    role == Role.OWNER ? "이미 쓰이고 있는 가게 이름입니다" : "이미 쓰이고 있는 닉네임입니다");
        }
        // 위 delete 를 DB 에 먼저 반영해야 UNIQUE 제약과 부딪히지 않는다.
        pendings.flush();

        String token = newToken();
        PendingSignup pending = pendings.save(new PendingSignup(
                email, passwordEncoder.encode(rawPassword), role, displayName,
                token, LocalDateTime.now().plus(properties.auth().verificationTtl())));

        mailer.send(pending.getEmail(), token);
    }

    /** 인증 메일 재발송. 토큰을 새로 만들어 옛 링크는 무효화한다. */
    @Transactional
    public void resendVerification(String rawEmail) {
        Optional<PendingSignup> found = pendings.findByEmail(normalizeEmail(rawEmail));
        if (found.isEmpty()) {
            // 대기 건이 없어도 성공처럼 조용히 끝낸다. 여기서 404 를 내면
            // 어떤 이메일이 가입 대기 중인지 알아낼 수 있다.
            return;
        }
        PendingSignup pending = found.get();

        LocalDateTime createdAt = pending.getCreatedAt();
        if (createdAt != null && createdAt.isAfter(LocalDateTime.now().minus(RESEND_COOLDOWN))) {
            throw new IllegalArgumentException(
                    "인증 메일을 방금 보냈습니다. " + RESEND_COOLDOWN.toSeconds() + "초 뒤에 다시 시도해 주세요");
        }

        String token = newToken();
        pending.renew(token, LocalDateTime.now().plus(properties.auth().verificationTtl()));
        mailer.send(pending.getEmail(), token);
    }

    // ---------- 인증 ----------

    /**
     * 메일 링크 처리. 여기서 실제 회원이 만들어진다.
     *
     * @return 가입이 확정된 이메일
     */
    @Transactional
    public String verify(String token) {
        PendingSignup pending = pendings.findByToken(token)
                // 이미 인증을 마쳤거나(대기 건 삭제됨) 만료돼 정리된 경우도 여기로 온다.
                // 어느 쪽인지 구분할 근거가 남아 있지 않으므로 한 문장으로 안내한다.
                .orElseThrow(() -> new IllegalArgumentException(
                        "인증 링크가 만료되었거나 이미 처리되었습니다"));

        if (pending.isExpired(LocalDateTime.now())) {
            throw new IllegalArgumentException("인증 링크가 만료되었습니다. 회원가입을 다시 진행해 주세요");
        }

        // 대기하는 동안 다른 사람이 같은 이메일·이름으로 먼저 가입을 마쳤을 수 있다.
        if (users.existsByEmail(pending.getEmail())) {
            throw new ConflictException("이미 가입된 이메일입니다");
        }
        if (users.existsByDisplayName(pending.getDisplayName())) {
            throw new ConflictException("이미 쓰이고 있는 이름입니다. 회원가입을 다시 진행해 주세요");
        }

        User created = users.save(pending.toUser());
        pendings.delete(pending);

        log.info("회원 생성: id={} email={} role={}", created.getId(), created.getEmail(), created.getRole());
        return created.getEmail();
    }

    /**
     * 프론트의 인증 대기 화면이 짧은 주기로 물어본다.
     * 회원이 존재한다는 것 자체가 인증이 끝났다는 뜻이다.
     */
    public boolean isVerified(String rawEmail) {
        return users.existsByEmail(normalizeEmail(rawEmail));
    }

    // ---------- 로그인 ----------

    @Transactional(readOnly = true)
    public LoginResult login(String rawEmail, String rawPassword) {
        User user = users.findByEmail(normalizeEmail(rawEmail))
                .orElseThrow(() -> new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        return new LoginResult(jwtProvider.issue(user), jwtProvider.ttlSeconds(), user);
    }

    // ---------- 정리 ----------

    /**
     * 만료된 대기 건을 치운다. 남겨두면 그 이메일과 닉네임이 영구히 예약된
     * 상태가 되어 정작 본인도 다시 가입할 수 없다.
     */
    @Scheduled(fixedDelay = 10, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    @Transactional
    public void purgeExpiredSignups() {
        long removed = pendings.deleteByExpiresAtBefore(LocalDateTime.now());
        if (removed > 0) {
            log.info("만료된 가입 대기 {}건 정리", removed);
        }
    }

    private static String newToken() {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        return HexFormat.of().formatHex(raw);
    }

    /**
     * 이메일은 소문자로 눕혀 저장한다. DB 콜레이션이 대소문자를 구분하지
     * 않으므로 중복 검사에는 영향이 없지만, 저장 형태를 통일해두면 나중에
     * 콜레이션을 바꾸거나 다른 DB 로 옮겨도 동작이 달라지지 않는다.
     */
    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public record LoginResult(String token, long expiresInSeconds, User user) {
    }
}
