package com.reviewticket.server.review;

import java.time.Instant;

/** POST /api/reviews 성공 응답. tickets 는 반환이 반영된 뒤의 잔여 티켓이다. */
public record ReviewCreateResponse(
        Long reviewId,
        Long orderId,
        Long storeId,
        Long menuId,
        Long userId,
        int reviewRating,
        String reviewContent,
        String reviewImageUrl,
        Instant reviewCreatedAt,
        double imageSimilarity,
        String compareImageUrl,
        int tickets) {
}
