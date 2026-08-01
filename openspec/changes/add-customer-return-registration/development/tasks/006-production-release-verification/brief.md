# Task Brief: 006-production-release-verification

## Goal

Release the complete feature through the GitHub 211 pipeline with automatic
migration and end-to-end production evidence.

## Vertical Slice

The same committed SHA is available on GitHub and Gitee, deploys automatically
to 211, and passes public form, RustFS, database, Return Delivery and admin review
verification.

## In Scope

- Idempotent migration runner and release blocking.
- GitHub workflow and server deployment integration.
- Logical commits, dual remote push, workflow observation and production probes.
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
