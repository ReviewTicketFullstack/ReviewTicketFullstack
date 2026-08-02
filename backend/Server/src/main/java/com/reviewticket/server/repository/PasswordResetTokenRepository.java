package com.reviewticket.server.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.reviewticket.server.domain.PasswordResetToken;
import com.reviewticket.server.domain.User;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    /** 재발송 간격 제한에 쓴다. 가장 최근 발급 건을 찾는다. */
    Optional<PasswordResetToken> findTopByUserOrderByIdDesc(User user);

    /** 만료·사용 완료 토큰 정리. */
    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}
