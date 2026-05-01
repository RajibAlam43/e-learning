# AGENTS.md: E-Learning Backend Development Guide

This guide helps AI agents understand the architecture and patterns of this multi-service Java/Spring Boot e-learning platform.

## Architecture Overview

**Multi-module Gradle monorepo** with three independently deployable services:
- **`:api`** - REST API server (port 8080) handling user-facing features, authentication, course management
- **`:common`** - Shared JPA entities, repositories, enums, DTOs (dependency for api & worker)
- **`:worker`** - Async background job processor consuming SQS messages, sending emails

All services share PostgreSQL database, Redis (Valkey) cache, and AWS SQS (ElasticMQ locally).

**Key Config Files:**
- Root: `settings.gradle`, `build.gradle`, `docker-compose.yml`
- Modules: `apps/{api,common,worker}/build.gradle`, `src/main/resources/application*.properties`
- Migrations: Flyway SQL files in `apps/api/src/main/resources/db/migration/`
- Deployment: Shell scripts in `scripts/stage/`
- CI/CD: GitHub Actions workflows in `.github/workflows/`

## Module Structure & Import Scanning

Each service scans `com.gii.api`, `com.gii.common`, `com.gii.worker` respectively:
```java
@SpringBootApplication(scanBasePackages = {"com.gii.api", "com.gii.common"})
@EntityScan(basePackages = {"com.gii.common.entity"})
@EnableJpaRepositories(basePackages = {"com.gii.common.repository"})
```

**Always put entities and repositories in `:common`** - they're shared. Controllers/services live in their module (api or worker).

## Core Patterns

### Controllers: Contract-First Interface Design
Controllers implement API interfaces for OpenAPI/Swagger documentation:
```java
@RestController
@RequiredArgsConstructor
public class StudentApiController implements StudentApi {
    @Override
    public ResponseEntity<StudentDashboardResponse> getStudentDashboard(...) {
        return ResponseEntity.ok(service.execute(...));
    }
}
```
Interfaces live in `model/` (e.g., `StudentApi`) with `@Tag`, `@Operation`, `@ApiResponse` Swagger annotations.

### Services: Execution Pattern with @Transactional
Services use singular responsibility with `execute()` or domain-specific methods:
```java
@Service
@RequiredArgsConstructor
@Transactional  // Critical for any data mutations
public class LoginService {
    private final UserRepository userRepository;
    
    public AuthResponse execute(LoginRequest request, HttpServletResponse response) {
        // Logic here, auto-rollback on exception
    }
}
```
**Convention:** Service class names end with `Service` (e.g., `LoginService`, `VerifyService`).

### Mappers: DTO Conversion Components
Use `@Component` mappers for converting entities ↔ DTOs:
```java
@Component
public class MediaAssetMapper {
    public MediaAsset toEntity(CreateMediaAssetRequest request, Lesson lesson) { ... }
    public void updateEntity(MediaAsset asset, UpdateMediaAssetRequest request) { ... }
    public MediaAssetResponse toResponse(MediaAsset asset) { ... }
}
```

### Entities: UUID-Based with Builder Pattern
All entities inherit from `BaseUuidEntity` and use `@SuperBuilder`:
```java
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_profiles")
public class UserProfile extends BaseUuidEntity {
    @Column(nullable = false)
    private String displayName;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
```
**Key:** Use `@JsonIgnore` on ManyToOne relationships to prevent circular serialization.

### Response Models: Records with @Builder
API responses use Java records with `@Builder` pattern:
```java
@Builder
public record AdminOrderItemResponse(
    UUID courseId,
    String courseName,
    BigDecimal priceBdt,
    BigDecimal discountBdt,
    BigDecimal finalAmount
) {}
```

### Authentication: JWT with Refresh Token Cookies
- **JWT issued via:** `JwtService` (used by `LoginService`, `RefreshService`)
- **Refresh tokens stored in:** Redis cache (`RefreshTokenStoreService`)
- **Cookie strategy:** `RefreshTokenCookieService` sets HttpOnly refresh_token cookie
- **Auth extraction:** `Authentication` parameter injected in controllers from Sprint Security context

## Async Processing: SQS Listener Pattern

Worker module consumes SQS messages:
```java
@Service
@Slf4j
public class JobsListener {
    @SqsListener(value = "${email.jobs.main.queue}")
    public void receiveEmailJobs(Message<String> payload) {
        // Process async job (email sending, etc.)
    }
}
```

**SQS Queue Configs by Profile:**
- **local:** ElasticMQ at `http://elasticmq:9324` (in docker-compose)
- **stage/prod:** Profile-specific queue URLs in `application-{stage,prod}.properties`

**Configuration:** `SqsConfig` bean toggles between local (endpoint override) and AWS SQS.

## Database & Migrations

**Flyway:** SQL migrations in `apps/api/src/main/resources/db/migration/` (V1__*, V2__*, etc.)

**Critical:** Hibernate is set to `validate` mode - schema changes ONLY via Flyway:
```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=false  # Spring doesn't auto-run; migrations handled separately
```

**Deployment flow:** Server has Flyway CLI (`/opt/flyway/flyway`) for independent migrations before app startup.

## Build & Test

**Build everything:**
```bash
./gradlew clean test bootJar
```

Outputs:
- `apps/api/build/libs/api.jar`
- `apps/worker/build/libs/worker.jar`
- `apps/common/build/libs/common-0.0.1-SNAPSHOT.jar` (library jar only)

**Java 25, Temurin JRE, Lombok code generation** all configured in root `build.gradle`.

## Profile-Based Configuration

Use Spring profiles for environment-specific settings:
- **local:** Docker Compose services (postgres, valkey, elasticmq)
- **stage:** Staging AWS/external service endpoints
- **prod:** Production AWS/external service endpoints

**Property files:** `application.properties` + `application-{profile}.properties`

Set via `SPRING_PROFILES_ACTIVE` env var (docker-compose, systemd service files).

## External Integrations

- **Video:** Mux (config: `mux.signing-key-id`, `mux.private-key-prem`) and Bunny (config: `bunny.token-security-key`)
- **Payments:** SSL Commerce, bKash, Nagad webhook handlers in `PaymentApiController`
- **Email:** Spring Mail in worker module

## Local Development

```bash
# Start all services (api, worker, postgres, redis, sqs)
docker-compose up -d

# View logs
docker-compose logs -f api
docker-compose logs -f worker

# Run migrations (if needed)
docker-compose exec postgres psql -U postgres -d elearning -f path/to/migration.sql

# Stop everything
docker-compose down
```

## Common Gotchas

1. **Scanning issue:** Forget `@EnableJpaRepositories` in a new service? Repos won't be found.
2. **Transactions:** Any service method mutating data must have `@Transactional` or rollbacks won't work.
3. **JSONB columns:** Use `@JdbcTypeCode(SqlTypes.JSON)` + `columnDefinition = "jsonb"` for type safety.
4. **Circular references:** Always `@JsonIgnore` on ManyToOne sides in entities.
5. **Queue config:** Different queue URLs per profile - check property files before assuming ElasticMQ.
6. **Flyway disabled:** Don't enable `spring.flyway.enabled=true` in app (migrations run separately).
7. **User email and phone number:** Don't forget that user may have either email or phone number or both (Only one is allowed during registration but other one can be added later).

## Key File References

- Architecture: `settings.gradle`, `build.gradle`
- API config: `apps/api/src/main/resources/application.properties`
- Worker config: `apps/worker/src/main/resources/application.properties`
- Common entities: `apps/common/src/main/java/com/gii/common/entity/`
- API controllers: `apps/api/src/main/java/com/gii/api/controller/`
- Services: `apps/api/src/main/java/com/gii/api/service/`
- Migrations: `apps/api/src/main/resources/db/migration/`

