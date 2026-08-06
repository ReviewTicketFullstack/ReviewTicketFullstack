package com.reviewticket.server.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * IP 별 로그인 실패 횟수를 추적해 무차별 대입을 막는다.
 *
 * 계정(이메일) 대신 IP 기준인 이유 — 봇은 이메일을 몰라도 여러 계정을
 * 넘나들며 시도할 수 있고, IP 는 요청 시점에 항상 알 수 있다. 다만 같은
 * 공인 IP 를 여러 사람이 공유하는 환경(NAT, 기숙사망 등)에서는 한 명의
 * 실패가 같은 IP 의 다른 사람까지 막을 수 있다는 트레이드오프가 있다.
 *
 * 서버 재시작 시 초기화된다 — 인스턴스가 하나뿐인 지금 구조에서는
 * 충분하고, DB 나 외부 저장소를 끌어올 정도의 요구는 아니다. 차단을
 * 사실상 영구(10년)로 두기로 했으므로, 특정 IP 를 오탐으로 잘못 막았을
 * 때 되돌리는 유일한 방법은 서버 재시작(전체 차단 목록 초기화)이다 —
 * 이 IP 만 골라서 푸는 기능은 없다.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_FAILURES = 5;

    /** 사실상 영구 차단. 같은 공인 IP 를 공유하는 다른 사람도 함께 막힐 수 있음을 감수한 값. */
    private static final Duration BLOCK_DURATION = Duration.ofDays(3650);

    private static final class Attempt {
        int failures;
        Instant blockedUntil;
    }

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    /** 로그인 시도 전에 부른다. 차단 중이면 남은 시간과 함께 예외를 던진다. */
    public void checkAllowed(String ip) {
        Attempt a = attempts.get(ip);
        if (a == null) {
            return;
        }
        synchronized (a) {
            Instant now = Instant.now();
            if (a.blockedUntil != null && now.isBefore(a.blockedUntil)) {
                long remaining = Duration.between(now, a.blockedUntil).toSeconds();
                throw new TooManyRequestsException(
                        "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요.", remaining);
            }
        }
    }

    /** 인증 실패 직후 부른다. 임계치를 넘기면 차단을 건다. */
    public void recordFailure(String ip) {
        Attempt a = attempts.computeIfAbsent(ip, key -> new Attempt());
        synchronized (a) {
            a.failures++;
            if (a.failures >= MAX_FAILURES) {
                a.blockedUntil = Instant.now().plus(BLOCK_DURATION);
            }
        }
    }

    /** 로그인 성공 시 부른다. 이전 실패 이력을 지운다. */
    public void recordSuccess(String ip) {
        attempts.remove(ip);
    }
}
