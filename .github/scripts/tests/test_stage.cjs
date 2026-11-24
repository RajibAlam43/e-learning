const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const { test } = require('node:test');

// Exercise the actual github-script step, including its trust and artifact checks.
const workflow = readFileSync(join(__dirname, '../../workflows/stage.yml'), 'utf8');
const script = workflow.split('          script: |\n')[1].split('\n      - uses:')[0];
const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor;
const verify = new AsyncFunction('github', 'context', 'core', 'process', script);

async function check({ event = 'workflow_run', artifacts = [{ name: 'release-manifest', expired: false }],
  run: overrides = {}, ref = 'refs/heads/main', current = 'commit' } = {}) {
  const run = { id: 123, conclusion: 'success', head_branch: 'main',
    head_repository: { full_name: 'owner/repo' }, path: '.github/workflows/build.yml',
    event: 'push', head_sha: 'commit', ...overrides };
  const outputs = {};
  let branchReads = 0;
  const github = {
    rest: {
      actions: { getWorkflowRun: async () => ({ data: run }), listWorkflowRunArtifacts: () => {} },
      repos: { getBranch: async () => { branchReads++; return { data: { commit: { sha: current } } }; } },
    },
    paginate: async () => artifacts,
  };
  await verify(github, { eventName: event, ref, repo: { owner: 'owner', repo: 'repo' } },
    { info: () => {}, setOutput: (key, value) => { outputs[key] = value; } },
    { env: { SOURCE_RUN_ID: '123' } });
  return { outputs, branchReads };
}

test('frontend-only CI skips backend deployment', async () => {
  assert.deepEqual(await check({ artifacts: [] }), {
    outputs: { deploy_backend: 'false' }, branchReads: 0,
  });
});

test('successful backend CI supplies deployment provenance', async () => {
  assert.deepEqual((await check()).outputs, { run_id: '123', sha: 'commit', deploy_backend: 'true' });
});

test('manual retry requires a backend artifact and main workflow ref', async () => {
  await assert.rejects(check({ event: 'workflow_dispatch', artifacts: [] }), /release-manifest/);
  await assert.rejects(check({ event: 'workflow_dispatch', ref: 'refs/heads/feature' }), /from main/);
});

test('expired and duplicate manifests are refused', async () => {
  await assert.rejects(check({ artifacts: [{ name: 'release-manifest', expired: true }] }), /unexpired/);
  await assert.rejects(check({ artifacts: [{ name: 'release-manifest' }, { name: 'release-manifest' }] }), /single/);
});

test('failed, PR, foreign and wrong-workflow runs are refused before skipping', async () => {
  for (const run of [{ conclusion: 'failure' }, { event: 'pull_request' },
    { head_branch: 'feature' }, { head_repository: { full_name: 'other/repo' } },
    { path: '.github/workflows/other.yml' }]) {
    await assert.rejects(check({ run, artifacts: [] }), /successful main image builds/);
  }
});

test('superseded backend runs do not auto-deploy', async () => {
  await assert.rejects(check({ current: 'newer-commit' }), /superseded/);
  assert.equal((await check({ event: 'workflow_dispatch', current: 'newer-commit' })).outputs.deploy_backend, 'true');
});
