package com.reviewticket.server.domain;

/**
 * 거부 사유. 원인이 서로 완전히 달라서 섞어 세면 안 된다 —
 * not_food 가 많으면 사용자가 엉뚱한 사진을 올리는 것이고,
 * menu_mismatch 가 많으면 모델이 메뉴를 헷갈리는 것이다. 고칠 곳이 다르다.
 */
public enum RejectionReason {
    /** P(non_food) >= tau */
    not_food,
    /** 음식 5개 중 argmax 가 주문 메뉴와 다름 */
    menu_mismatch,
    /** 해시가 같은 사진을 이미 올림. AI 를 부르기 전에 걸린다 */
    duplicate
}
