package com.reviewticket.server.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 인증 대기 중인 가입 요청.
 *
 * 이메일 인증이 끝나기 전에는 {@link User} 를 만들지 않는다. 인증하지 않은
 * 사람이 남의 이메일과 닉네임을 선점해 막아버리는 것을 방지하기 위함이다.
 * 링크를 눌러 인증이 확인되면 이 내용으로 User 를 만들고 이 행은 지운다.
 *
 * 비밀번호는 여기 들어올 때 이미 해싱된 상태다. 대기 표라고 해서 원문을
 * 두면, 인증되지 않은 요청이 쌓인 표가 그대로 비밀번호 목록이 된다.
 */
@Entity
@Table(name = "pending_signups")
public class PendingSignup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 190)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "display_name", nullable = false, length = 32)
    private String displayName;

    @Column(nullable = false, length = 64)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PendingSignup() {
    }

    public PendingSignup(String email, String passwordHash, Role role, String displayName,
            String token, LocalDateTime expiresAt) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.displayName = displayName;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    /** 재발송할 때 새 토큰으로 갈아치운다. 옛 링크는 그 순간 무효가 된다. */
    public void renew(String newToken, LocalDateTime newExpiresAt) {
        this.token = newToken;
        this.expiresAt = newExpiresAt;
    }

    /** 인증이 끝나 실제 회원으로 옮길 때. */
    public User toUser() {
        User user = new User(email, passwordHash, role, displayName);
        user.markEmailVerified();
        return user;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
