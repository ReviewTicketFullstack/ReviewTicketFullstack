package com.reviewticket.sdk.imageverify.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.reviewticket.sdk.imageverify.api.InferenceUnavailableException;
import com.reviewticket.sdk.imageverify.api.InvalidImageException;
import com.reviewticket.sdk.imageverify.spi.PairwiseModel;

/**
 * 이미지 두 장을 multipart 로 올려 유사도 하나를 받는 추론 서버 어댑터.
 *
 * <p>요청·응답 형태는 기존 추론 서버가 이미 쓰고 있던 것 그대로다 — 파트 이름
 * {@code reviewImage}/{@code compareImage}, 응답 키 {@code similarity}. 서버는
 * 한 줄도 바뀌지 않았고, 바뀌면 안 된다.
 *
 * <p>Spring 의 RestClient 가 아니라 JDK 내장 {@link HttpClient} 를 쓴다(ARCH-R1).
 * 타임아웃은 연결에 {@code connectTimeout}, 교환 전체에 요청 {@code timeout} 으로
 * 건다.
 */
public final class HttpPairwiseModel implements PairwiseModel, AutoCloseable {

    /**
     * 응답에서 similarity 값만 뽑는다. JSON 파서를 넣지 않으려는 것이고
     * (SPEC 결정 D-2), 이 응답은 키 하나짜리라 그 값어치를 못 한다.
     * 지수 표기(1.0E-4)까지 받는다 — 파이썬이 아주 작은 값을 그렇게 낼 수 있다.
     */
    private static final Pattern SIMILARITY =
            Pattern.compile("\"similarity\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][-+]?\\d+)?)");

    private final HttpClient client;
    private final URI endpoint;
    private final Duration timeout;

    public HttpPairwiseModel(URI endpoint, Duration timeout) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint 가 null 입니다");
        this.timeout = Objects.requireNonNull(timeout, "timeout 이 null 입니다");
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    /**
     * 이 백엔드는 서버가 어떤 모델을 쓰는지 알 방법이 없다 — 레거시 응답에는
     * 모델 정보가 없다. 캐시를 쓰지 않는 경로라 문제되지 않는다.
     */
    @Override
    public String modelId() {
        return "legacy-pairwise";
    }

    @Override
    public double similarity(byte[] candidate, byte[] reference) {
        MultipartBody body = MultipartBody.create()
                .filePart("reviewImage", "review.jpg", candidate)
                .filePart("compareImage", "compare.jpg", reference);

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Content-Type", body.contentType())
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.build()))
                .build();

        HttpResponse<String> response = send(request);
        int status = response.statusCode();

        if (status >= 400 && status < 500) {
            throw new InvalidImageException(
                    "추론 서버가 이미지를 거절했습니다: HTTP " + status);
        }
        if (status < 200 || status >= 300) {
            throw new InferenceUnavailableException(
                    "추론 서버가 오류를 응답했습니다: HTTP " + status);
        }
        return parseSimilarity(response.body());
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // 연결 거부, 타임아웃(HttpTimeoutException), 연결 끊김이 모두 여기로 온다.
            throw new InferenceUnavailableException(
                    "추론 서버가 응답하지 않거나 시간을 초과했습니다: " + endpoint, e);
        } catch (InterruptedException e) {
            // 인터럽트 상태를 삼키지 않는다 — 위쪽에서 종료 신호를 알아야 한다.
            Thread.currentThread().interrupt();
            throw new InferenceUnavailableException("추론 요청이 중단됐습니다", e);
        }
    }

    private double parseSimilarity(String body) {
        if (body == null || body.isBlank()) {
            throw new InferenceUnavailableException("추론 서버 응답이 비어 있습니다");
        }
        Matcher matcher = SIMILARITY.matcher(body);
        if (!matcher.find()) {
            throw new InferenceUnavailableException(
                    "추론 서버 응답에서 similarity 를 찾지 못했습니다: " + preview(body));
        }
        try {
            return Double.parseDouble(matcher.group(1));
        } catch (NumberFormatException e) {
            throw new InferenceUnavailableException(
                    "similarity 를 숫자로 읽지 못했습니다: " + matcher.group(1), e);
        }
    }

    /** 오류 메시지에 응답 전체를 싣지 않는다. 로그가 응답 본문으로 뒤덮이는 걸 막는다. */
    private static String preview(String body) {
        String trimmed = body.strip();
        return trimmed.length() <= 200 ? trimmed : trimmed.substring(0, 200) + "…";
    }

    @Override
    public void close() {
        client.close();
    }
}
