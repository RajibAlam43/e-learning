package com.gii.api.instructorapi;

import org.testcontainers.containers.PostgreSQLContainer;

final class SharedInstructorPostgresContainer {

  private SharedInstructorPostgresContainer() {}

  static final PostgreSQLContainer<?> INSTANCE =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("elearning_test")
          .withUsername("postgres")
          .withPassword("postgres");

  static {
    INSTANCE.start();
  }
}
