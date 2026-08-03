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
 * 로그인·가입·비밀번호변경·닉네임변경 시도 한 건. 성공/실패 상관없이 남긴다.
 *
 * 저장은 AuthAttemptLogger 가 별도 트랜잭션(REQUIRES_NEW)으로 처리한다 —
 * 시도한 작업이 실패해서 그쪽 트랜잭션이 롤백돼도 "시도했다"는 사실은
 * 남아야 하기 때문이다.
 */
@Entity
@Table(name = "auth_attempt_logs")
public class AuthAttemptLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthAction action;

    @Column(length = 190)
    private String email;

    @Column(name = "display_name", length = 32)
    private String displayName;

    @Column(nullable = false, length = 45)
    private String ip;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AuthAttemptLog() {
    }

    public AuthAttemptLog(AuthAction action, String email, String displayName, String ip, boolean success) {
        this.action = action;
        this.email = email;
        this.displayName = displayName;
        this.ip = ip;
        this.success = success;
    }

    public Long getId() {
        return id;
    }

    public AuthAction getAction() {
        return action;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIp() {
        return ip;
    }

    public boolean isSuccess() {
        return success;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
