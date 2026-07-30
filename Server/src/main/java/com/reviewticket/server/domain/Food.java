package com.reviewticket.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 고정 카탈로그 5건. 점주가 늘리거나 가격을 바꿀 수 없다.
 *
 * name 은 AI 모델이 돌려주는 라벨 문자열과 정확히 같아야 한다
 * (pizza / hamburger / chicken_wings / bibimbap / ramen).
 * 여기가 어긋나면 모든 리뷰가 '메뉴 불일치'로 거부된다.
 */
@Entity
@Table(name = "foods")
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32, unique = true)
    private String name;

    @Column(name = "name_ko", nullable = false, length = 32)
    private String nameKo;

    @Column(nullable = false)
    private int price;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    protected Food() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNameKo() {
        return nameKo;
    }

    public int getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
