package com.gii.api.authapi;

import org.testcontainers.containers.PostgreSQLContainer;

final class SharedAuthPostgresContainer {

  private SharedAuthPostgresContainer() {}

  static final PostgreSQLContainer<?> INSTANCE =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("elearning_test")
          .withUsername("postgres")
          .withPassword("postgres");

  static {
    INSTANCE.start();
  }
}
