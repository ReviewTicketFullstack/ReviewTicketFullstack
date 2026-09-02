package com.reviewticket.sdk.imageverify.http;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.reviewticket.sdk.imageverify.api.InferenceUnavailableException;
import com.reviewticket.sdk.imageverify.api.InvalidImageException;
import com.reviewticket.sdk.imageverify.testing.StubInferenceServer;

/** 추론 서버 어댑터의 인수 테스트. 스텁 HTTP 서버를 실제로 띄워 확인한다. */
class HttpPairwiseModelTest {

    private static final byte[] CANDIDATE = "candidate".getBytes();
    private static final byte[] REFERENCE = "reference".getBytes();
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    @Test
    @DisplayName("AC-70 레거시 요청·응답 형태를 그대로 지킨다")
    void ac70_legacyWireFormatIsPreserved() throws Exception {
        try (StubInferenceServer server =
                StubInferenceServer.respondingWith(200, "{\"similarity\": 0.8137254901960784}");
                HttpPairwiseModel model = new HttpPairwiseModel(server.similarityUri(), TIMEOUT)) {

            double similarity = model.similarity(CANDIDATE, REFERENCE);
            String sentBody = server.lastBodyAsText();

            assertAll(
                    () -> assertEquals(0.8137254901960784, similarity, "응답 키 similarity 를 그대로 읽는다"),
                    // 파트 이름이 바뀌면 추론 서버가 요청을 못 읽는다. 계약이다.
                    () -> assertTrue(sentBody.contains("name=\"reviewImage\""), "reviewImage 파트"),
                    () -> assertTrue(sentBody.contains("name=\"compareImage\""), "compareImage 파트"),
                    () -> assertTrue(sentBody.contains("candidate"), "후보 이미지가 reviewImage 로 간다"),
                    () -> assertTrue(sentBody.contains("reference"), "기준 이미지가 compareImage 로 간다"));
        }
    }

    @Test
    @DisplayName("AC-70 후보 이미지가 reviewImage 파트에 실린다 (순서 계약)")
    void ac70_candidateGoesIntoReviewImagePart() throws Exception {
        try (StubInferenceServer server =
                StubInferenceServer.respondingWith(200, "{\"similarity\": 0.5}");
                HttpPairwiseModel model = new HttpPairwiseModel(server.similarityUri(), TIMEOUT)) {

            model.similarity(CANDIDATE, REFERENCE);
            String body = server.lastBodyAsText();

            int reviewPart = body.indexOf("name=\"reviewImage\"");
            int comparePart = body.indexOf("name=\"compareImage\"");
            int candidateBytes = body.indexOf("candidate");
            int referenceBytes = body.indexOf("reference");

            // 후보는 reviewImage 뒤, compareImage 앞. 기준은 compareImage 뒤.
            assertAll(
                    () -> assertTrue(reviewPart < candidateBytes && candidateBytes < comparePart,
                            "후보 이미지가 reviewImage 파트 안에 있어야 한다"),
                    () -> assertTrue(comparePart < referenceBytes,
                            "기준 이미지가 compareImage 파트 안에 있어야 한다"));
        }
    }

    @Test
    @DisplayName("AC-20 추론 서버가 안 떠 있으면 InferenceUnavailable")
    void ac20_connectionRefused() throws Exception {
        // 아무도 듣고 있지 않은 포트. 서버를 띄웠다 바로 닫아 확실히 비운다.
        URI dead;
        try (StubInferenceServer server = StubInferenceServer.respondingWith(200, "{}")) {
            dead = server.similarityUri();
        }

        try (HttpPairwiseModel model = new HttpPairwiseModel(dead, TIMEOUT)) {
            assertThrows(InferenceUnavailableException.class,
                    () -> model.similarity(CANDIDATE, REFERENCE));
        }
    }

    @Test
    @DisplayName("AC-21 타임아웃이 실제로 걸린다")
    void ac21_timeoutIsEnforced() throws Exception {
        Duration shortTimeout = Duration.ofMillis(300);

        try (StubInferenceServer server = StubInferenceServer.delayedBy(5_000);
                HttpPairwiseModel model = new HttpPairwiseModel(server.similarityUri(), shortTimeout)) {

            long startedAt = System.nanoTime();
            assertThrows(InferenceUnavailableException.class,
                    () -> model.similarity(CANDIDATE, REFERENCE));
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

            // 제한 시간 근처에서 끊겼는지까지 본다 — 예외 타입만 맞고 5초를
            // 기다렸다면 타임아웃이 동작하지 않은 것이다.
            assertTrue(elapsedMillis < 3_000,
                    "타임아웃이 걸리지 않았습니다: " + elapsedMillis + "ms");
        }
    }

    @Test
    @DisplayName("AC-22 5xx 는 InferenceUnavailable")
    void ac22_serverErrorIsUnavailable() throws Exception {
        try (StubInferenceServer server = StubInferenceServer.respondingWith(500, "boom");
                HttpPairwiseModel model = new HttpPairwiseModel(server.similarityUri(), TIMEOUT)) {

            assertThrows(InferenceUnavailableException.class,
                    () -> model.similarity(CANDIDATE, REFERENCE));
        }
    }

    @Test
    @DisplayName("AC-23 비었거나 해석 불가한 응답은 InferenceUnavailable")
    void ac23_unreadableResponseIsUnavailable() throws Exception {
        try (StubInferenceServer empty = StubInferenceServer.respondingWith(200, "");
                StubInferenceServer garbage = StubInferenceServer.respondingWith(200, "not json at all");
                StubInferenceServer wrongKey =
                        StubInferenceServer.respondingWith(200, "{\"score\": 0.9}");
                HttpPairwiseModel emptyModel = new HttpPairwiseModel(empty.similarityUri(), TIMEOUT);
                HttpPairwiseModel garbageModel = new HttpPairwiseModel(garbage.similarityUri(), TIMEOUT);
                HttpPairwiseModel wrongKeyModel = new HttpPairwiseModel(wrongKey.similarityUri(), TIMEOUT)) {

            assertAll(
                    () -> assertThrows(InferenceUnavailableException.class,
                            () -> emptyModel.similarity(CANDIDATE, REFERENCE), "빈 본문"),
                    () -> assertThrows(InferenceUnavailableException.class,
                            () -> garbageModel.similarity(CANDIDATE, REFERENCE), "JSON 이 아님"),
                    () -> assertThrows(InferenceUnavailableException.class,
                            () -> wrongKeyModel.similarity(CANDIDATE, REFERENCE), "similarity 키 없음"));
        }
    }

    @Test
    @DisplayName("AC-24 4xx 는 InvalidImage")
    void ac24_clientErrorIsInvalidImage() throws Exception {
        try (StubInferenceServer server = StubInferenceServer.respondingWith(400, "cannot decode");
                HttpPairwiseModel model = new HttpPairwiseModel(server.similarityUri(), TIMEOUT)) {

            assertThrows(InvalidImageException.class,
                    () -> model.similarity(CANDIDATE, REFERENCE));
        }
    }

    @Test
    @DisplayName("AC-26 재시도하지 않는다")
    void ac26_noRetry() throws Exception {
        try (StubInferenceServer server = StubInferenceServer.respondingWith(500, "boom");
                HttpPairwiseModel model = new HttpPairwiseModel(server.similarityUri(), TIMEOUT)) {

            assertThrows(InferenceUnavailableException.class,
                    () -> model.similarity(CANDIDATE, REFERENCE));

            assertEquals(1, server.requestCount(), "요청을 한 번만 보내야 한다");
        }
    }

    @Test
    @DisplayName("지수 표기 응답도 읽는다")
    void parsesScientificNotation() throws Exception {
        try (StubInferenceServer server =
                StubInferenceServer.respondingWith(200, "{\"similarity\": 1.5e-4}");
                HttpPairwiseModel model = new HttpPairwiseModel(server.similarityUri(), TIMEOUT)) {

            assertEquals(1.5e-4, model.similarity(CANDIDATE, REFERENCE));
        }
    }
}
