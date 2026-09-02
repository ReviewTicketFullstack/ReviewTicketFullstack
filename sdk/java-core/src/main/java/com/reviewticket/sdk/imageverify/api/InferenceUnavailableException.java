package com.reviewticket.sdk.imageverify.api;

/**
 * 추론 백엔드가 응답하지 않았다. 연결 거부, 타임아웃, 5xx, 해석할 수 없는 응답.
 *
 * <p>부르는 쪽 잘못도 이미지 잘못도 아니다. 재시도는 SDK 가 하지 않는다 —
 * 재시도할지, 몇 번 할지는 호출자가 정할 정책이다.
 */
public class InferenceUnavailableException extends ImageVerifyException {

    public InferenceUnavailableException(String message) {
        super(message);
    }

    public InferenceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
