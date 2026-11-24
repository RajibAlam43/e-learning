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

The independent `.github/workflows/web.yml` checks frontend PR changes and builds
two image variants on relevant `main` pushes or a manual run from `main`. It builds
inside GitHub Actions, packages the checked standalone output using the Dockerfile's
`ci` target, and publishes Linux amd64 images under:

```text
ghcr.io/rajibalam43/e-learning/web:sha-COMMIT-run-RUN-ATTEMPT-stage
ghcr.io/rajibalam43/e-learning/web:sha-COMMIT-run-RUN-ATTEMPT-prod
```

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

Backend PR checks still run on frontend PRs to retain existing required checks.
Backend image publication/stage deployment does not run on frontend-only `main`
pushes. The frontend workflow is path-filtered; do not make its checks globally
required for unrelated PRs without a conditional required-check arrangement.

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

Create a Dokploy Docker-source application for the frontend and use the environment's
image digest. Configure GHCR read credentials, container port 3000, its public domain,
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
