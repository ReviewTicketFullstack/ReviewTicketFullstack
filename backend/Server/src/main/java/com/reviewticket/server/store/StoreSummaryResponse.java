package com.reviewticket.server.store;

/** 홈 목록의 가게 한 줄. GET /api/stores. */
public record StoreSummaryResponse(
        Long id,
        String name,
        String imageUrl,
        int reviewCount,
        double rating,
        boolean hasReviewEvent) {
}
