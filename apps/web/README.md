# E-learning frontend

Next.js App Router frontend, imported from the local `globalislamicinstitute`
repository at commit `7b5dbcc2aa6b9380f19488b8e92984c958398d67` on 2026-11-24.
This is a tracked-file snapshot, not a Git history/submodule import. The original
repository is unchanged. UI/business source, assets and dependency versions were
preserved; Next.js standalone output and repository/container integration were added.
No local secrets, node_modules, .next output or Git metadata were copied.

## Local development

Use Node.js 24 and npm with the committed lockfile. From this directory:

```sh
npm ci
cp .env.example .env.local
npm run dev
```

Set `NEXT_PUBLIC_APP_URL` to the frontend origin (for local development,
`http://localhost:3000`), `NEXT_PUBLIC_API_URL` to the browser-reachable API origin
and `API_URL` to the server-reachable API origin (local API: `http://localhost:8080`).
Set the public Google client ID if using Google sign-in. Do not put private tokens
or passwords in `NEXT_PUBLIC_*`. Keep `.env.local` untracked.

## Verification

```sh
npx --no-install next typegen
npm run check
```

`check` runs TypeScript, ESLint and the production build. Type generation must run
first on a fresh checkout. The imported application can fetch public API data while
prerendering the sitemap; a usable API improves that generated content. There are
no frontend test files in the imported tracked snapshot. Backend tests remain Gradle
tasks at the repository root.

## Docker and GHCR

The single CI job in `.github/workflows/build.yml` checks both backend and frontend
whenever either changes.
Frontend dependencies, type checking and linting run once. PRs build the stage
variant without publishing. Relevant `main` pushes or a manual run from `main`
build and publish stage, then production, using distinct tag suffixes. Each image
is packaged before the next build replaces `.next`. It builds inside GitHub
Actions, packages the checked standalone output using the Dockerfile's
`ci` target, and publishes Linux amd64 images under:

```text
ghcr.io/rajibalam43/e-learning/web:sha-COMMIT-run-RUN-ATTEMPT-stage
ghcr.io/rajibalam43/e-learning/web:sha-COMMIT-run-RUN-ATTEMPT-prod
```

The final main CI step also updates `web:stage-latest` and `web:prod-latest` from
their corresponding published digests after all required checks, builds and release
artifact uploads finish. Use the matching environment alias for a fixed Dokploy
image field, then pull/redeploy manually. Unique tags and digest manifests remain
available for exact release selection and rollback. There is no shared `web:latest`
because the two bundles embed different public settings.

`web-stage-release` / `web-prod-release` artifacts contain `web-release.json` with the
immutable image digest, source commit and public URLs. Configure repository variables
before adopting the workflow:

| Suffix | Stage variable | Production variable | Default |
| --- | --- | --- | --- |
| Frontend origin | `WEB_STAGE_APP_URL` | `WEB_PROD_APP_URL` | stage.globalislamicinstitute.com / globalislamicinstitute.com, HTTPS |
| Browser API origin | `WEB_STAGE_API_URL` | `WEB_PROD_API_URL` | stage-api.globalislamicinstitute.com / api.globalislamicinstitute.com, HTTPS |
| Google client ID | `WEB_STAGE_GOOGLE_CLIENT_ID` | `WEB_PROD_GOOGLE_CLIENT_ID` | Empty; supply when using Google sign-in |
| Extra image hosts | `WEB_STAGE_IMAGE_REMOTE_HOSTS` | `WEB_PROD_IMAGE_REMOTE_HOSTS` | Empty; existing allowlisted hosts remain |

All are non-secret build inputs. `NEXT_PUBLIC_BUILD_ID` is set to the source commit.
Next.js embeds public settings and image-host configuration when building: a stage
image cannot become a production image just by changing runtime environment values.
Qualify both variants and record the frontend/backend versions together. The backend
release manifest is unchanged and does not include the frontend artifact.

Changes to either component run backend tests/packaging plus frontend type checking,
linting and a stage build. Only changed components' images are published on main.
Frontend checks use the committed OpenAPI
types and do not establish runtime compatibility with the current backend. There is
currently no browser end-to-end suite or CI regeneration of those API types.
After successful main CI, enabled `stage.yml` deployment consumes `web-stage-release`
and deploys its exact image through Dokploy. Frontend-only runs deploy only the frontend;
combined runs wait for migration/API/worker success before deploying the frontend.
Set `DOKPLOY_WEB_APPLICATION_ID` in GitHub Environment `stage`, alongside the other
Dokploy deployment variables and key in the [runbook](../../docs/deployment.md).
The deployed image must pass its Docker health check. A `staged-web-release` artifact
records the deployed stage digest. Production stays manual and uses the **prod** image
from `web-prod-release`; never promote the stage bundle to production.
Backend image publication and deployment do not run on frontend-only `main`
pushes. Require the single
`CI / build` check in branch protection and remove obsolete frontend matrix checks
if configured. The CI job also completes for documentation-only PRs.

For a complete local/container build (not dependent on host-built .next output),
run from the repository root:

```sh
docker build -f apps/web/Dockerfile \
  --build-arg NEXT_PUBLIC_APP_URL=https://stage.globalislamicinstitute.com \
  --build-arg NEXT_PUBLIC_API_URL=https://stage-api.globalislamicinstitute.com \
  -t e-learning-web:local apps/web
```

The default `standalone` target builds within Linux. The `ci` target expects output
built on a matching Linux amd64 runner; do not use macOS-generated native dependencies
to produce the production CI image. Container health checks use `/robots.txt`, which
does not require the API/database to be online.

## Deployment remains manual

Create a Dokploy Docker-source application for the frontend and use `web:stage-latest`
or `web:prod-latest`, or the environment's recorded image digest for an exact release.
The production alias identifies the latest production-configured build; confirm its
stage variant was qualified before release. Configure GHCR read credentials, container port 3000, its public domain,
the planned 2 GB / 1.5 CPU limits and a server-only `API_URL` using the real internal
API service name where compatible. The non-root container starts `node server.js`.
Do not build on the live VPS. No workflow added here triggers Dokploy or production.
Automatic backend staging deployment remains its existing separate workflow.

For Vercel, point its project at this monorepo and set **Root Directory: `apps/web`**.
Set the appropriate build/runtime variables there, preserve custom domains and preview
isolation, and keep an explicit production promotion process. Docker/GHCR images are
used by Dokploy; Vercel uses its own supported source build/deployment process.
Changing the connected repository, provider settings, DNS or deployment is a separate
operator action; this import does not change the current Vercel site.
