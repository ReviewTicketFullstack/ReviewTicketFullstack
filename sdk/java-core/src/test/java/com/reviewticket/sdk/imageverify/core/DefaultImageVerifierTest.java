package com.reviewticket.sdk.imageverify.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.reviewticket.sdk.imageverify.api.ImageVerifier;
import com.reviewticket.sdk.imageverify.api.ImageVerifiers;
import com.reviewticket.sdk.imageverify.api.ImageVerifyException;
import com.reviewticket.sdk.imageverify.api.InferenceUnavailableException;
import com.reviewticket.sdk.imageverify.api.ReferenceImage;
import com.reviewticket.sdk.imageverify.api.Score;
import com.reviewticket.sdk.imageverify.api.VerificationResult;
import com.reviewticket.sdk.imageverify.api.VerifierConfig;
import com.reviewticket.sdk.imageverify.spi.PairwiseModel;
import com.reviewticket.sdk.imageverify.testing.FakePairwiseModel;

/**
 * 판정 로직의 인수 테스트. 추론 백엔드는 가짜라 네트워크가 필요 없다.
 *
 * <p>ACCEPTANCE.md 의 AC 번호를 메서드 이름에 그대로 쓴다 — 실패한 테스트에서
 * 어떤 계약이 깨졌는지 바로 문서로 찾아갈 수 있게.
 */
class DefaultImageVerifierTest {

    private static final byte[] CANDIDATE = "candidate-bytes".getBytes();

    private static ImageVerifier verifier(PairwiseModel model, double threshold) {
        return ImageVerifiers.using(model, VerifierConfig.withThreshold(threshold));
    }

    private static List<ReferenceImage> refs(String... keys) {
        List<ReferenceImage> list = new ArrayList<>();
        for (String key : keys) {
            list.add(ReferenceImage.ofBytes(key, ("bytes-of-" + key).getBytes()));
        }
        return list;
    }

    // ---- 검증 (AC-01 ~ AC-04) ----

    @Test
    @DisplayName("AC-01 일치하는 사진은 통과한다")
    void ac01_matchingImagePasses() throws Exception {
        try (ImageVerifier verifier = verifier(FakePairwiseModel.returning(0.91), 0.80)) {
            VerificationResult result = verifier.verify(refs("ref-1"), CANDIDATE);

            assertAll(
                    () -> assertTrue(result.matched()),
                    () -> assertEquals(0.91, result.similarity()),
                    () -> assertEquals("ref-1", result.matchedKey()),
                    () -> assertEquals(0.80, result.threshold()));
        }
    }

    @Test
    @DisplayName("AC-02 일치하지 않는 사진은 예외가 아니라 matched=false 다")
    void ac02_mismatchIsAResultNotAnException() throws Exception {
        try (ImageVerifier verifier = verifier(FakePairwiseModel.returning(0.62), 0.80)) {
            VerificationResult result = verifier.verify(refs("ref-1"), CANDIDATE);

            assertFalse(result.matched());
            assertEquals(0.62, result.similarity());
        }
    }

    @Test
    @DisplayName("AC-03 임계값 경계는 포함이다 (>=)")
    void ac03_thresholdIsInclusive() throws Exception {
        try (ImageVerifier exact = verifier(FakePairwiseModel.returning(0.80), 0.80);
                ImageVerifier below = verifier(FakePairwiseModel.returning(0.79999999), 0.80);
                ImageVerifier above = verifier(FakePairwiseModel.returning(0.80000001), 0.80)) {

            assertAll(
                    () -> assertTrue(exact.verify(refs("r"), CANDIDATE).matched(), "정확히 같으면 통과"),
                    () -> assertFalse(below.verify(refs("r"), CANDIDATE).matched(), "미만이면 거부"),
                    () -> assertTrue(above.verify(refs("r"), CANDIDATE).matched(), "초과면 통과"));
        }
    }

    @Test
    @DisplayName("AC-04 similarity 는 가공되지 않고 그대로 나온다")
    void ac04_similarityIsNotRounded() throws Exception {
        double raw = 0.8137254901960784;
        try (ImageVerifier verifier = verifier(FakePairwiseModel.returning(raw), 0.80)) {
            assertEquals(raw, verifier.verify(refs("r"), CANDIDATE).similarity());
        }
    }

    // ---- 다중 기준 이미지 (AC-10 ~ AC-15) ----

    @Test
    @DisplayName("AC-10/AC-11 최댓값과 그 key 를 고른다")
    void ac10_ac11_picksHighestScoreAndItsKey() throws Exception {
        List<ReferenceImage> references = refs("a", "b", "c", "d", "e");
        List<byte[]> order = references.stream().map(ReferenceImage::bytes).toList();
        PairwiseModel model = FakePairwiseModel.byReferenceBytes(order, 0.41, 0.55, 0.88, 0.32, 0.71);

        try (ImageVerifier verifier = verifier(model, 0.80)) {
            VerificationResult result = verifier.verify(references, CANDIDATE);

            assertEquals(0.88, result.similarity());
            assertEquals("c", result.matchedKey());
        }
    }

    @Test
    @DisplayName("AC-12 동점이면 입력 순서상 앞선 것이 이긴다")
    void ac12_tieGoesToTheFirst() throws Exception {
        List<ReferenceImage> references = refs("first", "middle", "last");
        List<byte[]> order = references.stream().map(ReferenceImage::bytes).toList();
        PairwiseModel model = FakePairwiseModel.byReferenceBytes(order, 0.88, 0.55, 0.88);

        try (ImageVerifier verifier = verifier(model, 0.80)) {
            // 저장되는 "어느 기준과 맞았는가"가 실행마다 흔들리면 안 된다.
            assertEquals("first", verifier.verify(references, CANDIDATE).matchedKey());
        }
    }

    @Test
    @DisplayName("AC-13 scores 는 입력 순서와 길이를 지킨다")
    void ac13_scoresPreserveInputOrder() throws Exception {
        List<ReferenceImage> references = refs("a", "b", "c");
        List<byte[]> order = references.stream().map(ReferenceImage::bytes).toList();
        PairwiseModel model = FakePairwiseModel.byReferenceBytes(order, 0.10, 0.20, 0.30);

        try (ImageVerifier verifier = verifier(model, 0.80)) {
            List<Score> scores = verifier.verify(references, CANDIDATE).scores();

            assertEquals(List.of("a", "b", "c"), scores.stream().map(Score::key).toList());
            assertEquals(List.of(0.10, 0.20, 0.30), scores.stream().map(Score::similarity).toList());
        }
    }

    @Test
    @DisplayName("AC-14 기준 이미지들은 동시에 처리된다")
    void ac14_referencesAreProcessedConcurrently() throws Exception {
        // 호출당 200ms. 순차라면 5장에 1000ms 이상, 동시면 한 번 수준으로 끝난다.
        FakePairwiseModel model = FakePairwiseModel.returning(0.1, 0.2, 0.3, 0.4, 0.5).withDelay(200);

        try (ImageVerifier verifier = verifier(model, 0.80)) {
            long startedAt = System.nanoTime();
            verifier.verify(refs("a", "b", "c", "d", "e"), CANDIDATE);
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

            assertTrue(elapsedMillis < 500,
                    "5장이 동시에 처리되지 않았습니다: " + elapsedMillis + "ms (순차라면 1000ms 이상)");
        }
    }

    @Test
    @DisplayName("AC-15 입력 계약 위반은 IllegalArgumentException 이다")
    void ac15_inputContractViolations() throws Exception {
        try (ImageVerifier verifier = verifier(FakePairwiseModel.returning(0.9), 0.80)) {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> verifier.verify(null, CANDIDATE), "references 가 null"),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> verifier.verify(List.of(), CANDIDATE), "references 가 빈 목록"),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> verifier.verify(refs("a"), null), "candidate 가 null"),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> verifier.verify(refs("a"), new byte[0]), "candidate 가 빈 배열"),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> ReferenceImage.ofBytes(null, CANDIDATE), "key 가 null"),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> ReferenceImage.ofBytes("  ", CANDIDATE), "key 가 공백"),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> verifier.verify(refs("dup", "dup"), CANDIDATE), "key 가 중복"));
        }
    }

    @Test
    @DisplayName("AC-15 maxReferences 를 넘기면 거절한다")
    void ac15_tooManyReferences() throws Exception {
        VerifierConfig config = new VerifierConfig(0.80, 2, 2);
        try (ImageVerifier verifier = ImageVerifiers.using(FakePairwiseModel.returning(0.9), config)) {
            assertThrows(IllegalArgumentException.class,
                    () -> verifier.verify(refs("a", "b", "c"), CANDIDATE));
        }
    }

    @Test
    @DisplayName("AC-15 기준 이미지의 바이트가 비면 IllegalArgumentException 이다")
    void ac15_emptyReferenceBytes() throws Exception {
        // 지연 로딩을 지키느라 이 검사는 실제로 읽는 시점에 일어난다. 스레드 안에서
        // 던져지므로, 감싸인 예외가 원래 타입으로 벗겨져 나오는지까지 확인한다.
        ReferenceImage empty = ReferenceImage.of("empty", () -> new byte[0]);
        try (ImageVerifier verifier = verifier(FakePairwiseModel.returning(0.9), 0.80)) {
            assertThrows(IllegalArgumentException.class,
                    () -> verifier.verify(List.of(empty), CANDIDATE));
        }
    }

    // ---- 실패 처리 (AC-25 ~ AC-27) ----

    @Test
    @DisplayName("AC-25 하나라도 실패하면 전체가 실패한다")
    void ac25_anyFailureFailsTheWholeVerification() throws Exception {
        // 5장 중 3번째만 실패. 나머지 4장의 최댓값으로 조용히 통과시키면 안 된다.
        FakePairwiseModel model = FakePairwiseModel.returning(0.95, 0.95, 0.95, 0.95, 0.95)
                .failingAt(2, i -> new InferenceUnavailableException("추론 서버 장애"));

        try (ImageVerifier verifier = verifier(model, 0.80)) {
            assertThrows(InferenceUnavailableException.class,
                    () -> verifier.verify(refs("a", "b", "c", "d", "e"), CANDIDATE));
        }
    }

    @Test
    @DisplayName("AC-27 SDK 는 자기 예외 계층만 던진다")
    void ac27_onlySdkExceptionsEscape() throws Exception {
        FakePairwiseModel model = FakePairwiseModel.returning(0.9)
                .failingAt(0, i -> new InferenceUnavailableException("장애"));

        try (ImageVerifier verifier = verifier(model, 0.80)) {
            Throwable thrown = assertThrows(Throwable.class,
                    () -> verifier.verify(refs("a"), CANDIDATE));

            // 애플리케이션 예외 이름을 여기 적어서 확인할 수는 없다 — 그 문자열이
            // 테스트 소스에 있는 것 자체를 가드레일(GR-02)이 막는다. 대신 나온
            // 예외가 SDK 계층 안에 있다는 것을 직접 확인한다. 더 강한 진술이다.
            assertTrue(thrown instanceof ImageVerifyException,
                    "SDK 예외 계층 밖의 예외가 나왔습니다: " + thrown.getClass());
        }
    }

    @Test
    @DisplayName("AC-50 직접 만든 백엔드로 네트워크 없이 전체 경로가 돈다")
    void ac50_customBackendReplacesTheModel() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        PairwiseModel custom = new PairwiseModel() {
            @Override
            public String modelId() {
                return "custom";
            }

            @Override
            public double similarity(byte[] candidate, byte[] reference) {
                calls.incrementAndGet();
                return 0.99;
            }
        };

        try (ImageVerifier verifier = verifier(custom, 0.80)) {
            assertTrue(verifier.verify(refs("a", "b"), CANDIDATE).matched());
            assertEquals(2, calls.get());
        }
    }

    @Test
    @DisplayName("지연 로딩: loader 는 기준 이미지당 한 번만 불린다")
    void loaderIsCalledAtMostOnce() throws Exception {
        AtomicInteger loads = new AtomicInteger();
        ReferenceImage reference = ReferenceImage.of("lazy", () -> {
            loads.incrementAndGet();
            return "bytes".getBytes();
        });

        try (ImageVerifier verifier = verifier(FakePairwiseModel.returning(0.9, 0.9), 0.80)) {
            verifier.verify(List.of(reference), CANDIDATE);
            verifier.verify(List.of(reference), CANDIDATE);
        }

        assertEquals(1, loads.get());
    }

    @Test
    @DisplayName("scores 는 밖에서 고칠 수 없다")
    void scoresAreImmutable() throws Exception {
        try (ImageVerifier verifier = verifier(FakePairwiseModel.returning(0.9), 0.80)) {
            List<Score> scores = verifier.verify(refs("a"), CANDIDATE).scores();
            assertThrows(UnsupportedOperationException.class,
                    () -> scores.add(new Score("x", 1.0)));
        }
    }

    @Test
    @DisplayName("동시 verify 가 서로 간섭하지 않는다")
    void concurrentVerifyIsSafe() throws Exception {
        PairwiseModel model = new PairwiseModel() {
            @Override
            public String modelId() {
                return "steady";
            }

            @Override
            public double similarity(byte[] candidate, byte[] reference) {
                return 0.85;
            }
        };

        try (ImageVerifier verifier = verifier(model, 0.80)) {
            List<Thread> threads = new ArrayList<>();
            boolean[] ok = new boolean[16];
            for (int i = 0; i < ok.length; i++) {
                int index = i;
                Thread thread = new Thread(() -> {
                    VerificationResult result = verifier.verify(refs("a", "b", "c"), CANDIDATE);
                    ok[index] = result.matched() && result.scores().size() == 3;
                });
                threads.add(thread);
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }
            assertTrue(allTrue(ok), "동시 실행 중 일부가 잘못된 결과를 냈습니다: " + Arrays.toString(ok));
        }
    }

    private static boolean allTrue(boolean[] values) {
        for (boolean value : values) {
            if (!value) {
                return false;
            }
        }
        return true;
    }
}
