package com.gii.api.meapi;

import org.testcontainers.containers.PostgreSQLContainer;

final class SharedMePostgresContainer {

  private SharedMePostgresContainer() {}

  static final PostgreSQLContainer<?> INSTANCE =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("elearning_test")
          .withUsername("postgres")
          .withPassword("postgres");

  static {
    INSTANCE.start();
  }
}
