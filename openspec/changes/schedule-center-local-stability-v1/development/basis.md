# Development Basis: schedule-center-local-stability-v1

## Requirements Reference

- `openspec/specs/ui-design/design.md`
- `openspec/specs/system-architecture/design.md`
- `openspec/specs/frontend-backend-data-flow/design.md`
- `openspec/specs/component-architecture/design.md`
- `openspec/changes/schedule-center-local-stability-v1/requirements.md`
- `openspec/changes/schedule-center-local-stability-v1/acceptance.md`
- `openspec/changes/schedule-center-local-stability-v1/spec-map.json`
- `openspec/changes/schedule-center-local-stability-v1/component-impact-map.json`

## Prototype Reference

- `openspec/changes/schedule-center-local-stability-v1/prototype/artifact/index.html`
- `openspec/changes/schedule-center-local-stability-v1/prototype/handoff.md`
- `openspec/changes/schedule-center-local-stability-v1/prototype/decision.json`

## Handoff Reference

Development is allowed only after the prototype handoff and decision are valid.
The prototype contract returned `ok:true` on `2026-07-29` and the approved
decision promotes only to this development gate.

## Component Architecture Constraint

Implementation must preserve high cohesion and low coupling. Any duplicated UI,
state, validation, formatting, or domain behavior that meets the extraction rule
must become a shared component, hook, utility, or service.

## First Vertical Slice

- `001-shipping-workbench` reimplements the approved outbound command center
  and extends the existing management order query contract required by the
  approved receiver-name/full-phone search.
- Prototype fixtures and static counts are prohibited in production source.
- The existing `QuickBindingView` becomes a thin compatibility entry while
  feature code moves under `camera-rental-schedule-center/src/features/shipping`.
- The existing `rental:xianyu:query` and `rental:xianyu:ship` permissions,
  tenant isolation, and shipment write gates remain authoritative.
- Complete receiver snapshots and seller remarks may be returned to authorized
  management views, but raw payloads, payment numbers, logs, exports, URLs,
  browser persistence, and fixtures remain outside the privacy boundary.

## Second Vertical Slice

- `002-admin-xianyu-config` moves all tenant XianGuanJia business configuration
  from process properties to the existing `xianyu_application` persistence
  boundary.
- The management page edits integration state, endpoint, AppKey, one-way
  AppSecret replacement, webhook address, supported job settings, and the real
  write switch. Enabling writes requires explicit confirmation.
- AppSecret is encrypted through the existing MyBatis encryption handler,
  omitted from all responses, and preserved when the replacement field is
  blank. Only the generic encryption master key remains infrastructure config.
- Read/write clients, webhooks, scheduled work, and shipment commands resolve
  persisted configuration. Missing state is denied by default, and shipment
  checks occur before assignment, remote invocation, dispatch, or local
  shipment persistence.
