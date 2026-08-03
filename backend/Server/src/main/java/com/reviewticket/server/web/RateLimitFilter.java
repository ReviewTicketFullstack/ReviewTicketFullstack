package com.reviewticket.server.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 요청량 제한을 실제로 적용하는 자리.
 *
 * 보안 필터보다 앞에 둔다 (RateLimitConfig 에서 순서를 지정한다). 거절되는
 * 요청은 토큰 검증이나 DB 조회에도 도달하지 않아야 한다. 뒤에 두면 막으려던
 * 비용을 이미 치른 뒤가 된다.
 *
 * 두 계층을 차례로 본다.
 *   1. 출발지별 빈도 — 한 곳에서 몰아치는 경우
 *   2. 전체 동시 처리 수 — 여러 곳에서 나눠 들어와 범인을 특정할 수 없는 경우
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final RequestRateLimiter ipLimiter;
    private final ConcurrentRequestLimiter concurrentLimiter;

    public RateLimitFilter(RequestRateLimiter ipLimiter, ConcurrentRequestLimiter concurrentLimiter) {
        this.ipLimiter = ipLimiter;
        this.concurrentLimiter = concurrentLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        // X-Forwarded-For 를 보지 않는다 — 요청자가 마음대로 넣을 수 있어,
        // 신뢰하면 헤더만 바꿔가며 제한을 우회할 수 있다. 앞단에 신뢰할 수 있는
        // 프록시를 두게 되면 그때 그 프록시가 붙인 값만 받도록 바꾼다.
        long retryAfter = ipLimiter.retryAfterSeconds(request.getRemoteAddr());
        if (retryAfter > 0) {
            reject(response, HttpStatus.TOO_MANY_REQUESTS, retryAfter,
                    "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
            return;
        }

        if (!concurrentLimiter.tryAcquire()) {
            reject(response, HttpStatus.SERVICE_UNAVAILABLE, 1,
                    "서버가 혼잡합니다. 잠시 후 다시 시도해 주세요.");
            return;
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // 처리 결과와 무관하게 자리를 돌려준다. 예외로 빠져나가도 마찬가지다.
            concurrentLimiter.release();
        }
    }

    /** 응답 형식을 ApiExceptionHandler 와 같게 맞춘다. 프론트가 message 만 보면 된다. */
    private void reject(HttpServletResponse response, HttpStatus status, long retryAfterSeconds,
            String message) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

        response.getWriter().write("""
                {"error":"%s","message":"%s","retryable":true}"""
                .formatted(status.getReasonPhrase(), message));
    }
}
