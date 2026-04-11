FROM gradle:jdk25 AS build
WORKDIR /app

COPY . .

RUN gradle :api:bootJar --no-daemon

FROM eclipse-temurin:25
WORKDIR /app

RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
USER appuser

COPY --from=build /app/apps/api/build/libs/api.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]