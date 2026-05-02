package com.gii.api.publicapi;

import org.testcontainers.containers.PostgreSQLContainer;

final class SharedPostgresContainer {

  private SharedPostgresContainer() {}

  static final PostgreSQLContainer<?> INSTANCE =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("elearning_test")
          .withUsername("postgres")
          .withPassword("postgres");

  static {
    INSTANCE.start();
  }
}
