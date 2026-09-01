package com.reviewticket.sdk.imageverify.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.reviewticket.sdk.imageverify.api.InferenceUnavailableException;
import com.reviewticket.sdk.imageverify.api.InvalidImageException;
import com.reviewticket.sdk.imageverify.spi.EmbeddingModel;

/**
 * 이미지 여러 장을 한 번에 올려 임베딩을 이진으로 받는 추론 서버 어댑터.
 *
 * <p>응답이 JSON 이 아니라 리틀엔디언 float32 연속 바이트다. JSON 파서를 넣지
 * 않으려는 것이고(SPEC 결정 D-2), 768차원 실수를 텍스트로 주고받으면 전송량이
 * 네 배쯤 되기 때문이기도 하다. 모델 정보는 헤더로 온다.
 */
public final class HttpEmbeddingModel implements EmbeddingModel, AutoCloseable {

    private static final String MODEL_ID_HEADER = "X-Model-Id";
    private static final String DIM_HEADER = "X-Embedding-Dim";
    private static final String COUNT_HEADER = "X-Embedding-Count";

    /** 핸드셰이크 응답에서 모델 이름만 뽑는다. 키 하나짜리라 파서를 들일 값어치가 없다. */
    private static final Pattern MODEL_FIELD =
            Pattern.compile("\"model\"\\s*:\\s*\"([^\"]*)\"");

    private final HttpClient client;
    private final URI embedEndpoint;
    private final URI healthEndpoint;
    private final Duration timeout;

    /**
     * 핸드셰이크로 받아 온 모델 식별자. 캐시 키에 쓰이므로 임베딩을 부르기 전에
     * 알아야 한다(SPEC §8.3). 매 응답의 헤더로도 갱신된다 — 서버가 모델을
     * 바꿔 달고 재기동한 경우를 잡기 위해서다.
     */
    private volatile String modelId;

    public HttpEmbeddingModel(URI baseUrl, String embedPath, Duration timeout) {
        Objects.requireNonNull(baseUrl, "baseUrl 이 null 입니다");
        Objects.requireNonNull(embedPath, "embedPath 가 null 입니다");
        this.timeout = Objects.requireNonNull(timeout, "timeout 이 null 입니다");
        this.embedEndpoint = baseUrl.resolve(embedPath);
        this.healthEndpoint = baseUrl.resolve("/");
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public String modelId() {
        String known = modelId;
        if (known != null) {
            return known;
        }
        synchronized (this) {
            if (modelId == null) {
                modelId = handshake();
            }
            return modelId;
        }
    }

    /** 서버에 어떤 모델을 쓰는지 한 번 묻는다. 이미 있던 상태 확인 경로를 그대로 쓴다. */
    private String handshake() {
        HttpRequest request = HttpRequest.newBuilder(healthEndpoint)
                .timeout(timeout)
                .GET()
                .build();

        HttpResponse<String> response = sendForText(request, healthEndpoint);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new InferenceUnavailableException(
                    "추론 서버 상태 확인에 실패했습니다: HTTP " + response.statusCode());
        }
        Matcher matcher = MODEL_FIELD.matcher(response.body() == null ? "" : response.body());
        if (!matcher.find()) {
            throw new InferenceUnavailableException(
                    "추론 서버가 모델 식별자를 알려주지 않았습니다: " + healthEndpoint);
        }
        return matcher.group(1);
    }

    @Override
    public List<float[]> embed(List<byte[]> images) {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("임베딩할 이미지가 없습니다");
        }

        MultipartBody body = MultipartBody.create();
        for (int i = 0; i < images.size(); i++) {
            // 파트 이름은 하나(images)를 반복한다 — FastAPI 가 이걸 목록으로 받는다.
            body.filePart("images", "image-" + i + ".jpg", images.get(i));
        }

        HttpRequest request = HttpRequest.newBuilder(embedEndpoint)
                .timeout(timeout)
                .header("Content-Type", body.contentType())
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.build()))
                .build();

        HttpResponse<byte[]> response = sendForBytes(request);
        int status = response.statusCode();
        if (status >= 400 && status < 500) {
            throw new InvalidImageException("추론 서버가 이미지를 거절했습니다: HTTP " + status);
        }
        if (status < 200 || status >= 300) {
            throw new InferenceUnavailableException("추론 서버가 오류를 응답했습니다: HTTP " + status);
        }

        // 서버가 모델을 바꿔 달았을 수 있다. 헤더가 최신이므로 그쪽을 따른다.
        header(response, MODEL_ID_HEADER).ifPresent(id -> this.modelId = id);

        return unpack(response.body(),
                intHeader(response, COUNT_HEADER),
                intHeader(response, DIM_HEADER),
                images.size());
    }

    /**
     * 이진 응답을 벡터 목록으로 되돌린다.
     *
     * <p>개수·차원·길이를 모두 검사한다. 여기서 어긋나면 벡터가 밀려 짝이 맞지
     * 않는다는 뜻이고, 그건 예외가 아니라 <b>조용히 틀린 판정</b>으로 이어진다.
     */
    private static List<float[]> unpack(byte[] body, int count, int dimension, int expectedCount) {
        if (body == null) {
            throw new InferenceUnavailableException("추론 서버 응답이 비어 있습니다");
        }
        if (count != expectedCount) {
            throw new InferenceUnavailableException(
                    "임베딩 개수가 요청과 다릅니다: 보낸 " + expectedCount + "장, 받은 " + count);
        }
        if (dimension <= 0) {
            throw new InferenceUnavailableException("임베딩 차원이 올바르지 않습니다: " + dimension);
        }
        long expectedBytes = (long) count * dimension * Float.BYTES;
        if (body.length != expectedBytes) {
            throw new InferenceUnavailableException("임베딩 본문 길이가 맞지 않습니다: "
                    + body.length + "바이트, 기대값 " + expectedBytes + "바이트");
        }

        ByteBuffer buffer = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
        List<float[]> vectors = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            float[] vector = new float[dimension];
            buffer.asFloatBuffer().get(vector);
            buffer.position(buffer.position() + dimension * Float.BYTES);
            vectors.add(vector);
        }
        return vectors;
    }

    private static Optional<String> header(HttpResponse<?> response, String name) {
        return response.headers().firstValue(name);
    }

    private static int intHeader(HttpResponse<?> response, String name) {
        String raw = header(response, name).orElseThrow(() -> new InferenceUnavailableException(
                "추론 서버 응답에 " + name + " 헤더가 없습니다"));
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new InferenceUnavailableException(name + " 헤더를 숫자로 읽지 못했습니다: " + raw, e);
        }
    }

    private HttpResponse<byte[]> sendForBytes(HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new InferenceUnavailableException(
                    "추론 서버가 응답하지 않거나 시간을 초과했습니다: " + embedEndpoint, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InferenceUnavailableException("추론 요청이 중단됐습니다", e);
        }
    }

    private HttpResponse<String> sendForText(HttpRequest request, URI target) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new InferenceUnavailableException(
                    "추론 서버가 응답하지 않거나 시간을 초과했습니다: " + target, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InferenceUnavailableException("추론 요청이 중단됐습니다", e);
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
