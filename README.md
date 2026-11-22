# E-learning backend

Java 25 / Spring Boot monorepo: `api`, shared `common`, and asynchronous `worker`.
PostgreSQL stores application data, including refresh tokens. AWS SQS connects API
and worker; local development uses ElasticMQ. Redis is not required.

## Delivery

GitHub Actions tests and builds API, worker and migration images on `main`, then
pushes them to GHCR. After staging is provisioned and explicitly enabled, successful
builds deploy automatically through staging Dokploy. Production deployment is
manual in its separate Dokploy dashboard, using the exact staging-tested digests.

The production hosting plan is one Hostinger KVM 4 (4 vCPU / 16 GB / 200 GB),
running Dokploy, internal-only self-hosted PostgreSQL, API, Next.js and worker.
Container memory ceilings are PostgreSQL 4 GB, API 3 GB, Next.js 2 GB and worker
2 GB; Next.js is capped at 1.5 CPU and worker at 1 CPU. Staging stays separate.
Cloudflare fronts the public services. Hourly Dokploy database dumps go to off-host
R2/B2, targeting up to one hour of disaster data loss; a full restore rehearsal is
required before launch. This single-server plan replaces the earlier DO production
topology. Upgrade the database to managed hosting first, then add app redundancy.

The [future DO plan](docs/deployment.md#future-digitalocean-production-plan) covers
managed PostgreSQL, app/worker droplets under production Dokploy, and either a third
droplet for Next.js or Vercel Pro. It includes a phased migration from Hostinger and
keeps staging separate; these future resources are not provisioned by this plan.

Read [the deployment and migration runbook](docs/deployment.md) for infrastructure,
GitHub configuration, secrets, database permissions, backups, cutover and releases.
It also covers video portability, load testing and proposed application changes.
The latest hosting revision is documentation only: runtime configuration and code
changes require user confirmation. The production environment example still contains
obsolete DO database settings; use the runbook's internal DB settings instead.

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
Use Dokploy's separate Database/API/Worker services for hosted environments, with
a separate Next.js service in production. The frontend image pipeline is maintained
separately; the backend workflow does not publish a frontend image.
