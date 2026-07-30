package com.reviewticket.server.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.reviewticket.server.config.ReviewTicketProperties;
import com.reviewticket.server.domain.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT 발급과 검증.
 *
 * 담는 정보는 세 가지다.
 *   sub  회원 번호
 *   role 역할 (화면 분기와 권한 판단)
 *   tv   토큰 버전 — 비밀번호가 바뀌면 서버의 값이 올라가 이 토큰이 죽는다
 *
 * 이메일이나 닉네임은 담지 않는다. 닉네임은 바뀔 수 있어서 토큰에 박아두면
 * 변경 후에도 옛 이름이 화면에 남는다. 이름은 항상 DB 에서 읽는다.
 */
@Component
public class JwtProvider {

    /** 토큰 버전 클레임 이름. 짧게 쓰는 이유는 토큰 길이를 아끼기 위함이다. */
    static final String CLAIM_TOKEN_VERSION = "tv";
    static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final Duration ttl;

    public JwtProvider(ReviewTicketProperties properties) {
        String secret = properties.auth().jwtSecret();
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        // HS256 은 최소 32바이트 키를 요구한다. 짧으면 라이브러리가 예외를 던지는데,
        // 그 메시지만 보면 원인을 알기 어려워 여기서 미리 잡아 설명한다.
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "reviewticket.auth.jwt-secret 이 너무 짧습니다 (32바이트 이상 필요, 현재 "
                            + bytes.length + "바이트). application-local.yml 에 긴 무작위 문자열을 넣어 주세요.");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.ttl = properties.auth().tokenTtl();
    }

    public String issue(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TOKEN_VERSION, user.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** 서명과 만료만 본다. 토큰 버전 대조는 DB 를 봐야 하므로 필터가 한다. */
    public ParsedToken parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new ParsedToken(
                    Long.parseLong(claims.getSubject()),
                    claims.get(CLAIM_TOKEN_VERSION, Integer.class));
        } catch (JwtException | IllegalArgumentException e) {
            // 위조, 만료, 형식 오류를 구분하지 않는다 — 어느 쪽이든 결과는 401 이고,
            // 어떤 이유로 실패했는지 알려주면 공격자에게 힌트가 된다.
            return null;
        }
    }

    public long ttlSeconds() {
        return ttl.toSeconds();
    }

    public record ParsedToken(long userId, Integer tokenVersion) {
    }
}
