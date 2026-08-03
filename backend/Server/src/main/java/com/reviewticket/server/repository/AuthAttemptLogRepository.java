package com.reviewticket.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.reviewticket.server.domain.AuthAttemptLog;

public interface AuthAttemptLogRepository extends JpaRepository<AuthAttemptLog, Long> {
}
