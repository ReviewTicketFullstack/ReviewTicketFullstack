package com.reviewticket.server.order;

import java.time.Instant;

/**
 * 주문내역 카드 한 장.
 *
 * @param menuName       주문 시점 스냅샷. 메뉴 이름이 나중에 바뀌어도 그대로다
 * @param price          주문 시점 스냅샷. 원 단위 정수
 * @param hasReviewBadge 리뷰 버튼을 띄울지 (주문 시점의 review_event)
 * @param reviewStatus   "available" 이면 지금 리뷰를 쓸 수 있다. 그 밖에는
 *                       "not_available". 이미 리뷰를 썼는지 보는 "pending" 은
 *                       리뷰 표가 생길 때 붙인다
 * @param reviewDeadline 리뷰 마감 시각. 화면의 남은 시간 카운트다운에 쓴다
 */
public record OrderResponse(
        Long id,
        Long storeId,
        String storeName,
        String menuName,
        int price,
        boolean hasReviewBadge,
        String reviewStatus,
        Instant reviewDeadline,
        Instant createdAt) {
}
