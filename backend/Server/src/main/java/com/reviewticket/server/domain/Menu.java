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
 * 가게의 메뉴 한 줄.
 *
 * 프로토타입 단계에서는 수정 API 가 없다 — 가게가 만들어질 때 5종이 함께
 * 들어가고 그 뒤로 고정이다. 확장 때 바로 쓸 수 있도록 표는 미리 갖춰 둔다.
 *
 * imageUrl 은 화면 썸네일이자 AI 검증의 비교 기준 사진(compare image)이다.
 */
@Entity
@Table(name = "menu_table")
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "menu_name", nullable = false, length = 32)
    private String name;

    @Column(name = "menu_price", nullable = false)
    private int price;

    @Column(name = "menu_image_url", length = 255)
    private String imageUrl;

    @Column(name = "review_event", nullable = false)
    private boolean reviewEvent;

    @Column(name = "menu_created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "menu_latest_update", insertable = false, updatable = false)
    private LocalDateTime latestUpdate;

    protected Menu() {
    }

    public Menu(Store store, String name, int price, String imageUrl, boolean reviewEvent) {
        this.store = store;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.reviewEvent = reviewEvent;
    }

    public Long getId() {
        return id;
    }

    public Store getStore() {
        return store;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isReviewEvent() {
        return reviewEvent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLatestUpdate() {
        return latestUpdate;
    }
}
