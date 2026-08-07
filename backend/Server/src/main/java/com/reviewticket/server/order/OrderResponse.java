package com.reviewticket.server.order;

import java.time.Instant;

/**
 * 주문내역 카드 한 장. GET /api/orders.
 *
 * @param menuName       주문 시점 스냅샷
 * @param price          주문 시점 스냅샷. 원 단위 정수
 * @param hasReviewBadge 고객이 리뷰이벤트에 실제로 참여 신청했는지
 * @param reviewDeadline 리뷰 작성 마감 시각. 화면의 카운트다운 기준이다.
 *                       참여하지 않은 주문(hasReviewBadge=false)은 null
 * @param reviewStatus   not_available / available / expired / done 중 하나.
 *                       표에 저장하지 않고 조회 시점에 계산한다
 */
public record OrderResponse(
        Long id,
        Long storeId,
        String storeName,
        Long menuId,
        String menuName,
        int price,
        boolean hasReviewBadge,
        Instant reviewDeadline,
        Instant createdAt,
        String reviewStatus) {
}
