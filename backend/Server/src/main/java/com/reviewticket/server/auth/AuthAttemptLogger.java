package com.reviewticket.server.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.reviewticket.server.domain.AuthAction;

/**
 * 로그인·가입·비밀번호변경·닉네임변경 시도를 IP 와 함께 기록한다.
 *
 * 저장은 {@link AuthAttemptLogWriter} 에 맡긴다 — 별도 트랜잭션에 남겨야
 * 시도한 작업이 롤백돼도 "시도했다"는 기록이 살아남고, 그 분리는 빈 경계를
 * 넘을 때만 성립한다. 저장이 실패해도(DB 순단 등) 예외를 삼켜서 로그 기능
 * 장애가 로그인·가입 같은 주요 기능을 막지 않게 한다.
 */
@Component
public class AuthAttemptLogger {

    private static final Logger log = LoggerFactory.getLogger(AuthAttemptLogger.class);

    private final AuthAttemptLogWriter writer;

    public AuthAttemptLogger(AuthAttemptLogWriter writer) {
        this.writer = writer;
    }

    public void record(AuthAction action, String email, String displayName, String ip, boolean success) {
        try {
            writer.write(action, email, displayName, ip, success);
        } catch (RuntimeException e) {
            log.warn("시도 로그 저장 실패: action={} ip={}", action, ip, e);
        }
    }
}
