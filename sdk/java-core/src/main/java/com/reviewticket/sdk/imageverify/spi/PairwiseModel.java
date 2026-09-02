package com.reviewticket.sdk.imageverify.spi;

/**
 * 이미지 두 장을 받아 유사도 하나를 돌려주는 백엔드.
 *
 * <p>임베딩을 밖으로 내주지 않으므로 캐시를 걸 수 없다 — 판정마다 두 장을 모두
 * 다시 계산한다. Phase 2 에서 임베딩을 값으로 꺼내는 백엔드가 생기면 그쪽이
 * 기본이 되고, 이 인터페이스는 호환·롤백 경로로 남는다.
 *
 * <p>직접 구현해 넘기면 HTTP 도 파이썬도 없이 SDK 전체 경로를 돌릴 수 있다.
 * 그게 모델 교체 가능성의 실질적 증명이다(ARCH-R4).
 */
public interface PairwiseModel {

    /**
     * 이 백엔드가 쓰는 모델의 식별자.
     *
     * <p>SDK 는 이 문자열을 해석하지 않는다. Phase 3 부터 캐시 키에 섞여, 모델이
     * 바뀌면 캐시가 저절로 무효화되는 용도로 쓰인다.
     */
    String modelId();

    /**
     * @param candidate 후보 이미지 (검증받는 쪽)
     * @param reference 기준 이미지 (대조 대상)
     * @return 유사도. 보통 0~1 이지만 SDK 는 범위를 강제하지 않는다
     */
    double similarity(byte[] candidate, byte[] reference);
}
