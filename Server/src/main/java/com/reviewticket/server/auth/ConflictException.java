package com.reviewticket.server.auth;

/**
 * 이미 쓰이고 있는 이메일이나 닉네임처럼, 요청 형식은 맞지만 현재 상태와
 * 충돌해 처리할 수 없는 경우. 409 로 나간다.
 *
 * 400(형식 오류)과 구분하는 이유 — 프론트가 다르게 반응해야 한다.
 * 형식 오류는 입력을 고치라고 하고, 충돌은 다른 값을 쓰라고 안내한다.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
