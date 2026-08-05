package com.reviewticket.server.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 가게 한 곳. 사장 회원가입이 확정될 때 함께 만들어진다.
 *
 * 이름의 정답은 여기다 — users.display_name 은 계정 표시명이고, 가게 이름은
 * 나중에 따로 바뀔 수 있어야 한다.
 */
@Entity
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 32)
    private String name;

    /** 없으면 프론트가 회색 박스로 대체한다. */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** DB 의 DEFAULT / ON UPDATE 가 채운다. */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected Store() {
    }

    public Store(User owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    public void changeName(String newName) {
        this.name = newName;
    }

    public void changeImageUrl(String newImageUrl) {
        this.imageUrl = newImageUrl;
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
