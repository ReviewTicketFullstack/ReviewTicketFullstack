package com.reviewticket.server.ai;

import java.time.Duration;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.reviewticket.server.config.ReviewTicketProperties;

/**
 * FastAPI 추론 서버 호출. 확률 6개만 받아온다.
 *
 * 승인/거부 판단은 여기서 하지 않는다 — 주문한 메뉴를 아는 건 Spring 뿐이고,
 * FastAPI 가 판정을 내리면 브라우저가 FastAPI 를 직접 불러 응답을 위조할 수 있다.
 * 판정은 {@link DecisionRule} 이 한다.
 *
 * 호출은 반드시 @Transactional 밖에서 한다. 외부 HTTP 를 트랜잭션 안에 넣으면
 * DB 커넥션을 수백 ms 붙잡는다.
 */
@Component
public class AiClient {

    private final RestClient restClient;

    public AiClient(ReviewTicketProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 타임아웃을 안 걸면 추론 서버가 멎었을 때 요청 스레드가 영원히 묶인다.
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .baseUrl(properties.ai().baseUrl())
                .requestFactory(factory)
                .build();
    }

    /** FastAPI 응답 형태: {"probs": {"pizza": 0.97, ...}} */
    private record PredictResponse(Map<String, Double> probs) {
    }

    /**
     * @param imageBytes 이미 축소된 이미지 바이트
     * @return 클래스 이름 -> 확률 6개
     */
    public Map<String, Double> predict(byte[] imageBytes, String filename) {
        ByteArrayResource part = new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                // 이름이 없으면 FastAPI 의 UploadFile 이 파트를 파일로 인식하지 않는다
                return filename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", part);

        try {
            PredictResponse response = restClient.post()
                    .uri("/predict")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(PredictResponse.class);

            if (response == null || response.probs() == null || response.probs().isEmpty()) {
                throw new AiUnavailableException("추론 서버 응답에 probs 가 없다", null);
            }
            return response.probs();

        } catch (AiUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new AiUnavailableException("추론 서버 호출 실패", e);
        }
    }

    /** 서버가 살아 있는지. 기동 점검용. */
    public boolean isHealthy() {
        try {
            restClient.get().uri("/health").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
