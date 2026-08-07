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
 * 리뷰 한 건. AI 사진 검증을 통과한 것만 존재한다.
 *
 * 통과하지 못한 시도는 행을 만들지 않는다 — 판정에 쓴 사진은 서버 메모리에만
 * 있다가 통과할 때만 디스크에 기록되고, 그때 이 행도 함께 만들어진다.
 * 그래서 승인/거절을 구분하는 상태 컬럼이 없다. 이 표에 있다는 것 자체가
 * 승인됐다는 뜻이다.
 *
 * store, menu 는 order 를 통해 알 수 있지만 그대로 복사해 둔다 — 가게별
 * 리뷰 목록을 뽑을 때 order 표를 거치지 않기 위해서다.
 *
 * compareImageUrl 은 판정 당시 비교했던 메뉴 표본 사진의 주소를 그대로
 * 복사해 둔 것이다. 사장이 나중에 메뉴 사진을 바꿔도, 이 리뷰가 그때 무엇과
 * 비교되어 통과했는지는 바뀌지 않아야 한다.
 */
@Entity
@Table(name = "customer_review_table")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "review_rating", nullable = false)
    private int rating;

    @Column(name = "review_content", nullable = false, length = 255)
    private String content;

    @Column(name = "review_image_url", nullable = false, length = 255)
    private String imageUrl;

    @Column(name = "review_created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "image_similarity", nullable = false)
    private double imageSimilarity;

    @Column(name = "compare_image_url", nullable = false, length = 255)
    private String compareImageUrl;

    protected Review() {
    }

    public Review(Order order, int rating, String content, String imageUrl,
            double imageSimilarity, String compareImageUrl) {
        this.order = order;
        this.store = order.getStore();
        this.menu = order.getMenu();
        this.user = order.getCustomer();
        this.rating = rating;
        this.content = content;
        this.imageUrl = imageUrl;
        this.imageSimilarity = imageSimilarity;
        this.compareImageUrl = compareImageUrl;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public Store getStore() {
        return store;
    }

    public Menu getMenu() {
        return menu;
    }

    public User getUser() {
        return user;
    }

    public int getRating() {
        return rating;
    }

    public String getContent() {
        return content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public double getImageSimilarity() {
        return imageSimilarity;
    }

    public String getCompareImageUrl() {
        return compareImageUrl;
    }
}
