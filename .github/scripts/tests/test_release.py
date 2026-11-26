import copy
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


SPEC = importlib.util.spec_from_file_location("release", Path(__file__).parents[1] / "release.py")
release = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(release)
REPOSITORY = "owner/e-learning"
SHA = "a" * 40
MANIFEST = {
    "schema_version": 1,
    "repository": REPOSITORY,
    "source_sha": SHA,
    "build_run_id": "123",
    "images": {
        name: f"ghcr.io/{REPOSITORY}/{name}@sha256:{index}" + "f" * 63
        for name, index in (("api", "1"), ("worker", "2"), ("migrator", "3"))
    },
}
IDS = {"api": "api-id", "worker": "worker-id", "migrator": "migration-id"}
WEB_IDS = {"api": IDS["api"], "worker": IDS["worker"], "web": "web-id"}
WEB_MANIFEST = {
    "service": "web", "environment": "stage", "source_sha": SHA, "build_run_id": "123",
    "image": f"ghcr.io/{REPOSITORY}/web@sha256:" + "4" * 64,
    "public_app_url": "https://stage.example.test", "public_api_url": "https://api-stage.example.test",
}


class FakeDokploy(release.Dokploy):
    """Exercise orchestration with real polling and simulated Dokploy responses."""

    def __init__(self):
        super().__init__("https://stage.example.test", "secret", timeout=0, interval=0)
        self.calls = []
        self.jobs = {}
        self.migration_exit = 0
        self.migration_running = False
        self.environment_name = "stage"
        self.runtime_profile = "stage"
        self.health = "healthy"
        self.fail_deployment = None
        self.wrong_image = False
        self.environment_overrides = {}
        self.source_overrides = {}
        self.server_id = None

    def request(self, endpoint, payload=None, **query):
        self.calls.append((endpoint, payload, query))
        if endpoint == "environment.one":
            return {"name": self.environment_name}
        if endpoint in ("application.one", "compose.one"):
            identity = query.get("applicationId", query.get("composeId"))
            return {
                "name": identity, "appName": identity,
                "environmentId": self.environment_overrides.get(identity, "env-stage"),
                "sourceType": self.source_overrides.get(identity, "raw" if identity == IDS["migrator"] else "docker"),
                "composeType": "docker-compose", "serverId": self.server_id,
                "env": f"SPRING_PROFILES_ACTIVE={self.runtime_profile}",
            }
        if endpoint.startswith("deployment."):
            identity = query.get("applicationId", query.get("composeId"))
            return self.jobs.get(identity, [])
        if endpoint.endswith(".deploy"):
            identity = payload.get("applicationId", payload.get("composeId"))
            self.jobs[identity] = [{
                "title": payload["title"],
                "status": "error" if identity == self.fail_deployment else "done",
            }]
            return None
        if endpoint == "docker.getContainersByAppNameMatch":
            identity = query["appName"]
            if identity == IDS["migrator"] and identity not in self.jobs:
                return [{"containerId": "old-migration"}] if self.migration_running else []
            return [{"containerId": identity}]
        if endpoint == "docker.getConfig":
            identity = query["containerId"]
            if identity == "old-migration":
                return {"Config": {"Image": "old"}, "State": {"Running": True}}
            if identity == IDS["migrator"]:
                return {
                    "Config": {
                        "Image": MANIFEST["images"]["migrator"],
                        "Labels": {"com.gii.release": "stage-456-1"},
                    },
                    "State": {"Status": "exited", "ExitCode": self.migration_exit},
                }
            service = "api" if identity == IDS["api"] else "worker"
            image = WEB_MANIFEST["image"] if identity == WEB_IDS["web"] else MANIFEST["images"][service]
            return {
                "Config": {"Image": "old" if self.wrong_image else image},
                "State": {"Running": True, "Health": {"Status": self.health}},
            }
        if endpoint.endswith(".update"):
            return True
        raise AssertionError(f"Unhandled fake endpoint: {endpoint}")


class ReleaseTest(unittest.TestCase):
    def test_frontend_manifest_refuses_production_mutable_images_and_wrong_provenance(self):
        self.assertEqual(release.validate_web_manifest(WEB_MANIFEST, REPOSITORY, SHA, "123"), WEB_MANIFEST)
        for key, value in (
            ("environment", "prod"), ("service", "api"), ("source_sha", "b" * 40),
            ("build_run_id", "999"), ("image", f"ghcr.io/{REPOSITORY}/web:stage-latest"),
            ("image", "ghcr.io/other/repo/web@sha256:" + "4" * 64),
        ):
            with self.subTest(key=key):
                with self.assertRaises(ValueError):
                    release.validate_web_manifest({**WEB_MANIFEST, key: value}, REPOSITORY, SHA, "123")

    def test_frontend_only_release_updates_only_web_and_inspects_the_remote_server(self):
        client = FakeDokploy()
        client.server_id = "stage-vps"
        release.stage_web_release(client, WEB_MANIFEST, WEB_IDS, "stage-456-1")
        changes = [row for row in client.calls if row[1] is not None]
        self.assertEqual([row[0] for row in changes], ["application.update", "application.deploy"])
        self.assertEqual(changes[0][1], {"applicationId": WEB_IDS["web"], "dockerImage": WEB_MANIFEST["image"]})
        inspections = [row for row in client.calls if row[0].startswith("docker.")]
        self.assertTrue(inspections)
        self.assertTrue(all(row[2]["serverId"] == "stage-vps" for row in inspections))

    def test_frontend_preflight_rejects_wrong_environment_source_or_profile_before_mutation(self):
        for reason in ("production", "different environment", "source", "profile", "busy", "duplicate", "worker id"):
            client = FakeDokploy()
            ids = WEB_IDS.copy()
            if reason == "production":
                client.environment_name = "production"
            elif reason == "different environment":
                client.environment_overrides[WEB_IDS["web"]] = "env-prod"
            elif reason == "source":
                client.source_overrides[WEB_IDS["web"]] = "github"
            elif reason == "profile":
                client.runtime_profile = "prod"
            elif reason == "busy":
                client.jobs[WEB_IDS["web"]] = [{"title": "other", "status": "running"}]
            elif reason == "duplicate":
                ids["web"] = ids["api"]
            else:
                ids["web"] = ids["worker"]
            with self.subTest(reason=reason):
                with self.assertRaises((ValueError, RuntimeError)):
                    release.stage_web_release(client, WEB_MANIFEST, ids, "stage-456-1")
                self.assertFalse(any(row[1] is not None for row in client.calls))

    def test_failed_unhealthy_or_old_frontend_does_not_count_as_a_successful_release(self):
        for reason in ("deployment", "unhealthy", "old image"):
            client = FakeDokploy()
            if reason == "deployment":
                client.fail_deployment = WEB_IDS["web"]
            elif reason == "unhealthy":
                client.health = "unhealthy"
            else:
                client.wrong_image = True
            with self.subTest(reason=reason):
                with self.assertRaises(RuntimeError):
                    release.stage_web_release(client, WEB_MANIFEST, WEB_IDS, "stage-456-1")

    def test_frontend_command_records_only_the_successfully_deployed_stage_manifest(self):
        env = {
            "GITHUB_REPOSITORY": REPOSITORY, "EXPECTED_SOURCE_SHA": SHA, "EXPECTED_BUILD_RUN_ID": "123",
            "GITHUB_RUN_ID": "456", "GITHUB_RUN_ATTEMPT": "1", "DOKPLOY_URL": "https://stage.example.test",
            "DOKPLOY_API_KEY": "secret", "DOKPLOY_API_APPLICATION_ID": IDS["api"],
            "DOKPLOY_WEB_APPLICATION_ID": WEB_IDS["web"],
            "DOKPLOY_WORKER_APPLICATION_ID": IDS["worker"],
        }
        for failure in (False, True):
            with self.subTest(failure=failure), tempfile.TemporaryDirectory() as temp:
                manifest = Path(temp) / "web-release.json"
                manifest.write_text(json.dumps(WEB_MANIFEST))
                output = Path(temp) / "deployed"
                client = FakeDokploy()
                client.wrong_image = failure
                argv = ["release.py", "stage-web", "--manifest", str(manifest), "--output", str(output)]
                with patch.dict("os.environ", env), patch.object(release.sys, "argv", argv), patch.object(release, "Dokploy", return_value=client):
                    if failure:
                        with self.assertRaises(RuntimeError):
                            release.main()
                        self.assertFalse(output.exists())
                    else:
                        release.main()
                        self.assertEqual(json.loads((output / "web-release.json").read_text()), WEB_MANIFEST)

    def test_manifest_accepts_only_the_successful_builds_immutable_images(self):
        self.assertEqual(release.validate_manifest(MANIFEST, REPOSITORY.upper(), SHA, "123"), MANIFEST)

    def test_manifest_rejects_mutable_tags_foreign_images_and_revisions(self):
        mutations = [
            ("images", {**MANIFEST["images"], "api": f"ghcr.io/{REPOSITORY}/api:latest"}),
            ("images", {**MANIFEST["images"], "api": "ghcr.io/other/repo/api@sha256:" + "a" * 64}),
            ("source_sha", "b" * 40), ("build_run_id", "999"),
            ("repository", "other/repo"), ("images", {"api": MANIFEST["images"]["api"]}),
        ]
        for key, value in mutations:
            with self.subTest(key=key, value=value):
                changed = copy.deepcopy(MANIFEST)
                changed[key] = value
                with self.assertRaises(ValueError):
                    release.validate_manifest(changed, REPOSITORY, SHA, "123")

    def test_success_runs_migration_before_any_application_update(self):
        client = FakeDokploy()
        compose = release.stage_release(client, MANIFEST, IDS, "stage-456-1")
        endpoints = [row[0] for row in client.calls]
        migration_exit_check = next(
            index for index, row in enumerate(client.calls)
            if row[0] == "docker.getConfig" and row[2].get("containerId") == IDS["migrator"]
        )
        self.assertLess(migration_exit_check, endpoints.index("application.update"))
        app_updates = [row[1] for row in client.calls if row[0] == "application.update"]
        self.assertEqual([row["dockerImage"] for row in app_updates], [MANIFEST["images"]["api"], MANIFEST["images"]["worker"]])
        self.assertNotIn("__MIGRATOR_IMAGE__", compose)
        self.assertIn("stage-456-1", compose)
        self.assertIn("${FLYWAY_PASSWORD", compose)

    def test_failed_migration_never_updates_or_deploys_applications(self):
        client = FakeDokploy()
        client.migration_exit = 1
        with self.assertRaisesRegex(RuntimeError, "Migration exited"):
            release.stage_release(client, MANIFEST, IDS, "stage-456-1")
        self.assertFalse(any(row[0].startswith("application.") and row[1] is not None for row in client.calls))

    def test_deployment_done_is_not_migration_success(self):
        client = FakeDokploy()
        client.migration_exit = 137
        with self.assertRaises(RuntimeError):
            release.stage_release(client, MANIFEST, IDS, "stage-456-1")
        self.assertEqual(client.jobs[IDS["migrator"]][0]["status"], "done")

    def test_existing_running_migration_blocks_a_second_release(self):
        client = FakeDokploy()
        client.migration_running = True
        with self.assertRaisesRegex(RuntimeError, "still running"):
            release.stage_release(client, MANIFEST, IDS, "stage-456-1")
        self.assertFalse(any(row[1] is not None for row in client.calls))

    def test_production_environment_is_refused_before_mutation(self):
        client = FakeDokploy()
        client.environment_name = "prod"
        with self.assertRaisesRegex(ValueError, "production is manual"):
            release.stage_release(client, MANIFEST, IDS, "stage-456-1")
        self.assertFalse(any(row[1] is not None for row in client.calls))

    def test_active_deployment_blocks_mutations(self):
        client = FakeDokploy()
        client.jobs[IDS["worker"]] = [{"title": "other-release", "status": "running"}]
        with self.assertRaisesRegex(RuntimeError, "existing deployment"):
            release.stage_release(client, MANIFEST, IDS, "stage-456-1")
        self.assertFalse(any(row[1] is not None for row in client.calls))

    def test_local_or_production_runtime_profile_blocks_staging_release(self):
        for profile in ("local", "prod", "stage,local"):
            client = FakeDokploy()
            client.runtime_profile = profile
            with self.subTest(profile=profile):
                with self.assertRaisesRegex(ValueError, "SPRING_PROFILES_ACTIVE"):
                    release.stage_release(client, MANIFEST, IDS, "stage-456-1")
                self.assertFalse(any(row[1] is not None for row in client.calls))

    def test_failed_api_rollout_stops_before_worker_changes(self):
        client = FakeDokploy()
        client.fail_deployment = IDS["api"]
        with self.assertRaisesRegex(RuntimeError, "failed"):
            release.stage_release(client, MANIFEST, IDS, "stage-456-1")
        updates = [row[1] for row in client.calls if row[0] == "application.update"]
        self.assertEqual([row["applicationId"] for row in updates], [IDS["api"]])

    def test_unhealthy_or_old_image_cannot_mark_release_successful(self):
        for wrong_image, health in ((False, "unhealthy"), (True, "healthy")):
            client = FakeDokploy()
            client.wrong_image, client.health = wrong_image, health
            with self.subTest(wrong_image=wrong_image, health=health):
                with self.assertRaisesRegex(RuntimeError, "Timed out"):
                    release.stage_release(client, MANIFEST, IDS, "stage-456-1")

    def test_stale_migration_container_cannot_satisfy_current_release(self):
        client = FakeDokploy()
        client.jobs[IDS["migrator"]] = []
        with self.assertRaisesRegex(RuntimeError, "Timed out"):
            client.wait_migration({"appName": IDS["migrator"]}, MANIFEST["images"]["migrator"], "stage-789-1")

    def test_unsafe_or_unencrypted_api_origin_is_rejected(self):
        for url in ("http://stage.test", "https://secret@stage.test", "https://stage.test?key=secret", "https://stage.test/api"):
            with self.subTest(url=url):
                with self.assertRaises(ValueError):
                    release.Dokploy(url, "secret")

    def test_manual_production_artifact_uses_the_same_tested_digests(self):
        env = {"GITHUB_SERVER_URL": "https://github.com", "GITHUB_REPOSITORY": REPOSITORY, "GITHUB_RUN_ID": "456"}
        with tempfile.TemporaryDirectory() as temp, patch.dict("os.environ", env):
            output = Path(temp)
            compose = release.render_migration(MANIFEST["images"]["migrator"], "stage-456-1")
            release.write_release(output, MANIFEST, compose)
            self.assertEqual(json.loads((output / "release.json").read_text()), MANIFEST)
            production = (output / "PRODUCTION.md").read_text()
            for image in MANIFEST["images"].values():
                self.assertIn(image, production)
            self.assertIn("migration.compose.yml", production)


if __name__ == "__main__":
    unittest.main()
