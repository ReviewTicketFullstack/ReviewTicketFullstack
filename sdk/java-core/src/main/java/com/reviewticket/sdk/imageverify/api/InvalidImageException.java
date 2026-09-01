package com.reviewticket.sdk.imageverify.api;

/**
 * 추론 백엔드가 이미지를 디코드하지 못했다 (4xx).
 *
 * <p>SDK 는 바이트를 해석하지 않으므로 형식이 잘못됐다는 사실을 스스로 알 수
 * 없다. 이 판단은 전적으로 추론 서버가 알려 준 것이다.
 */
public class InvalidImageException extends ImageVerifyException {

    public InvalidImageException(String message) {
        super(message);
    }

    public InvalidImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
