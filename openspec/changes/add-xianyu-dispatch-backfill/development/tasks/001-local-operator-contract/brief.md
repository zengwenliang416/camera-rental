# Task Brief: 001-local-operator-contract

## Goal

Freeze the approved Web-only correction contract so a shop administrator can
repair an already-shipped channel order without scanner hardware or another
XianGuanJia shipment request.

## Parent Artifacts

- `openspec/changes/add-xianyu-dispatch-backfill/requirements.md`
- `openspec/changes/add-xianyu-dispatch-backfill/acceptance.md`
- `openspec/changes/add-xianyu-dispatch-backfill/prototype/handoff.md`

## Vertical Slice

The order page exposes one bounded local correction flow for status `21` or
`22`, with explicit exclusions and limitations preserved from requirements
through implementation and verification.

## In Scope

- Checklist items `1.1`, `1.2`, and `1.3`.
- Local-only requirements, acceptance assertions, scope lock, API/data-flow
  design, and accepted tenant/waybill/order-item limitations.

## Out Of Scope

- Production Java or Vue edits.
- Scanner, staff app, remote shipment, database migration, configuration, or
  new permission design.

## Files Allowed

- `openspec/changes/add-xianyu-dispatch-backfill/requirements.md`
- `openspec/changes/add-xianyu-dispatch-backfill/acceptance.md`
- `openspec/changes/add-xianyu-dispatch-backfill/design.md`
- `openspec/changes/add-xianyu-dispatch-backfill/spec-map.json`
- `openspec/changes/add-xianyu-dispatch-backfill/component-impact-map.json`
- `openspec/changes/add-xianyu-dispatch-backfill/scope.json`
- `openspec/changes/add-xianyu-dispatch-backfill/tasks.md`

## Components To Create

- The change-level local dispatch backfill contract and acceptance evidence.

## Components To Reuse

- Foundation UI, system architecture, frontend/backend data-flow, and component
  architecture specifications.

## Components To Extract

- No production component extraction is allowed in this contract-only slice.

## TDD Requirement

- Convert every rule into an acceptance assertion before relying on production
  implementation evidence.

## Verification Commands

- `openspec validate add-xianyu-dispatch-backfill --strict`
- `SPECNAV_CHANGE=add-xianyu-dispatch-backfill node /Users/wenliang_zeng/.codex/plugins/cache/specnav-marketplace/specnav-prototype/0.3.0/scripts/prototype-contract.js --json`

## Stop Conditions

- Scope lock mismatch.
- Any requirement that would invoke XianGuanJia write APIs or require scanner
  hardware.
- Any new migration, configuration, permission code, or user-to-shop data-scope
  decision.

## Unsafe Assumptions

- Tenant isolation is accepted for this change but is not equivalent to
  user-to-shop authorization.
- One waybill maps to one device and a multi-item order uses the first rental
  item only because those limitations were explicitly approved.
