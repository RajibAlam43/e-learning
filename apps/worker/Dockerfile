FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /app
COPY . .
RUN ./gradlew :worker:bootJar --no-daemon

FROM eclipse-temurin:25-jre-noble AS runtime
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system appgroup \
    && useradd --system --gid appgroup appuser
USER appuser
ENV SERVER_ADDRESS=0.0.0.0
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=120s --retries=4 \
    CMD curl --fail --silent --output /dev/null http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM runtime AS ci
COPY --chown=appuser:appgroup apps/worker/build/libs/worker.jar /app/app.jar

FROM runtime AS standalone
COPY --from=build --chown=appuser:appgroup /app/apps/worker/build/libs/worker.jar /app/app.jar
