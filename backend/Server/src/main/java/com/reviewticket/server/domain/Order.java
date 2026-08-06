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
 * 주문 한 건. 주문 1건 = 메뉴 1개다 (장바구니와 수량 개념이 없다).
 *
 * menuName, price, reviewEvent 는 menu 를 가리키는 대신 주문 시점의 값을
 * 복사해 둔다. 매번 조인하면 사장이 가격을 올릴 때 과거 주문 금액까지 따라
 * 바뀌고, 메뉴가 지워지면 이름이 사라진다.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Column(name = "menu_name", nullable = false, length = 50)
    private String menuName;

    @Column(nullable = false)
    private int price;

    @Column(name = "review_event", nullable = false)
    private boolean reviewEvent;

    @Column(name = "review_deadline", nullable = false)
    private LocalDateTime reviewDeadline;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Order() {
    }

    /** 스냅샷 세 값은 넘겨받은 메뉴에서 그 자리에서 복사한다. */
    public Order(User user, Menu menu, LocalDateTime reviewDeadline) {
        this.user = user;
        this.store = menu.getStore();
        this.menu = menu;
        this.menuName = menu.getName();
        this.price = menu.getPrice();
        this.reviewEvent = menu.isReviewEvent();
        this.reviewDeadline = reviewDeadline;
    }

    /**
     * 지금 리뷰를 쓸 수 있는 주문인지. 리뷰 대상이면서 마감 전이어야 한다.
     *
     * "이미 리뷰를 썼는지"는 보지 않는다 — 리뷰 표가 아직 없다. 그 판정은
     * 리뷰 기능이 붙을 때 pending 상태와 함께 추가한다.
     */
    public boolean isReviewable(LocalDateTime now) {
        return reviewEvent && reviewDeadline.isAfter(now);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Store getStore() {
        return store;
    }

    public Menu getMenu() {
        return menu;
    }

    public String getMenuName() {
        return menuName;
    }

    public int getPrice() {
        return price;
    }

    public boolean isReviewEvent() {
        return reviewEvent;
    }

    public LocalDateTime getReviewDeadline() {
        return reviewDeadline;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
