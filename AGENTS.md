# AGENTS.md: E-Learning Development Guide

This guide covers the Spring Boot services and Next.js frontend in this monorepo.

## Architecture Overview

**Multi-module Gradle monorepo** with two independently deployable applications and a shared library:
- **`:api`** - REST API server (port 8080) handling user-facing features, authentication, course management
- **`:common`** - Shared JPA entities, repositories, enums, DTOs (dependency for api & worker)
- **`:worker`** - Async background job processor consuming SQS messages, sending emails
- **`apps/web`** - Next.js App Router frontend (port 3000); independent npm project, not a Gradle module

API and worker share PostgreSQL and AWS SQS (ElasticMQ locally). Redis is not required.

**Approved production hosting plan:** one Hostinger KVM 4 (4 shared vCPU / 16 GB /
200 GB) in Singapore, running Dokploy, API, Next.js and worker, plus DO managed
PostgreSQL in Singapore. User-reported mean host-to-DB latency is approximately
1.5 ms. No production application PostgreSQL container runs on the VPS. Memory
ceilings: API 3 GB, Next.js 2 GB, worker 2 GB. CPU caps: Next.js 1.5, worker 1.
Production DB clients use the DO public endpoint with verified TLS and trusted-source
IP restrictions. Stage stays separate with internal-only Dokploy PostgreSQL.
Cloudflare fronts public services. Verify managed recovery and plan independent
hourly R2/B2 exports for the retained one-hour target; external-DB export setup needs
approval. Test restore/rebuild before launch, then add app redundancy as needed.

**Future DO compute alternative:** retain managed PostgreSQL with separate app and worker droplets,
plus either a third Next.js frontend droplet managed by production Dokploy or Vercel
Pro instead. One production Dokploy manages the DO hosts; staging stays independent.
See the runbook's future DigitalOcean plan for compute migration, networking,
frontend choices and cutover. This is a future option, not a change to the Hostinger
launch decision or authorization to provision infrastructure.

**Change authorization:** the frontend import into `apps/web` and its Docker/GHCR
publishing workflow were explicitly authorized. Other hosting-plan implementation,
application changes, tests, workflow/configuration changes and infrastructure actions
still require explicit user confirmation unless authorized by a newer request.
Do not describe proposed video/player behavior or queued exams as already implemented.

**Planned observability:** Sentry error tracking for API, worker and Next.js, alongside
Dokploy metrics/logs and independent phone-alerting uptime/backup checks. SDK setup,
source maps, DSNs, privacy controls and release tags require confirmation; no Sentry
integration is currently implemented. See the runbook's Sentry plan.

**Networking/data cutover:** same-host server traffic uses verified Dokploy internal
service names; browsers/external callbacks use public HTTPS. Worker uses SQS and DB
directly; production DB traffic goes to DO over verified public TLS, while stage DB
traffic is internal. Next.js calls API, not DB. Preserve DO stage data through a rehearsed export/
restore into the separate Hostinger stage; initialize production cleanly and promote
only explicitly selected content. Record the old DO DB retirement window, retain a
verified independent backup, and obtain approval before destructive retirement.

**Key Config Files:**
- Root: `settings.gradle`, `build.gradle`, `docker-compose.yml`
- App-only runtime: `docker-compose.app-only.yml`
- Modules: `apps/{api,common,worker}/build.gradle`, `src/main/resources/application*.properties`
- Migrations: Flyway SQL files in `apps/api/src/main/resources/db/migration/`
- Deployment: `docs/deployment.md`, `deploy/`, and `.github/scripts/release.py`
- CI/CD: GitHub Actions workflows in `.github/workflows/`
- Frontend: `apps/web/package.json`, `package-lock.json`, `next.config.ts`, `Dockerfile`, `.github/workflows/build.yml`

## Frontend development and delivery

`apps/web` was copied from `globalislamicinstitute` at source commit
`7b5dbcc2aa6b9380f19488b8e92984c958398d67`. Business/UI source and dependency versions
were preserved; the original repository stays unchanged. See `apps/web/README.md`.
Use Node.js 24 and `npm ci` inside `apps/web`; run `npx --no-install next typegen` then
`npm run check`. Keep npm dependencies, .next output and real environment files ignored.
Use the frontend directory as Docker build context; the root backend context excludes it.

`build.yml` runs one CI job. Changes to either backend or frontend run both backend
tests/packaging and frontend type checking, linting and a stage build. Publication
is limited to changed components. These checks do not establish runtime API compatibility.
Frontend checks run once; PRs build stage only, while relevant main builds publish stage then prod
with distinct image tag suffixes and release artifacts. Publishing is main-only;
frontend-only runs do not publish/deploy backend images. Public Next.js variables are build-time inputs; runtime `API_URL` can
use the internal API service. Deployments are manual; the backend migration/release
flow does not deploy the frontend. Vercel uses Root Directory `apps/web` if selected.
No Sentry/video/exam business changes are authorized by the repository import.

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
- **Refresh tokens stored in:** PostgreSQL (`RefreshTokenStoreService`)
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
For local Docker-only runs, `docker-compose.yml` sets `SPRING_FLYWAY_ENABLED=true` on the API container.

**Deployment flow:** Actions publishes API, worker and Flyway migration images to GHCR. Stage deploys automatically through Dokploy after the dedicated migration Compose container exits 0. Production releases are manual in production Dokploy, using the successful stage release artifact; Actions never triggers production. API/worker do not run hosted migrations at startup.

Create distinct migration/API/worker users, then run `deploy/db/permissions.sql` with its psql identifier variables as an authorized admin. See `docs/deployment.md` for internal stage Hostinger PostgreSQL and production DO managed PostgreSQL Singapore, ownership, backups and cutover. The production env example's private DO host must be replaced with the managed public endpoint/verified CA for Hostinger; correcting configuration requires confirmation.

## Build & Test

**Build everything:**
```bash
./gradlew clean test bootJar
```

**API test split:** `test` excludes `@Tag("integration")` tests; run `./gradlew integrationTest` for the API integration suite backed by the shared Testcontainers Postgres container in `apps/api/src/test/java/com/gii/api/testsupport/SharedPostgresContainer.java`.

The container defaults to PostgreSQL 18 (`ELEARNING_TEST_POSTGRES_IMAGE` overrides it).
For a disposable native loopback DB, use `ELEARNING_TEST_DATABASE_URL` plus username/password;
the database must end in `_test` and tests delete application data. See the runbook.

**Code quality gates (configured in all modules):**
```bash
./gradlew spotlessApply
./gradlew checkstyleMain checkstyleTest
```

Outputs:
- `apps/api/build/libs/api.jar`
- `apps/worker/build/libs/worker.jar`
- `apps/common/build/libs/common-0.0.1-SNAPSHOT.jar` (library jar only)

**Java 25, Temurin JRE, Lombok code generation** all configured in root `build.gradle`.

## Profile-Based Configuration

Use Spring profiles for environment-specific settings:
- **local:** Docker Compose services (postgres, elasticmq)
- **stage:** Staging AWS/external service endpoints
- **prod:** Production AWS/external service endpoints

**Property files:** `application.properties` + `application-{profile}.properties`

Set via `SPRING_PROFILES_ACTIVE` in Docker Compose or each Dokploy service environment.

## External Integrations

- **Video:** Mux (config: `mux.signing-key-id`, `mux.private-key-pem`) and Bunny (config: `bunny.token-security-key`)
- **Object Storage:** Cloudflare R2 S3-compatible signed downloads via `R2PresignedUrlService` (config: `storage.r2.*`)
- **Payments:** SSL Commerce, bKash, Nagad webhook handlers in `PaymentApiController`
- **Live classes:** Zoom and Google Calendar/Google Meet providers in `apps/api/src/main/java/com/gii/api/service/live/`
- **Email:** Amazon SES in worker module (`EmailDeliveryService`, `SesConfig`)
- **SMS:** Bulksmsbd OTP delivery in worker module (`SmsDeliveryService`)

Launch uses Mux; review Bunny Stream around 200k delivered minutes/month or significant
library growth. Archive originals in R2/B2 and audit the existing `MediaAsset.provider`
and `providerAssetId` abstraction for portability before proposing schema changes.
720p caps, 480p mobile defaults, no next-lesson preload and 15–30 second progress pings
are planned settings/behavior to verify, not asserted current behavior. Quiz submission
is currently synchronous; worker-queued submissions require an approved durable,
idempotent design and frontend result-state changes. See the runbook launch checklist.

## Local Development

```bash
# Start local services (api, worker, postgres, sqs)
docker-compose up -d

# Start only API/worker containers (when DB/SQS are external)
docker-compose -f docker-compose.app-only.yml up -d

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
- Common shared code: `apps/common/src/main/java/com/gii/common/{dto,enums,entity,repository}/`
- API config: `apps/api/src/main/resources/application.properties`
- API services/config: `apps/api/src/main/java/com/gii/api/{service,config}/`
- API live class providers: `apps/api/src/main/java/com/gii/api/service/live/`
- API test support: `apps/api/src/test/java/com/gii/api/testsupport/`
- Worker config: `apps/worker/src/main/resources/application.properties`
- Worker services/listeners/config: `apps/worker/src/main/java/com/gii/worker/{service,listener,config}/`
- Common entities: `apps/common/src/main/java/com/gii/common/entity/`
- API controllers: `apps/api/src/main/java/com/gii/api/controller/`
- Services: `apps/api/src/main/java/com/gii/api/service/`
- Migrations: `apps/api/src/main/resources/db/migration/`
- Deployment and cutover runbook: `docs/deployment.md`
- Database permissions: `deploy/db/permissions.sql`
- Migration job and image: `deploy/migration.compose.yml`, `deploy/migrator/Dockerfile`
- Runtime environment examples: `deploy/env/`
