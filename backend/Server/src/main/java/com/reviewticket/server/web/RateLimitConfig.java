package com.reviewticket.server.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 요청량 제한 필터 등록.
 *
 * {@link RateLimitFilter} 에 @Component 를 붙이지 않고 여기서 직접 등록하는
 * 이유 — 붙이면 Spring Boot 가 모든 경로에 자동 등록해, 아래에서 지정한 경로와
 * 순서가 무시된 채 한 번 더 적용된다.
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
            RequestRateLimiter ipLimiter, ConcurrentRequestLimiter concurrentLimiter) {

        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>(new RateLimitFilter(ipLimiter, concurrentLimiter));

        // API 만 건다. 비용이 큰 것은 DB·해싱·메일이 붙은 이쪽이고,
        // 정적 파일까지 세면 화면 한 번 여는 것만으로 토큰이 크게 줄어든다.
        registration.addUrlPatterns("/api/*");

        // 보안 필터를 포함해 무엇보다 먼저 실행한다. 거절할 요청에 인증 처리
        // 비용을 쓰지 않기 위해서다.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return registration;
    }
}
