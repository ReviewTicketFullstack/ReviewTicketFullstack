package com.reviewticket.server.mail;

/**
 * "이 주소로 비밀번호 재설정 메일을 보내야 한다"는 사실만 담은 이벤트.
 *
 * 트랜잭션 커밋 이후에 보내기 위함이다. 서비스 안에서 곧바로 보내면 그 뒤
 * 커밋이 실패했을 때 DB 에 없는 토큰의 링크가 이미 도착한다.
 */
public record PasswordResetMailRequested(String email, String token) {
}
