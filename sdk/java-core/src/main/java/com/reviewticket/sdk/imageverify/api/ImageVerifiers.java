package com.reviewticket.sdk.imageverify.api;

import java.net.URI;
import java.time.Duration;

import com.reviewticket.sdk.imageverify.core.CosineSimilarity;
import com.reviewticket.sdk.imageverify.core.EmbeddingImageVerifier;
import com.reviewticket.sdk.imageverify.core.PairwiseImageVerifier;
import com.reviewticket.sdk.imageverify.http.HttpEmbeddingModel;
import com.reviewticket.sdk.imageverify.http.HttpPairwiseModel;
import com.reviewticket.sdk.imageverify.spi.EmbeddingModel;
import com.reviewticket.sdk.imageverify.spi.PairwiseModel;
import com.reviewticket.sdk.imageverify.spi.SimilarityMetric;

/**
 * {@link ImageVerifier} 를 조립하는 곳.
 *
 * <p>이 클래스가 있는 이유 — 소비자가 {@code core}·{@code http} 패키지를 직접
 * 건드리지 않고도 SDK 를 쓸 수 있어야 한다. 그 두 패키지는 내부 구현이라
 * 예고 없이 바뀔 수 있고, 애플리케이션이 거기 의존하면 가드레일(GR-05)이
 * 실패한다. 조립은 여기를 통한다.
 *
 * <p>Spring 없이도 이 한 줄이면 동작한다. Starter 는 편의 계층일 뿐 요구사항이
 * 아니라는 것을 실제로 보증하는 자리다(ARCH-R1).
 */
public final class ImageVerifiers {

    private ImageVerifiers() {
    }

    /**
     * 이미지 두 장을 올려 유사도를 받는 추론 서버에 붙는다.
     *
     * <p>돌려받은 객체는 스레드풀과 HTTP 연결을 들고 있다. 다 쓰면
     * {@link ImageVerifier#close()} 를 부른다 — Spring 빈으로 등록하면
     * 컨테이너가 알아서 부른다.
     *
     * @param similarityEndpoint 유사도 엔드포인트의 전체 주소
     * @param timeout            연결과 응답 각각에 거는 제한 시간
     */
    public static ImageVerifier pairwiseOverHttp(URI similarityEndpoint, Duration timeout,
            VerifierConfig config) {
        return new PairwiseImageVerifier(new HttpPairwiseModel(similarityEndpoint, timeout), config);
    }

    /**
     * 임베딩을 받아 와 유사도를 SDK 안에서 계산하는 추론 서버에 붙는다.
     *
     * <p>{@link #pairwiseOverHttp} 와 판정 결과는 같아야 하지만 왕복 횟수가 다르다.
     * 필요한 이미지를 한 번에 보내고 비교는 자바에서 한다.
     *
     * @param baseUrl   추론 서버의 기준 주소 (예: {@code http://localhost:8000})
     * @param embedPath 임베딩 엔드포인트 경로 (예: {@code /embed})
     */
    public static ImageVerifier embeddingOverHttp(URI baseUrl, String embedPath, Duration timeout,
            VerifierConfig config) {
        return new EmbeddingImageVerifier(
                new HttpEmbeddingModel(baseUrl, embedPath, timeout), new CosineSimilarity(), config);
    }

    /**
     * 직접 만든 백엔드로 조립한다. 다른 모델로 갈아끼우거나, 테스트에서 네트워크
     * 없이 전체 경로를 돌릴 때 쓴다.
     */
    public static ImageVerifier using(PairwiseModel model, VerifierConfig config) {
        return new PairwiseImageVerifier(model, config);
    }

    /** 직접 만든 임베딩 백엔드로 조립한다. 척도는 코사인을 쓴다. */
    public static ImageVerifier using(EmbeddingModel model, VerifierConfig config) {
        return new EmbeddingImageVerifier(model, new CosineSimilarity(), config);
    }

    /** 임베딩 백엔드와 척도를 모두 직접 정해 조립한다. */
    public static ImageVerifier using(EmbeddingModel model, SimilarityMetric metric,
            VerifierConfig config) {
        return new EmbeddingImageVerifier(model, metric, config);
    }
}
