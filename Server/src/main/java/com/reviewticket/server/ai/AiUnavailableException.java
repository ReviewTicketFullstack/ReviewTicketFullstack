package com.reviewticket.server.ai;

/**
 * 추론 서버에 닿지 못했거나 응답이 깨졌을 때.
 *
 * 이걸 '거부'로 처리하면 안 된다 — 우리 서버 잘못으로 정상 사용자가 티켓을
 * 잃는다. 재시도 가능한 503 으로 내려 사용자가 다시 올리게 한다.
 */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
