package com.reviewticket.server.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.reviewticket.server.config.ReviewTicketProperties;
import com.reviewticket.server.domain.RejectionReason;

/** 판정 규칙만 검증한다. DB 도 HTTP 도 필요 없다. */
class DecisionRuleTest {

    private final DecisionRule rule = new DecisionRule(
            new ReviewTicketProperties("./Uploaded_pictures", "./failure-log", "./demo",
                    new ReviewTicketProperties.Ai("http://localhost:8000", 0.10),
                    new ReviewTicketProperties.Duplicate(8)));

    private static Map<String, Double> probs(double bibimbap, double chickenWings,
            double hamburger, double nonFood, double pizza, double ramen) {
        return Map.of(
                "bibimbap", bibimbap,
                "chicken_wings", chickenWings,
                "hamburger", hamburger,
                "non_food", nonFood,
                "pizza", pizza,
                "ramen", ramen);
    }

    @Test
    @DisplayName("주문 메뉴와 일치하면 승인")
    void approvesMatchingMenu() {
        Decision d = rule.decide(probs(0.001, 0.002, 0.001, 0.00005, 0.974, 0.021), "pizza");

        assertThat(d.approved()).isTrue();
        assertThat(d.predicted()).isEqualTo("pizza");
        assertThat(d.reason()).isNull();
    }

    @Test
    @DisplayName("P(non_food) >= tau 이면 '음식 아님'으로 거부")
    void rejectsNonFood() {
        Decision d = rule.decide(probs(0.0007, 0.00004, 0.00007, 0.998, 0.001, 0.00007), "pizza");

        assertThat(d.approved()).isFalse();
        assertThat(d.reason()).isEqualTo(RejectionReason.not_food);
        assertThat(d.message()).isEqualTo("음식 사진이 아닙니다");
    }

    @Test
    @DisplayName("음식은 맞지만 주문 메뉴와 다르면 '메뉴 불일치'로 거부")
    void rejectsWrongMenu() {
        Decision d = rule.decide(probs(0.001, 0.002, 0.001, 0.00005, 0.974, 0.021), "ramen");

        assertThat(d.approved()).isFalse();
        assertThat(d.reason()).isEqualTo(RejectionReason.menu_mismatch);
        assertThat(d.predicted()).isEqualTo("pizza");
        assertThat(d.message()).isEqualTo("주문한 메뉴와 사진이 일치하지 않습니다");
    }

    @Test
    @DisplayName("음식끼리 헷갈려 최대 확률이 낮아도 음식으로 통과한다 — 최대 확률에 임계값을 걸지 않는 이유")
    void lowConfidenceStillPassesFoodGate() {
        // ramen 0.45 / bibimbap 0.40 : "음식인 건 확실, 어느 음식인지 애매"
        // 최대 확률 게이트였다면 여기서 정상 사용자가 티켓을 잃는다.
        Decision d = rule.decide(probs(0.40, 0.05, 0.05, 0.02, 0.03, 0.45), "ramen");

        assertThat(d.approved()).isTrue();
        assertThat(d.predicted()).isEqualTo("ramen");
    }

    @Test
    @DisplayName("tau 미만인 non_food 는 메뉴 argmax 에 끼어들지 못한다")
    void nonFoodNeverWinsMenuArgmax() {
        // non_food 0.09 가 어떤 음식보다도 크지만 tau(0.10) 미만이라 통과한다.
        // argmax 에 non_food 를 넣었다면 predicted 가 non_food 가 되어 판정이 무너진다.
        Decision d = rule.decide(probs(0.02, 0.01, 0.08, 0.09, 0.03, 0.77), "ramen");

        assertThat(d.approved()).isTrue();
        assertThat(d.predicted()).isEqualTo("ramen");
    }

    @Test
    @DisplayName("tau 경계값은 거부 쪽 (>= 이지 > 가 아니다)")
    void tauBoundaryRejects() {
        Decision d = rule.decide(probs(0.30, 0.10, 0.10, 0.10, 0.20, 0.20), "bibimbap");

        assertThat(d.approved()).isFalse();
        assertThat(d.reason()).isEqualTo(RejectionReason.not_food);
    }

    @Test
    @DisplayName("주문 메뉴가 null 이면 '음식인가'만 본다")
    void skipsMenuCheckWhenNoOrder() {
        Decision d = rule.decide(probs(0.001, 0.002, 0.001, 0.00005, 0.974, 0.021), null);

        assertThat(d.approved()).isTrue();
        assertThat(d.predicted()).isEqualTo("pizza");
    }
}
