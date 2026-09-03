package com.reviewticket.server.auth;

import java.util.Hashtable;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 이메일 도메인에 메일을 받을 서버가 있는지 DNS 로 확인한다.
 *
 * probe.com 같이 메일 서버 자체가 없는 도메인으로 가입 요청이 오면,
 * 실제로 인증메일 발송을 시도하기 전에 걸러낸다. 그러지 않으면 매번
 * 반송(bounce)이 쌓이고, 반송이 누적되면 우리 발신 주소가 스팸으로
 * 찍혀 정상 유저에게 갈 메일까지 스팸함으로 빠질 수 있다.
 *
 * MX 레코드만 확인한다 — RFC 5321 은 MX 가 없으면 A 레코드로 폴백해도
 * 된다고 정하지만, 실무에서는 웹사이트용 A 레코드만 있고 메일은 아예
 * 안 받는 도메인이 훨씬 흔하다(예: probe.com — A 레코드는 있지만
 * 그 서버는 SMTP 자체를 안 받아 반송이 발생했다). A 레코드까지
 * 폴백하면 이런 케이스를 다 통과시켜버려 검사 의미가 없어진다.
 *
 * DNS 조회 자체가 실패한 경우(네트워크 문제 등, 도메인이 정말 없는
 * 것과는 다르다)는 판단 보류로 보고 통과시킨다 — 우리 쪽 DNS 문제로
 * 정상 유저의 가입을 막으면 안 된다.
 */
@Component
public class EmailDomainValidator {

    private static final Logger log = LoggerFactory.getLogger(EmailDomainValidator.class);

    public boolean hasMailServer(String email) {
        String domain = extractDomain(email);
        if (domain == null) {
            return false;
        }

        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("com.sun.jndi.dns.timeout.initial", "3000");
        env.put("com.sun.jndi.dns.timeout.retries", "1");

        try {
            InitialDirContext ctx = new InitialDirContext(env);
            Attribute mx = ctx.getAttributes(domain, new String[] { "MX" }).get("MX");
            return mx != null && mx.size() > 0;
        } catch (NameNotFoundException e) {
            // 도메인 자체가 없거나, MX·A 레코드가 전혀 없음 — 메일을 받을 수 없다.
            return false;
        } catch (NamingException e) {
            log.warn("도메인 MX 조회 실패, 판단 보류로 통과시킴: {}", domain, e);
            return true;
        }
    }

    private String extractDomain(String email) {
        int at = email.indexOf('@');
        if (at < 0 || at == email.length() - 1) {
            return null;
        }
        return email.substring(at + 1);
    }
}
