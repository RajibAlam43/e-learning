# Deployment and database runbook

## Architecture and ownership

| Environment | Services | Database operator |
| --- | --- | --- |
| Stage | One Hostinger VPS: Dokploy, API, worker, PostgreSQL, migration job; optional frontend | You, through Dokploy |
| Prod | Singapore Hostinger KVM 4: Dokploy, API, Next.js, worker and one-off migration job; Singapore DO managed PostgreSQL | DigitalOcean operates production PostgreSQL |

The 2026-11-24 production decision supersedes the all-in-one self-hosted production
DB plan: application compute stays on Hostinger Singapore, and PostgreSQL is managed
by DO in Singapore from launch. Moving compute to DO remains a
[future option](#future-digitalocean-production-plan), including a frontend droplet or Vercel Pro.
The production KVM 4 has 4 shared vCPUs, 16 GB RAM and 200 GB disk. Keep the previously
planned staging installation separate; the production resource budget below does
not include a staging stack. Do not put staging on the production host by default.

Use independent stage and production Dokploy installations. Create an `e-learning`
project in each, with environments named `stage` and `prod`, respectively. These are
independent configuration records for the same application. API, worker and frontend
share their environment's dashboard; production needs no remote deployment server
or multi-host Swarm cluster at launch.

API, worker, frontend and migration are separate service entries; stage's database
has its own entry too. Routine API/worker deployments never recreate PostgreSQL.
The migrator is a one-off container; it is
not a database. Stage PostgreSQL is a native Dokploy Database service with a persistent
volume and no published port. Production has no application PostgreSQL container on
the KVM 4: it connects to DO's public DB endpoint using verified TLS and trusted-source
restrictions. Dokploy's own control-plane data services remain on the VPS. A shared
project does not automatically create networking or dependencies. AWS SQS, SES, Mux
and off-host object storage remain external services.

The user measured approximately **1.5 ms average latency** from Hostinger Singapore
to DO managed PostgreSQL Singapore. This is a user-reported measurement, not reproduced
by this change or an availability/tail-latency guarantee. Record the measurement method
and confirm pooled TLS query latency and p95/p99 API behavior under representative load.

Builds and application images use Linux amd64. Provision x86-64 VPSs; add/test
multi-platform builds before selecting ARM hosts.

### Production resource budget and accepted tradeoffs

| Service | Container memory limit | CPU limit |
| --- | --- | --- |
| Production PostgreSQL | External DO managed plan; no VPS memory allocation | Provider-managed |
| Spring Boot API | 3 GB | Tune after load testing; no fixed cap decided |
| Next.js | 2 GB | 1.5 vCPU |
| Worker | 2 GB | 1 vCPU |
| Migrator | 512 MB, temporary | 1 vCPU, temporary |

The three steady-state application limits total 7 GB. Leave the remaining
nominal 9 GB for Linux, Dokploy and its own services, Traefik, filesystem cache,
backups and transient migration/deployment overlap. This is a starting budget, not
a capacity guarantee. Limits are ceilings, not reservations; CPU caps do not reserve
cores. Benchmark overlapping work on the four shared vCPUs. Avoid overlapping
backups, migrations and rollouts when their combined load would exhaust headroom.

Configure these limits in Dokploy and verify the running containers actually have
them. Keep Java heap below the full container limit to allow native memory, thread
stacks and buffers. GitHub Actions builds images; do not compile Java or Next.js on
the production VPS during normal releases.

The VPS is a single point of failure for API, frontend, worker and deployment control;
its failure no longer implies losing the production DB. DO database availability/HA
depends on the selected plan. Cloudflare cannot serve live dynamic requests when the
application origin is down. Cross-provider routing remains an external dependency.
Shared vCPU performance can vary. Hostinger's introductory pricing requires prepayment
and renews at a higher rate; track the actual purchased term and renewal amount.
The accepted disaster data-loss target is up to one hour, subject to successful
off-host backups. Recovery time is measured in the restore rehearsal, not guaranteed.

### Documentation scope and implementation status

The hosting plan records intended infrastructure; it does not provision Dokploy or
change running services. A later explicit request authorized copying the frontend
into `apps/web` and adding its Docker/GHCR publishing workflow. Other planned
application/configuration/infrastructure work still requires confirmation. The import
preserves existing frontend business behavior; no Sentry/video/exam changes were added.

In particular, `deploy/env/production-overrides.env.example` still uses a DO **private**
endpoint placeholder. Hostinger must use the managed cluster's **public** endpoint,
with the verified TLS/CA settings below. Correcting that configuration file remains
pending confirmation. The backend workflow publishes API, worker and
migrator images only; the independent `web.yml` publishes Next.js stage/prod image
variants from `apps/web`. Frontend deployment stays manual. Application work needing verification
or implementation is tracked under [Launch readiness](#launch-readiness-and-app-level-practices).

## What is implemented

- `build.yml`: PR checks; tests, SDK alignment and jar packaging on `main`; publishes
  API, worker and migrator images to GHCR after these gates succeed. Formatting and
  Checkstyle run as reports because the repository has pre-existing violations and
  the previous pipeline did not block on them. No production secrets.
- `stage.yml`: after a successful main build, obtains its manifest and calls only
  staging Dokploy over HTTPS. It runs migration first, checks actual container exit
  code 0, then updates API and worker digests and waits for Docker health checks.
- Every image reference in `release.json` is digest-pinned. Unique tags are for
  discovery; deployment uses `ghcr.io/...@sha256:...`, never `latest`.
- A successful stage release publishes `staged-release`: `release.json`, a rendered
  `migration.compose.yml`, and `PRODUCTION.md`. Production uses those exact images.
- Production is manual in Dokploy. There is no production Actions job, webhook,
  API key, SSH connection, or deploy trigger in these workflows.
- Redis dependencies/configuration are removed. Refresh tokens already use Postgres.
- `web.yml`: checks/builds `apps/web` and publishes environment-specific Linux amd64
  frontend images to GHCR. It records `web-stage-release` / `web-prod-release` artifacts
  with immutable digests. It never triggers Dokploy or production deployment.

The frontend workflow runs for frontend/workflow changes. Backend PR checks still
run to preserve existing required checks; backend image publication and automatic
stage deployment are skipped for frontend-only `main` pushes. Keep branch protection
compatible with path-filtered frontend checks. For a combined release, qualify both
workflows and record their image digests/commits together.

Stage automation starts disabled. Set repository variable `STAGE_AUTO_DEPLOY_ENABLED`
to `true` only after completing setup and the controlled data cutover below. Keeping
this switch off still permits builds/image publication without touching hosts.

## Provisioning: no custom host bootstrap scripts

1. Provision a fresh production Hostinger KVM 4 in Singapore and a separate DO managed
   PostgreSQL cluster in Singapore. Use the VPS Dokploy template or official installer.
   Keep stage on its own installation. Dokploy performs
   its own host setup; no project-specific bootstrap script is required.
2. Use separate dashboard domains, HTTPS, strong login protection/2FA, and SSH keys.
   Restrict dashboard and SSH access using provider firewalls/VPN as appropriate.
   The GitHub staging runner must reach staging's HTTPS API. Its API key needs service
   read/update, deployment read/create and Docker read permissions on these services.
   Container inspection can expose environment values: restrict that key accordingly.
3. Expose API and Next.js HTTP/HTTPS through Traefik only. Do not publish PostgreSQL,
   Docker API, worker port 8080 or extra API/frontend host ports. Docker-published ports can bypass UFW;
   review provider firewall rules and external reachability explicitly.
4. For stage, attach API/worker/migrator to the DB's internal Docker network and keep
   its external-port field empty; verify no host port is published. For production,
   allowlist the Hostinger server's actual outbound public IP in DO trusted sources,
   verify DNS/TLS to the public managed endpoint and mount the required CA read-only.
   Do not expose an unrestricted DB endpoint or assume Singapore location provides
   a shared private network between providers. Admin connections use authorized TLS
   clients; production app services never receive DB admin credentials.
5. Choose and pin one supported PostgreSQL major version for both environments.
   Local Compose and Testcontainers default to PostgreSQL 18. Set repository variable
   `ELEARNING_TEST_POSTGRES_IMAGE` (e.g. `postgres:18-alpine`) and align local Compose
   with the selected production version before release qualification.
   Matching extensions, collation/locale and privileges also matters.
6. Apply the production resource table above, including Next.js, and benchmark the
   complete stack. Keep staging limits appropriate to its own separate VPS.
7. Configure disk/log retention, Docker image cleanup, OS security updates and alerts.
   Do not prune persistent volumes. Back up Dokploy configuration separately from
   application data. Configure off-host backups and phone alerts before launch.
   Either environment's single-host failure takes all services on that host down.

Fresh hosts are preferable to installing over the old systemd/Caddy
setup. Caddy occupies ports 80/443 required by Traefik. If reusing a host, preserve
configuration, stop/disable legacy API/worker/Caddy units during a controlled cutover,
and check for duplicate SQS consumers. No legacy bootstrap scripts should be run.

## GHCR and GitHub setup

Use the repository's GitHub token for publishing. Grant the repository Actions access
to all three GHCR packages; check package permissions if packages already exist.
The package names for this repository are:

```text
ghcr.io/rajibalam43/e-learning/api
ghcr.io/rajibalam43/e-learning/worker
ghcr.io/rajibalam43/e-learning/migrator
```

Configure a read-only GHCR credential in each Dokploy installation's Registry section.
Also configure the API and worker Docker source's private-registry credentials. Check
that both stage and production hosts can pull the packages; keep
credentials in Dokploy rather than adding them to Compose files. Migration Compose
uses the registry login configured by Dokploy. Rotate credentials independently per
environment where possible. Preserve release digests while they are deployed or
eligible for rollback; Actions artifact retention is 90 days, so archive production
release manifests outside Actions before expiry if needed for long-term history.

Protect `main` and require Build and Publish Images before merging. The deployment
workflow accepts only successful main build artifacts from this repository; it does
not execute tooling from a PR or artifact. Automatic releases refuse superseded main
commits; retry a specific successful build manually when needed.

Create GitHub Environment **stage** and add:

| Kind | Name | Value |
| --- | --- | --- |
| Variable | `DOKPLOY_URL` | Stage dashboard HTTPS origin, without `/api` |
| Variable | `DOKPLOY_API_APPLICATION_ID` | Stage API application ID |
| Variable | `DOKPLOY_WORKER_APPLICATION_ID` | Stage worker application ID |
| Variable | `DOKPLOY_MIGRATION_COMPOSE_ID` | Stage migration Compose service ID |
| Secret | `DOKPLOY_API_KEY` | Stage-only key with permissions described above |

The enable switch is a **repository** variable, not an environment variable. Stage
does not need approval reviewers for automatic deployment. Optional repository secret
`SONAR_TOKEN` enables non-blocking analysis. Database, AWS, payments and runtime
credentials are now configured in Dokploy, not copied from GitHub to host `.env` files.
After cutover, retire old GitHub SSH/runtime secrets and old deployment keys only
after confirming nothing else uses them. Never give the stage key access to prod.

## Database setup and application configuration

### Stage

Create a native PostgreSQL Database service in stage Dokploy with persistent storage,
an explicit version, a strong admin password and **no external port**. Record its
internal hostname. Ensure API, worker and migrator can reach it on an explicitly
shared internal network. The migration template defaults to `dokploy-network`; set
`MIGRATION_NETWORK` to the real attached network if different. An overlay network must
be attachable for a Compose container to join it.

Create database `gii_stage_db` and distinct login roles `gii_stage_migration`,
`gii_stage_api`, `gii_stage_worker`, using a database client/tunnel or Dokploy terminal.
Use different passwords outside the repository. Dokploy's default database user is
not a substitute for this application permissions model. Do not put admin/migration
credentials into API or worker.

Run `deploy/db/permissions.sql` using psql with its four identifier variables, as shown
in the file. It grants schema creation to the migration role, current/future data
access to application roles, and installs pgcrypto/citext. An administrator may need
membership in the migration role to alter its default privileges. The migration role
must own migrated/restored application objects; grants alone do not permit ALTER TABLE
on tables owned by a different role.

### Production

Create a **clean** `gii_prod_db` on DO managed PostgreSQL in Singapore and distinct
`gii_prod_migration`, `gii_prod_api` and `gii_prod_worker` roles. Do not create an
application PostgreSQL service on production Dokploy or apply the old 4 GB DB cap.
Verify the managed version, extensions and role permissions before applying the
permissions SQL with production identifiers through an authorized admin TLS client.
Do not repurpose the old stage database or import stage test accounts/payments.
Keep database admin credentials outside runtime services.

Use the actual DO public hostname/port and CA requirements, for example for a cluster
requiring a downloaded CA mounted at `/run/secrets/postgres-ca.crt`:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://DO_PUBLIC_HOST:25060/gii_prod_db?sslmode=verify-full&sslrootcert=/run/secrets/postgres-ca.crt
FLYWAY_URL=jdbc:postgresql://DO_PUBLIC_HOST:25060/gii_prod_db?sslmode=verify-full&sslrootcert=/run/secrets/postgres-ca.crt
```

Set `SPRING_DATASOURCE_USERNAME`/`SPRING_DATASOURCE_PASSWORD` separately for API and
worker. Supply migration credentials only to the migrator. Mount the required CA
read-only in API/worker and set the migrator's `POSTGRES_CA_CERT` to its PEM value.
Verify the actual cluster's port and certificate requirements rather than assuming
this example applies to every DO edition. Restrict trusted sources to the required
egress/recovery clients. Never disable server identity verification to fix connectivity.

### Runtime services

Create API and worker as Dokploy **Applications → Docker source** on their environment's
local host. Configure registry credentials before the first deployment. Use
`deploy/env/api.env.example` and `worker.env.example` as stage checklists. Fill every
placeholder; use `production-overrides.env.example` as a per-service production
checklist, replacing its private DB host placeholder with the actual public managed
endpoint and matching CA settings. It is not one combined environment to share between services.

Preserve multiline PEM/CA values using Dokploy's quoted multiline syntax. Verify
private-key parsing and SSL connections before cutover. Keep environment secrets at
service scope: the migrator should receive only migration configuration. Stage/prod
must have separate queues, AWS permissions, buckets, payment credentials, Google
clients and callback URLs. SQS/SES currently use `ap-southeast-1` in application code.

API listens on container port 8080. Attach its public domain via Traefik. Worker gets
no public domain/port. Both images contain `/actuator/health` Docker health checks,
non-root users and a 120-second startup allowance. Configure Dokploy to retain/use
the image health check; if overriding it, preserve its path and startup time. Allow
at least 60 seconds stop grace time (Spring graceful shutdown is 45 seconds).
Health checks alone don't prove queue delivery; use functional smoke tests too.

`SPRING_FLYWAY_ENABLED=false` and Hibernate validation remain enabled in hosted
environments. The local Compose exception enables Flyway only for development.
Production payment URLs and worker SMS URL must be explicitly supplied; no live
provider endpoint is guessed in configuration. Check cookies, proxy forwarding,
CORS, OAuth redirects, webhooks and provider allowlists against the final domains.

### Next.js frontend

Run Next.js as a separate Dokploy application on the production host with a 2 GB
memory limit and 1.5 vCPU cap. Its Next.js App Router source now lives in `apps/web`,
copied from the frontend repository without business/UI changes. Standalone output
supports the container runtime. Use the stage/prod image from the independent
`web.yml` pipeline and record its digest beside the backend release. See
[the frontend guide](../apps/web/README.md) for import provenance, local verification,
repository build variables, image tags and artifacts. This import does not change
the existing Vercel site or configure Dokploy remotely.

Configure the frontend domain through Traefik, its real container port, health check,
production API URL, cookie/auth behavior and runtime/build-time environment separately.
Anything embedded as `NEXT_PUBLIC_*` is public and may require rebuilding the image
when changed. Public URLs/Google ID and image-host allowlists are compiled into the
stage/prod variants; changing runtime variables cannot promote one variant into the
other. Supply any non-default `WEB_STAGE_*` / `WEB_PROD_*` repository variables before
building, and validate Google login when used. Runtime `API_URL` remains server-only.
Server-side calls can use the internal API address where compatible;
browser requests must use the public HTTPS API hostname. Align frontend/backend
versions and drain gracefully during updates. Verify the Next.js multi-instance cache
and Server Action requirements before later adding replicas.

Before leaving Vercel, verify SSR pages, login/refresh, redirects, optimized images,
course/lesson routes, quiz flows and payment return pages. Correct canonical/Open
Graph URLs to the frontend hostname; the inspected staging homepage used the API
hostname. Keep Vercel available for a deliberate rollback window until the production
frontend passes smoke tests.

### Internal networking and public endpoints

Use actual Dokploy-generated service DNS names and container ports for same-host
server-to-server traffic. Project/environment labels alone do not establish DNS
reachability: attach each required client to the appropriate Docker network and
verify resolution/connectivity from inside its running container.

| Caller | Destination | Connection |
| --- | --- | --- |
| Next.js server runtime | API | Internal HTTP service address, e.g. `http://API_INTERNAL_HOST:8080`, where auth/routing permit |
| API | PostgreSQL | Prod: DO public managed JDBC endpoint with verified TLS/IP allowlist; stage: internal DB service |
| Worker | PostgreSQL | Same environment-specific endpoint; separate worker DB role |
| Migrator | PostgreSQL | Same environment-specific endpoint; migration role and one run per release |
| Browser | Frontend and API | Public HTTPS hostnames; Docker service names are not browser-resolvable |
| Payment/OAuth/webhook provider | API callback | Public HTTPS hostname with the expected externally registered URL |
| API/worker | AWS SQS/SES and other external providers | Provider endpoints; these do not become local Docker services |

The worker currently consumes SQS and accesses PostgreSQL directly; do not add an
API dependency merely to implement internal routing. Next.js calls the API rather
than connecting directly to PostgreSQL. Keep public and server-only API base URLs
separate; do not embed a Docker hostname in `NEXT_PUBLIC_*`. On a later Vercel move,
server-side requests must use the public API because Vercel is outside this network.

Do not use `localhost` for another container. Stage DB traffic stays internal;
production DB traffic intentionally crosses to DO over its TLS public endpoint.
Keep stage DB/worker ports unpublished; frontend/API ingress goes through
Traefik. Prefer network membership limited to the clients that need it, and do not
assume a network named `dokploy-network` isolates every project from every other
project. Verify the migrator's external network is attachable as required by Compose.

Test server-side login/session behavior before replacing public API calls with an
internal address. Forward only the required cookies/auth headers, preserve the real
public HTTPS host/origin where needed, and keep callback/redirect/cookie domains public.
Check CORS using the actual staging frontend, including
`https://globalislamicinstitue.vercel.app` while that deployment remains in use.
Staging and production must not share DB networks, credentials or queues.

## Cloudflare and monitoring

Use Cloudflare DNS with proxied production frontend/API records, TLS **Full (strict)**,
and a valid origin certificate in Traefik. Retain the required DNS/certificate recovery
access outside the VPS. Cloudflare supplies edge caching and DDoS protection; it does
not provide a second application origin. Restrict direct origin access where practical,
while preserving certificate renewal and approved operational access.

Cache public static assets. Bypass shared caching for authentication, cookies and
personalized pages, API responses containing private data, exams and payment callbacks.
Do not apply a blanket cache-everything rule. Verify cookie isolation between users,
forwarded HTTPS/client IP handling, CORS and webhook reachability after proxying.
Course video delivery remains on Mux/Bunny rather than through the VPS.

Protect the Dokploy dashboard with 2FA or Cloudflare Access, preferably both. Do not
leave a publicly reachable alternate dashboard port/IP that bypasses Access. Keep
recovery codes and an SSH recovery path outside the server. Stage automation still
needs authenticated machine access to the stage API: test its Access/service-token
integration before protecting that endpoint; the existing release client does not
currently add Cloudflare Access service-token headers.

Choose a free external uptime monitor with phone notifications and test delivery
before launch. Free app push notifications can satisfy this; verify the chosen plan
if SMS or voice calls are required rather than assuming they are free. Monitor the
frontend, API health and a representative user journey. Separately alert on backup
failure/staleness, disk growth, memory/OOM, CPU pressure, DB connections and queue age/
DLQs. An external heartbeat/dead-man check must notice missing backups even when the
VPS and Dokploy themselves are down.

### Planned Sentry error tracking and log operations

Add **Sentry for API, worker and Next.js** before production launch as a separate
observability task. No Sentry SDK, DSN wiring, source-map upload or account setup is
implemented by this documentation update. Obtain confirmation before changing code,
dependencies, build pipelines or runtime configuration; business behavior stays outside
this task. Dokploy metrics, local logs and external uptime/backup alerts remain in use.

1. Create separate Sentry projects for API, worker and frontend under the production
   operator's account. Start with the free Developer plan if its user access, event
   quotas and notification options meet requirements; verify current
   [plan allowances](https://sentry.io/pricing/) rather than assuming unlimited events
   or free SMS/phone alerts. Keep the external phone-alerting plan independent.
2. Select the official Spring Boot integration compatible with the pinned Java/Spring
   versions for API/worker, and the Next.js SDK for frontend browser/server errors.
   Verify worker listener/job failures are captured with useful stack traces while
   expected retries/validation failures do not flood quotas or create duplicate events.
3. Plan per-service `SENTRY_DSN`, environment (`stage`/`prod`) and release identifiers
   derived from the deployed commit/image or frontend deployment. These names describe
   intended integration inputs, not environment variables already wired in this repo.
   Separate stage/prod filtering and alert routing; tag worker errors by job type
   without including full job payloads or recipient identities.
4. Upload frontend source maps during its CI/Vercel build so minified production errors
   are actionable. Keep upload/auth tokens only in CI/provider secrets, never browser
   bundles or committed files. A browser DSN is intentionally public; it is not an
   account-management or source-map-upload token. Do not expose raw source maps publicly.
5. Disable default PII collection and review SDK hooks/scrubbing. Exclude cookies,
   authorization headers, passwords, JWTs, Mux/R2 signed URL query tokens, payment
   payloads, email/phone details and exam answers. Review existing application logs
   before exporting them; log retention/export must not silently expose these values.
6. Start with error tracking. Leave replay/profiling disabled unless explicitly needed;
   choose conservative trace sampling, event filters and quota controls separately.
   Set production error alerts and monitor usage. Reporting must not delay requests,
   prevent startup, or cause SQS jobs to fail merely because Sentry is unavailable.
7. Validate on staging with synthetic non-sensitive errors from API, a worker job and
   frontend browser/server code. Confirm correct project/environment/release, readable
   stack traces, secret scrubbing, retry/deduplication behavior and notification delivery.
   Verify the application still works when outbound Sentry reporting fails. Do not
   manufacture a real payment, email or SMS failure to test reporting.

Keep stdout/stderr logs available in Dokploy with bounded Docker log rotation and
disk retention. Correlate errors using non-sensitive request/job identifiers; record
deployments and migration outcomes with the release. Error tracking is not an audit
log, uptime monitor, off-host backup or replacement for DB/queue health monitoring.

## Configure the migration service in each Dokploy

Create a **Docker Compose** service named `migrator` in the same environment as API
and worker. Use **Raw** source and **Docker Compose**, not Stack. Run it on the local
Hostinger host for each environment. It consumes a prebuilt GHCR
image; it does not build source on the host.

Set service-scoped environment from `deploy/env/migrator.env.example`. Set production
values from the production checklist instead. `FLYWAY_URL` must use the migration
user's target DB, not application credentials. For the production managed DB use:

```text
jdbc:postgresql://DO_PUBLIC_HOST:25060/gii_prod_db?sslmode=verify-full&sslrootcert=/run/secrets/postgres-ca.crt
```

The Compose template supports a CA file using inline configs (requires Docker Compose
2.23.1+). Stage can leave `POSTGRES_CA_CERT` empty for its internal non-TLS DB;
production supplies the managed cluster CA. In stage, `MIGRATION_NETWORK` attaches
to the internal DB network; in production, use an existing app-host network with
working DNS/internet egress. It does not make the external DO DB a local Docker service.
Keep **Create .env file** enabled, **Randomize** and **Isolated Deployment** disabled,
no public domains/ports, and no independent auto-deploy trigger. Do not add app/DB
containers to this dedicated Compose definition. It has `restart: "no"`.

For stage, automation supplies the rendered Compose content before deploying it.
For production, paste the `migration.compose.yml` from the successful staged-release
artifact. Do not deploy the repository's unsubstituted `__...__` template. A new release
label forces Compose to replace the previous job container. To rerun the same release
manually, append `--force-recreate` to the **full default Compose command shown in
Dokploy** (its custom command replaces the whole default), or change the release label.

For the initial stage cutover, before automation is enabled, download `release.json`
from a successful main build's `release-manifest` artifact. Copy the repository's
migration template into stage Dokploy, replace `__MIGRATOR_IMAGE__` with that manifest's
`images.migrator` digest and `__RELEASE_ID__` with a unique label such as
`initial-cutover-BUILD_RUN_ID`, then deploy and verify exit 0. Set initial API/worker
image fields from the same manifest. After cutover, enable/retry stage automation to
produce the successful staged-release artifact required for production promotion.

Successful migration leaves an **exited (0)** container, which is expected. Dokploy's
deployment status Done only indicates Compose was deployed; check the actual exit
code, container's image digest/release label and Flyway logs. A nonzero exit, timeout,
missing container or unverifiable status blocks application deployment. The stage
helper performs these checks using Dokploy's container inspection API without logging
inspection bodies or credentials. Keep exited migration containers until verification
finishes; automatic cleanup during migration verification can cause a safe failure.

## Releases

### Automatic staging

After setup/cutover, enable `STAGE_AUTO_DEPLOY_ENABLED=true`. A successful main image
build triggers stage deployment. The release helper validates the manifest's build
run, commit and image digests, confirms all service IDs belong to an environment
literally named `stage`, checks for active deployments/running migrations, then:

1. Updates and deploys the dedicated migration Compose service.
2. Waits for that deployment and the matching migration container's successful exit.
3. Updates/deploys API and checks a running healthy container uses the exact digest.
4. Updates/deploys worker and performs the same check, including remote server lookup.
5. Publishes the staged-release artifact for manual production promotion.

Releases serialize under one stage concurrency group and are never cancelled by a
new push. A newer pending run may replace another pending run in GitHub's concurrency
queue; use manual workflow_dispatch with `build_run_id` to retry a selected successful
main build. Do not deploy concurrently from the stage dashboard. If the workflow is
cancelled or times out, inspect Dokploy: a remote job may continue running.

### Manual production: no GitHub-triggered deployment

1. Select a **successful Deploy Stage with Dokploy run** and download its
   `staged-release` artifact. The build's release-manifest alone is not staging approval.
   Review `PRODUCTION.md`, release notes, pending migrations and recovery point.
2. Ensure only one operator/release is active. For breaking schema changes, stop writes
   and drain/stop worker processing first, using a planned maintenance window.
3. In production Dokploy paste the artifact's migration Compose file into the existing
   migrator service. Preserve its production secrets/network. Deploy it.
4. Verify the matching migration container exited **0** and Flyway applied the expected
   versions to the correct DB. If anything fails, stop here; do not deploy applications.
5. Set API's Docker image to the artifact's API digest and deploy; wait for healthy.
   Set worker's image to its worker digest and deploy; wait for healthy. Keep registry
   credentials intact. Do not rebuild images or substitute mutable tags.
6. When the frontend changes, download `web-prod-release` from a successful frontend
   build and manually deploy its compatible, qualified digest in Dokploy too. Confirm
   that commit's stage variant was tested and production public settings are correct.
   It is not included in the backend staged-release artifact. Record both versions
   and verify each rollout fits host RAM. Frontend-only releases need no Flyway job
   or backend rollout; use the current compatible backend schema/version.
7. Check login/refresh/logout, course reads, email/SMS queue processing, enrollment and
   payment flows/provider callbacks. Watch queue age, DLQs, errors and DB connections.
8. Record digests, schema version, operator and time in your release log. Keep previous
   digests and backups until the rollback window closes.

Independent Dokploy services do not provide automatic cross-service ordering. These
production steps are deliberately operator-controlled. Full production automation
would require a separate explicit decision and is not configured here.

## Migrations and recovery rules

Add a new `V...__description.sql` for each change. Never edit applied migrations or
auto-run `repair`, `baseline` or `clean` to make a failure disappear. The migration
image pins Flyway 12.4.0 and disables clean, baseline-on-migrate and out-of-order mode.
Flyway validates checksums before applying pending SQL. API and worker share one
database, so run one migration job, not one per application instance.

Test both a fresh DB and upgrading a copy of the previous release's schema with data.
The current history includes dropped columns/restructured tables; don't assume every
pending historical migration is safe with a currently running old app. Prefer expand,
deploy/backfill, then contract in a later release. Long DDL can lock live tables; review
lock/time budgets and nontransactional operations separately.

When migration fails, applications stay on their prior images, but earlier migrations
in that run may already have committed. PostgreSQL transactional DDL does not make the
entire release atomic. Inspect history and actual schema before retrying. If migration
succeeds and deployment fails, the new schema remains. There is no automatic database
rollback. Rolling back one/both application digests is safe only if the schema supports
them. Prefer a forward fix when data compatibility is uncertain.

## Backups, restore and PostgreSQL maintenance

### Managed production backups and independent exports

Use DO's managed PostgreSQL backups/recovery and verify the purchased cluster's
actual retention, recovery point, standby/HA settings and restore process before launch.
Test a restore into an isolated cluster; managed recovery creates a new cluster and
requires validating/reconnecting clients. Do not assume every managed plan provides
the same HA or recovery guarantees. Restoring an older state requires reconciling
payment events and queued jobs before processing resumes.

Retain the prior **up-to-one-hour disaster data-loss target** for independent recovery
unless explicitly revised. Plan hourly logical `pg_dump` exports from the managed DB
to R2 or B2 outside both application hosting and the DB provider. This requires a
separately approved/tested external-DB backup job (for example, a scheduled backup
container managed through Dokploy), verified TLS, scoped DB/bucket credentials and
working DO trusted sources. Dokploy's native backup for a local PostgreSQL Database
service does not automatically cover the external production cluster. That job has
not been implemented or configured by this change. Use separate prefixes/credentials
for production backups, stage backups and video originals.

Run a manual backup and verify the uploaded object, then verify at least one scheduled
run. Set an explicit retention policy; a starting point is 168 hourly copies (seven
days), adjusted for measured database size, storage costs and recovery needs. Longer
retention can use a separately configured daily archive. Ensure lifecycle expiration
does not remove required copies. Protect recovery access and encryption credentials
outside the VPS; keep backup permissions scoped to the destination.

The accepted recovery-point target is **at most one hour of data loss**. An hourly
schedule alone cannot guarantee that: a missed run, upload failure or long-running
dump makes the latest recoverable snapshot older. Monitor the last successful
recoverable backup's snapshot time, backup duration, failures and object availability;
alert before/when the one-hour budget is breached. If duration makes the target
unattainable, shorten the interval or revisit the backup design. These are logical
snapshots, not continuous WAL archiving/PITR. Take an additional backup before risky
migrations and verify it completed.

Stage remains separate: configure its native Dokploy PostgreSQL off-host backups and
document its retention/recovery window. Test independently; stage backup credentials
must not access prod. Hostinger snapshots alone do not cover the external production DB.

Dokploy's documented PostgreSQL dump excludes owners and grants. Recreate roles,
restore application objects under the migration owner, then reapply permissions and
verify API/worker access. Do not restore as an application user or assume restored
admin-owned objects can later be altered by the migrator. Verify the actual restored
ownership, including `flyway_schema_history`, before enabling releases. Back up
Dokploy configuration separately to off-host storage after configuration/secret changes
and on a schedule; its configuration backup does not replace this application DB dump.
Retain image digests, PostgreSQL/Dokploy versions, secret recovery access, DNS details
and original video inventory outside the VPS too.

### Application-host failure versus database recovery

Before launch, rehearse rebuilding application hosting on a separate VPS and restoring
a database copy in isolation. A DO droplet or Hostinger in another region is acceptable
for replacement compute. Do not overwrite production to rehearse. Recovery must work
without access to the original Hostinger VPS.

1. Declare the incident and determine whether only compute or also the DB is affected.
   If DO PostgreSQL is healthy, keep the live DB intact: replacing the VPS requires
   reconnecting to it, not restoring an older dump and discarding valid transactions.
   For DB recovery, record the selected backup/PITR point and pause writes/releases.
   Isolate/fence the old host if reachable. Keep replacement workers stopped and
   prevent competing writers if the old server later returns.
2. Provision an adequately sized x86-64 VPS with persistent disk and install the
   recorded compatible Dokploy version. Restore its configuration or recreate the
   documented services. Restored services must not start processing real jobs/writes
   before their database and secrets are verified.
3. For a host-only failure, allowlist the replacement host egress IP and verify TLS
   to the existing DB. For DB loss, restore a new managed cluster using DO recovery
   or restore the latest verified independent dump to an appropriate PostgreSQL host.
   A temporary self-hosted fallback needs additional memory/disk beyond the application
   budget and an internal-only DB port. Verify extensions, roles, migration ownership,
   grants and `flyway_schema_history`; use compatible PostgreSQL/restore tooling.
4. Deploy the API, worker and frontend digests compatible with the live/recovered schema,
   configure secrets, networks and limits, and check Flyway history/schema validation.
   Supply the actual DB endpoint/CA and keep the migrator stopped unless a deliberate
   schema upgrade is needed. Do not deploy `latest` during recovery.
5. Validate representative accounts, enrollments, lesson progress, quiz attempts and
   payments. Test application-role access and migration ownership. Rehearsals use
   isolated queues and sandbox/disabled external side effects, never production jobs,
   email, SMS, payment calls or callbacks from the restored copy.
6. For an actual disaster, reconcile external payment events, SQS/DLQs and other side
   effects since the dump. Restoring the DB does not rewind queues, payment providers
   or Mux. Lost DB records can coexist with already-delivered jobs or paid orders;
   preserve evidence and resolve those before enabling affected processing.
7. Validate origin HTTPS and health using a temporary protected hostname or a local
   hostname override. Point Cloudflare's production DNS origin records to the new IP
   only after validation; retain the existing public hostnames. Restore provider
   allowlists/callback reachability, then enable the intended worker and user traffic.
8. Verify login, lesson, quiz and payment flows, run a new off-host backup, and test
   phone alerts. Record actual recovery time, recovered snapshot time and data loss.
   Keep the previous host fenced until safe retirement; do not flip back after new
   writes without a data reconciliation plan.

Record the rehearsal date, backup object/snapshot, versions/digests, elapsed recovery
time, ownership checks and smoke-test results. Production launch requires a passing
restore and a tested notification. Repeat after material backup/version changes.
Recovery duration includes provisioning, any required restore, validation and DNS
transition; it is not an instant failover. Full DB restore is not routine app rollback
or a required step for an application-host replacement with a healthy managed DB.

Pin PostgreSQL versions. Plan minor updates/restarts and test major upgrades separately;
changing a container major tag is not a data migration. Persistent storage protects
against container replacement, not disk/host failure or deletion. Monitor disk, DB
connections, slow queries and queue backlogs. During rolling updates, old/new app
instances may coexist and temporarily double connection-pool demand.

## Video hosting and portability

Stay on **Mux** for launch. Track actual delivered minutes, stored library minutes,
resolution and invoice totals. Roughly 100 students is a planning estimate, not a
promise of free hosting: Mux currently includes 100,000 delivery minutes/month, while
storage and other applicable usage can still be charged. Recheck pricing at launch.

Review moving to **Bunny Stream** when delivery regularly exceeds approximately
200,000 minutes/month or library growth makes storage cost significant. This is a
review trigger, not an automatic migration or a universal break-even point. Compare
current bills, bandwidth/resolution mix, migration effort and playback requirements.

Keep original video files in R2 or B2 independently of the streaming provider and
outside the VPS. Record a stable object key and a mapping from each lesson/media
record to its original. Verify originals are recoverable before deleting source
files. Use authenticated uploads/signed access; do not expose private course originals.

The desired abstraction is a generic `video_provider` + `video_id`, with provider-
specific playback/upload metadata behind it. The existing `MediaAsset` already has
`provider`, `providerAssetId`, `providerLibraryId` and `playbackId`; Mux-specific upload
and webhook records also exist. Reuse/audit that abstraction rather than blindly
renaming schema fields. A provider change needs a mapped re-ingestion, playback-token,
webhook and player verification plan. Original-file archival and end-to-end portability
are not established merely by the current fields; any required code/schema changes
need confirmation first.

Target a maximum playback resolution of **720p**, default mobile playback to **480p**,
and disable preloading of the next lesson. Check provider/player settings, mobile
quality selection and text legibility; provider encoding quality (such as Mux `basic`)
does not itself prove a 720p playback cap. These frontend/provider settings remain
to be verified or implemented with approval.

## Launch readiness and app-level practices

The following is the launch checklist. Entries describe required verification or
planned work, not a claim that changes have been implemented by this doc update.

- Apply and inspect all application container limits and the managed DB's selected
  plan/settings; no production application DB runs inside the VPS budget. For stage,
  tune its local PostgreSQL memory/connections against its own smaller host budget.
  Confirm VPS disk headroom for images, logs and export scratch space, and monitor
  managed DB storage separately. Verify graceful shutdown and rollout headroom.
- Set DB pools explicitly. The current API defaults are max 10/min idle 2; worker
  defaults are max 5/min idle 1. Set the corresponding Dokploy overrides
  `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` and
  `SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE`, then validate against load and PostgreSQL's
  `max_connections`. Budget separately for migrations, backups, administration and
  overlapping replicas. Next.js should call the API, not open its own DB pool.
- Run **k6** against login/refresh, lesson access/progress and quiz start/submit on a
  production-sized isolated stack with representative data. Test expected concurrency
  and exam submission bursts, with simultaneous worker and backup load. Record p95/p99
  latency, errors, CPU throttling, memory, disk I/O, pool waits and queue age. Agree
  pass/fail targets before the run and prevent real email/SMS/payment side effects.
- Send video progress pings every **15–30 seconds**, rather than every few seconds.
  Flush appropriately on pause/end/navigation, account for browser unload limitations,
  and preserve accurate completion semantics. Verify/change this in the frontend
  under `apps/web` with approval; it is not configured by this runbook.
- Let students open exam pages early to spread loading, without revealing questions
  or starting attempts before the allowed time. Preserve server-enforced access and
  deadlines. Early loading alone does not remove database/write bursts.
- The planned burst-handling path is queued exam submissions processed by the worker.
  The current `QuizAttemptSubmitService` processes submissions synchronously, and
  `JobsListener` has email/SMS/payment consumers, not an exam consumer. Before changing
  this, approve an API/worker/frontend design that durably records acceptance and
  authoritative submission time before acknowledging, handles retries idempotently,
  survives a queue-publication failure and exposes pending/result states. A queue
  buffers processing; it does not remove the initial durable-write requirement.
- Complete the off-host restore rehearsal, test phone alerts, verify Cloudflare cache
  isolation and TLS, and run final login/lesson/quiz/payment smoke tests before launch.
- Complete the approved Sentry integration and synthetic error/alert checks for API,
  worker and frontend; verify the internal network paths and data-cutover evidence.

### Changes requiring confirmation before implementation

1. Update the production example's private DB hostname to a public DO endpoint
   placeholder and verify CA mounts/trusted sources. Approve/configure the external
   managed-DB export job. Limits, backups, Cloudflare and DB settings have not been
   applied remotely by this documentation change.
2. Configure/test the frontend image pipeline's repository variables, GHCR read access
   and manual Dokploy service. Source import, Dockerfile and publishing workflow are
   implemented; deployment configuration and rollout still need operator approval.
3. Add k6 scenarios and agreed load thresholds.
4. Implement any missing video-original archival, provider portability, playback caps,
   mobile defaults, preload changes and progress throttling.
5. Design and implement queued exam submissions and any early-opening changes.
6. Add Sentry error tracking to API/worker and frontend, its build/release metadata,
   source maps, privacy controls and alert configuration. This is observability work,
   not authorization to change business behavior.

## Upgrade path

Managed PostgreSQL in Singapore is now part of the launch baseline, rather than a
future first upgrade. Scale its plan, pooling or standby/HA configuration based on
measured load and availability needs; retain independent exports and restore tests.
The reported 1.5 ms mean cross-provider latency supports the selected placement,
but does not replace production p95/p99/load measurements or outage recovery tests.

Then add application redundancy: another host running compatible API/frontend/worker
instances, health-aware traffic routing independent of a single host, and tested
failover. Audit shared secrets, Next.js cache/Server Actions behavior, duplicate jobs,
scheduler coordination and total DB pool demand. Size the remaining host to carry
the expected load after a failure. A second VPS alone does not establish HA, and a
load balancer/Dokploy manager located only on the failed host can still block recovery.

### Future DigitalOcean production plan

This is a future compute/frontend alternative to Hostinger, not an instruction to
provision droplets now. DO managed PostgreSQL Singapore is already in the launch
baseline and can remain the same cluster during a compute move. Staging remains
on its separate Hostinger installation. Moving compute to DO does not itself add HA.

| Component | Future placement | Management |
| --- | --- | --- |
| API | DO app droplet; production Dokploy can also run here initially | Production Dokploy, prebuilt GHCR image |
| Worker | Separate DO worker droplet | Same production Dokploy, Remote Server, prebuilt GHCR image |
| Database | DO managed PostgreSQL | DO operates the database; migrations remain a one-off Dokploy job |
| Frontend, option A | A third, separate DO frontend droplet running Next.js | Same production Dokploy, Remote Server, prebuilt frontend image |
| Frontend, option B | Vercel Pro instead of the frontend droplet | Vercel manages frontend hosting; coordinate its production release separately |
| Migrator | One-off container on the app droplet | Production Dokploy; run once per backend release |
| Cache | No dedicated cache droplet planned | Revisit only when application requirements justify it |
| Originals/backups | R2 or B2, outside the application hosts | Retain independent recovery access and retention |

API/worker separation is the baseline DO layout here. If application redundancy is
needed, deliberately extend it with replicas and independent health-aware routing;
do not mistake the separate worker/frontend hosts for redundant API capacity. Choose
droplet sizes from observed container usage and load tests, leaving OS/Dokploy/rollout
headroom. Do not copy a 3 GB API limit onto a droplet with only 3 GB total RAM.

Use one production Dokploy installation to manage the DO app, worker and optional
frontend droplets, with separate services in the same production project/environment.
Do not create a dashboard installation per application or give staging control of
production. Use Dokploy's supported server setup instead of reviving the old project
bootstrap scripts. Moving the production control plane from Hostinger requires its
own tested configuration/secret backup and restore or deliberate service recreation;
moving an image does not transfer dashboard configuration automatically.

#### Current hybrid baseline and future private networking

Launch keeps API/worker/Next.js on Hostinger Singapore and PostgreSQL on DO Singapore.
Use the managed public endpoint with verified TLS and narrowly allowlisted Hostinger
egress IPs. A DO private endpoint is not directly reachable from Hostinger. The user
reports 1.5 ms average latency for this pair; retain measured query/API tail-latency
checks. There is no self-hosted production DB to migrate as a prerequisite.

When backend compute moves to DO, place its droplets in the managed database's
region/VPC and switch API, worker and migrator to the private endpoint, retaining
verified TLS and required trusted-source restrictions. Evaluate Singapore first for
the Bangladesh/South Asia audience, then validate real routes and regional product
availability. Obtain the actual endpoint, port and certificate requirements from the
selected cluster rather than copying stale values. DO documents the distinction
between [public and private database connections](https://docs.digitalocean.com/products/databases/postgresql/how-to/connect/).

Retain independent logical backups to R2/B2 and verify managed backup/PITR retention
and recovery on the purchased plan. Backup scheduling may need a different mechanism
for an external managed database; the native Dokploy local-Postgres backup job must
not be assumed to cover it automatically. Rehearse recovery even when the managed
recovery service is enabled. Keep provider permissions, migration ownership and
extensions compatible. Any DB rollback after production writes needs reconciliation,
not just restoring an old dump; an ordinary compute move keeps the live DB intact.

#### Frontend option A: dedicated DO droplet

Deploy Next.js as a Docker-source application on a separate frontend droplet, managed
remotely by production Dokploy. Build its image with this monorepo's `web.yml`, retain
the digest with each release, and promote production manually. Validate memory/CPU,
health checks, image optimization, persistent/cache needs and graceful updates.
Use a private API address for server-side calls only where the application's auth
and routing allow it; browsers continue using the public HTTPS API hostname. The
frontend does not connect directly to PostgreSQL.

Keep Cloudflare DNS/proxy, origin TLS and private-response cache exclusions for this
droplet. The separate frontend host isolates its resource load but remains a single
frontend origin until replicas and failover routing are added. Budget its droplet,
backups and maintenance separately from the app/worker droplets and managed DB.

#### Frontend option B: Vercel Pro

Keep or move Next.js to Vercel Pro and omit the frontend droplet. This is the managed
frontend alternative to Dokploy hosting, not another Dokploy-managed service. Review
the current [Vercel Pro pricing and usage allowances](https://vercel.com/pricing) when
adopting it, and configure spend alerts. Select a supported server-rendering region
near the public DO API where possible; CDN assets being nearby does not imply SSR
execution is nearby.

Use the public HTTPS API endpoint for Vercel server-side calls and browser traffic;
Vercel is not a member of the DO VPC. Configure the frontend's custom domain using
Vercel's domain instructions, retaining Cloudflare as DNS provider. Do not assume
the droplet's Cloudflare proxy/cache settings should be copied unchanged to Vercel.
Keep production cookies, CORS, OAuth/payment return URLs, canonical metadata and
preview-environment isolation aligned with the public frontend/API hostnames.

Coordinate frontend/backend versions and use an explicit production promotion step;
do not introduce automatic production promotion as part of this documentation plan.
Vercel uses its supported frontend build/deploy workflow rather than consuming the
backend GHCR release artifact. Connect the monorepo with Vercel Root Directory
`apps/web` and set its build/runtime variables before switching from the source repo.
Record the frontend commit/deployment identifier with
the backend image digests and verify rollback compatibility.

#### Compute and frontend cutover

Prepare destination services with the currently deployed versions first, keeping a
hosting move separate from new application/schema changes. Verify DB access, GHCR
pulls, secrets, domains, TLS, health checks and alerts before switching traffic. A
compute-only move reuses the live managed DB; no dump/restore is needed unless the
database itself changes. Move
workers with a controlled handover and in-flight-job accounting. Verify login, lessons,
quizzes, progress, payments/callbacks and video playback before retiring old hosts.
Update Cloudflare's origin/DNS or the frontend domain target as applicable. Keep the
previous services available but fenced during the rollback window, and never run
workers attached to different databases against the same production queues.

No DO resources, Vercel plan changes, workflow changes or runtime configuration are
applied by this documentation. Implementing any phase requires user confirmation.

## One-time stage cutover from the existing DO setup

The intended destination is the separate Hostinger staging installation, not the
production KVM 4. A KVM 1 is a budget candidate only after verifying the full staging
stack fits 4 GB with smaller limits; the production 7 GB application ceilings do not
include stage's additional local DB and cannot be copied to this smaller VPS.
If retaining DO staging temporarily, budget for it: powered-off droplets still incur
charges, and the managed DB remains a separate billable resource. Stopping the apps
is not a zero-cost staging strategy. See [DO billing](https://docs.digitalocean.com/support/i-dont-recognize-a-charge-on-my-invoice/).

1. Inventory the actual droplets, DB version, migration history, data size, existing
   runtime secrets, DNS, provider callback URLs, queues/DLQs and permissions. Names
   in old scripts are systemd service names, not proof of actual DO resource names.
2. Keep automatic stage deploy disabled. Provision/configure Hostinger services and
   backup destinations. Create roles and test a DO export/Hostinger restore including
   Flyway history on a separate rehearsal database. Verify ownership and extensions.
3. Run upgrade rehearsal from the current DB's real version to the proposed release.
   Use separate rehearsal queues or keep all rehearsal workers/listeners stopped.
4. Start a maintenance window: stop old API writes and all old stage workers. Drain
   or account for queued/in-flight jobs. Never run workers pointing at different DBs
   against the same SQS queues. Confirm no scheduled producers keep writing.
5. Take a final consistent dump/export; restore into Hostinger with correct ownership,
   preserve flyway_schema_history, reapply permissions, and verify representative data.
6. Run the release's migration job. Configure final stage DB endpoints/secrets and
   switch DNS/callback routing. Start API and exactly the intended worker deployment.
7. Smoke-test auth, courses, payment sandbox, callbacks and email/SMS. Run an off-host
   backup and restore rehearsal. Then enable automatic stage deployment and trigger
   the selected successful main build via stage.yml if necessary.
8. Keep the old infrastructure stopped but available for a recorded, bounded rollback window.
   Switching back after new writes needs a data/queue reconciliation plan, not just DNS.
   Retire old resources only after validation and an independent backup. DO documents
   that destroying a database cluster also destroys its associated managed backups.

### Data selection, validation and retirement

Before cutover, inventory what the DO staging database actually contains and identify
what must survive: course/reference content, object/video identifiers and any legitimate
records that cannot be recreated. Record the export snapshot time, source PostgreSQL
version, migration history, dump object/checksum and the destination database. Encrypt
the off-host export and retain its recovery credentials outside both servers.

For **stage-to-stage migration**, rehearse a full consistent `pg_dump`/restore of the
stage DB, including Flyway history, ownership and grants as described above. Validate
representative row counts, relations, object keys and media-provider references. DB
dumps do not contain R2/B2 objects, video files or SQS messages: verify those separately
and keep restore rehearsals away from real outbound side effects. Preserve the existing
stage identity/environment while changing its host; update internal endpoints and
verify container connectivity before enabling automated deployment.

For **production initialization**, create a clean DB with production credentials and
the full migration history. Explicitly select any course/reference content to promote,
review a reproducible import and validate IDs/relations against the production schema.
Do not restore the whole stage DB over production or copy test users, refresh tokens,
sandbox payments, test enrollments, stage queue state or stage secrets by default.
Review content references to stage buckets/Mux assets and remap deliberately to the
intended production storage/account. Any import scripts or application changes need
confirmation before implementation. Deploy production manually after data validation.

For the **old DO staging DB**, record the responsible operator and the rollback-window
end date. Keep it inaccessible to new writers/consumers after cutover. Retain an
independent export in R2/B2 and verify a restore without relying on DO's managed backups.
Once the new stage is validated and the rollback window closes, obtain explicit
retirement approval before destroying the old DB/droplets and any unused resources.
Confirm billing afterward; snapshots, volumes or other retained resources can continue
to incur charges. This runbook does not authorize deleting existing infrastructure.

Do not confuse this stage relocation/selected-content promotion with a later migration
of a **live production database** to a different cluster/provider. DO managed PostgreSQL
Singapore is already the production baseline. Any later DB migration must preserve
production users, payments, enrollments and progress through the controlled write pause,
final export/restore, validation and reconciliation in the upgrade plan.

## Local verification without Docker

The normal suites use Testcontainers. If Docker is unavailable, create an isolated
PostgreSQL instance/database and set these environment variables when running Gradle:

```sh
ELEARNING_TEST_DATABASE_URL=jdbc:postgresql://127.0.0.1:55449/elearning_deployment_test \
ELEARNING_TEST_DATABASE_USERNAME=postgres \
ELEARNING_TEST_DATABASE_PASSWORD=your_local_test_password \
./gradlew --no-daemon verifyAwsSdkVersionAlignment test :api:integrationTest bootJar
```

Tests delete application data. The override deliberately accepts only loopback hosts,
a database name ending in `_test`, and a JDBC URL without query/embedded credentials.
Use a fresh disposable database, never a real stage/prod DB or a tunnel to one. Choose
the same PostgreSQL version as the deployment target for final release qualification.

## Reference documentation

- [Dokploy remote servers](https://docs.dokploy.com/docs/core/remote-servers)
- [Dokploy Compose and private registries](https://docs.dokploy.com/docs/core/docker-compose)
- [Dokploy providers/registry login](https://docs.dokploy.com/docs/core/providers)
- [Dokploy database backups](https://docs.dokploy.com/docs/core/databases/backups)
- [Dokploy backup/restore format](https://docs.dokploy.com/docs/core/databases/restore)
- [Dokploy configuration backups](https://docs.dokploy.com/docs/core/backups)
- [Dokploy container inspection API](https://docs.dokploy.com/docs/api/docker)
- [Dokploy application and Compose APIs](https://docs.dokploy.com/docs/api/application)
- [DigitalOcean PostgreSQL security](https://docs.digitalocean.com/products/databases/postgresql/how-to/secure/)
- [DigitalOcean PostgreSQL connections](https://docs.digitalocean.com/products/databases/postgresql/how-to/connect/)
- [DigitalOcean PostgreSQL recovery](https://docs.digitalocean.com/products/databases/postgresql/how-to/restore-from-backups/)
- [Vercel plans and pricing](https://vercel.com/pricing)
- [Cloudflare Full (strict) origin TLS](https://developers.cloudflare.com/ssl/origin-configuration/ssl-modes/full-strict/)
- [Mux usage pricing](https://www.mux.com/docs/pricing/overview)
- [Sentry Spring Boot integration](https://docs.sentry.io/platforms/java/guides/spring-boot/)
- [Sentry Next.js integration](https://docs.sentry.io/platforms/javascript/guides/nextjs/)
- [Sentry plans and quotas](https://sentry.io/pricing/)
- [Flyway migrate](https://documentation.red-gate.com/flyway/reference/commands/migrate)

Pin/test the installed Dokploy version against these API contracts before enabling
automation. Requalify container inspection, Compose env/config handling, registry
pulls and rollout health after a Dokploy upgrade.
