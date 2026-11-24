"""Select checks and main-only image publication for the single CI job."""

import json
import os
from pathlib import Path
import subprocess


def components(paths):
    backend = web = False
    for path in paths:
        if path.startswith("apps/web/"):
            web = True
        elif path in {
            ".github/workflows/build.yml", ".github/workflows/web.yml",
            ".github/scripts/ci.py", ".github/scripts/tests/test_ci.py",
        }:
            backend = web = True
        elif path.endswith(".md") or path.startswith("docs/"):
            continue
        else:
            # Unknown paths conservatively require backend checks.
            backend = True
    return backend, web


def changed_paths(event_name, event, head):
    if event_name == "workflow_dispatch":
        return None
    base = (event.get("pull_request", {}).get("base", {}).get("sha")
            if event_name == "pull_request" else event.get("before"))
    if not base or set(base) == {"0"}:
        return None
    try:
        # No rename detection: both the removed and added paths affect selection.
        result = subprocess.run(
            ["git", "diff", "--name-only", "--no-renames", "-z", base, head, "--"],
            check=True, capture_output=True, text=True,
        )
    except subprocess.CalledProcessError:
        return None
    return [path for path in result.stdout.split("\0") if path]


def selection(event_name, ref, paths):
    backend, web = (True, True) if paths is None else components(paths)
    publish = ref == "refs/heads/main" and event_name in {"push", "workflow_dispatch"}
    return {
        # Either component changing runs both sets of checks. Publication still
        # uses changed components so unchanged images are not republished.
        "backend": backend or web, "web": backend or web,
        "publish_backend": backend and publish, "publish_web": web and publish,
    }


if __name__ == "__main__":
    event = json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text())
    event_name = os.environ["GITHUB_EVENT_NAME"]
    selected = selection(event_name, os.environ["GITHUB_REF"], changed_paths(
        event_name, event, os.environ["GITHUB_SHA"],
    ))
    with open(os.environ["GITHUB_OUTPUT"], "a") as output:
        for name, enabled in selected.items():
            output.write(f"{name}={str(enabled).lower()}\n")
    print(json.dumps(selected))
