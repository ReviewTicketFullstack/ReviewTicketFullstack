package com.reviewticket.server.order;

import java.time.Instant;

/**
 * 주문내역 카드 한 장. GET /api/orders.
 *
 * @param menuName          주문 시점 스냅샷
 * @param menuPrice         주문 시점 스냅샷. 원 단위 정수
 * @param reviewEventApply  고객이 리뷰이벤트에 실제로 참여 신청했는지
 * @param reviewDeadline    리뷰 작성 가능 기간(초). apply=false 면 null — 이
 *                          주문에 적용된 정책이 무엇이었는지를 남기는 자리일 뿐,
 *                          실제로 적용됐다는 뜻은 아니다
 * @param expireTime        카운트다운의 기준 시각. apply=false 면 null
 * @param reviewStatus      notApplied / available / expired / done 중 하나.
 *                          표에 저장하지 않고 조회 시점에 계산한다
 */
public record OrderResponse(
        Long orderId,
        Long storeId,
        String storeName,
        Long menuId,
        String menuName,
        int menuPrice,
        boolean reviewEventApply,
        Integer reviewDeadline,
        Instant orderedAt,
        Instant expireTime,
        String reviewStatus) {
}
