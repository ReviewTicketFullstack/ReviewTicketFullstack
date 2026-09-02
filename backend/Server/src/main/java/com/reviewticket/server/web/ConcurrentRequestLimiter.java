package com.reviewticket.server.web;

import java.util.concurrent.Semaphore;

import org.springframework.stereotype.Component;

/**
 * 동시에 처리 중인 요청 수 상한.
 *
 * {@link RequestRateLimiter} 가 출발지별로 보는 것과 달리 전체를 본다. 여러
 * 곳에서 나눠 들어와 범인을 특정할 수 없는 경우를 맡는다.
 *
 * 상한을 {@value #MAX_IN_FLIGHT} 로 둔 이유 — 톰캣 작업 스레드가 100개다
 * (application.yml 의 server.tomcat.threads.max). 그보다 낮게 잡아야 거절
 * 응답을 만들어 보낼 여유가 남는다. 상한을 100 이상으로 두면 톰캣 큐가 먼저
 * 차서 이 장치가 개입할 틈이 없다.
 *
 * 넘친다고 전체를 일정 시간 닫지 않는다. 공격자가 노리는 결과가 정확히
 * 그것이라, 짧게 한 번 때려 오래 닫아두는 증폭 수단이 된다. 감당할 수 있는
 * 만큼은 계속 받고 넘치는 만큼만 거절해야 하며, 그래야 부하가 걷히는 즉시
 * 회복한다. 차단 상태를 두지 않는 것도 같은 이유다.
 */
@Component
public class ConcurrentRequestLimiter {

    private static final int MAX_IN_FLIGHT = 80;

    private final Semaphore slots = new Semaphore(MAX_IN_FLIGHT);

    /** 자리를 하나 잡는다. 성공하면 반드시 {@link #release()} 로 돌려줘야 한다. */
    public boolean tryAcquire() {
        return slots.tryAcquire();
    }

    public void release() {
        slots.release();
    }
}
