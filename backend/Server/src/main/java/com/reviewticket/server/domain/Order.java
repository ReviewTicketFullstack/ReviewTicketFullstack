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
 * menuName, price 는 menu 를 가리키는 대신 주문 시점의 값을 복사해 둔다.
 * 매번 조인하면 사장이 가격을 올릴 때 과거 주문 금액까지 따라 바뀌고,
 * 메뉴가 지워지면 이름이 사라진다.
 *
 * reviewDeadline(기간, 초)과 expireTime(그 주문의 실제 마감 시각)을 나눈
 * 이유 — 앞의 것은 "이 주문에 적용된 정책"이고 뒤의 것은 "이 주문이 끝나는
 * 순간"이다. 화면의 카운트다운은 expireTime 만 보면 된다. 이벤트에
 * 참여하지 않은 주문(reviewEventApply=false)은 둘 다 채우지 않는다 —
 * 마감이라는 개념 자체가 없기 때문이다.
 *
 * 이 주문에 리뷰가 달렸는지는 여기서 판단하지 않는다. Review 표에 이
 * order 를 가리키는 행이 있는지로 판단하며, 그 조회는 서비스가 한다.
 */
@Entity
@Table(name = "customer_order_table")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Column(name = "menu_name", nullable = false, length = 32)
    private String menuName;

    @Column(name = "menu_price", nullable = false)
    private int price;

    @Column(name = "review_event_apply", nullable = false)
    private boolean reviewEventApply;

    /** 초 단위. apply=false 면 NULL. */
    @Column(name = "review_deadline")
    private Integer reviewDeadline;

    /**
     * DB 에 DEFAULT CURRENT_TIMESTAMP 가 있지만 값은 애플리케이션이 직접 채운다.
     *
     * 이유가 둘이다. 하나는 expireTime 을 이 값 기준으로 계산해야 하기 때문이다 —
     * DB 가 채우게 두면 주문 시각은 DB 시계, 마감 시각은 앱 시계가 되어 둘이
     * 어긋난다(실측으로 59.992초짜리 "60초" 마감이 나왔다). 다른 하나는 INSERT
     * 후 이 값을 다시 읽어오지 않으면 방금 만든 주문의 응답에서 null 로 나가고,
     * 프론트가 그 사본을 저장했다가 new Date(null) 로 1970년을 표시하기 때문이다.
     */
    @Column(name = "ordered_at", updatable = false)
    private LocalDateTime orderedAt;

    /** ordered_at + reviewDeadline. apply=false 면 NULL. */
    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    protected Order() {
    }

    /**
     * 스냅샷 값은 넘겨받은 메뉴에서 그 자리에서 복사한다.
     *
     * 마감 시각은 밖에서 받지 않고 여기서 주문 시각으로부터 직접 계산한다 —
     * 두 값이 반드시 같은 시계에서 나오도록 한곳에 묶어 둔다.
     */
    public Order(User customer, Menu menu, boolean reviewEventApply, Integer reviewDeadlineSeconds) {
        this.customer = customer;
        this.store = menu.getStore();
        this.menu = menu;
        this.menuName = menu.getName();
        this.price = menu.getPrice();
        this.reviewEventApply = reviewEventApply;
        this.orderedAt = LocalDateTime.now();
        this.reviewDeadline = reviewEventApply ? reviewDeadlineSeconds : null;
        this.expireTime = reviewEventApply ? this.orderedAt.plusSeconds(reviewDeadlineSeconds) : null;
    }

    public boolean isExpired(LocalDateTime now) {
        return expireTime != null && expireTime.isBefore(now);
    }

    public Long getId() {
        return id;
    }

    public User getCustomer() {
        return customer;
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

    public boolean isReviewEventApply() {
        return reviewEventApply;
    }

    public Integer getReviewDeadline() {
        return reviewDeadline;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }
}
