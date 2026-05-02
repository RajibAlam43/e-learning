package com.gii.api.adminapi;

import org.testcontainers.containers.PostgreSQLContainer;

final class SharedAdminPostgresContainer {

  private SharedAdminPostgresContainer() {}

  static final PostgreSQLContainer<?> INSTANCE =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("elearning_test")
          .withUsername("postgres")
          .withPassword("postgres");

  static {
    INSTANCE.start();
  }
}
