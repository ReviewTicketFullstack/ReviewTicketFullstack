package com.reviewticket.server.auth;

/**
 * 번호로 찾는 리소스가 없는 경우. 404 로 나간다.
 *
 * ValidationException(400)과 구분하는 이유 — 400은 "보낸 값의 형식이
 * 틀렸다"는 뜻이고, 404는 "형식은 맞는데 그 번호로 된 게 없다"는 뜻이다.
 * REST 관례상 존재하지 않는 리소스는 404 가 맞다.
 */
public class NotFoundException extends RuntimeException {

    private final String errorCode;

    public NotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
