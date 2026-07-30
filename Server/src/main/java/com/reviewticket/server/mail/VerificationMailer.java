package com.reviewticket.server.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.reviewticket.server.config.ReviewTicketProperties;

/**
 * 이메일 인증 메일 발송.
 *
 * 발송 실패를 예외로 터뜨리지 않고 로그만 남긴다. 이유 — 메일 서버가
 * 잠깐 죽었다고 가입 자체를 되돌리면, 이미 만들어진 회원 정보를 지우는
 * 보상 처리가 필요해지고 그 과정에서 실패하면 상태가 더 꼬인다.
 * 가입은 성공시키고, 사용자는 인증 대기 화면에서 재발송을 누르면 된다.
 *
 * SMTP 설정이 비어 있으면 발송을 건너뛰고 링크를 로그에 남긴다.
 * 팀원이 자기 SMTP 계정 없이도 가입 흐름을 끝까지 테스트할 수 있어야 한다.
 */
@Component
public class VerificationMailer {

    private static final Logger log = LoggerFactory.getLogger(VerificationMailer.class);

    /**
     * SMTP 설정(spring.mail.host)이 없으면 Spring 이 JavaMailSender 를 아예 만들지
     * 않는다. 필수 의존성으로 받으면 그때 서버가 뜨지 못한다 — 메일 계정이 없는
     * 팀원도 나머지를 개발할 수 있어야 하므로 선택 의존성으로 받는다.
     */
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ReviewTicketProperties properties;

    public VerificationMailer(ObjectProvider<JavaMailSender> mailSenderProvider,
            ReviewTicketProperties properties) {
        this.mailSenderProvider = mailSenderProvider;
        this.properties = properties;
    }

    /**
     * 별도 스레드에서 보낸다. 동기로 보내면 가입 요청이 SMTP 왕복을 기다리게 되고,
     * 메일 서버가 느리면 사용자가 멈춘 화면을 본다. 발송 성공 여부는 가입 성공과
     * 무관하므로(실패해도 재발송으로 해결한다) 기다릴 이유가 없다.
     *
     * 인자를 문자열로만 받는 이유 — 다른 스레드에서 JPA 엔티티를 건드리면
     * 지연 로딩이 세션 밖에서 터진다. 필요한 값만 미리 뽑아 넘긴다.
     */
    @Async
    public void send(String to, String token) {
        String link = properties.auth().baseUrl() + "/api/auth/verify?token=" + token;
        String from = properties.mail().from();
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

        if (mailSender == null || from == null || from.isBlank()) {
            // 개발 중에는 이 링크를 눌러 인증을 끝낼 수 있다.
            log.warn("SMTP 설정이 없어 메일을 보내지 않는다. 인증 링크: {}", link);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("[ReviewTicket] 이메일 인증을 완료해 주세요");
        message.setText("""
                ReviewTicket 회원가입을 확인합니다.

                아래 링크를 눌러 이메일 인증을 완료해 주세요.

                %s

                이 링크는 %d분 뒤 만료됩니다.
                본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.
                """.formatted(link, properties.auth().verificationTtl().toMinutes()));

        try {
            mailSender.send(message);
            log.info("인증 메일 발송: {}", to);
        } catch (MailException e) {
            // 링크를 함께 남긴다 — 개발 중에는 이걸 직접 눌러 진행할 수 있다.
            log.error("인증 메일 발송 실패: {} / 인증 링크: {}", to, link, e);
        }
    }
}
