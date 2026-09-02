package com.reviewticket.sdk.imageverify.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VerifierConfigTest {

    @Test
    @DisplayName("기본값이 현행 애플리케이션 설정과 같다")
    void defaultsMatchCurrentBehaviour() {
        VerifierConfig config = VerifierConfig.defaults();

        assertAll(
                () -> assertEquals(0.80, config.matchThreshold()),
                () -> assertEquals(5, config.parallelism()));
    }

    @Test
    @DisplayName("범위 밖 설정은 만드는 시점에 거절된다")
    void invalidConfigurationIsRejectedEagerly() {
        assertAll(
                () -> assertThrows(ConfigurationException.class,
                        () -> VerifierConfig.withThreshold(1.5), "임계값이 1 초과"),
                () -> assertThrows(ConfigurationException.class,
                        () -> VerifierConfig.withThreshold(-2.0), "임계값이 -1 미만"),
                () -> assertThrows(ConfigurationException.class,
                        () -> VerifierConfig.withThreshold(Double.NaN), "임계값이 NaN"),
                () -> assertThrows(ConfigurationException.class,
                        () -> new VerifierConfig(0.8, 0, 5), "parallelism 이 0"),
                () -> assertThrows(ConfigurationException.class,
                        () -> new VerifierConfig(0.8, 5, 0), "maxReferences 가 0"));
    }

    @Test
    @DisplayName("경계값은 허용된다")
    void boundariesAreAccepted() {
        assertAll(
                () -> assertEquals(1.0, VerifierConfig.withThreshold(1.0).matchThreshold()),
                () -> assertEquals(-1.0, VerifierConfig.withThreshold(-1.0).matchThreshold()));
    }
}
