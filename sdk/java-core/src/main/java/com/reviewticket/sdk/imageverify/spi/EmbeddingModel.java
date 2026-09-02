package com.reviewticket.sdk.imageverify.spi;

import java.util.List;

/**
 * 이미지를 벡터로 바꾸는 백엔드.
 *
 * <p>{@link PairwiseModel} 과 달리 임베딩을 값으로 내준다. 그래서 부르는 쪽이
 * 그것을 보관해 두었다가 재사용할 수 있다 — 기준 이미지는 좀처럼 바뀌지 않으므로
 * 이게 실질적인 성능 차이를 만든다.
 *
 * <p>유사도 계산은 이 인터페이스의 일이 아니다. 벡터만 내주면 어떤 척도로 비교할지는
 * {@link SimilarityMetric} 이 정한다.
 */
public interface EmbeddingModel {

    /**
     * 이 백엔드가 쓰는 모델의 식별자. SDK 는 해석하지 않고 문자열로만 다룬다.
     *
     * <p>캐시 키에 섞이므로 임베딩을 부르기 <b>전에</b> 알 수 있어야 한다.
     * 모델이 바뀌면 이 값이 바뀌고, 그것만으로 캐시가 무효화된다.
     */
    String modelId();

    /**
     * 입력과 같은 순서로, L2 정규화된 벡터를 돌려준다.
     *
     * <p>정규화를 구현체 쪽에 맡기는 것은 의도적이다 — 정규화 지점이 두 곳으로
     * 나뉘면 부동소수점 결과가 미세하게 갈린다.
     *
     * @return {@code images.size()} 와 같은 길이. 각 벡터의 차원은 서로 같아야 한다
     */
    List<float[]> embed(List<byte[]> images);
}
