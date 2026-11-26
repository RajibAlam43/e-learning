"""Build manifests and orchestrate STAGING only via Dokploy's HTTPS API.

Production has no automated execution path here. Its operator uses the output
artifact in Dokploy. Never print API responses: inspections contain credentials.
"""

import argparse
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SERVICES = ("api", "worker", "migrator")
DIGEST = re.compile(r"sha256:[0-9a-f]{64}")


def required(name):
    value = os.environ.get(name, "").strip()
    if not value:
        raise ValueError(f"Missing {name}")
    return value


def validate_manifest(data, repository, source_sha=None, build_run_id=None):
    if data.get("schema_version") != 1 or data.get("repository") != repository.lower():
        raise ValueError("Release belongs to another repository or unsupported format")
    sha = data.get("source_sha", "")
    if not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise ValueError("Invalid source revision")
    if source_sha is not None and sha != source_sha:
        raise ValueError("Manifest revision differs from successful build")
    if not re.fullmatch(r"[1-9][0-9]*", str(data.get("build_run_id", ""))):
        raise ValueError("Invalid build run ID")
    if build_run_id is not None and str(data["build_run_id"]) != str(build_run_id):
        raise ValueError("Manifest belongs to another build run")
    images = data.get("images", {})
    if set(images) != set(SERVICES):
        raise ValueError("Release must contain API, worker and migrator images")
    for service in SERVICES:
        prefix = f"ghcr.io/{repository.lower()}/{service}@"
        image = images[service]
        if not isinstance(image, str) or not image.startswith(prefix):
            raise ValueError(f"Unexpected registry/repository for {service}")
        if not DIGEST.fullmatch(image[len(prefix):]):
            raise ValueError(f"{service} must use an immutable digest")
    return data


def create_manifest():
    repository = required("GITHUB_REPOSITORY").lower()
    data = {
        "schema_version": 1,
        "repository": repository,
        "source_sha": required("GITHUB_SHA"),
        "build_run_id": required("GITHUB_RUN_ID"),
        "images": {
            service: f"ghcr.io/{repository}/{service}@{required(service.upper() + '_DIGEST')}"
            for service in SERVICES
        },
    }
    return validate_manifest(data, repository)


def validate_web_manifest(data, repository, source_sha, build_run_id):
    if data.get("service") != "web" or data.get("environment") != "stage":
        raise ValueError("Only a stage frontend release may deploy automatically")
    if data.get("source_sha") != source_sha or not re.fullmatch(r"[0-9a-f]{40}", source_sha):
        raise ValueError("Frontend revision differs from successful build")
    if str(data.get("build_run_id")) != build_run_id or not re.fullmatch(r"[1-9][0-9]*", build_run_id):
        raise ValueError("Frontend manifest belongs to another build run")
    prefix = f"ghcr.io/{repository.lower()}/web@"
    image = data.get("image")
    if not isinstance(image, str) or not image.startswith(prefix) or not DIGEST.fullmatch(image[len(prefix):]):
        raise ValueError("Frontend must use an immutable image from this repository")
    return data


def render_migration(image, release_id):
    if not re.fullmatch(r"[a-zA-Z0-9._-]+", release_id):
        raise ValueError("Unsafe release ID")
    template = (ROOT / "deploy/migration.compose.yml").read_text()
    replacements = {
        "${MIGRATOR_IMAGE:-ghcr.io/rajibalam43/e-learning/migrator:latest}": image,
        "${MIGRATION_RELEASE_ID:-manual}": release_id,
    }
    for token, value in replacements.items():
        if template.count(token) != 1:
            raise ValueError("Migration Compose defaults changed; update release rendering")
        template = template.replace(token, value)
    return template


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        # Never forward the API key through an unexpected redirect.
        return None


class Dokploy:
    def __init__(self, url, api_key, timeout=1200, interval=5):
        parsed = urllib.parse.urlsplit(url)
        if (parsed.scheme != "https" or not parsed.netloc or parsed.username
                or parsed.query or parsed.fragment or parsed.path not in ("", "/")):
            raise ValueError("DOKPLOY_URL must be an HTTPS origin without credentials/query")
        self.url = url.rstrip("/")
        self.api_key = api_key
        self.timeout = timeout
        self.interval = interval
        self.opener = urllib.request.build_opener(NoRedirect)

    def request(self, endpoint, payload=None, **query):
        url = self.url + "/api/" + endpoint
        if query:
            url += "?" + urllib.parse.urlencode(query)
        request = urllib.request.Request(
            url,
            data=json.dumps(payload).encode() if payload is not None else None,
            headers={"x-api-key": self.api_key, "Content-Type": "application/json"},
        )
        try:
            with self.opener.open(request, timeout=30) as response:
                content = response.read()
                return json.loads(content) if content else None
        except urllib.error.HTTPError as error:
            raise RuntimeError(f"Dokploy {endpoint}: HTTP {error.code}; see dashboard logs") from None
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError):
            raise RuntimeError(f"Dokploy {endpoint}: connection/response failed") from None

    def poll(self, check, description):
        deadline = time.monotonic() + self.timeout
        while True:
            result = check()
            if result:
                return result
            if time.monotonic() >= deadline:
                raise RuntimeError(f"Timed out waiting for {description}; inspect Dokploy before retrying")
            time.sleep(self.interval)

    def deployments(self, kind, service_id):
        endpoint, key = (
            ("deployment.allByCompose", "composeId") if kind == "compose"
            else ("deployment.all", "applicationId")
        )
        result = self.request(endpoint, **{key: service_id})
        if not isinstance(result, list):
            raise RuntimeError("Unexpected Dokploy deployment response; verify installed API version")
        return result

    def assert_idle(self, kind, service_id):
        if any(item.get("status") == "running" for item in self.deployments(kind, service_id)):
            raise RuntimeError("An existing deployment is active; inspect Dokploy before retrying")

    def deploy(self, kind, service_id, title):
        key = "composeId" if kind == "compose" else "applicationId"
        self.request(f"{kind}.deploy", {key: service_id, "title": title})

        def completed():
            matches = [item for item in self.deployments(kind, service_id) if item.get("title") == title]
            if len(matches) > 1:
                raise RuntimeError("Duplicate release deployment; inspect Dokploy")
            if not matches:
                return False
            status = matches[0].get("status")
            if status in ("error", "cancelled"):
                raise RuntimeError(f"{title} failed; inspect Dokploy deployment logs")
            return status == "done"

        self.poll(completed, title)

    def containers(self, service, compose=False):
        query = {"appName": service["appName"]}
        if compose:
            query["appType"] = "docker-compose"
        if service.get("serverId"):
            query["serverId"] = service["serverId"]
        rows = self.request("docker.getContainersByAppNameMatch", **query)
        if not isinstance(rows, list):
            raise RuntimeError("Unexpected Dokploy container list")
        result = []
        for row in rows:
            query = {"containerId": row["containerId"]}
            if service.get("serverId"):
                query["serverId"] = service["serverId"]
            config = self.request("docker.getConfig", **query)
            if isinstance(config, dict) and config.get("Config"):
                result.append(config)
        return result

    def assert_no_running_migration(self, service):
        for container in self.containers(service, compose=True):
            if container.get("State", {}).get("Running"):
                raise RuntimeError("A migration container is still running; inspect it before retrying")

    def wait_migration(self, service, image, release_id):
        def completed():
            matches = [
                row for row in self.containers(service, compose=True)
                if row["Config"].get("Image") == image
                and row["Config"].get("Labels", {}).get("com.gii.release") == release_id
            ]
            if not matches:
                return False
            if len(matches) != 1:
                raise RuntimeError("Expected exactly one migration container")
            state = matches[0].get("State", {})
            if state.get("OOMKilled") or state.get("Dead"):
                raise RuntimeError("Migration container was killed; application deployment stopped")
            if state.get("Status") != "exited":
                return False
            if state.get("ExitCode") != 0:
                raise RuntimeError(f"Migration exited unsuccessfully (code {state.get('ExitCode')}); application deployment stopped")
            return True

        self.poll(completed, "migration container exit code 0")

    def wait_healthy(self, service, image):
        def healthy():
            for row in self.containers(service):
                config, state = row["Config"], row.get("State", {})
                owner = config.get("Labels", {}).get("com.docker.swarm.service.name")
                if owner and owner != service["appName"]:
                    continue
                if config.get("Image") == image and state.get("Running"):
                    if state.get("Health", {}).get("Status") == "healthy":
                        return True
            return False

        self.poll(healthy, f"healthy {service['name']} container using release digest")


def stage_release(client, manifest, ids, release_id):
    # Complete every read-only preflight before updating the migration or apps.
    if len(set(ids.values())) != 3:
        raise ValueError("API, worker and migration must be distinct services")
    api = client.request("application.one", applicationId=ids["api"])
    worker = client.request("application.one", applicationId=ids["worker"])
    migrator = client.request("compose.one", composeId=ids["migrator"])
    environment_id = api.get("environmentId")
    if not environment_id or worker.get("environmentId") != environment_id or migrator.get("environmentId") != environment_id:
        raise ValueError("All three services must belong to the same staging environment")
    environment = client.request("environment.one", environmentId=environment_id)
    if environment.get("name") != "stage":
        raise ValueError("Automation only targets an environment named stage; production is manual")
    if any(app.get("sourceType") != "docker" for app in (api, worker)):
        raise ValueError("API and worker must use Dokploy's Docker source")
    for app in (api, worker):
        profiles = re.findall(r"(?m)^\s*SPRING_PROFILES_ACTIVE\s*=\s*([^\r\n]*)", app.get("env") or "")
        if not profiles or profiles[-1].strip().strip("\"'") != "stage":
            raise ValueError("Set SPRING_PROFILES_ACTIVE=stage explicitly on each staging application")
    if migrator.get("sourceType") != "raw" or migrator.get("composeType") != "docker-compose":
        raise ValueError("Migrator must be a dedicated Raw Docker Compose service")
    for name, kind in (("api", "application"), ("worker", "application"), ("migrator", "compose")):
        client.assert_idle(kind, ids[name])
    client.assert_no_running_migration(migrator)

    compose = render_migration(manifest["images"]["migrator"], release_id)
    client.request("compose.update", {
        "composeId": ids["migrator"], "composeFile": compose,
        "createEnvFile": True, "randomize": False, "isolatedDeployment": False,
        "autoDeploy": False, "pullImages": True,
    })
    print("Running the staging migration through Dokploy", flush=True)
    client.deploy("compose", ids["migrator"], release_id + "-migrate")
    client.wait_migration(migrator, manifest["images"]["migrator"], release_id)
    print("Migration completed successfully", flush=True)

    for name, service in (("api", api), ("worker", worker)):
        image = manifest["images"][name]
        # Partial update preserves registry credentials, runtime secrets and networking.
        client.request("application.update", {"applicationId": ids[name], "dockerImage": image})
        client.deploy("application", ids[name], release_id + "-" + name)
        client.wait_healthy(service, image)
        print(f"{name} is healthy on the release image", flush=True)
    return compose


def stage_web_release(client, manifest, ids, release_id):
    if len(set(ids.values())) != len(ids):
        raise ValueError("Frontend, API and worker must be distinct services")
    api = client.request("application.one", applicationId=ids["api"])
    web = client.request("application.one", applicationId=ids["web"])
    environment_id = api.get("environmentId")
    if not environment_id or web.get("environmentId") != environment_id:
        raise ValueError("Frontend and API must belong to the same staging environment")
    environment = client.request("environment.one", environmentId=environment_id)
    if environment.get("name") != "stage":
        raise ValueError("Automation only targets an environment named stage; production is manual")
    if web.get("sourceType") != "docker":
        raise ValueError("Frontend must use Dokploy's Docker source")
    profiles = re.findall(r"(?m)^\s*SPRING_PROFILES_ACTIVE\s*=\s*([^\r\n]*)", api.get("env") or "")
    if not profiles or profiles[-1].strip().strip("\"'") != "stage":
        raise ValueError("Set SPRING_PROFILES_ACTIVE=stage explicitly on the staging API")
    client.assert_idle("application", ids["web"])
    image = manifest["image"]
    client.request("application.update", {"applicationId": ids["web"], "dockerImage": image})
    client.deploy("application", ids["web"], release_id + "-web")
    client.wait_healthy(web, image)
    print("Frontend is healthy on the stage release image", flush=True)


def write_release(directory, manifest, compose):
    directory.mkdir(parents=True, exist_ok=True)
    (directory / "release.json").write_text(json.dumps(manifest, indent=2) + "\n")
    (directory / "migration.compose.yml").write_text(compose)
    lines = [
        "# Staging-verified release", "",
        f"Commit: `{manifest['source_sha']}`", "",
        f"Staging run: {required('GITHUB_SERVER_URL')}/{required('GITHUB_REPOSITORY')}/actions/runs/{required('GITHUB_RUN_ID')}", "",
        "Production is manual in its production Dokploy environment:", "",
        "1. Confirm a recovery point and schema compatibility; serialize production releases.",
        "2. Paste migration.compose.yml into the production migrator's Raw Compose source.",
        "3. Deploy the migrator; verify its container exited with code 0 (deployment Done is insufficient).",
        "4. Set the API and worker Docker images below and deploy each; verify healthy containers.",
        "5. Smoke-test authentication, courses, queue processing and payment callbacks.", "",
    ]
    lines.extend(f"- {name}: `{manifest['images'][name]}`" for name in SERVICES)
    lines.extend(["", "Read docs/deployment.md for credentials, networks, recovery and cutover.", ""])
    (directory / "PRODUCTION.md").write_text("\n".join(lines))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    create = commands.add_parser("manifest")
    create.add_argument("--output", type=Path, required=True)
    stage = commands.add_parser("stage")
    stage.add_argument("--manifest", type=Path, required=True)
    stage.add_argument("--output", type=Path, required=True)
    web = commands.add_parser("stage-web")
    web.add_argument("--manifest", type=Path, required=True)
    web.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.command == "manifest":
        args.output.write_text(json.dumps(create_manifest(), indent=2) + "\n")
        return
    if args.command == "stage-web":
        manifest = validate_web_manifest(
            json.loads(args.manifest.read_text()), required("GITHUB_REPOSITORY"),
            required("EXPECTED_SOURCE_SHA"), required("EXPECTED_BUILD_RUN_ID"),
        )
        ids = {
            "api": required("DOKPLOY_API_APPLICATION_ID"),
            "worker": required("DOKPLOY_WORKER_APPLICATION_ID"),
            "web": required("DOKPLOY_WEB_APPLICATION_ID"),
        }
        release_id = "stage-" + required("GITHUB_RUN_ID") + "-" + required("GITHUB_RUN_ATTEMPT")
        client = Dokploy(required("DOKPLOY_URL"), required("DOKPLOY_API_KEY"))
        stage_web_release(client, manifest, ids, release_id)
        args.output.mkdir(parents=True, exist_ok=True)
        (args.output / "web-release.json").write_text(json.dumps(manifest, indent=2) + "\n")
        return
    manifest = validate_manifest(
        json.loads(args.manifest.read_text()), required("GITHUB_REPOSITORY"),
        required("EXPECTED_SOURCE_SHA"), required("EXPECTED_BUILD_RUN_ID"),
    )
    ids = {
        "api": required("DOKPLOY_API_APPLICATION_ID"),
        "worker": required("DOKPLOY_WORKER_APPLICATION_ID"),
        "migrator": required("DOKPLOY_MIGRATION_COMPOSE_ID"),
    }
    release_id = "stage-" + required("GITHUB_RUN_ID") + "-" + required("GITHUB_RUN_ATTEMPT")
    client = Dokploy(required("DOKPLOY_URL"), required("DOKPLOY_API_KEY"))
    compose = stage_release(client, manifest, ids, release_id)
    write_release(args.output, manifest, compose)


if __name__ == "__main__":
    try:
        main()
    except (ValueError, RuntimeError, KeyError) as error:
        print(f"Release stopped: {error}", file=sys.stderr)
        sys.exit(1)
