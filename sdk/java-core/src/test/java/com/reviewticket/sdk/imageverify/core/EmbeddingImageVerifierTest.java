package com.reviewticket.sdk.imageverify.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.reviewticket.sdk.imageverify.api.ImageVerifier;
import com.reviewticket.sdk.imageverify.api.ImageVerifiers;
import com.reviewticket.sdk.imageverify.api.InferenceUnavailableException;
import com.reviewticket.sdk.imageverify.api.ReferenceImage;
import com.reviewticket.sdk.imageverify.api.Score;
import com.reviewticket.sdk.imageverify.api.VerificationResult;
import com.reviewticket.sdk.imageverify.api.VerifierConfig;
import com.reviewticket.sdk.imageverify.spi.EmbeddingModel;

/**
 * 임베딩 백엔드의 판정 로직. 네트워크 없이 가짜 모델로 돈다.
 *
 * <p>쌍 단위 백엔드와 <b>같은 계약</b>을 지켜야 하므로, P1 에서 확인한 항목들을
 * 이쪽에서도 다시 확인한다. 백엔드를 바꿨을 때 판정이 달라지지 않는 것이
 * Phase 2 의 전부다.
 */
class EmbeddingImageVerifierTest {

    private static final byte[] CANDIDATE = "candidate".getBytes();

    /** 이미지 바이트를 정해진 벡터로 바꿔 주는 가짜 모델. */
    private static final class FakeEmbeddingModel implements EmbeddingModel {
        private final java.util.Map<String, float[]> vectors = new java.util.HashMap<>();
        private final AtomicInteger calls = new AtomicInteger();
        private final List<Integer> batchSizes = new ArrayList<>();
        private String modelId = "fake-embed";

        FakeEmbeddingModel put(byte[] image, float... vector) {
            vectors.put(new String(image), vector);
            return this;
        }

        @Override
        public String modelId() {
            return modelId;
        }

        @Override
        public List<float[]> embed(List<byte[]> images) {
            calls.incrementAndGet();
            batchSizes.add(images.size());
            List<float[]> result = new ArrayList<>(images.size());
            for (byte[] image : images) {
                float[] vector = vectors.get(new String(image));
                if (vector == null) {
                    throw new IllegalStateException("모르는 이미지입니다: " + new String(image));
                }
                result.add(vector);
            }
            return result;
        }
    }

    private static List<ReferenceImage> refs(String... keys) {
        List<ReferenceImage> list = new ArrayList<>();
        for (String key : keys) {
            list.add(ReferenceImage.ofBytes(key, ("bytes-of-" + key).getBytes()));
        }
        return list;
    }

    private static ImageVerifier verifier(EmbeddingModel model, double threshold) {
        return ImageVerifiers.using(model, VerifierConfig.withThreshold(threshold));
    }

    @Test
    @DisplayName("AC-01/02/03 임계값 판정이 쌍 단위 백엔드와 같다")
    void thresholdBehavesIdentically() throws Exception {
        // 후보와 정확히 같은 방향 → 유사도 1.0, 직교 → 0.0
        FakeEmbeddingModel model = new FakeEmbeddingModel()
                .put(CANDIDATE, 1f, 0f)
                .put("bytes-of-same".getBytes(), 1f, 0f)
                .put("bytes-of-orthogonal".getBytes(), 0f, 1f);

        try (ImageVerifier verifier = verifier(model, 0.80)) {
            VerificationResult matched = verifier.verify(refs("same"), CANDIDATE);
            VerificationResult missed = verifier.verify(refs("orthogonal"), CANDIDATE);

            assertAll(
                    () -> assertTrue(matched.matched()),
                    () -> assertEquals(1.0, matched.similarity(), 1e-12),
                    () -> assertFalse(missed.matched()),
                    () -> assertEquals(0.0, missed.similarity(), 1e-12));
        }
    }

    @Test
    @DisplayName("AC-10/11/13 최댓값·key·순서가 보존된다")
    void picksBestAndPreservesOrder() throws Exception {
        FakeEmbeddingModel model = new FakeEmbeddingModel()
                .put(CANDIDATE, 1f, 0f)
                .put("bytes-of-a".getBytes(), 0.6f, 0.8f)     // cos = 0.6
                .put("bytes-of-b".getBytes(), 0.96f, 0.28f)   // cos = 0.96
                .put("bytes-of-c".getBytes(), 0.8f, 0.6f);    // cos = 0.8

        try (ImageVerifier verifier = verifier(model, 0.90)) {
            VerificationResult result = verifier.verify(refs("a", "b", "c"), CANDIDATE);

            assertAll(
                    () -> assertEquals("b", result.matchedKey()),
                    () -> assertEquals(0.96, result.similarity(), 1e-6),
                    () -> assertEquals(List.of("a", "b", "c"),
                            result.scores().stream().map(Score::key).toList()));
        }
    }

    @Test
    @DisplayName("AC-12 동점이면 앞선 것이 이긴다")
    void tieGoesToTheFirst() throws Exception {
        FakeEmbeddingModel model = new FakeEmbeddingModel()
                .put(CANDIDATE, 1f, 0f)
                .put("bytes-of-first".getBytes(), 1f, 0f)
                .put("bytes-of-second".getBytes(), 1f, 0f);

        try (ImageVerifier verifier = verifier(model, 0.80)) {
            assertEquals("first", verifier.verify(refs("first", "second"), CANDIDATE).matchedKey());
        }
    }

    @Test
    @DisplayName("한 번의 요청으로 후보와 기준 전부를 보낸다")
    void sendsOneBatchContainingCandidateAndReferences() throws Exception {
        FakeEmbeddingModel model = new FakeEmbeddingModel()
                .put(CANDIDATE, 1f, 0f)
                .put("bytes-of-a".getBytes(), 1f, 0f)
                .put("bytes-of-b".getBytes(), 1f, 0f)
                .put("bytes-of-c".getBytes(), 1f, 0f);

        try (ImageVerifier verifier = verifier(model, 0.80)) {
            verifier.verify(refs("a", "b", "c"), CANDIDATE);
        }

        // 왕복 1회, 그 안에 후보 1장 + 기준 3장.
        assertAll(
                () -> assertEquals(1, model.calls.get(), "요청은 한 번이어야 한다"),
                () -> assertEquals(List.of(4), model.batchSizes, "후보 1 + 기준 3"));
    }

    @Test
    @DisplayName("AC-15 입력 계약은 쌍 단위 백엔드와 동일하다")
    void inputContractIsShared() throws Exception {
        FakeEmbeddingModel model = new FakeEmbeddingModel().put(CANDIDATE, 1f, 0f);

        try (ImageVerifier verifier = verifier(model, 0.80)) {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> verifier.verify(null, CANDIDATE)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> verifier.verify(List.of(), CANDIDATE)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> verifier.verify(refs("a"), null)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> verifier.verify(refs("a"), new byte[0])),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> verifier.verify(refs("dup", "dup"), CANDIDATE)));
        }
    }

    @Test
    @DisplayName("돌려받은 임베딩 개수가 어긋나면 조용히 넘어가지 않는다")
    void mismatchedEmbeddingCountFails() throws Exception {
        EmbeddingModel liar = new EmbeddingModel() {
            @Override
            public String modelId() {
                return "liar";
            }

            @Override
            public List<float[]> embed(List<byte[]> images) {
                // 보낸 것보다 적게 돌려준다. 짝이 밀리면 조용히 틀린 판정이 된다.
                return List.of(new float[] { 1f, 0f });
            }
        };

        try (ImageVerifier verifier = verifier(liar, 0.80)) {
            assertThrows(InferenceUnavailableException.class,
                    () -> verifier.verify(refs("a", "b"), CANDIDATE));
        }
    }

    @Test
    @DisplayName("AC-51 척도를 갈아끼울 수 있다")
    void ac51_customMetricReplacesCosine() throws Exception {
        FakeEmbeddingModel model = new FakeEmbeddingModel()
                .put(CANDIDATE, 1f, 0f)
                .put("bytes-of-a".getBytes(), 0f, 1f);   // 코사인이라면 0.0 이라 거부됐을 것

        try (ImageVerifier verifier = ImageVerifiers.using(model, (a, b) -> 1.0,
                VerifierConfig.withThreshold(0.80))) {
            assertTrue(verifier.verify(refs("a"), CANDIDATE).matched(),
                    "커스텀 척도가 쓰이지 않았습니다");
        }
    }

    @Test
    @DisplayName("AC-50 네트워크 없이 임베딩 백엔드 전체 경로가 돈다")
    void ac50_customEmbeddingBackend() throws Exception {
        FakeEmbeddingModel model = new FakeEmbeddingModel()
                .put(CANDIDATE, 1f, 0f)
                .put("bytes-of-a".getBytes(), 1f, 0f);

        try (ImageVerifier verifier = verifier(model, 0.80)) {
            assertTrue(verifier.verify(refs("a"), CANDIDATE).matched());
        }
        assertEquals("fake-embed", model.modelId());
    }
}
