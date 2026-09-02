package com.reviewticket.sdk.imageverify.spi;

/**
 * 벡터 두 개가 얼마나 닮았는지 재는 방법. 기본 구현은 코사인이다.
 *
 * <p>바꿔 끼울 수 있게 열어 둔 이유 — 임베딩 모델이 바뀌면 어울리는 척도도
 * 달라질 수 있다. 유클리드 거리나 내적이 더 맞는 모델이 있다.
 */
@FunctionalInterface
public interface SimilarityMetric {

    /**
     * @return 클수록 닮은 것. 범위는 척도마다 다르므로 SDK 가 강제하지 않는다
     * @throws IllegalArgumentException 두 벡터의 길이가 다른 경우
     */
    double between(float[] a, float[] b);
}
