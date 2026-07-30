package com.reviewticket.server.ai;

import com.reviewticket.server.domain.RejectionReason;

/**
 * 판정 결과.
 *
 * @param approved  승인 여부
 * @param reason    거부 사유. 승인이면 null
 * @param predicted 음식 5개 중 argmax. 음식 아님으로 걸리면 "non_food"
 * @param pNonFood  P(non_food). 판정에 실제로 쓴 값
 */
public record Decision(boolean approved, RejectionReason reason, String predicted, double pNonFood) {

    /** 사용자에게 보여줄 문구 (FE-4.6 / FE-4.8). */
    public String message() {
        if (approved) {
            return "리뷰가 등록되었습니다";
        }
        return switch (reason) {
            case not_food -> "음식 사진이 아닙니다";
            case menu_mismatch -> "주문한 메뉴와 사진이 일치하지 않습니다";
            case duplicate -> "이미 등록한 사진입니다";
        };
    }
}
