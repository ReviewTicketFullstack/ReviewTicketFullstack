package com.reviewticket.server.review;

import java.time.Instant;

/**
 * GET /api/stores/{storeId}/reviews. 모르는 손님도 볼 수 있는 화면이라
 * userId, orderId, imageSimilarity, compareImageUrl 은 싣지 않는다 —
 * AI 판정 근거는 사장(6.3)에게만 필요하다.
 */
public record ReviewPublicResponse(
        Long reviewId,
        Long menuId,
        String menuName,
        String displayName,
        int reviewRating,
        String reviewContent,
        String reviewImageUrl,
        Instant reviewCreatedAt) {
}
