# E-learning monorepo

Java 25 / Spring Boot applications (`api`, shared `common`, asynchronous `worker`)
and the Next.js frontend in [`apps/web`](apps/web/README.md).
PostgreSQL stores application data, including refresh tokens. AWS SQS connects API
and worker; local development uses ElasticMQ. Redis is not required.

## Delivery

GitHub Actions tests and builds API, worker and migration images on `main`, then
pushes them to GHCR. After staging is provisioned and explicitly enabled, successful
builds deploy automatically through staging Dokploy. Production deployment is
manual in its separate Dokploy dashboard, using the exact staging-tested digests.

The same CI job checks/builds `apps/web` and publishes stage/prod variants to GHCR
on relevant main runs. PRs check and build without publishing. Frontend deployment remains manual; its release artifacts are
separate from the backend manifest. Backend images are not republished on frontend-only
`main` pushes. See the [frontend guide](apps/web/README.md) for Node.js 24, local commands,
build variables, Docker and Vercel's `apps/web` Root Directory.

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
`.github/workflows/build.yml` checks affected components and publishes images only
from `main`. Backend and frontend retain separate release artifacts; frontend-only
runs do not deploy the backend. Production and frontend deployments remain manual.
