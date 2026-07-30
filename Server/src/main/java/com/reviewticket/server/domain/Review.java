package com.reviewticket.server.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
 * 승인된 리뷰. 거부된 시도는 여기 오지 않고 {@link AiRejection} 으로 간다.
 *
 * AI 판정 근거를 같이 저장하는 이유 — 거부된 사진은 파일을 남기지 않기 때문에
 * 확률이 유일한 증거다. 나중에 tau 를 다시 잡거나 실제 배달 사진으로 성능을
 * 재측정할 때 이 값이 없으면 사진을 처음부터 다시 모아야 한다.
 */
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 주문했다고 간주한 메뉴. 로그인·주문이 붙으면 orders 에서 온다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expected_food_id", nullable = false)
    private Food expectedFood;

    /** 1~5 정수. DB 의 CHECK 제약이 범위를 지킨다. */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 1000)
    private String content;

    /** 1600px q85 축소본의 경로. 원본은 저장하지 않는다. */
    @Column(name = "image_path", nullable = false, length = 255)
    private String imagePath;

    @Column(name = "image_sha256", nullable = false, length = 64)
    private String imageSha256;

    /** 부호 있는 64비트로 통일한다 (Java long 과 MySQL BIGINT 가 둘 다 signed). */
    @Column(name = "image_phash", nullable = false)
    private long imagePhash;

    @Column(name = "ai_predicted_label", nullable = false, length = 32)
    private String aiPredictedLabel;

    /** 판정에 실제로 쓰는 값. 최대 확률이 아니라 이것으로 음식 여부를 가른다. */
    @Column(name = "ai_p_non_food", nullable = false, precision = 7, scale = 6)
    private BigDecimal aiPNonFood;

    /** 확률 6개 전부. MySQL JSON 컬럼. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_probs", nullable = false)
    private Map<String, Double> aiProbs;

    /** DB 의 DEFAULT CURRENT_TIMESTAMP(3) 가 채운다. */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Review() {
    }

    public Review(Food expectedFood, int rating, String content, String imagePath,
            String imageSha256, long imagePhash, String aiPredictedLabel,
            BigDecimal aiPNonFood, Map<String, Double> aiProbs) {
        this.expectedFood = expectedFood;
        this.rating = rating;
        this.content = content;
        this.imagePath = imagePath;
        this.imageSha256 = imageSha256;
        this.imagePhash = imagePhash;
        this.aiPredictedLabel = aiPredictedLabel;
        this.aiPNonFood = aiPNonFood;
        this.aiProbs = aiProbs;
    }

    public Long getId() {
        return id;
    }

    public Food getExpectedFood() {
        return expectedFood;
    }

    public int getRating() {
        return rating;
    }

    public String getContent() {
        return content;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getImageSha256() {
        return imageSha256;
    }

    public long getImagePhash() {
        return imagePhash;
    }

    public String getAiPredictedLabel() {
        return aiPredictedLabel;
    }

    public BigDecimal getAiPNonFood() {
        return aiPNonFood;
    }

    public Map<String, Double> getAiProbs() {
        return aiProbs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
