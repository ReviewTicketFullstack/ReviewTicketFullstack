package com.reviewticket.server.auth;

/**
 * 이미 쓰이고 있는 이메일이나 닉네임처럼, 요청 형식은 맞지만 현재 상태와
 * 충돌해 처리할 수 없는 경우. 409 로 나간다.
 *
 * 400(형식 오류)과 구분하는 이유 — 프론트가 다르게 반응해야 한다.
 * 형식 오류는 입력을 고치라고 하고, 충돌은 다른 값을 쓰라고 안내한다.
 */
public class ConflictException extends RuntimeException {

    private final String errorCode;

    /**
     * errorCode 는 필수다 — 응답에 나가는 것은 이 코드뿐이고, 문구는 화면이
     * 채운다. message 는 서버 로그와 스택트레이스에서 읽으려고 남긴다.
     */
    public ConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
