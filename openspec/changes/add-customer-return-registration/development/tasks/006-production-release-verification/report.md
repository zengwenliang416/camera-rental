# Task Report: 006-production-release-verification

## Status

DONE

## Files Changed

- `ops/github-deploy/apply-migrations.sh`, migration manifest and shell tests.
- GitHub build/release integration and RustFS packaging/install integration.
- Production-80 runtime preparation and release checks.
- Fixed-entry migration 037 packaging and artifact verification.
- GitHub source-bundle delivery without a Gitee checkout dependency.

## What Changed

- Added checksum-pinned numbered migration execution before active release switching.
- Added migration replay and changed-checksum blocking tests.
- Added release packaging for migrations and RustFS assets.
- Added first-install persistent RustFS credentials and least-privilege application credentials.
- Completed local backend, Nuxt, admin, shell, Compose and browser validation.
- Added the fixed `/return` entry, receiver-mobile-last-four verification,
  secure cookie sessions and legacy token-route redirect.

## TDD Evidence

- Migration runner test proves first apply, replay skip and checksum-change rejection.
- Incremental build helper regression passed.
- Backend, Nuxt and admin validation commands passed.
- The current model-prefix follow-up passed the 61-test backend return suite,
  the project-defined Nuxt utility suite, Nuxt typecheck and production build,
  a focused mobile Playwright submission case, JSON parsing and scoped
  whitespace checks.

## Verification Commands

- `bash ops/github-deploy/tests/migration-runner-test.sh`
- `bash ops/github-deploy/tests/incremental-build-lib-test.sh`
- `bash -n ops/rustfs/*.sh ops/github-deploy/*.sh ops/github-deploy/tests/*.sh`
- Backend, Nuxt and admin commands recorded in `development/validation-log.jsonl`.
- `bun test tests/return-registration.test.ts`
- `/Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=ReturnSerialNormalizerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `bun run build`
- Scoped `git diff --check` and `jq empty` for the active change artifacts.

## Concerns

- GitHub push, workflow completion and production-80 E2E are intentionally owned
  by the verification stage.
- Only orders already converted to an internal rental order with active device
  assignments can pass verification; current production data has two such orders.

## Scope Deviations

- None recorded.

## Follow-up Needed

- Commit and push the feature-owned paths to GitHub, observe the production-80
  workflow and execute privacy-safe production verification.

## Adjudication

The development release path is complete. Production execution and evidence are
owned by the verification stage and must not be inferred from local checks.
