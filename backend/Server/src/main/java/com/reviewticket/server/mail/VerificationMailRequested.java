package com.reviewticket.server.mail;

/**
 * "이 주소로 인증 메일을 보내야 한다"는 사실만 담은 이벤트.
 *
 * 왜 이벤트로 돌리나 — 발송을 트랜잭션 커밋 이후로 미루기 위함이다.
 * 서비스 안에서 곧바로 보내면, 그 뒤 커밋이 실패했을 때 DB 에 존재하지
 * 않는 토큰의 링크가 이미 사용자 메일함에 도착해 있다. 사용자는 링크를
 * 눌러도 "만료되었거나 이미 처리되었습니다"만 보게 되고, 원인을 알 수 없다.
 *
 * 엔티티가 아니라 문자열만 담는다. 수신 측이 다른 스레드에서 돌기 때문에
 * JPA 엔티티를 넘기면 지연 로딩이 세션 밖에서 터진다.
 */
public record VerificationMailRequested(String email, String token) {
}
