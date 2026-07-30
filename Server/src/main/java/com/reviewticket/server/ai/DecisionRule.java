package com.reviewticket.server.ai;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.reviewticket.server.config.ReviewTicketProperties;
import com.reviewticket.server.domain.RejectionReason;

/**
 * AI_Model/src/predict.py 의 decide() 를 그대로 옮긴 것.
 *
 *   P(non_food) >= tau                -> 거부, "음식 아님"
 *   argmax(음식 5개) != 주문 메뉴       -> 거부, "메뉴 불일치"
 *   그 외                              -> 승인
 *
 * 일부러 하지 않는 것 둘 — 원본 주석과 같은 이유다.
 *
 * 1. 최대 확률(confidence)에 임계값을 걸지 않는다. 6클래스 softmax 에서 최대
 *    확률은 두 이유로 낮아진다: 음식이 아니거나, 음식들끼리 헷갈리거나.
 *    ramen 0.45 / bibimbap 0.40 / non_food 0.02 는 "음식인 건 확실, 어느
 *    음식인지 모름"인데 최대 확률 게이트를 걸면 정상 사용자가 티켓을 잃는다.
 *    음식 여부는 P(non_food) 하나로만 판단한다.
 *
 * 2. 메뉴 argmax 에 non_food 를 넣지 않는다. tau 미만이라 통과한 non_food 가
 *    argmax 를 이겨버리면 메뉴 판정이 무너진다.
 *
 * FastAPI 가 이름 붙은 Map 으로 돌려주므로 인덱스 순서를 다룰 일이 없다.
 * (가중치에 박힌 인덱스 순서와 어긋날 위험이 여기서는 생기지 않는다.)
 */
@Component
public class DecisionRule {

    public static final String NON_FOOD = "non_food";

    /** 주문 가능한 음식 5종. non_food 는 클래스일 뿐 메뉴가 아니라 여기 없다. */
    public static final List<String> FOOD_CLASSES =
            List.of("bibimbap", "chicken_wings", "hamburger", "pizza", "ramen");

    private final double tau;

    public DecisionRule(ReviewTicketProperties properties) {
        this.tau = properties.ai().tau();
    }

    public double tau() {
        return tau;
    }

    /**
     * @param probs       FastAPI 가 준 확률 6개
     * @param orderedMenu 주문한 메뉴의 AI 라벨. null 이면 메뉴 검사를 건너뛰고
     *                    '음식인가'만 본다
     */
    public Decision decide(Map<String, Double> probs, String orderedMenu) {
        Double pNonFoodValue = probs.get(NON_FOOD);
        if (pNonFoodValue == null) {
            throw new AiUnavailableException("확률에 " + NON_FOOD + " 가 없다: " + probs.keySet(), null);
        }
        double pNonFood = pNonFoodValue;

        if (pNonFood >= tau) {
            return new Decision(false, RejectionReason.not_food, NON_FOOD, pNonFood);
        }

        String predicted = argmaxAmongFood(probs);

        if (orderedMenu != null && !predicted.equals(orderedMenu)) {
            return new Decision(false, RejectionReason.menu_mismatch, predicted, pNonFood);
        }

        return new Decision(true, null, predicted, pNonFood);
    }

    /** 음식 5개 중에서만 최댓값을 고른다. */
    private String argmaxAmongFood(Map<String, Double> probs) {
        String best = null;
        double bestValue = Double.NEGATIVE_INFINITY;

        for (String food : FOOD_CLASSES) {
            Double value = probs.get(food);
            if (value == null) {
                throw new AiUnavailableException("확률에 " + food + " 가 없다: " + probs.keySet(), null);
            }
            if (value > bestValue) {
                bestValue = value;
                best = food;
            }
        }
        return best;
    }
}
