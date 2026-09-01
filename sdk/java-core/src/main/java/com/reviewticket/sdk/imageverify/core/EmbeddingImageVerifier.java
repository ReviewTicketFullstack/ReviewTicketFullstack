package com.reviewticket.sdk.imageverify.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.reviewticket.sdk.imageverify.api.ImageVerifier;
import com.reviewticket.sdk.imageverify.api.InferenceUnavailableException;
import com.reviewticket.sdk.imageverify.api.ReferenceImage;
import com.reviewticket.sdk.imageverify.api.Score;
import com.reviewticket.sdk.imageverify.api.VerificationResult;
import com.reviewticket.sdk.imageverify.api.VerifierConfig;
import com.reviewticket.sdk.imageverify.spi.EmbeddingModel;
import com.reviewticket.sdk.imageverify.spi.SimilarityMetric;

/**
 * 임베딩을 받아 와 유사도를 여기서 계산하는 판정 구현.
 *
 * <p>{@link PairwiseImageVerifier} 와 판정 결과는 같아야 하지만 일하는 방식이
 * 다르다. 쌍마다 한 번씩 서버를 부르는 대신, 필요한 이미지를 <b>한 번에</b>
 * 보내 벡터를 받고 비교는 자바에서 한다.
 *
 * <p>표본 5장 기준으로 HTTP 왕복이 5회에서 1회로, 임베딩 계산이 10회에서 6회로
 * 준다(후보 1 + 기준 5). 스레드풀도 필요 없다 — 요청이 하나뿐이라 나눠 쏠 것이 없다.
 *
 * <p>진짜 이득은 Phase 3 에서 나온다. 기준 이미지의 벡터를 캐시해 두면 웜 상태에서
 * 계산이 1회로 줄어든다. 지금은 캐시가 없어 매번 전부 계산한다.
 */
public final class EmbeddingImageVerifier implements ImageVerifier {

    private final EmbeddingModel model;
    private final SimilarityMetric metric;
    private final VerifierConfig config;

    public EmbeddingImageVerifier(EmbeddingModel model, SimilarityMetric metric, VerifierConfig config) {
        this.model = Objects.requireNonNull(model, "model 이 null 입니다");
        this.metric = Objects.requireNonNull(metric, "metric 이 null 입니다");
        this.config = Objects.requireNonNull(config, "config 가 null 입니다");
    }

    @Override
    public VerificationResult verify(List<ReferenceImage> references, byte[] candidate) {
        Verifications.validate(references, candidate, config);

        // 후보를 맨 앞에 두고 기준 이미지를 이어 붙인다. 한 번의 요청으로 끝내려는
        // 것이고, 순서를 고정해 두어야 돌아온 벡터를 다시 짝지을 수 있다.
        List<byte[]> batch = new ArrayList<>(references.size() + 1);
        batch.add(candidate);
        for (ReferenceImage reference : references) {
            batch.add(reference.bytes());
        }

        List<float[]> vectors = model.embed(batch);
        if (vectors == null || vectors.size() != batch.size()) {
            throw new InferenceUnavailableException("임베딩 개수가 요청과 다릅니다: 보낸 "
                    + batch.size() + "장, 받은 " + (vectors == null ? "null" : vectors.size()));
        }

        float[] candidateVector = vectors.get(0);
        List<Score> scores = new ArrayList<>(references.size());
        for (int i = 0; i < references.size(); i++) {
            scores.add(new Score(references.get(i).key(), metric.between(candidateVector, vectors.get(i + 1))));
        }

        return Verifications.resultOf(scores, config);
    }

    @Override
    public void close() {
        // 백엔드가 자원을 들고 있으면(HTTP 연결 등) 같이 반납한다.
        if (model instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                // 종료 중의 실패로 애플리케이션 종료를 막지 않는다.
            }
        }
    }
}
