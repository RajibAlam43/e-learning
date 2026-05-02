package com.gii.api.quizapi;

import org.testcontainers.containers.PostgreSQLContainer;

final class SharedQuizPostgresContainer {

  private SharedQuizPostgresContainer() {}

  static final PostgreSQLContainer<?> INSTANCE =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("elearning_test")
          .withUsername("postgres")
          .withPassword("postgres");

  static {
    INSTANCE.start();
  }
}
