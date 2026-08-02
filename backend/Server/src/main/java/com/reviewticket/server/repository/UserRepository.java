package com.reviewticket.server.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.reviewticket.server.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /**
     * 중복 검사용. DB 콜레이션이 utf8mb4_0900_ai_ci 라 대소문자를 구분하지 않으므로
     * 'Abc@x.com' 으로 가입한 뒤 'abc@x.com' 을 물어도 true 가 나온다.
     */
    boolean existsByEmail(String email);

    boolean existsByDisplayName(String displayName);
}
