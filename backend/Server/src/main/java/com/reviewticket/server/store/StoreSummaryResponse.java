package com.reviewticket.server.store;

/** 홈 목록의 가게 한 줄. GET /api/stores. */
public record StoreSummaryResponse(
        Long storeId,
        String storeName,
        String logoUrl,
        int reviewNumber,
        double reviewValue,
        boolean isReviewing) {
}
