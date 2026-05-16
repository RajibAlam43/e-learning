package com.gii.api.testsupport;

import org.testcontainers.containers.PostgreSQLContainer;

public final class SharedPostgresContainer {

  private SharedPostgresContainer() {}

  public static final PostgreSQLContainer<?> INSTANCE =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("elearning_test")
          .withUsername("postgres")
          .withPassword("postgres");

  static {
    INSTANCE.start();
  }
}
