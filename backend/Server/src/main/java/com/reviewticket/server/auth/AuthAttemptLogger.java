package com.reviewticket.server.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.reviewticket.server.domain.AuthAction;
import com.reviewticket.server.domain.AuthAttemptLog;
import com.reviewticket.server.repository.AuthAttemptLogRepository;

/**
 * 로그인·가입·비밀번호변경·닉네임변경 시도를 IP 와 함께 기록한다.
 *
 * REQUIRES_NEW 로 별도 트랜잭션에 저장한다 — 시도한 작업 자체가 실패해서
 * 호출 쪽 트랜잭션이 롤백돼도 "시도했다"는 기록은 남아야 한다.
 * 저장이 실패해도(DB 순단 등) 예외를 삼켜서 로그 기능 장애가
 * 로그인·가입 같은 주요 기능을 막지 않게 한다.
 */
@Component
public class AuthAttemptLogger {

    private static final Logger log = LoggerFactory.getLogger(AuthAttemptLogger.class);

    private final AuthAttemptLogRepository logs;

    public AuthAttemptLogger(AuthAttemptLogRepository logs) {
        this.logs = logs;
    }

    public void record(AuthAction action, String email, String displayName, String ip, boolean success) {
        try {
            save(action, email, displayName, ip, success);
        } catch (RuntimeException e) {
            log.warn("시도 로그 저장 실패: action={} ip={}", action, ip, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void save(AuthAction action, String email, String displayName, String ip, boolean success) {
        logs.save(new AuthAttemptLog(action, email, displayName, ip, success));
    }
}
