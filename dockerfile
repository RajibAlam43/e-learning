# Build stage
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY . .
RUN ./gradlew bootJar

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app

# Create non-root user
RUN useradd -r -u 10001 appuser

# Copy app and switch user
COPY --from=build /app/build/libs/*.jar app.jar
USER appuser

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70"
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]