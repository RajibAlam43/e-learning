package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.api.testsupport.SharedPostgresContainer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Rehearse the actual pre-academic upgrade against an owned, disposable database. */
@Tag("integration")
class AcademicMigrationCompatibilityIt {
  private static final List<String> TABLES =
      List.of(
          "users",
          "course_templates",
          "course_offerings",
          "collections",
          "collection_courses",
          "orders",
          "order_items",
          "order_item_courses",
          "enrollments",
          "collection_enrollments",
          "certificates");
  private static final String ADDED_COLUMNS =
      "ARRAY['credits','semester_id','required_credits',"
          + "'elective_credit_limit','expected_duration_months','access_duration_months',"
          + "'program_snapshot','program_access_managed','independent_access_available',"
          + "'independent_access_expires_at']";

  @Test
  void realUpgradePreservesHistoryAndReadOnlyReviewNeverAwardsHistoricalCredits() throws Exception {
    final var db = SharedPostgresContainer.INSTANCE;
    final var name =
        "academic_migration_" + UUID.randomUUID().toString().replace("-", "") + "_test";
    final var url = db.getJdbcUrl().substring(0, db.getJdbcUrl().lastIndexOf('/') + 1) + name;
    try (var maintenance =
        DriverManager.getConnection(db.getJdbcUrl(), db.getUsername(), db.getPassword())) {
      try (var command = maintenance.createStatement()) {
        command.execute("CREATE DATABASE " + name);
      }
      try {
        Flyway.configure()
            .dataSource(url, db.getUsername(), db.getPassword())
            .locations("classpath:db/migration")
            .target("23")
            .load()
            .migrate();
        try (var connection =
            DriverManager.getConnection(url, db.getUsername(), db.getPassword())) {
          try (var fixture = getClass().getResourceAsStream("/fixtures/academics-v23.sql");
              var statement = connection.createStatement()) {
            statement.execute(new String(fixture.readAllBytes(), StandardCharsets.UTF_8));
          }
          final var before = snapshot(connection);
          final var flyway =
              Flyway.configure()
                  .dataSource(url, db.getUsername(), db.getPassword())
                  .locations("classpath:db/migration")
                  .load();
          flyway.migrate();
          flyway.validate();
          assertThat(snapshot(connection)).isEqualTo(before);
          assertThat(query(connection, "SELECT count(*) FROM academic_achievements"))
              .isEqualTo("0");
          assertThat(query(connection, "SELECT count(*) FROM program_course_registrations"))
              .isEqualTo("0");
          final var json = new ObjectMapper();
          final var paid =
              json.readTree(
                  query(
                      connection,
                      "SELECT program_snapshot::text FROM collection_enrollments "
                          + "WHERE source_order_item_id IS NOT NULL"));
          assertThat(paid.path("requiredCredits").isNull()).isTrue();
          assertThat(paid.path("courses").get(0).path("courseId").asText())
              .isEqualTo("00000000-0000-0000-0000-000000000021");
          final var manual =
              json.readTree(
                  query(
                      connection,
                      "SELECT program_snapshot::text FROM collection_enrollments "
                          + "WHERE source_order_item_id IS NULL"));
          assertThat(manual.path("courses").get(0).path("courseId").asText())
              .isEqualTo("00000000-0000-0000-0000-000000000022");
          assertThatThrownBy(
                  () -> {
                    try (var statement = connection.createStatement()) {
                      statement.execute(
                          "UPDATE collection_enrollments SET program_snapshot='{}'::jsonb");
                    }
                  })
              .hasMessageContaining("Purchased program requirements are immutable");
          final var review = runReview(connection);
          assertThat(review.path("transaction_read_only").asText()).isEqualTo("on");
          assertThat(review.path("historical_backfill_applied").asBoolean()).isFalse();
          assertThat(review.path("historical_credit_candidates").size()).isEqualTo(1);
          assertThat(
                  review
                      .path("historical_credit_candidates")
                      .get(0)
                      .path("completed_cohorts")
                      .asInt())
              .isEqualTo(2);
          assertThat(
                  review
                      .path("review_counts")
                      .path("purchased_courses_missing_from_snapshot")
                      .asInt())
              .isZero();
          assertThat(snapshot(connection)).isEqualTo(before);
          assertThat(query(connection, "SELECT count(*) FROM academic_achievements"))
              .isEqualTo("0");
          assertThat(flyway.migrate().migrationsExecuted).isZero();
        }
      } finally {
        try (var command = maintenance.createStatement()) {
          command.execute("DROP DATABASE " + name + " WITH (FORCE)");
        }
      }
    }
  }

  private Map<String, List<String>> snapshot(Connection connection) throws Exception {
    final var result = new LinkedHashMap<String, List<String>>();
    for (String table : TABLES) {
      final var rows = new java.util.ArrayList<String>();
      try (var statement = connection.createStatement();
          var rs =
              statement.executeQuery(
                  "SELECT (to_jsonb(t) - "
                      + ADDED_COLUMNS
                      + ")::text FROM "
                      + table
                      + " t ORDER BY 1")) {
        while (rs.next()) {
          rows.add(rs.getString(1));
        }
      }
      result.put(table, rows);
    }
    return result;
  }

  private String query(Connection connection, String sql) throws Exception {
    try (var statement = connection.createStatement();
        var rs = statement.executeQuery(sql)) {
      rs.next();
      return rs.getString(1);
    }
  }

  private com.fasterxml.jackson.databind.JsonNode runReview(Connection connection)
      throws Exception {
    String result = null;
    try (var statement = connection.createStatement()) {
      boolean hasResult =
          statement.execute(
              Files.readString(Path.of("../../deploy/db/academic-migration-review.sql")));
      while (true) {
        if (hasResult) {
          try (var rows = statement.getResultSet()) {
            if (rows.next()) {
              result = rows.getString(1);
            }
          }
        } else if (statement.getUpdateCount() == -1) {
          break;
        }
        hasResult = statement.getMoreResults();
      }
    }
    return new ObjectMapper().readTree(result);
  }
}
