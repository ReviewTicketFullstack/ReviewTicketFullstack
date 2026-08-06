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
 * 나중에 따로 바뀔 수 있어야 한다. 둘은 PATCH /api/stores/me 를 거칠 때만
 * 함께 바뀐다(StoreService). PATCH /api/me/name 은 사장 계정을 거절하므로
 * 반대 방향으로 어긋날 일이 없다.
 *
 * reviewNumber, reviewValue 는 리뷰가 저장될 때마다 갱신해 두는 캐시값이다.
 * 매번 세면 홈 목록에서 가게마다 리뷰를 훑어야 해 목록 길이만큼 쿼리가 늘어난다.
 */
@Entity
@Table(name = "store_table")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "store_name", nullable = false, length = 32)
    private String name;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(name = "review_number", nullable = false)
    private int reviewNumber;

    @Column(name = "review_value", nullable = false)
    private double reviewValue;

    /** 리뷰이벤트 대상 메뉴가 하나라도 있는지. 가게 생성 시 한 번만 계산한다(메뉴 수정 API가 없어 그 뒤로 안 바뀐다). */
    @Column(name = "is_reviewing", nullable = false)
    private boolean reviewing;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 이름/로고가 마지막으로 바뀐 시각. DB에 ON UPDATE 를 걸지 않고 이 필드를
     * changeInfo() 에서 직접 쓴다 — DB의 ON UPDATE CURRENT_TIMESTAMP 는 어느
     * 컬럼이 바뀌었는지 가리지 않아, 걸어 두면 리뷰 통계 갱신(recordReview)에도
     * 같이 움직여 버린다.
     */
    @Column(name = "latest_update", insertable = false)
    private LocalDateTime latestUpdate;

    protected Store() {
    }

    public Store(User owner, String name) {
        this.owner = owner;
        this.name = name;
        this.reviewNumber = 0;
        this.reviewValue = 0.0;
        this.reviewing = false;
    }

    /** 가게 생성 시 시드 메뉴를 넣은 직후 한 번 부른다. */
    public void markReviewing(boolean reviewing) {
        this.reviewing = reviewing;
    }

    /** PATCH /api/stores/me. 이름과 로고를 통째로 덮어쓴다. */
    public void changeInfo(String newName, String newLogoUrl) {
        this.name = newName;
        this.logoUrl = newLogoUrl;
        this.latestUpdate = LocalDateTime.now();
    }

    /**
     * 리뷰가 새로 저장될 때 부른다. 누적 평균을 다시 계산해 소수 첫째 자리까지 반올림한다.
     */
    public void recordReview(int rating) {
        double total = reviewValue * reviewNumber + rating;
        reviewNumber += 1;
        reviewValue = Math.round((total / reviewNumber) * 10) / 10.0;
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

    public String getLogoUrl() {
        return logoUrl;
    }

    public int getReviewNumber() {
        return reviewNumber;
    }

    public double getReviewValue() {
        return reviewValue;
    }

    public boolean isReviewing() {
        return reviewing;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLatestUpdate() {
        return latestUpdate;
    }
}
