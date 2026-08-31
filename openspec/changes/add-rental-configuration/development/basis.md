# Development Basis: add-rental-configuration

## Requirements Reference

- `openspec/changes/add-rental-configuration/requirements.md`
- `openspec/changes/add-rental-configuration/acceptance.md`
- `openspec/changes/add-rental-configuration/spec-map.json`
- `openspec/changes/add-rental-configuration/component-impact-map.json`
- `openspec/specs/ui-design/design.md`
- `openspec/specs/system-architecture/design.md`
- `openspec/specs/frontend-backend-data-flow/design.md`
- `openspec/specs/component-architecture/design.md`

## Prototype Reference

- `openspec/changes/add-rental-configuration/prototype/handoff.md`
- `openspec/changes/add-rental-configuration/prototype/decision.json`
- `openspec/changes/add-rental-configuration/prototype/artifact/index.html`

## Handoff Reference

The approved `ui-html` variant is `admin-three-tab-precise-mapping-v1`.
Production development may begin only after the prototype handoff, decision,
scope lock, task ownership and committed Git baseline are all valid.

## Implementation Boundary

- Backend ownership is limited to the rental module and its incremental MySQL
  migrations.
- Admin ownership is limited to rental APIs, rental views, locale dictionaries
  and focused tests.
- The external XianGuanJia integration remains read-only during development.
- Historical reconciliation is implemented and dry-run tested locally, but no
  production order is changed without a separate operations authorization.
- Existing fulfillment and financial facts are never rewritten by configuration
  or remark reconciliation.

## Component Architecture Constraint

Implementation must preserve high cohesion and low coupling. Any duplicated UI,
state, validation, formatting, or domain behavior that meets the extraction rule
must become a shared component, hook, utility, or service.
