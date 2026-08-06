package com.reviewticket.server.review;

import java.time.Instant;

/**
 * GET /api/stores/me/orders/pending 목록 항목. reviewStatus 는 "pending"(기한 남음)
 * 또는 "expired"(기한 지남) 둘 중 하나다 — 사장 화면이 두 숫자를 같이 보여줘서
 * 엔드포인트를 나누지 않고 한 목록에 같이 싣는다.
 */
public record PendingOrderResponse(
        Long orderId,
        String displayName,
        Long menuId,
        String menuName,
        int menuPrice,
        Instant orderedAt,
        Instant expireTime,
        String reviewStatus) {
}
