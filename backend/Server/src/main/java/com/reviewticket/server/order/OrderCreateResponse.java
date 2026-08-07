package com.reviewticket.server.order;

import java.time.Instant;

/**
 * POST /api/orders 응답. GET /api/orders(OrderResponse)와 같은 필드에 tickets 하나가
 * 더 붙는다 — 잠금이 반영된 뒤의 잔여 티켓이다. 목록 조회 때는 안 실어 보낸다,
 * 티켓 잔여 수는 주문·리뷰로 값이 바뀌는 순간에만 필요하지 볼 때마다 필요한 게 아니라서다.
 */
public record OrderCreateResponse(
        Long id,
        Long storeId,
        String storeName,
        Long menuId,
        String menuName,
        int price,
        boolean hasReviewBadge,
        Instant reviewDeadline,
        Instant createdAt,
        String reviewStatus,
        int tickets) {
}
