package com.reviewticket.server.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml 의 reviewticket.* 를 그대로 받는다. */
@ConfigurationProperties(prefix = "reviewticket")
public record ReviewTicketProperties(String demoDir, Auth auth, Mail mail, Cors cors) {

    /**
     * @param jwtSecret       서명 키. 32바이트 이상. application-local.yml 에 둔다
     * @param tokenTtl        로그인 토큰 유효 기간
     * @param verificationTtl 이메일 인증 링크 유효 기간
     * @param baseUrl         인증 링크에 쓸 서버 주소. 폰으로 테스트하려면
     *                        localhost 가 아니라 PC 의 LAN 주소여야 한다
     */
    public record Auth(String jwtSecret, Duration tokenTtl, Duration verificationTtl, String baseUrl) {
    }

    /** @param from 발신 주소. 비어 있으면 메일을 보내지 않고 링크를 로그에 남긴다 */
    public record Mail(String from) {
    }

    /**
     * 프론트엔드를 다른 출처에서 띄울 때 필요한 허용 목록.
     *
     * @param allowedOrigins 와일드카드 패턴을 쓸 수 있다 (예: http://localhost:*).
     *                       쿠키를 쓰지 않고 Authorization 헤더로만 인증하므로
     *                       자격증명 허용(allowCredentials)은 켜지 않는다 —
     *                       그래야 패턴에 * 를 쓸 수 있고 CSRF 위험도 없다.
     */
    public record Cors(List<String> allowedOrigins) {
    }
}
