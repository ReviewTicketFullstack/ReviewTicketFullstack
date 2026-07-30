package com.reviewticket.server.review;

import java.util.Map;

import com.reviewticket.server.ai.Decision;

/**
 * 승인이든 거부든 같은 형태로 내려준다. 거부는 정상적인 업무 결과이지
 * 요청이 잘못된 것이 아니므로 HTTP 는 200 이다.
 *
 * 거부되어도 프론트는 입력한 별점과 텍스트를 유지해야 한다 (FE-4.6 / FE-4.8).
 *
 * @param probs 확률 6개. 개발·시연 중 판정 근거를 눈으로 보기 위해 같이 내린다.
 */
public record ReviewSubmitResponse(
        boolean approved,
        String reason,
        String message,
        String predicted,
        double pNonFood,
        Long reviewId,
        Map<String, Double> probs) {

    public static ReviewSubmitResponse from(Decision decision, Long reviewId, Map<String, Double> probs) {
        return new ReviewSubmitResponse(
                decision.approved(),
                decision.reason() == null ? null : decision.reason().name(),
                decision.message(),
                decision.predicted(),
                decision.pNonFood(),
                reviewId,
                probs);
    }
}
