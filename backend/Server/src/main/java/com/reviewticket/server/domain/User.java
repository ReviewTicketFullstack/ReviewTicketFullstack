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
 * 회원. id 는 가입 순서대로 붙는 내부 번호이며 화면에 노출하지 않는다.
 *
 * displayName 하나에 고객 닉네임과 사장 가게 이름을 함께 담는다. 두 이름이
 * 서로 겹치는 것까지 막기로 했으므로 컬럼 하나에 UNIQUE 를 걸면 끝이고,
 * 중복 검사 API 도 하나로 충분하다. 화면 좌측 상단 표시 이름이 역할과
 * 무관하게 이 필드라는 점도 프론트를 단순하게 만든다.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 190)
    private String email;

    /** BCrypt 해시. 원문 비밀번호는 어디에도 남기지 않는다. */
    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "display_name", nullable = false, length = 32)
    private String displayName;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    /**
     * 발급한 JWT 를 무효화하는 장치. 비밀번호를 바꿀 때 +1 한다.
     * 요청에 실린 토큰의 tv 클레임이 이 값과 다르면 거부한다.
     * 이게 없으면 비밀번호를 바꿔도 탈취된 옛 토큰이 만료까지 살아있다.
     */
    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    /** DB 의 DEFAULT / ON UPDATE 가 채운다. */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "tickets", insertable = false, updatable = false)
    private int tickets;

    

    protected User() {
    }

    public User(String email, String passwordHash, Role role, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.displayName = displayName;
        this.emailVerified = false;
        this.tokenVersion = 0;
    }

    /** 이메일 인증 링크를 눌렀을 때. 이미 인증된 계정에 다시 불려도 무해하다. */
    public void markEmailVerified() {
        this.emailVerified = true;
    }

    public void changeDisplayName(String newName) {
        this.displayName = newName;
    }

    /**
     * 비밀번호 교체. 토큰 버전을 함께 올려 다른 기기의 옛 토큰을 끊는다.
     * 두 동작을 한 메서드에 묶은 이유 — 호출하는 쪽이 버전 올리기를
     * 잊으면 무효화가 조용히 실패한다. 잊을 수 없게 만든다.
     */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.tokenVersion++;
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

    public boolean isEmailVerified() {
        return emailVerified;
    }
    
    public int getTickets() {
        return tickets;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
