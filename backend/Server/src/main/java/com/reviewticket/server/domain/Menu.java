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
 * 가격은 원 단위 정수다. "18,000원" 같은 문자열로 두면 계산에 쓸 수 없고,
 * 표시 형식은 화면마다 다를 수 있으므로 포맷은 프론트가 맡는다.
 */
@Entity
@Table(name = "menus")
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** 이 메뉴를 주문하면 리뷰를 쓸 수 있는지. */
    @Column(name = "review_event", nullable = false)
    private boolean reviewEvent;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected Menu() {
    }

    public Menu(Store store, String name, int price, boolean reviewEvent) {
        this.store = store;
        this.name = name;
        this.price = price;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
