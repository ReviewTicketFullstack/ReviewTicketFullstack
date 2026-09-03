package com.reviewticket.server.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.reviewticket.server.domain.Store;
import com.reviewticket.server.domain.User;

public interface StoreRepository extends JpaRepository<Store, Long> {

    // 홈 목록 - 최신순. 등록 역순
    Page<Store> findAllByOrderByIdDesc(Pageable pageable);

    // 홈 목록 - 리뷰 많은 순
    Page<Store> findAllByOrderByReviewNumberDescIdDesc(Pageable pageable);

    boolean existsByOwner(User owner);

    /** GET/PATCH /api/stores/me. 사장 본인 가게를 찾는다. */
    Optional<Store> findByOwner(User owner);
}
