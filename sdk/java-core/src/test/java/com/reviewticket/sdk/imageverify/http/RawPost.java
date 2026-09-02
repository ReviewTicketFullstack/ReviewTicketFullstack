package com.reviewticket.sdk.imageverify.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 계약 테스트 전용 — 프로덕션 코드와 <b>똑같은</b> multipart 본문을 보내고
 * 응답 본문을 통째로 돌려준다.
 *
 * <p>{@link HttpPairwiseModel} 은 similarity 하나만 뽑아내므로, 서버가 함께
 * 실어 보내는 sha256 을 확인하려면 응답 원문이 필요하다. 본문 생성은
 * {@link MultipartBody} 를 그대로 쓴다 — 여기서 다르게 만들면 정작 검증하려던
 * 것을 검증하지 못한다.
 */
final class RawPost {

    private RawPost() {
    }

    static String post(URI endpoint, byte[] candidate, byte[] reference) throws Exception {
        MultipartBody body = MultipartBody.create()
                .filePart("reviewImage", "review.jpg", candidate)
                .filePart("compareImage", "compare.jpg", reference);

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", body.contentType())
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.build()))
                .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new AssertionError(
                        "서버가 200 이 아닌 응답을 냈습니다: HTTP " + response.statusCode()
                                + " — 요청 형식이 거절됐다는 뜻입니다.\n본문: " + response.body());
            }
            return response.body();
        }
    }
}
