package com.gii.api.studentapi;

import org.testcontainers.containers.PostgreSQLContainer;

final class SharedStudentPostgresContainer {

  private SharedStudentPostgresContainer() {}

  static final PostgreSQLContainer<?> INSTANCE =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("elearning_test")
          .withUsername("postgres")
          .withPassword("postgres");

  static {
    INSTANCE.start();
  }
}
