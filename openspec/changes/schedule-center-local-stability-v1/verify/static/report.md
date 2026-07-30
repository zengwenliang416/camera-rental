# Static Report

## Domain

static

## Verdict

green

## Inputs Reviewed

- Verification plan, approved cases, domain matrix, traceability matrix, package scripts, Maven module, migration manifest, and changed production sources.

## Evidence

- `commands.jsonl` records backend, admin, schedule-center, migration, banned-pattern, line-ceiling, and whitespace checks.
- Production source maximum is 524 lines after extracting the order message dictionary.
- Migrations 029 through 031 match their SpecNav audit copies.

## Commands Run

- Backend tests and package.
- Admin type check and production build.
- Schedule-center tests, type check, and production build.
- Legacy configuration scans, source-size scan, migration checksum comparison, and `git diff --check`.

## Findings

- Required static and structural checks passed.
- No maintained runtime XGJ environment/property fallback or `credentialReference` production path remains.
- No production TypeScript, TSX, or CSS file exceeds the approved 600-line limit.
- The order pagination, complete receiver mapping, and translation split pass TypeScript and Vite production builds.

## Required Fixes

- None.

## Residual Risk

- The anchor scan is advisory because no enforcing annotation policy is configured.

## Follow-up Domain Routing

- Behavioral assertions route to unit and E2E; human interaction quality routes to sensory.

## Incremental Rerun

- At `2026-07-30T09:38:30Z`, `pnpm lint`, `pnpm build`, and focused `git diff --check` passed after the select-arrow and schedule status-badge changes.
- The touched production files contain 150, 28, and 233 lines respectively, all below the 600-line ceiling.
- The fix remains centralized: one global native-select rule, one shared badge behavior, and one schedule-table column constraint.
- At `2026-07-30T10:44:21Z`, the committed release state was revalidated with backend tests, admin type/build, schedule-center test/lint/build, and repository whitespace checks.
- At `2026-07-30T11:51:07Z`, the backend reactor tests, admin type check, schedule-center tests/type check/build, incremental deployment helper test, Bash syntax checks, and `git diff --check` passed.
