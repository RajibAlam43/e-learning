package com.gii.api.certificateapi;

import org.testcontainers.containers.PostgreSQLContainer;

final class SharedCertificatePostgresContainer {

  private SharedCertificatePostgresContainer() {}

  static final PostgreSQLContainer<?> INSTANCE =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("elearning_test")
          .withUsername("postgres")
          .withPassword("postgres");

  static {
    INSTANCE.start();
  }
}
