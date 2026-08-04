package com.reviewticket.server.auth;

/**
 * 입력 형식 오류. 400 으로 나간다.
 *
 * errorCode 로 프론트가 어떤 조건에 걸렸는지 문자열 비교 없이 분기할 수 있다.
 * message 는 사람이 읽는 문구라 바뀔 수 있지만, errorCode 는 고정값이라
 * 문구가 바뀌어도 프론트 분기 로직이 깨지지 않는다.
 */
public class ValidationException extends IllegalArgumentException {

    private final String errorCode;

    public ValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
