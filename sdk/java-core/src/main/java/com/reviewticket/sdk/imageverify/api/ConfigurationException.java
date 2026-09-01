package com.reviewticket.sdk.imageverify.api;

/**
 * 설정값이 유효 범위를 벗어났다.
 *
 * <p>객체를 만드는 시점에 던진다. 잘못된 임계값으로 조용히 이상하게 도는 것보다
 * 기동 자체가 실패하는 편이 낫다.
 */
public class ConfigurationException extends ImageVerifyException {

    public ConfigurationException(String message) {
        super(message);
    }
}
