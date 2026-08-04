package com.reviewticket.server.auth;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.reviewticket.server.domain.AuthAction;
import com.reviewticket.server.domain.AuthAttemptLog;
import com.reviewticket.server.repository.AuthAttemptLogRepository;

/**
 * 시도 로그를 호출 쪽과 분리된 트랜잭션에 저장한다.
 *
 * {@link AuthAttemptLogger} 와 클래스를 나눈 이유 — 같은 클래스 안에서
 * 자기 메서드를 부르면 스프링 프록시를 거치지 않아 @Transactional 이 통째로
 * 무시된다. 빈 경계를 넘어야 REQUIRES_NEW 가 실제로 적용된다.
 *
 * 메서드를 public 으로 두는 이유도 같다 — 트랜잭션 애노테이션은 기본 설정에서
 * public 메서드만 대상으로 하므로, 접근 범위를 좁히면 조용히 무시된다.
 */
@Component
public class AuthAttemptLogWriter {

    private final AuthAttemptLogRepository logs;

    public AuthAttemptLogWriter(AuthAttemptLogRepository logs) {
        this.logs = logs;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(AuthAction action, String email, String displayName, String ip, boolean success) {
        logs.save(new AuthAttemptLog(action, email, displayName, ip, success));
    }
}
