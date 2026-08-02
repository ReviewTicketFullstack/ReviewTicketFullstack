package com.reviewticket.server.domain;

/**
 * 회원 역할. 화면 분기와 API 접근 제어의 기준이 된다.
 *
 * 한 계정이 두 역할을 겸할 수는 없다 — 가입 시 하나를 골라 고정한다.
 * 사장이 손님으로도 주문하고 싶다면 다른 이메일로 가입해야 한다.
 * 겸업을 허용하면 "이 주문의 티켓은 누구 것인가"가 애매해진다.
 */
public enum Role {
    CUSTOMER,
    OWNER
}
