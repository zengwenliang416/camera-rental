# Unit Report

## Domain

unit

## Verdict

green

## Inputs Reviewed

- Approved cases, domain matrix, changed behavior, backend test suite, schedule-center test suite, and development validation log.

## Evidence

- `test-map.json` maps every approved case to behavior-facing tests.
- `test-quality-rubric.json` confirms public behavior, edge, empty, error, boundary, and execution quality.
- `coverage-notes.md` records the relevant asynchronous and write-gate coverage.

## Commands Run

- `cd camera-rental-server && mvn -pl yudao-module-rental/yudao-module-rental-biz -am test`
- `cd camera-rental-schedule-center && pnpm test`

## Findings

- 257 backend rental-module tests and 77 schedule-center tests passed.
- Tests cover tenant, permission, secret, stale-state, duplicate-command, synchronization, search, preference, focus, and server-authoritative mutation boundaries.
- Order regressions cover complete receiver name, phone, and address search, masked/shared mapping separation, page clamping, and bounded rendering.
- Assertions target public results and state rather than private method implementation.

## Required Fixes

- None.

## Residual Risk

- Real third-party write behavior is covered through client/service tests and disabled-gate E2E rather than a production shipment.

## Follow-up Domain Routing

- Full UI/API/database journeys route to E2E; hostile boundary checks route to redteam.

## Incremental Rerun

- At `2026-07-30T09:38:30Z`, the schedule-center test suite was rerun after the layout fix.
- All 77 tests passed; no application state, scheduling rule, order behavior, or write-gate regression was introduced.
- The changed CSS presentation itself is verified in E2E and sensory evidence rather than through implementation-coupled unit assertions.
- At `2026-07-30T10:44:21Z`, the release suite was rerun against committed source: 257 backend rental tests and 77 schedule-center tests passed.
