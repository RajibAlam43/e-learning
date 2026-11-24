# Pin Flyway independently of the Java runtime. No application is started here.
FROM redgate/flyway:12.4.0
COPY apps/api/src/main/resources/db/migration/ /flyway/sql/
CMD ["-connectRetries=10", "-validateOnMigrate=true", "-cleanDisabled=true", "-baselineOnMigrate=false", "-outOfOrder=false", "migrate"]
