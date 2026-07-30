# E2E Report

## Domain

e2e

## Verdict

green

## Inputs Reviewed

- Approved user cases, domain matrix, requirements, prototype handoff, development handoff, local runtime, browser artifacts, API-backed DOM, and database migration evidence.

## Evidence

- `flows.json` defines six representative cross-boundary flows covering all six approved cases.
- `run-log.jsonl` records the actual local runtime results.
- `runtime-evidence.json` contains runtime, browser, and database surfaces.
- `runs/schedule-center-verification-20260730` contains responsive, shipping, preference, focus, console, DOM, screenshot, and database artifacts.
- `runs/schedule-center-verification-20260730/orders-pagination-verification.json` contains privacy-safe order pagination, complete delivery-field, search, responsive, and console results.

## Commands Run

- Local backend, admin Vite, and schedule-center Vite runtime checks.
- Browser route matrix across 360, 390, 768, and 1440 CSS pixel widths.
- Device and pending-order search, write-gate, privacy-state, preference, focus, and console checks.
- Twenty-seven PyMySQL read-only database queries.

## Findings

- All six E2E flows passed and cover all approved cases.
- All six routes passed at four widths, producing 24 successful responsive checks.
- The authorized rental-orders page renders 10 cards by default, supports forward/back pagination, displays complete receiver name, phone, and address, and searches by all three fields.
- Authorized shipping review supports device SN, receiver name, full phone, and order number while preserving privacy after clear.
- The local verification fixture kept writes disabled, the button stayed disabled, and no optimistic success was created.

## Required Fixes

- None.

## Residual Risk

- No real third-party shipment write was executed.
- Remote third-party availability and production network behavior remain outside this local verification.

## Follow-up Domain Routing

- Production write validation belongs to operations with explicit authorization and rollback controls.

## Incremental Rerun

- At `2026-07-30T09:38:30Z`, the live local schedule center was navigated through schedule, orders, devices, and exceptions.
- All seven visible native selects reported `padding-right: 40px` and `background-position: calc(100% - 14px) 50%`.
- The “空闲在库” badge reported `62px × 24px`, `white-space: nowrap`, and `flex-shrink: 0` inside a `224px` sticky row header.
- At the available 1280px browser viewport, page width did not exceed the viewport; the timeline retained intentional internal horizontal scrolling.
- The current browser surface could not be resized for a fresh mobile capture, so the rerun relies on existing mobile evidence plus unchanged responsive shell rules and the successful production build.
- At `2026-07-30T10:44:21Z`, the release-state runtime evidence was reconciled with the committed tree and fresh admin/schedule-center production builds; no journey implementation changed after the recorded browser run.
- At `2026-07-30T11:51:07Z`, the public admin and schedule-center routes returned HTTP 200 and both release-info endpoints reported commit `b5968cf2`.
