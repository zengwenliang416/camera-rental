# Development Basis: xian-guanjia-data-integration-v1

## Requirements Reference

- `openspec/specs/ui-design/design.md`
- `openspec/specs/system-architecture/design.md`
- `openspec/specs/frontend-backend-data-flow/design.md`
- `openspec/specs/component-architecture/design.md`
- `openspec/changes/xian-guanjia-data-integration-v1/requirements.md`
- `openspec/changes/xian-guanjia-data-integration-v1/acceptance.md`
- `openspec/changes/xian-guanjia-data-integration-v1/spec-map.json`
- `openspec/changes/xian-guanjia-data-integration-v1/component-impact-map.json`

## Prototype Reference

- `openspec/changes/xian-guanjia-data-integration-v1/prototype/handoff.md`
- `openspec/changes/xian-guanjia-data-integration-v1/prototype/decision.json`
- `openspec/changes/xian-guanjia-data-integration-v1/prototype/data-flow-map.md`

## Handoff Reference

The approved `data-flow` handoff freezes the following production boundaries:

- external channel access is read-only and backend-only;
- raw evidence precedes normalization and conversion;
- incomplete product/date data becomes manual review rather than a fabricated
  schedule;
- concrete device assignment rechecks half-open occupied intervals in one
  transaction;
- production code is reimplemented under the development gate, never copied
  from prototype fixtures.

## Component Architecture Constraint

Implementation must preserve high cohesion and low coupling. Any duplicated UI,
state, validation, formatting, or domain behavior that meets the extraction rule
must become a shared component, hook, utility, or service.

## Scope Reference

- `openspec/changes/xian-guanjia-data-integration-v1/scope.json`
- First implementation packet:
  `openspec/changes/xian-guanjia-data-integration-v1/development/tasks/001-rental-foundation/brief.md`
