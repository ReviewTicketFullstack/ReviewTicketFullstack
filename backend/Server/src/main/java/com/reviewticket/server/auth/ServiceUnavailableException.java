package com.reviewticket.server.auth;

/**
 * 이쪽 잘못도 요청 보낸 쪽 잘못도 아닌, 의존하는 외부 서비스의 장애. 503 으로 나간다.
 *
 * 지금은 AI 서버(이미지 유사도 판정)가 꺼져 있거나 응답 시간을 넘긴 경우에만 쓴다.
 */
public class ServiceUnavailableException extends RuntimeException {

    private final String errorCode;

    public ServiceUnavailableException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
