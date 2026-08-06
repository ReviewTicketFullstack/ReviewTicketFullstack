package com.reviewticket.server.review;

import java.time.Instant;

/** GET /api/stores/me/reviews 목록 항목. 자기 가게 리뷰라 판정 근거(imageSimilarity, compareImageUrl)까지 싣는다. */
public record ReviewOwnerItemResponse(
        Long reviewId,
        Long orderId,
        Long menuId,
        String menuName,
        String displayName,
        int reviewRating,
        String reviewContent,
        String reviewImageUrl,
        Instant reviewCreatedAt,
        double imageSimilarity,
        String compareImageUrl) {
}
