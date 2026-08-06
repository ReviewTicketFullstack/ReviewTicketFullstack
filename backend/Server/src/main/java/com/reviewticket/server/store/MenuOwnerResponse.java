package com.reviewticket.server.store;

import java.time.Instant;

/** GET /api/stores/me/menus. 사장 본인 화면용이라 storeId 와 시각까지 함께 싣는다. */
public record MenuOwnerResponse(
        Long storeId,
        Long menuId,
        String menuName,
        int menuPrice,
        String menuImageUrl,
        boolean reviewEvent,
        Instant menuCreatedAt,
        Instant menuLatestUpdate) {
}
