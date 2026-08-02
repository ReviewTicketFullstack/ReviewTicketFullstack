package com.reviewticket.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * UserDetailsServiceAutoConfiguration 을 제외하는 이유 — 이 자동 설정은
 * UserDetailsService 빈이 없으면 사용자 이름 'user' 와 무작위 비밀번호로
 * 메모리 계정을 하나 만들고, 그 비밀번호를 시작 로그에 찍는다.
 *
 * 우리는 JWT 로만 인증하므로 그 계정을 쓸 통로(폼 로그인, HTTP Basic)가
 * 둘 다 꺼져 있어 실제 위험은 없다. 다만 매번 로그에 비밀번호가 남고
 * "기본 계정이 열려 있나" 하는 오해를 부르므로 아예 만들지 않는다.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
@EnableAsync // 인증 메일을 별도 스레드에서 보낸다 (VerificationMailer)
@EnableScheduling // 만료된 가입 대기 건 정리 (AuthService.purgeExpiredSignups)
public class ServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServerApplication.class, args);
	}

}
