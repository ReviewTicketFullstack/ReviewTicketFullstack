package com.reviewticket.server.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 거부된 시도 로그. 이미지 파일은 저장하지 않는다 (거부는 메모리에서 버린다).
 * 남는 건 해시와 확률뿐이므로 이게 유일한 사후 분석 근거다.
 */
@Entity
@Table(name = "ai_rejections")
public class AiRejection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expected_food_id", nullable = false)
    private Food expectedFood;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RejectionReason reason;

    /** duplicate 로 걸리면 AI 를 부르지 않으므로 null 이다. */
    @Column(name = "ai_predicted_label", length = 32)
    private String aiPredictedLabel;

    @Column(name = "ai_p_non_food", precision = 7, scale = 6)
    private BigDecimal aiPNonFood;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_probs")
    private Map<String, Double> aiProbs;

    @Column(name = "image_sha256", nullable = false, length = 64)
    private String imageSha256;

    @Column(name = "image_phash", nullable = false)
    private long imagePhash;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AiRejection() {
    }

    public AiRejection(Food expectedFood, RejectionReason reason, String aiPredictedLabel,
            BigDecimal aiPNonFood, Map<String, Double> aiProbs,
            String imageSha256, long imagePhash) {
        this.expectedFood = expectedFood;
        this.reason = reason;
        this.aiPredictedLabel = aiPredictedLabel;
        this.aiPNonFood = aiPNonFood;
        this.aiProbs = aiProbs;
        this.imageSha256 = imageSha256;
        this.imagePhash = imagePhash;
    }

    public Long getId() {
        return id;
    }

    public Food getExpectedFood() {
        return expectedFood;
    }

    public RejectionReason getReason() {
        return reason;
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

    public String getImageSha256() {
        return imageSha256;
    }

    public long getImagePhash() {
        return imagePhash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
