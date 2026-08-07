package com.reviewticket.server.store;

import java.time.Instant;

/** GET /api/stores/me. 사장 본인 화면용이라 ownerId, 생성/수정 시각까지 싣는다. */
public record StoreMeResponse(
        Long storeId,
        Long ownerId,
        String storeName,
        String logoUrl,
        int reviewNumber,
        double reviewValue,
        boolean isReviewing,
        Instant createdAt,
        Instant latestUpdate) {
}
