package com.reviewticket.server.auth;

/**
 * 역할이 맞지 않아 거부하는 경우. 403 으로 나간다.
 *
 * 401(UnauthorizedException)과 다르다 — 로그인 자체는 됐지만, 그 역할로는
 * 이 기능을 쓸 수 없는 경우다. 예: 사장 계정이 고객 전용 API 를 부르거나
 * 그 반대인 경우.
 */
public class ForbiddenException extends RuntimeException {

    private final String errorCode;

    public ForbiddenException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
