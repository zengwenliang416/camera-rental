# Development Basis: add-xianyu-dispatch-backfill

## Requirements Reference

- `openspec/changes/add-xianyu-dispatch-backfill/requirements.md`
- `openspec/changes/add-xianyu-dispatch-backfill/acceptance.md`
- `openspec/changes/add-xianyu-dispatch-backfill/spec-map.json`
- `openspec/changes/add-xianyu-dispatch-backfill/component-impact-map.json`
- `openspec/specs/ui-design/design.md`
- `openspec/specs/system-architecture/design.md`
- `openspec/specs/frontend-backend-data-flow/design.md`
- `openspec/specs/component-architecture/design.md`

## Prototype Reference

- `openspec/changes/add-xianyu-dispatch-backfill/prototype/handoff.md`
- `openspec/changes/add-xianyu-dispatch-backfill/prototype/decision.json`
- `openspec/changes/add-xianyu-dispatch-backfill/prototype/artifact/index.html`

## Handoff Reference

The prototype contract returned `ok:true` after explicit approval of
`ui-html / admin-dialog-v1`. Production development remains blocked until the
standard-lane Git baseline tracks `tasks.md` and the vertical-slice task packets
are created.

## Approved Scope

- `openspec/changes/add-xianyu-dispatch-backfill/scope.json`
- Backend: rental module API and biz roots only.
- Admin: existing Xianyu API module, channel-order page/dialog, and `zh-CN`/`en`
  locale files only.
- No database migration, configuration, dependency, staff, uni-app, customer
  Web, or external XianGuanJia write work is allowed.

## Production Contracts

- `POST /admin-api/rental/xianyu/order/dispatch-backfill`
- Existing permission: `rental:xianyu:ship`
- Eligible local statuses: `21`, `22`
- Required local transaction: conversion, assignment, occupied schedule,
  dispatch, `ADMIN_BACKFILL` Shipment, outbound Delivery, and channel-order
  logistics update.
- Forbidden dependency: `XianyuWriteClient` and Xianyu write-enabled runtime
  configuration.

## Component Architecture Constraint

Implementation must preserve high cohesion and low coupling. Any duplicated UI,
state, validation, formatting, or domain behavior that meets the extraction rule
must become a shared component, hook, utility, or service.

The approved component boundary keeps the bounded form state inside
`XianyuDispatchBackfillDialog` and the aggregate transaction inside
`XianyuOrderShipService.backfillDispatch`. No new shared hook or service is
required for the single use site.
