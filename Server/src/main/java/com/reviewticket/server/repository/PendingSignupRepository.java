package com.reviewticket.server.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.reviewticket.server.domain.PendingSignup;

public interface PendingSignupRepository extends JpaRepository<PendingSignup, Long> {

    Optional<PendingSignup> findByToken(String token);

    Optional<PendingSignup> findByEmail(String email);

    /**
     * 중복 검사는 users 와 이 표를 함께 봐야 한다. 대기 중인 이름도 예약된
     * 것으로 취급하지 않으면 두 사람이 같은 닉네임으로 동시에 대기하고,
     * 나중에 인증한 쪽이 실패한다.
     */
    boolean existsByEmail(String email);

    boolean existsByDisplayName(String displayName);

    /** 만료된 대기 건 정리. 지우지 않으면 이름이 영구히 예약된 상태로 남는다. */
    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}
