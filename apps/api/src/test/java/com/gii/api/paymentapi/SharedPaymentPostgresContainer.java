package com.gii.api.paymentapi;

import org.testcontainers.containers.PostgreSQLContainer;

final class SharedPaymentPostgresContainer {

  private SharedPaymentPostgresContainer() {}

  static final PostgreSQLContainer<?> INSTANCE =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("elearning_test")
          .withUsername("postgres")
          .withPassword("postgres");

  static {
    INSTANCE.start();
  }
}
