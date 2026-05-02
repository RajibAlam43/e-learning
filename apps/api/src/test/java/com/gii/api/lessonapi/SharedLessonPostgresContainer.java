package com.gii.api.lessonapi;

import org.testcontainers.containers.PostgreSQLContainer;

final class SharedLessonPostgresContainer {

  private SharedLessonPostgresContainer() {}

  static final PostgreSQLContainer<?> INSTANCE =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("elearning_test")
          .withUsername("postgres")
          .withPassword("postgres");

  static {
    INSTANCE.start();
  }
}
