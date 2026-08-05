package com.reviewticket.server.web;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * IP 별 요청 빈도 제한 (토큰 버킷).
 *
 * 버킷에 토큰이 초당 {@value #REFILL_PER_SECOND} 개씩 채워지고 최대
 * {@value #BUCKET_CAPACITY} 개까지 담긴다. 요청 하나가 토큰 하나를 쓰고,
 * 바닥나면 그 IP 를 {@link #BLOCK} 동안 막는다.
 *
 * 고정 시간 창(예: 10초에 N회)을 쓰지 않는 이유 — 창이 바뀌는 순간 카운터가
 * 초기화되므로, 경계에 걸쳐 쏘면 짧은 순간에 두 배가 통과한다. 토큰 버킷은
 * 창 개념이 없어 매 요청마다 그 시점의 잔량만 본다.
 *
 * 버킷을 보충 속도보다 크게 잡은 이유 — 화면 하나가 API 를 여러 개 동시에
 * 부르는 것은 정상이다. 그런 순간 몰림은 통과시키고, 지속적인 폭주만 묶는다.
 *
 * 로그인 실패 횟수를 세는 {@link com.reviewticket.server.auth.LoginRateLimiter}
 * 와는 목적이 다르다. 저쪽은 비밀번호 무차별 대입을 막고, 이쪽은 요청량 자체를
 * 막는다. 그래서 이쪽 차단은 짧다 — 오탐이 나도 스스로 풀려야 한다.
 */
@Component
public class RequestRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RequestRateLimiter.class);

    /** 초당 보충되는 토큰 수. 지속 가능한 요청 속도가 된다. */
    private static final double REFILL_PER_SECOND = 40;

    /** 버킷 최대 크기. 한 번에 몰아서 보낼 수 있는 양이다. */
    private static final double BUCKET_CAPACITY = 160;

    /** 토큰이 바닥났을 때의 차단 시간. 짧게 둬야 오탐이 스스로 풀린다. */
    private static final Duration BLOCK = Duration.ofMinutes(1);

    /**
     * 추적할 IP 수 상한.
     *
     * 상한이 없으면 출발지를 바꿔가며 보내 이 표를 부풀리는 것 자체가 새로운
     * 공격이 된다. 상한에 닿으면 새 IP 는 추적하지 않고 통과시킨다 — 막아버리면
     * 정상 사용자를 스스로 차단하게 되고, 그 상황은 전역 동시 처리 수 제한
     * ({@link ConcurrentRequestLimiter})이 받는다.
     */
    private static final int MAX_TRACKED_IPS = 10_000;

    /** 이 시간 동안 요청이 없는 IP 는 표에서 지운다. */
    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(10);

    private static final class Bucket {
        double tokens = BUCKET_CAPACITY;
        long lastRefillNanos = System.nanoTime();
        long blockedUntilNanos;
        long lastSeenNanos = System.nanoTime();
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * 요청 하나를 처리해도 되는지 판단하고, 되면 토큰을 하나 쓴다.
     *
     * @return 통과면 0, 막히면 남은 차단 시간(초). 그대로 Retry-After 에 쓴다
     */
    public long retryAfterSeconds(String ip) {
        Bucket bucket = buckets.get(ip);

        if (bucket == null) {
            // 표가 가득 찼으면 새 IP 는 추적하지 않는다. 위 MAX_TRACKED_IPS 주석 참고.
            if (buckets.size() >= MAX_TRACKED_IPS) {
                log.warn("추적 중인 IP 가 상한({})에 도달해 새 출발지는 제한하지 않는다", MAX_TRACKED_IPS);
                return 0;
            }
            bucket = buckets.computeIfAbsent(ip, key -> new Bucket());
        }

        synchronized (bucket) {
            long now = System.nanoTime();
            bucket.lastSeenNanos = now;

            if (bucket.blockedUntilNanos > now) {
                return secondsUntil(bucket.blockedUntilNanos, now);
            }

            refill(bucket, now);

            if (bucket.tokens < 1) {
                bucket.blockedUntilNanos = now + BLOCK.toNanos();
                log.warn("요청 빈도 초과로 차단: ip={} {}초", ip, BLOCK.toSeconds());
                return BLOCK.toSeconds();
            }

            bucket.tokens -= 1;
            return 0;
        }
    }

    /**
     * 지난 시간만큼 토큰을 채운다.
     *
     * 벽시계(Instant) 대신 nanoTime 을 쓰는 이유 — 시각 동기화나 서머타임으로
     * 벽시계가 뒤로 밀리면 경과 시간이 음수가 되어 계산이 깨진다.
     */
    private void refill(Bucket bucket, long now) {
        double elapsedSeconds = (now - bucket.lastRefillNanos) / 1_000_000_000.0;
        if (elapsedSeconds <= 0) {
            return;
        }

        bucket.tokens = Math.min(BUCKET_CAPACITY, bucket.tokens + elapsedSeconds * REFILL_PER_SECOND);
        bucket.lastRefillNanos = now;
    }

    private long secondsUntil(long targetNanos, long now) {
        // 올림한다. 0.2초 남았을 때 "0초 뒤 재시도"라고 알려주면 바로 다시 걸린다.
        return Math.max(1, (targetNanos - now + 999_999_999L) / 1_000_000_000L);
    }

    /** 오랫동안 조용한 IP 를 지운다. 지우지 않으면 표가 계속 자란다. */
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    public void purgeIdleBuckets() {
        long now = System.nanoTime();
        long cutoff = now - IDLE_TIMEOUT.toNanos();

        int before = buckets.size();
        buckets.values().removeIf(bucket -> {
            synchronized (bucket) {
                // 차단 중인 IP 는 남겨둔다. 지우면 차단이 그 자리에서 풀린다.
                return bucket.lastSeenNanos < cutoff && bucket.blockedUntilNanos <= now;
            }
        });

        int removed = before - buckets.size();
        if (removed > 0) {
            log.info("요청 빈도 표 정리 — {}건 삭제, {}건 유지", removed, buckets.size());
        }
    }
}
