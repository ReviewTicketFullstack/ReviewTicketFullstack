package com.reviewticket.server.domain;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

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
 * PATCH /api/stores/me/menus/{menuId} 로 사장이 대표 사진·표본 사진·
 * 리뷰이벤트 여부를 고칠 수 있다. 메뉴 이름·가격은 여전히 고정이다 —
 * 가게가 만들어질 때 5종이 함께 들어가고 그 둘은 그 뒤로 안 바뀐다.
 *
 * imageUrl(대표 사진)과 sampleImageUrl1~5(표본 사진)는 용도가 다르다.
 * 대표 사진은 메뉴판·목록 썸네일일 뿐 AI 검증에는 쓰이지 않는다. AI 검증은
 * 표본 사진 중 값이 있는 것만 전부와 대조해 가장 높은 유사도를 쓴다
 * (ReviewService). 표본 5칸은 동등하다 — 순서에 의미를 두지 않는다.
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

    @Column(name = "sample_image_url_1", length = 255)
    private String sampleImageUrl1;

    @Column(name = "sample_image_url_2", length = 255)
    private String sampleImageUrl2;

    @Column(name = "sample_image_url_3", length = 255)
    private String sampleImageUrl3;

    @Column(name = "sample_image_url_4", length = 255)
    private String sampleImageUrl4;

    @Column(name = "sample_image_url_5", length = 255)
    private String sampleImageUrl5;

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

    /**
     * PATCH /api/stores/me/menus/{menuId}. 대표 사진·표본 사진·리뷰이벤트
     * 여부를 한 번에 통째로 덮어쓴다 — StoreService.updateMyMenu 가 부른다.
     *
     * sampleImageUrls 는 길이 5 이하로 검증된 뒤 들어온다(StoreService).
     * 5보다 짧으면 남는 칸은 비운다.
     */
    public void applyEdit(String imageUrl, List<String> sampleImageUrls, boolean reviewEvent) {
        this.imageUrl = imageUrl;
        this.sampleImageUrl1 = sampleAt(sampleImageUrls, 0);
        this.sampleImageUrl2 = sampleAt(sampleImageUrls, 1);
        this.sampleImageUrl3 = sampleAt(sampleImageUrls, 2);
        this.sampleImageUrl4 = sampleAt(sampleImageUrls, 3);
        this.sampleImageUrl5 = sampleAt(sampleImageUrls, 4);
        this.reviewEvent = reviewEvent;
    }

    private static String sampleAt(List<String> urls, int index) {
        return index < urls.size() ? urls.get(index) : null;
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

    /** 표본 사진 5칸. 순서 그대로 돌려주며, 비어 있는 칸은 null 이다. */
    public List<String> getSampleImageUrls() {
        return Arrays.asList(sampleImageUrl1, sampleImageUrl2, sampleImageUrl3, sampleImageUrl4, sampleImageUrl5);
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
