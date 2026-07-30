package com.reviewticket.server.account;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.reviewticket.server.auth.ConflictException;
import com.reviewticket.server.auth.JwtProvider;
import com.reviewticket.server.auth.PasswordPolicy;
import com.reviewticket.server.auth.UnauthorizedException;
import com.reviewticket.server.domain.Role;
import com.reviewticket.server.domain.User;
import com.reviewticket.server.repository.UserRepository;

@Service
public class AccountService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AccountService(UserRepository users, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    /**
     * 닉네임(사장이면 가게 이름) 변경.
     *
     * 필터가 넘겨준 User 는 이미 영속 상태가 끝난 객체일 수 있으므로
     * 여기서 id 로 다시 읽는다. 그래야 변경이 실제로 반영된다.
     */
    @Transactional
    public String changeDisplayName(long userId, String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        User user = load(userId);

        if (name.isEmpty()) {
            throw new IllegalArgumentException(
                    user.getRole() == Role.OWNER ? "가게 이름을 입력해 주세요" : "닉네임을 입력해 주세요");
        }
        if (name.length() > 32) {
            throw new IllegalArgumentException("이름은 32자 이하여야 합니다");
        }
        // 지금 쓰는 이름을 그대로 다시 넣은 경우. 중복이라고 막으면 이상하다.
        if (name.equals(user.getDisplayName())) {
            return name;
        }
        if (users.existsByDisplayName(name)) {
            throw new ConflictException(
                    user.getRole() == Role.OWNER ? "이미 쓰이고 있는 가게 이름입니다" : "이미 쓰이고 있는 닉네임입니다");
        }

        user.changeDisplayName(name);
        return name;
    }

    /**
     * 비밀번호 변경. 기존 비밀번호를 먼저 확인한다 — 이게 없으면 토큰이
     * 탈취된 순간 계정이 통째로 넘어간다.
     *
     * @return 새로 발급한 토큰. 토큰 버전이 올라가 방금까지 쓴 토큰은 죽으므로,
     *         이걸 돌려주지 않으면 본인이 로그아웃된다.
     */
    @Transactional
    public String changePassword(long userId, String currentPassword, String newPassword, String newPasswordConfirm) {
        User user = load(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("기존 비밀번호가 올바르지 않습니다");
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new IllegalArgumentException("새 비밀번호가 서로 다릅니다");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("기존과 다른 비밀번호를 입력해 주세요");
        }
        PasswordPolicy.require(newPassword);

        user.changePassword(passwordEncoder.encode(newPassword));
        // 토큰 버전이 이미 올라간 상태의 user 로 발급해야 새 토큰이 유효하다.
        return jwtProvider.issue(user);
    }

    private User load(long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("로그인이 필요합니다"));
    }
}
