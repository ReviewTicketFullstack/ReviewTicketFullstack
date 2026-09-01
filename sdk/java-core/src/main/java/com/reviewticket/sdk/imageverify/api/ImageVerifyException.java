package com.reviewticket.sdk.imageverify.api;

/**
 * SDK 가 던지는 모든 예외의 뿌리.
 *
 * <p>부르는 쪽은 이 타입 하나만 잡아도 SDK 에서 나온 실패를 전부 처리할 수 있다.
 * 애플리케이션 예외로 번역하는 자리가 딱 한 곳이 되게 하려는 의도다.
 *
 * <p>SDK 는 애플리케이션 예외를 절대 던지지 않는다(ARCH-R6). 이름조차 알지 못한다.
 */
public abstract class ImageVerifyException extends RuntimeException {

    protected ImageVerifyException(String message) {
        super(message);
    }

    protected ImageVerifyException(String message, Throwable cause) {
        super(message, cause);
    }
}
