package com.reviewticket.server.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.reviewticket.server.domain.User;

import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /**
     * 중복 검사용. DB 콜레이션이 utf8mb4_0900_ai_ci 라 대소문자를 구분하지 않으므로
     * 'Abc@x.com' 으로 가입한 뒤 'abc@x.com' 을 물어도 true 가 나온다.
     */
    boolean existsByEmail(String email);

    boolean existsByDisplayName(String displayName);

    /**
     * 티켓을 건드리는 트랜잭션 전용 조회. 행에 배타 락을 걸어 같은 사용자의
     * 요청이 겹쳐도 한 번에 하나씩만 지나가게 한다.
     *
     * 평범한 findById 로 읽으면 락이 없어, 두 요청이 같은 tickets 값을 보고
     * 각자 검사를 통과한 뒤 각자 차감한다 — 보유 티켓보다 많은 이벤트 주문이
     * 생긴다.
     *
     * 락을 "맨 앞에서" 잡는 것이 중요하다. 주문 생성은 customer_order_table 에
     * INSERT 하면서 외래키 때문에 users 행에 공유 락을 먼저 건다. 그 뒤에 티켓을
     * UPDATE 하려면 배타 락으로 올려야 하는데, 두 요청이 동시에 올리려 하면
     * 서로가 쥔 공유 락을 기다려 데드락(MySQL 1213)이 난다. 처음부터 배타 락을
     * 쥐고 들어가면 승격 자체가 없어 두 번째 요청은 그냥 앞이 끝날 때까지
     * 기다렸다가 갱신된 값을 읽는다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
