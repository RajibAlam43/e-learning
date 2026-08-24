package com.gii.api.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleAuthEmailLockService {

  private final JdbcTemplate jdbcTemplate;

  public void lock(String normalizedEmail) {
    jdbcTemplate.query(
        "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
        preparedStatement -> preparedStatement.setString(1, normalizedEmail),
        resultSet -> null);
  }
}
