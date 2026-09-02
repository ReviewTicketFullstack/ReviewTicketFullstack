package com.reviewticket.server.auth;

/**
 * 로그인 실패, 기존 비밀번호 불일치 등. 401 로 나간다.
 *
 * 메시지는 항상 뭉뚱그린다 — "이메일이 없습니다" 와 "비밀번호가 틀렸습니다"
 * 를 구분해 알려주면 어떤 이메일이 가입돼 있는지 알아낼 수 있다.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
