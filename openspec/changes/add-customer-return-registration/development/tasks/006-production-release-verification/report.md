# Task Report: 006-production-release-verification

## Status

DONE_WITH_CONCERNS

## Files Changed

- `ops/github-deploy/apply-migrations.sh`, migration manifest and shell tests.
- GitHub build/release integration and RustFS packaging/install integration.
- RustFS checksum download mirror fallback for the domestic 211 host.

## What Changed

- Added checksum-pinned numbered migration execution before active release switching.
- Added migration replay and changed-checksum blocking tests.
- Added release packaging for migrations and RustFS assets.
- Added first-install persistent RustFS credentials and least-privilege application credentials.
- Completed local backend, Nuxt, admin, shell, Compose and browser validation.

## TDD Evidence

- Migration runner test proves first apply, replay skip and checksum-change rejection.
- Incremental build helper regression passed.
- Backend, Nuxt and admin validation commands passed.

## Verification Commands

- `bash ops/github-deploy/tests/migration-runner-test.sh`
- `bash ops/github-deploy/tests/incremental-build-lib-test.sh`
- `bash -n ops/rustfs/*.sh ops/github-deploy/*.sh ops/github-deploy/tests/*.sh`
- Backend, Nuxt and admin commands recorded in `development/validation-log.jsonl`.

## Concerns

- GitHub/Gitee push, workflow completion and 211 production E2E are not yet executed.

## Scope Deviations

- None recorded.

## Follow-up Needed

- Commit feature-owned paths, push the same SHA to both remotes, observe GitHub Actions and execute synthetic production verification.

## Adjudication

Task 006 remains open until GitHub/Gitee SHA parity and 211 production evidence exist.
