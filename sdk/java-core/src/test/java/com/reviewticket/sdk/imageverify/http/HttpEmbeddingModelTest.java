package com.reviewticket.sdk.imageverify.http;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.reviewticket.sdk.imageverify.api.InferenceUnavailableException;
import com.reviewticket.sdk.imageverify.api.InvalidImageException;
import com.reviewticket.sdk.imageverify.testing.StubInferenceServer;

/** {@code /embed} 어댑터의 인수 테스트. 스텁 HTTP 서버를 실제로 띄운다. */
class HttpEmbeddingModelTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    /** 벡터들을 서버가 보내는 형식(리틀엔디언 float32 연속)으로 만든다. */
    private static byte[] pack(float[]... vectors) {
        int dimension = vectors[0].length;
        ByteBuffer buffer = ByteBuffer.allocate(vectors.length * dimension * Float.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (float[] vector : vectors) {
            for (float value : vector) {
                buffer.putFloat(value);
            }
        }
        return buffer.array();
    }

    private static void sendBinary(com.sun.net.httpserver.HttpExchange exchange, int status,
            byte[] body, String modelId, int dimension, int count) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
        if (modelId != null) {
            exchange.getResponseHeaders().add("X-Model-Id", modelId);
        }
        exchange.getResponseHeaders().add("X-Embedding-Dim", String.valueOf(dimension));
        exchange.getResponseHeaders().add("X-Embedding-Count", String.valueOf(count));
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
        exchange.close();
    }

    private static HttpEmbeddingModel modelFor(StubInferenceServer server) {
        URI base = URI.create(server.similarityUri().resolve("/").toString());
        return new HttpEmbeddingModel(base, "/embed", TIMEOUT);
    }

    @Test
    @DisplayName("AC-31 배치 순서와 차원이 보존된다")
    void ac31_batchOrderAndDimensionPreserved() throws Exception {
        float[] first = { 1f, 0f, 0f, 0f };
        float[] second = { 0f, 1f, 0f, 0f };
        float[] third = { 0f, 0f, 1f, 0f };
        byte[] packed = pack(first, second, third);

        try (StubInferenceServer server = StubInferenceServer.start(
                exchange -> sendBinary(exchange, 200, packed, "stub-model", 4, 3));
                HttpEmbeddingModel model = modelFor(server)) {

            List<float[]> vectors = model.embed(List.of(
                    "a".getBytes(), "b".getBytes(), "c".getBytes()));

            assertAll(
                    () -> assertEquals(3, vectors.size(), "개수"),
                    () -> assertEquals(4, vectors.get(0).length, "차원"),
                    // i번째 벡터가 i번째 이미지의 것이어야 한다. 서로 다른 세 벡터로
                    // 순서가 밀리는 사고를 잡는다.
                    () -> assertArrayEquals(first, vectors.get(0), 0f),
                    () -> assertArrayEquals(second, vectors.get(1), 0f),
                    () -> assertArrayEquals(third, vectors.get(2), 0f));
        }
    }

    @Test
    @DisplayName("AC-31 파트 이름 images 를 반복해 보낸다")
    void ac31_repeatsImagesPartName() throws Exception {
        byte[] packed = pack(new float[] { 1f }, new float[] { 1f });

        try (StubInferenceServer server = StubInferenceServer.start(
                exchange -> sendBinary(exchange, 200, packed, "stub-model", 1, 2));
                HttpEmbeddingModel model = modelFor(server)) {

            model.embed(List.of("first".getBytes(), "second".getBytes()));
            String body = server.lastBodyAsText();

            int occurrences = body.split("name=\"images\"", -1).length - 1;
            assertEquals(2, occurrences, "images 파트가 두 번 있어야 합니다");
            assertTrue(body.contains("first") && body.contains("second"), "두 이미지가 모두 실려야 합니다");
        }
    }

    @Test
    @DisplayName("본문 길이가 헤더와 어긋나면 거절한다")
    void bodyLengthMustMatchHeaders() throws Exception {
        // 헤더는 3개라 하지만 본문에는 2개만 담겨 있다. 그대로 읽으면 짝이 밀린다.
        byte[] shortBody = pack(new float[] { 1f, 0f }, new float[] { 0f, 1f });

        try (StubInferenceServer server = StubInferenceServer.start(
                exchange -> sendBinary(exchange, 200, shortBody, "stub-model", 2, 3));
                HttpEmbeddingModel model = modelFor(server)) {

            assertThrows(InferenceUnavailableException.class,
                    () -> model.embed(List.of("a".getBytes(), "b".getBytes(), "c".getBytes())));
        }
    }

    @Test
    @DisplayName("개수가 요청과 다르면 거절한다")
    void countMustMatchRequest() throws Exception {
        byte[] packed = pack(new float[] { 1f }, new float[] { 1f });

        try (StubInferenceServer server = StubInferenceServer.start(
                exchange -> sendBinary(exchange, 200, packed, "stub-model", 1, 2));
                HttpEmbeddingModel model = modelFor(server)) {

            assertThrows(InferenceUnavailableException.class,
                    () -> model.embed(List.of("only-one".getBytes())));
        }
    }

    @Test
    @DisplayName("필수 헤더가 없으면 거절한다")
    void missingHeadersAreRejected() throws Exception {
        try (StubInferenceServer server = StubInferenceServer.start(exchange -> {
            byte[] body = pack(new float[] { 1f });
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
            exchange.close();
        }); HttpEmbeddingModel model = modelFor(server)) {

            assertThrows(InferenceUnavailableException.class,
                    () -> model.embed(List.of("a".getBytes())));
        }
    }

    @Test
    @DisplayName("4xx 는 InvalidImage, 5xx 는 InferenceUnavailable")
    void statusMappingMatchesPairwiseBackend() throws Exception {
        try (StubInferenceServer bad = StubInferenceServer.respondingWith(400, "cannot decode");
                StubInferenceServer broken = StubInferenceServer.respondingWith(500, "boom");
                HttpEmbeddingModel badModel = modelFor(bad);
                HttpEmbeddingModel brokenModel = modelFor(broken)) {

            assertAll(
                    () -> assertThrows(InvalidImageException.class,
                            () -> badModel.embed(List.of("a".getBytes()))),
                    () -> assertThrows(InferenceUnavailableException.class,
                            () -> brokenModel.embed(List.of("a".getBytes()))));
        }
    }

    @Test
    @DisplayName("modelId 는 핸드셰이크로 받아 오고 한 번만 묻는다")
    void modelIdComesFromHandshakeAndIsCachedOnce() throws Exception {
        try (StubInferenceServer server = StubInferenceServer.start(exchange -> {
            String json = "{\"status\": \"ok\", \"model\": \"handshake-model\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
            exchange.close();
        }); HttpEmbeddingModel model = modelFor(server)) {

            assertEquals("handshake-model", model.modelId());
            assertEquals("handshake-model", model.modelId());
            // 두 번 물어도 왕복은 한 번. 캐시 키를 만들 때마다 서버를 부르면 안 된다.
            assertEquals(1, server.requestCount(), "핸드셰이크는 한 번만 일어나야 합니다");
        }
    }

    @Test
    @DisplayName("응답 헤더의 모델 식별자가 핸드셰이크 값보다 우선한다")
    void responseHeaderUpdatesModelId() throws Exception {
        byte[] packed = pack(new float[] { 1f });

        try (StubInferenceServer server = StubInferenceServer.start(
                exchange -> sendBinary(exchange, 200, packed, "model-after-restart", 1, 1));
                HttpEmbeddingModel model = modelFor(server)) {

            model.embed(List.of("a".getBytes()));
            // 서버가 모델을 바꿔 달고 재기동한 상황. 캐시가 낡은 식별자를 계속
            // 쓰면 새 모델의 벡터를 옛 키로 저장하게 된다.
            assertEquals("model-after-restart", model.modelId());
        }
    }

    @Test
    @DisplayName("추론 서버가 없으면 InferenceUnavailable")
    void connectionRefused() throws Exception {
        URI dead;
        try (StubInferenceServer server = StubInferenceServer.respondingWith(200, "{}")) {
            dead = server.similarityUri().resolve("/");
        }

        try (HttpEmbeddingModel model = new HttpEmbeddingModel(dead, "/embed", TIMEOUT)) {
            assertThrows(InferenceUnavailableException.class,
                    () -> model.embed(List.of("a".getBytes())));
        }
    }
}
