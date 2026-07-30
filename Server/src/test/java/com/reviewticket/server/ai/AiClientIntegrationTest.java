package com.reviewticket.server.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.reviewticket.server.domain.RejectionReason;

/**
 * 진짜 FastAPI 를 호출한다. 추론 서버가 떠 있어야 한다:
 *
 *   cd C:\dev\ReviewTicket\AI_Model
 *   .venv-serve\Scripts\python.exe -m uvicorn src.serve:app --port 8000
 *
 * 서버가 없으면 실패가 아니라 건너뛴다 (assumeTrue).
 */
@SpringBootTest
// 실서비스 DB 를 비우지 않도록 reviewticket_test 로 붙는다
@ActiveProfiles({ "local", "test" })
class AiClientIntegrationTest {

    /** 학습에 쓰지 않은 test 셋 사진. */
    private static final Path DATASET = Path.of("..", "..", "AI_Model", "dataset", "test");

    @Autowired
    private AiClient aiClient;

    @Autowired
    private DecisionRule decisionRule;

    private byte[] firstImage(String className) throws IOException {
        try (var files = Files.list(DATASET.resolve(className))) {
            Path first = files.filter(Files::isRegularFile).sorted().findFirst().orElseThrow();
            return Files.readAllBytes(first);
        }
    }

    @Test
    @DisplayName("실제 피자 사진 -> 확률 6개 -> pizza 주문이면 승인")
    void realPizzaPhotoIsApproved() throws IOException {
        assumeTrue(aiClient.isHealthy(), "FastAPI(:8000) 가 떠 있지 않아 건너뜀");

        Map<String, Double> probs = aiClient.predict(firstImage("pizza"), "pizza.jpg");

        assertThat(probs).hasSize(6)
                .containsKeys("bibimbap", "chicken_wings", "hamburger", "non_food", "pizza", "ramen");
        assertThat(probs.values().stream().mapToDouble(Double::doubleValue).sum())
                .isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.01));

        Decision d = decisionRule.decide(probs, "pizza");
        assertThat(d.approved()).isTrue();
        assertThat(d.predicted()).isEqualTo("pizza");
    }

    @Test
    @DisplayName("실제 피자 사진 + ramen 주문 -> 메뉴 불일치로 거부")
    void realPizzaPhotoRejectedWhenRamenOrdered() throws IOException {
        assumeTrue(aiClient.isHealthy(), "FastAPI(:8000) 가 떠 있지 않아 건너뜀");

        Map<String, Double> probs = aiClient.predict(firstImage("pizza"), "pizza.jpg");
        Decision d = decisionRule.decide(probs, "ramen");

        assertThat(d.approved()).isFalse();
        assertThat(d.reason()).isEqualTo(RejectionReason.menu_mismatch);
    }

    @Test
    @DisplayName("건물 사진 -> 음식 아님으로 거부")
    void realNonFoodPhotoIsRejected() throws IOException {
        assumeTrue(aiClient.isHealthy(), "FastAPI(:8000) 가 떠 있지 않아 건너뜀");

        Map<String, Double> probs = aiClient.predict(firstImage("non_food"), "building.jpg");
        Decision d = decisionRule.decide(probs, "pizza");

        assertThat(d.approved()).isFalse();
        assertThat(d.reason()).isEqualTo(RejectionReason.not_food);
        assertThat(probs.get("non_food")).isGreaterThanOrEqualTo(decisionRule.tau());
    }

    @Test
    @DisplayName("추론 서버가 없으면 거부가 아니라 AiUnavailableException")
    void unreachableServerIsNotARejection() {
        AiClient dead = new AiClient(new com.reviewticket.server.config.ReviewTicketProperties(
                "./Uploaded_pictures", "./failure-log", "./demo",
                // 아무것도 듣고 있지 않은 포트
                new com.reviewticket.server.config.ReviewTicketProperties.Ai("http://127.0.0.1:59999", 0.10),
                new com.reviewticket.server.config.ReviewTicketProperties.Duplicate(8)));

        assertThat(dead.isHealthy()).isFalse();

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> dead.predict(new byte[] { 1, 2, 3 }, "x.jpg"))
                .isInstanceOf(AiUnavailableException.class);
    }
}
