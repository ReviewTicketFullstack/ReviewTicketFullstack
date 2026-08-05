package com.reviewticket.server.auth;

/**
 * 요청 빈도 제한(rate limit)에 걸림. 429 로 나간다.
 *
 * 남은 차단 시간을 함께 담는다 — 화면이 "3:20 후 다시 시도해 주세요"처럼
 * 남은 시간을 세어 보여주고 그동안 로그인·가입 버튼을 막는 데 쓴다.
 * 이 값이 없으면 "잠시 후"라는 막연한 안내밖에 할 수 없다.
 */
public class TooManyRequestsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
