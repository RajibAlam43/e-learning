package com.gii.api.testsupport;

import java.net.URI;
import java.util.Set;
import org.testcontainers.containers.PostgreSQLContainer;

public final class SharedPostgresContainer {

  private SharedPostgresContainer() {}

  public static final TestDatabase INSTANCE = initialize();

  private static TestDatabase initialize() {
    String externalUrl = System.getenv("ELEARNING_TEST_DATABASE_URL");
    if (externalUrl != null && !externalUrl.isBlank()) {
      URI uri = URI.create(externalUrl.replaceFirst("^jdbc:", ""));
      if (!externalUrl.startsWith("jdbc:postgresql://")
          || !"postgresql".equals(uri.getScheme())
          || uri.getHost() == null
          || !Set.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost())
          || uri.getPath() == null
          || !uri.getPath().matches("/[a-zA-Z0-9_]+_test")
          || uri.getQuery() != null
          || uri.getUserInfo() != null
          || uri.getFragment() != null) {
        throw new IllegalArgumentException(
            "External tests require a loopback PostgreSQL URL, a database ending in _test,"
                + " and no query/userinfo; tests delete application data");
      }
      return new TestDatabase(
          externalUrl,
          System.getenv().getOrDefault("ELEARNING_TEST_DATABASE_USERNAME", "postgres"),
          System.getenv().getOrDefault("ELEARNING_TEST_DATABASE_PASSWORD", ""));
    }
    PostgreSQLContainer<?> container =
        new PostgreSQLContainer<>(
                System.getenv().getOrDefault("ELEARNING_TEST_POSTGRES_IMAGE", "postgres:18-alpine"))
            .withDatabaseName("elearning_test")
            .withUsername("postgres")
            .withPassword("postgres");
    container.start();
    return new TestDatabase(
        container.getJdbcUrl(), container.getUsername(), container.getPassword());
  }

  public record TestDatabase(String jdbcUrl, String username, String password) {
    public String getJdbcUrl() {
      return jdbcUrl;
    }

    public String getUsername() {
      return username;
    }

    public String getPassword() {
      return password;
    }
  }
}
