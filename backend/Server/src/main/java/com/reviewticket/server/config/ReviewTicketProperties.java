package com.reviewticket.server.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml 의 reviewticket.* 를 그대로 받는다. */
@ConfigurationProperties(prefix = "reviewticket")
public record ReviewTicketProperties(
        String demoDir, Auth auth, Mail mail, Cors cors, Order order, Ai ai, Upload upload, Review review) {

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
     * @param reviewTtl 주문 후 리뷰를 쓸 수 있는 기간. 주문 시각에 이 값을 더해
     *                  expire_time 을 정한다(customer_order_table.review_deadline 은
     *                  이 값을 초 단위로 스냅샷한 것). 정책이 바뀌면 이 줄만 고치면 되고,
     *                  프론트는 서버가 내려준 마감 시각을 그대로 표시한다
     */
    public record Order(Duration reviewTtl) {
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

    /**
     * 리뷰 사진 AI 유사도 판정 서버.
     *
     * @param serverUrl      두 이미지를 받아 유사도를 돌려주는 엔드포인트 주소
     * @param timeout        이 시간 안에 응답이 없으면 AI_SERVER_UNAVAILABLE 로 처리한다.
     *                       500장 실측 기준 최대 응답 시간이 3.5초였던 것에 여유를 둔 값이다
     * @param matchThreshold 이 값 이상이어야 리뷰가 저장된다 (0~1)
     */
    public record Ai(String serverUrl, Duration timeout, double matchThreshold) {
    }

    /**
     * 이미지 업로드(가게 로고). 리뷰 사진은 이 설정을 쓰지 않는다 — 판정에 실패한
     * 사진이 디스크에 남으면 안 되므로 리뷰 쪽은 별도 경로로 처리한다(ReviewService).
     *
     * @param dir            저장 폴더. 상대경로면 서버 실행 위치 기준이다
     * @param baseUrl        저장된 파일을 내려줄 때 앞에 붙는 경로. url = baseUrl + "/" + 파일명
     * @param targetLongEdge 저장 전 리사이즈 목표 크기(긴 변, px). 이보다 작은 원본은 그대로 둔다
     */
    public record Upload(String dir, String baseUrl, int targetLongEdge) {
    }

    /**
     * 리뷰 작성 규칙.
     *
     * @param contentMinLength 텍스트 후기 최소 길이
     * @param contentMaxLength 텍스트 후기 최대 길이
     * @param minImageLongEdge 리뷰 사진의 최소 긴 변(px). 이보다 작으면 거절한다(IMAGE_TOO_SMALL).
     *                         업로드(Upload.targetLongEdge)와 같은 값으로 맞춰 둔다 — 하한과
     *                         목표치가 같아야 사진이 늘어나는 일 없이 항상 줄이기만 하면 된다
     */
    public record Review(int contentMinLength, int contentMaxLength, int minImageLongEdge) {
    }

    /**
     * 홈 가게 목록.
     *
     * @param pinnedName 목록 맨 위에 고정할 가게 이름. 비어 있으면 고정하지 않는다
     */
    public record Store(String pinnedName) {
    }
}
