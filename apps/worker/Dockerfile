FROM gradle:jdk25 AS build
WORKDIR /app

COPY . .

RUN gradle :worker:bootJar --no-daemon

FROM eclipse-temurin:25
WORKDIR /app

RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
USER appuser

COPY --from=build /app/apps/worker/build/libs/worker.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]