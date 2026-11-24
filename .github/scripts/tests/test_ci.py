import importlib.util
from pathlib import Path
import subprocess
import unittest
from unittest.mock import patch


spec = importlib.util.spec_from_file_location("ci", Path(__file__).parents[1] / "ci.py")
ci = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ci)


class CiSelectionTests(unittest.TestCase):
    def test_frontend_pr_checks_without_publishing(self):
        self.assertEqual(ci.selection("pull_request", "refs/pull/115/merge", ["apps/web/src/app.ts"]), {
            "backend": True, "web": True, "publish_backend": False, "publish_web": False,
        })

    def test_main_frontend_push_does_not_publish_backend(self):
        self.assertEqual(ci.selection("push", "refs/heads/main", ["apps/web/package.json"]), {
            "backend": True, "web": True, "publish_backend": False, "publish_web": True,
        })

    def test_backend_main_push_checks_frontend_without_republishing_it(self):
        self.assertEqual(ci.selection("push", "refs/heads/main", ["apps/api/build.gradle"]), {
            "backend": True, "web": True, "publish_backend": True, "publish_web": False,
        })

    def test_backend_pr_checks_both_without_publication(self):
        self.assertEqual(ci.selection("pull_request", "refs/pull/115/merge", ["apps/api/src/controller.java"]), {
            "backend": True, "web": True, "publish_backend": False, "publish_web": False,
        })

    def test_combined_main_push_checks_and_publishes_both(self):
        paths = ["apps/api/build.gradle", "apps/web/package.json"]
        self.assertTrue(all(ci.selection("push", "refs/heads/main", paths).values()))

    def test_shared_workflow_checks_both(self):
        self.assertEqual(ci.components([".github/workflows/build.yml"]), (True, True))
        self.assertEqual(ci.components([".github/workflows/web.yml"]), (True, True))

    def test_docs_only_runs_no_builds(self):
        self.assertEqual(ci.components(["README.md", "docs/deployment.md"]), (False, False))

    def test_unknown_paths_are_conservative(self):
        self.assertEqual(ci.components(["new-build-config"]), (True, False))

    def test_manual_main_builds_and_publishes_both(self):
        self.assertTrue(all(ci.selection("workflow_dispatch", "refs/heads/main", None).values()))

    def test_manual_branch_checks_both_without_publication(self):
        result = ci.selection("workflow_dispatch", "refs/heads/feature", None)
        self.assertTrue(result["backend"] and result["web"])
        self.assertFalse(result["publish_backend"] or result["publish_web"])

    def test_pr_never_publishes_even_with_main_ref(self):
        result = ci.selection("pull_request", "refs/heads/main", None)
        self.assertFalse(result["publish_backend"] or result["publish_web"])

    def test_missing_base_falls_back_to_both(self):
        self.assertIsNone(ci.changed_paths("push", {"before": "0" * 40}, "a" * 40))

    @patch.object(ci.subprocess, "run")
    def test_diff_failure_falls_back_to_both(self, run):
        run.side_effect = subprocess.CalledProcessError(128, "git")
        self.assertIsNone(ci.changed_paths("push", {"before": "a" * 40}, "b" * 40))

    @patch.object(ci.subprocess, "run")
    def test_renames_select_old_and_new_components(self, run):
        run.return_value.stdout = "apps/api/file\0apps/web/file\0"
        paths = ci.changed_paths("pull_request", {"pull_request": {"base": {"sha": "a" * 40}}}, "b" * 40)
        self.assertEqual(ci.components(paths), (True, True))
        self.assertIn("--no-renames", run.call_args.args[0])


if __name__ == "__main__":
    unittest.main()
