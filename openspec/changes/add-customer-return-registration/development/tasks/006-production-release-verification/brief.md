# Task Brief: 006-production-release-verification

## Goal

Prepare the complete feature for the GitHub production-80 pipeline with
automatic migration and replayable verification evidence.

## Vertical Slice

The selected feature-owned commit can deploy from GitHub to `154.9.235.80`
without using Gitee, and the verification stage can prove the public form,
RustFS, database, Return Delivery and admin review behavior.

## In Scope

- Idempotent migration runner and release blocking.
- GitHub workflow and server deployment integration.
- Feature-owned staging boundary, GitHub-only source delivery, workflow observation and production probes.
- Reuse current incremental build and release directories; extract migration execution as a tested shell library.

## Files Allowed

- `ops/github-deploy`
- `ops/rustfs`
- `.github/workflows/deploy-211.yml`
- `openspec/changes/add-customer-return-registration`

## Verification Commands

- `bash ops/github-deploy/tests/incremental-build-lib-test.sh`
- Shell tests for migration execution and rollback behavior.
- GitHub Actions run inspection and public production probes.

## Stop Conditions

- Stop release activation on migration, RustFS, backend health or Nuxt health failure.
- Stop before pushing if staged files include unrelated dirty worktree changes.
- Stop before production verification if a test would expose real customer data.
- Keep the 211 rental services disabled; retain its database and release only as a rollback source.
