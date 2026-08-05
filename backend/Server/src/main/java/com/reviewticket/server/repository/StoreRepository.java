package com.reviewticket.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.reviewticket.server.domain.Store;
import com.reviewticket.server.domain.User;

public interface StoreRepository extends JpaRepository<Store, Long> {

    /** 홈 목록. 최신 가게가 먼저 온다. */
    List<Store> findAllByOrderByIdDesc();

    boolean existsByOwner(User owner);

    boolean existsByName(String name);
}
