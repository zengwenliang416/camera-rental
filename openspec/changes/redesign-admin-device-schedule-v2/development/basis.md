# Development Basis: redesign-admin-device-schedule-v2

## Requirements Reference

- `openspec/changes/redesign-admin-device-schedule-v2/requirements.md`
- `openspec/changes/redesign-admin-device-schedule-v2/acceptance.md`
- `openspec/changes/redesign-admin-device-schedule-v2/spec-map.json`
- `openspec/changes/redesign-admin-device-schedule-v2/component-impact-map.json`

## Prototype Reference

- `openspec/changes/redesign-admin-device-schedule-v2/prototype/handoff.md`
- `openspec/changes/redesign-admin-device-schedule-v2/prototype/decision.json`

## Handoff Reference

Development is allowed only after the prototype handoff and decision are valid.

## Approved Source

- `openspec/changes/redesign-admin-device-schedule-v2/prototype/artifact/index.html`
- Approved variant: `device-search-v7`

## Scope Reference

- `openspec/changes/redesign-admin-device-schedule-v2/scope.json`
- Production edits are limited to the rental backend, schedule admin surface,
  targeted locales/tests, the additive migration path and this change directory.

## Foundation References

- `openspec/specs/ui-design/design.md`
- `openspec/specs/system-architecture/design.md`
- `openspec/specs/frontend-backend-data-flow/design.md`
- `openspec/specs/component-architecture/design.md`

## Change Contract References

- `openspec/changes/redesign-admin-device-schedule-v2/spec-map.json`
- `openspec/changes/redesign-admin-device-schedule-v2/component-impact-map.json`
- `openspec/changes/redesign-admin-device-schedule-v2/prototype/artifact/index.html`

## Component Architecture Constraint

Implementation must preserve high cohesion and low coupling. Any duplicated UI,
state, validation, formatting, or domain behavior that meets the extraction rule
must become a shared component, hook, utility, or service.
