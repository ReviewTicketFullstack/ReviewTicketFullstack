package com.reviewticket.server.auth;

/** 요청 빈도 제한(rate limit)에 걸림. 429 로 나간다. */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
