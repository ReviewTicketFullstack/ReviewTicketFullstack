package com.reviewticket.server.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * IP 별 가입/재발송 요청 빈도를 제한한다.
 *
 * 로그인 제한과 목적이 다르다 — 크리덴셜 탈취가 아니라 스팸성 남용(존재
 * 하지 않는 이메일로 인증메일 낭비, 스캐너의 무작위 가입 시도) 방지가
 * 목적이라 차단을 짧게 둔다. 같은 IP 를 여러 사람이 공유하는 환경에서
 * 여러 명이 동시에 가입을 시도하는 것도 자연스러운 상황이라, 오래
 * 막으면 무고한 사람들이 함께 오래 묶인다.
 *
 * 1분에 5회 초과 시 10분 차단 — 사람이 손으로 가입폼을 1분에 5번 넘게
 * 제출하는 일은 거의 없고, 자동화된 요청은 보통 이보다 빠르다.
 */
@Component
public class SignupRateLimiter {

    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(10);

    private static final class Window {
        Instant windowStart;
        int count;
        Instant blockedUntil;
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /** 요청 처리 전에 부른다. 허용 범위를 넘으면 예외를 던진다. */
    public void checkAllowed(String ip) {
        Window w = windows.computeIfAbsent(ip, key -> new Window());
        synchronized (w) {
            Instant now = Instant.now();

            if (w.blockedUntil != null) {
                if (now.isBefore(w.blockedUntil)) {
                    throw new TooManyRequestsException("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
                }
                // 차단 기간이 지났다 — 새로 시작.
                w.blockedUntil = null;
                w.windowStart = null;
                w.count = 0;
            }

            if (w.windowStart == null || Duration.between(w.windowStart, now).compareTo(WINDOW) > 0) {
                w.windowStart = now;
                w.count = 1;
                return;
            }

            w.count++;
            if (w.count > MAX_REQUESTS) {
                w.blockedUntil = now.plus(BLOCK_DURATION);
                throw new TooManyRequestsException("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
            }
        }
    }
}
