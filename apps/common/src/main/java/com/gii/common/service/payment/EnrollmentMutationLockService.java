package com.gii.common.service.payment;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Serializes a student's enrollment/credit-choice mutations across API and worker. */
@Service
@RequiredArgsConstructor
public class EnrollmentMutationLockService {
  private final JdbcTemplate jdbc;

  public void lock(UUID userId) {
    jdbc.queryForList(
        "SELECT pg_advisory_xact_lock(hashtextextended(?,71925411))", userId.toString());
  }
}
