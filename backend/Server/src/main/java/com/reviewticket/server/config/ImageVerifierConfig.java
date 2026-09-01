package com.reviewticket.server.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.reviewticket.sdk.imageverify.api.ImageVerifier;
import com.reviewticket.sdk.imageverify.api.ImageVerifiers;
import com.reviewticket.sdk.imageverify.api.VerifierConfig;

/**
 * 이미지 검증 SDK 를 애플리케이션 설정으로 조립한다.
 *
 * <p>이 클래스는 Phase 4 에서 Spring Boot Starter 로 대체될 자리다. 그때까지는
 * 애플리케이션이 직접 조립한다 — SDK 쪽에 Spring 을 넣지 않기 위해서다(ARCH-R1).
 *
 * <p>{@code core}·{@code http} 패키지를 직접 import 하지 않는다. 그건 SDK 내부라
 * 예고 없이 바뀔 수 있고, 애플리케이션이 거기 의존하면 가드레일 GR-05 가 막는다.
 * 조립은 {@link ImageVerifiers} 를 통한다.
 */
@Configuration
public class ImageVerifierConfig {

    /**
     * 돌려주는 객체는 스레드풀과 HTTP 연결을 들고 있다. AutoCloseable 이라
     * Spring 이 컨텍스트를 닫을 때 close() 를 알아서 부른다 — 예전에
     * ReviewService 가 들고 있던 @PreDestroy 가 이걸로 대체됐다.
     */
    @Bean
    ImageVerifier imageVerifier(ReviewTicketProperties properties) {
        return ImageVerifiers.pairwiseOverHttp(
                URI.create(properties.ai().serverUrl()),
                properties.ai().timeout(),
                VerifierConfig.withThreshold(properties.ai().matchThreshold()));
    }
}
