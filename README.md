# E-learning monorepo

Java 25 / Spring Boot applications (`api`, shared `common`, asynchronous `worker`)
and the Next.js frontend in [`apps/web`](apps/web/README.md).
PostgreSQL stores application data, including refresh tokens. AWS SQS connects API
and worker; local development uses ElasticMQ. Redis is not required.

## Delivery

GitHub Actions tests and builds API, worker and migration images on `main`, then
pushes them to GHCR. After staging is provisioned and explicitly enabled, successful
builds deploy automatically through staging Dokploy. Production deployment is
manual in its production Dokploy environment. Release manifests retain exact digests
for promotion and rollback; manual deployments can also use the moving tags below.

The same CI job checks/builds `apps/web` and publishes stage/prod variants to GHCR
on relevant main runs. PRs check and build without publishing. Once enabled, staging
frontend deployment is automatic through Dokploy; production remains manual. Frontend
release artifacts are separate from the backend manifest. Backend images are not republished on frontend-only
`main` pushes. See the [frontend guide](apps/web/README.md) for Node.js 24, local commands,
build variables, Docker and Vercel's `apps/web` Root Directory.

After all required CI checks, builds and release artifact uploads finish, the final
step updates moving tags for the components published by that main run:

```text
ghcr.io/rajibalam43/e-learning/api:latest
ghcr.io/rajibalam43/e-learning/worker:latest
ghcr.io/rajibalam43/e-learning/migrator:latest
ghcr.io/rajibalam43/e-learning/web:stage-latest
ghcr.io/rajibalam43/e-learning/web:prod-latest
```

These can be configured once in Dokploy for manual pull/redeployment in stage or
production. Publishing does not itself replace running containers. Moving tags
refer to the last publication, not necessarily a stage-qualified release; record
deployed digests and coordinate migration/API/worker versions. See the
[release runbook](docs/deployment.md#manual-production-no-github-triggered-deployment).

For the migrator, Dokploy can read `deploy/migration.compose.yml` directly from this
GitHub repository on `main`, with auto-deploy disabled. It pulls `migrator:latest`
when you deploy; database credentials and the CA stay in Dokploy. CI also uploads
the file as `migration-compose` on backend publications. No manual YAML editing is
needed. See the [migration setup](docs/deployment.md#configure-the-migration-service-in-each-dokploy).

Production uses one Hostinger KVM 4 in Singapore (4 vCPU / 16 GB / 200 GB) running
Dokploy, API, Next.js and worker, plus DO managed PostgreSQL in Singapore. The user
measured approximately 1.5 ms average host-to-DB latency. API/worker/migrator use the
managed public endpoint with verified TLS and an allowlisted VPS egress IP.
Memory ceilings are API 3 GB, Next.js 2 GB and worker 2 GB; Next.js is capped at
1.5 CPU and worker at 1 CPU. No application PostgreSQL container runs on production.
Staging stays separate with its own internal Dokploy PostgreSQL. Cloudflare fronts
public services. Verify DO recovery and plan independent hourly R2/B2 exports for
the retained one-hour data-loss target; the external-DB export job requires approval.
Test restore/rebuild before launch, then add application redundancy as needed.

R2 uses a private `R2_BUCKET` for lesson files/certificates and a public
`R2_PUBLIC_BUCKET` for thumbnails. Production requires the latter explicitly;
connect `assets.globalislamicinstitute.com` only to the public bucket. See the
[R2 setup and existing-thumbnail cutover](docs/deployment.md#r2-private-files-and-public-thumbnails).

The [future DO plan](docs/deployment.md#future-digitalocean-production-plan) covers
app/worker droplets under production Dokploy, keeping the existing managed DB, and
either a third droplet for Next.js or Vercel Pro. It includes compute migration from Hostinger and
keeps staging separate; these future resources are not provisioned by this plan.

Read [the deployment and migration runbook](docs/deployment.md) for infrastructure,
GitHub configuration, secrets, database permissions, backups, cutover and releases.
It also covers video portability, load testing, internal/public network paths, staging
data migration/DO retirement, and the planned Sentry integration for API, worker and
Next.js. Sentry instrumentation is not implemented yet and requires confirmation.
The frontend copy and its Docker/GHCR pipeline were explicitly authorized. Other
planned changes still require user confirmation. The production environment example contains
a private DO DB hostname placeholder; use the runbook's public managed-DB connection
settings for Hostinger instead.

The [admin student directory API](docs/admin-users.md) provides paginated name/email/phone
search over accounts with the Student role, including those with other roles, no orders,
or only a phone number.

## Verification

```sh
./gradlew verifyAwsSdkVersionAlignment test :api:integrationTest bootJar
python3 -m unittest discover -s .github/scripts/tests -v
```

Docker must be running for database-backed tests, or use the documented disposable
loopback PostgreSQL test database option. Build outputs are
`apps/api/build/libs/api.jar` and `apps/worker/build/libs/worker.jar`.

Formatting/Checkstyle reports run separately (`./gradlew spotlessCheck checkstyleMain
checkstyleTest --continue`); existing repository violations are reported without
blocking image publication, matching the previous pipeline's blocking gates.

## Local containers

```sh
docker compose up -d --build
```

The local Compose file is a development setup with placeholder provider credentials
and local-only authentication settings; it must not be used as a stage/prod template.
Configure real integration credentials if exercising external providers locally.
Stage uses Dokploy's separate Database/API/Worker services. Production Dokploy runs
API/Worker/Next.js and connects to DO managed PostgreSQL. A single CI job in
`.github/workflows/build.yml` runs both backend and frontend checks/builds when
either changes, while publishing images only for changed components from `main`.
Backend and frontend retain separate release artifacts; frontend-only
runs deploy only the stage frontend. Production deployments remain manual. Configure
the [staging deployment settings](docs/deployment.md#ghcr-and-github-setup) before enabling automation.
