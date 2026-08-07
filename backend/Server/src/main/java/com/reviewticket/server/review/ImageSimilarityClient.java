package com.reviewticket.server.review;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.reviewticket.server.auth.ServiceUnavailableException;
import com.reviewticket.server.config.ReviewTicketProperties;

/**
 * 리뷰 사진과 메뉴 표본 사진 두 장을 AI 서버로 보내 유사도를 받는다.
 *
 * 요청/응답 형태는 지금 이 저장소 안에서 검증할 방법이 없다 — AI 서버는
 * 별도 파이썬 프로젝트라 실제로 맞춰보기 전까지는 가정값이다. 파트 이름
 * (reviewImage, compareImage)과 응답 필드(similarity)는 AI 서버 쪽 구현에
 * 맞춰 나중에 바뀔 수 있다.
 */
@Component
public class ImageSimilarityClient {

    private final RestClient restClient;
    private final ReviewTicketProperties properties;

    public ImageSimilarityClient(ReviewTicketProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = (int) properties.ai().timeout().toMillis();
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);

        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /** @return 0~1 사이의 유사도. AI 서버가 안 떠 있거나 타임아웃이면 ServiceUnavailableException 을 던진다. */
    public double measureSimilarity(byte[] reviewImage, byte[] compareImage) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("reviewImage", namedResource(reviewImage, "review.jpg"));
        body.add("compareImage", namedResource(compareImage, "compare.jpg"));

        try {
            SimilarityResponse response = restClient.post()
                    .uri(properties.ai().serverUrl())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(SimilarityResponse.class);

            if (response == null) {
                throw new ServiceUnavailableException("AI_SERVER_UNAVAILABLE", "AI 서버 응답이 비어 있습니다", null);
            }
            return response.similarity();
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("AI_SERVER_UNAVAILABLE",
                    "AI 서버가 응답하지 않거나 시간을 초과했습니다", e);
        }
    }

    private static ByteArrayResource namedResource(byte[] bytes, String filename) {
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private record SimilarityResponse(double similarity) {
    }
}
