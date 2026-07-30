# Facticity Report

## Domain

facticity

## Verdict

green

## Inputs Reviewed

- Requirements, acceptance criteria, prototype handoff, development handoff, approved user cases, traceability matrix, changed repository files, runtime artifacts, and migration manifest.

## Evidence

- `claims.jsonl` verifies seven behavior claims against current code, browser artifacts, tests, and database evidence.
- `repo-inventory.json` maps server, admin, schedule-center, and documentation responsibilities without unmapped changes.
- `runs/schedule-center-verification-20260730/database-verification.json` records 27 read-only checks and contains no customer or secret values.
- `runs/schedule-center-verification-20260730/orders-pagination-verification.json` records only counts and booleans for authorized order pagination, complete delivery fields, search, responsiveness, and console state.

## Commands Run

- Repository inventory and changed-file inspection.
- Focused legacy configuration scans recorded in `verify/static/commands.jsonl`.
- Browser and database evidence inspection.

## Findings

- Maintained behavior matches the approved requirements and acceptance statements.
- No invented route, API, configuration fallback, database effect, or runtime count was used as proof.
- Shared dashboard/device snapshots retain masked customer fields; complete receiver name, phone, and address are mapped separately for the authorized rental-orders page.
- The real-write path remains deliberately unexecuted; production write enablement is handled as an accepted operations risk.

## Required Fixes

- None.

## Residual Risk

- CodeGraph is advisory and not indexed in the current session.
- Real XianGuanJia shipment mutation behavior is supported by tests and gates rather than a production write.

## Follow-up Domain Routing

- Static verifies structure and banned patterns; unit verifies behavior coverage; E2E verifies local integration; sensory verifies human-facing quality.

## Incremental Rerun

- At `2026-07-30T09:38:30Z`, current source and browser-computed styles were re-inspected after the schedule filter and device-status layout fix.
- The browser reported `40px` select right padding, a `14px` arrow inset, a `224px` sticky identity column, and `white-space: nowrap` with `flex-shrink: 0` for the “空闲在库” badge.
- No shipment mutation, customer-data capture, or production write was performed.
- At `2026-07-30T10:44:21Z`, release-commit traceability and the clean tracked worktree were re-inspected; no production source changed after the verified state.
- At `2026-07-30T11:51:07Z`, both public release-info endpoints were reconciled with commit `b5968cf2`, the applied migration evidence, and the verified source state.
