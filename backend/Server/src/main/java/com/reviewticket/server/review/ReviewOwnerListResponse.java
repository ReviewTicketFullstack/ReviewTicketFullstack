package com.reviewticket.server.review;

import java.util.List;

/**
 * GET /api/stores/me/reviews 응답. reviewNumber, reviewValue 를 목록과 함께
 * 내리는 이유 — 나중에 페이지네이션이 붙으면 프론트가 배열을 훑어 직접 평균을
 * 낼 경우 화면에 보이는 몇 건만 가지고 계산해 값이 틀어진다. store_table 에
 * 이미 있는 값을 그대로 싣는다.
 */
public record ReviewOwnerListResponse(
        int reviewNumber,
        double reviewValue,
        List<ReviewOwnerItemResponse> reviews) {
}
