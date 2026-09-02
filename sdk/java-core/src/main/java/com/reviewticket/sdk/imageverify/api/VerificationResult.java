package com.reviewticket.sdk.imageverify.api;

import java.util.List;

/**
 * 판정 결과.
 *
 * <p>임계값 미달은 오류가 아니라 {@code matched=false} 인 결과다. 그것을 예외로
 * 바꿀지는 부르는 쪽의 정책이다.
 *
 * @param matched    {@code similarity >= threshold}
 * @param similarity 최고 유사도. 백엔드가 준 값 그대로 — 반올림도 클램프도 하지 않는다.
 *                   부르는 쪽이 사용자에게 "일치율 62%" 처럼 보여줄 수 있어야 한다
 * @param matchedKey 최고 유사도를 낸 기준 이미지의 key. 동점이면 입력 순서상 앞선 것
 * @param threshold  이 판정에 쓰인 임계값. 결과만 보고도 판정을 재현할 수 있게 함께 싣는다
 * @param scores     기준 이미지 전부의 점수. 입력과 같은 순서, 같은 길이
 */
public record VerificationResult(
        boolean matched,
        double similarity,
        String matchedKey,
        double threshold,
        List<Score> scores) {

    public VerificationResult {
        scores = List.copyOf(scores);
    }
}
