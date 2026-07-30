package com.reviewticket.server.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.reviewticket.server.domain.Food;

public interface FoodRepository extends JpaRepository<Food, Long> {

    /** AI 라벨 문자열로 찾는다 (pizza, hamburger, ...). */
    Optional<Food> findByName(String name);
}
