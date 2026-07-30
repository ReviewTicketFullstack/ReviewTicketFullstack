package com.reviewticket.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.reviewticket.server.domain.AiRejection;

public interface AiRejectionRepository extends JpaRepository<AiRejection, Long> {
}
