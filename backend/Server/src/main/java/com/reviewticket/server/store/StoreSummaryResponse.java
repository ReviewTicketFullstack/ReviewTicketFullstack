package com.reviewticket.server.store;

/**
 * 홈 목록의 가게 한 줄.
 *
 * @param rating         리뷰 평균 별점. 리뷰 표가 아직 없어 0 으로 나간다
 * @param reviewCount    리뷰 개수. 같은 이유로 0
 * @param hasReviewEvent 리뷰 대상 메뉴가 하나라도 있으면 true (가게 이름 옆 배지)
 */
public record StoreSummaryResponse(
        Long id,
        String name,
        String imageUrl,
        double rating,
        int reviewCount,
        boolean hasReviewEvent) {
}
