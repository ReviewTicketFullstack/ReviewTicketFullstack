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
 * 인증·재설정 메일 발송.
 *
 * 발송 실패를 예외로 터뜨리지 않고 로그만 남긴다. 메일 서버가 잠깐 죽었다고
 * 가입이나 재설정 요청 자체를 되돌리면 상태가 더 꼬인다. 사용자는 재발송을
 * 누르면 된다.
 *
 * SMTP 설정이 비어 있으면 발송을 건너뛴다. 토큰이 담긴 링크는 로그에 남기지
 * 않는다 — 그 자체로 가입·재설정을 완료시킬 수 있는 자격증명이기 때문이다.
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
     * 회원가입 이메일 인증 메일.
     *
     * 별도 스레드에서 보낸다. 동기로 보내면 요청이 SMTP 왕복을 기다리게 되고,
     * 메일 서버가 느리면 사용자가 멈춘 화면을 본다. 발송 성공 여부는 요청 성공과
     * 무관하므로(실패해도 재발송으로 해결한다) 기다릴 이유가 없다.
     *
     * 인자를 문자열로만 받는 이유 — 다른 스레드에서 JPA 엔티티를 건드리면
     * 지연 로딩이 세션 밖에서 터진다. 필요한 값만 미리 뽑아 넘긴다.
     */
    @Async
    public void send(String to, String token) {
        String link = properties.auth().baseUrl() + "/api/auth/verify?token=" + token;
        sendMail(to, "[ReviewTicket] 이메일 인증을 완료해 주세요", """
                ReviewTicket 회원가입을 확인합니다.

                아래 링크를 눌러 이메일 인증을 완료해 주세요.

                %s

                이 링크는 %d분 뒤 만료됩니다.
                본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.
                """.formatted(link, properties.auth().verificationTtl().toMinutes()));
    }

    /** 비밀번호 재설정 메일. */
    @Async
    public void sendPasswordReset(String to, String token) {
        String link = properties.auth().baseUrl() + "/api/auth/password-reset?token=" + token;
        sendMail(to, "[ReviewTicket] 비밀번호 재설정", """
                비밀번호 재설정을 요청하셨습니다.

                아래 링크를 눌러 새 비밀번호를 설정해 주세요.

                %s

                이 링크는 %d분 뒤 만료됩니다.
                본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다. 비밀번호는 바뀌지 않습니다.
                """.formatted(link, properties.auth().verificationTtl().toMinutes()));
    }

    private void sendMail(String to, String subject, String text) {
        String from = properties.mail().from();
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

        if (mailSender == null || from == null || from.isBlank()) {
            // 링크는 로그에 남기지 않는다 (자격증명이다). 발송이 꺼져 있다는 사실만 알린다.
            log.warn("SMTP 설정이 없어 메일을 보내지 않는다: {} / {}", to, subject);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        try {
            mailSender.send(message);
            log.info("메일 발송: {} / {}", to, subject);
        } catch (MailException e) {
            log.error("메일 발송 실패: {} / {}", to, subject, e);
        }
    }
}
