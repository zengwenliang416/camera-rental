# Domain Report: static

## Domain

static

## Verdict

green

## Inputs Reviewed

- requirements.md
- acceptance.md
- development/handoff-to-verify.md
- verify/command-results.md

## Evidence

- Backend compilation through the full Maven Reactor.
- Admin TypeScript, ESLint, and Stylelint checks.
- Shell syntax, migration-copy integrity, and manifest contract checks.
- Static credential, PII, exception-message, and third-party write-client
  scans.

## Commands Run

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am test`
- `pnpm ts:check`
- Targeted ESLint and rental Vue Stylelint checks.
- `bash -n camera-rental-server/scripts/setup-local.sh`
- Migration SHA-256 and SpecNav migration contract checks.
- Static security and third-party write-client scans.

## Findings

- Current backend/admin sources and migration audit artifacts pass the static
  checks listed above.

## Required Fixes

- No required fixes remain for this domain.

## Residual Risk

- Static checks cannot prove runtime authorization, browser behavior, or
  database execution.

## Follow-up Domain Routing

- Keep facticity, redteam, E2E, and sensory blocked until runtime evidence
  exists.
