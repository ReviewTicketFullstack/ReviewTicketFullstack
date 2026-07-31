package com.reviewticket.server.mail;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 가입·재설정 메일을 트랜잭션 커밋 이후에만 보낸다.
 *
 * AFTER_COMMIT 이므로 트랜잭션이 롤백되면 호출되지 않는다. 즉 "메일은 갔는데
 * DB 에는 없다"는 상태가 만들어지지 않는다. 반대 방향(저장은 됐는데 메일 실패)은
 * 남지만, 그건 재발송으로 해결된다.
 */
@Component
public class VerificationMailListener {

    private final VerificationMailer mailer;

    public VerificationMailListener(VerificationMailer mailer) {
        this.mailer = mailer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationMailRequested(VerificationMailRequested event) {
        mailer.send(event.email(), event.token());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetMailRequested(PasswordResetMailRequested event) {
        mailer.sendPasswordReset(event.email(), event.token());
    }
}
