# Development Basis: add-rental-logistics-tracking

## Requirements Reference

- `openspec/specs/ui-design/design.md`
- `openspec/specs/system-architecture/design.md`
- `openspec/specs/frontend-backend-data-flow/design.md`
- `openspec/specs/component-architecture/design.md`
- `openspec/changes/add-rental-logistics-tracking/requirements.md`
- `openspec/changes/add-rental-logistics-tracking/acceptance.md`
- `openspec/changes/add-rental-logistics-tracking/spec-map.json`
- `openspec/changes/add-rental-logistics-tracking/component-impact-map.json`

## Prototype Reference

- `openspec/changes/add-rental-logistics-tracking/prototype/handoff.md`
- `openspec/changes/add-rental-logistics-tracking/prototype/decision.json`
- `openspec/changes/add-rental-logistics-tracking/prototype/artifact/index.html`

## Handoff Reference

Requirements, acceptance, prototype verification, explicit approval, and the
scope lock are green. Production implementation must follow the five vertical
slices in `openspec/changes/add-rental-logistics-tracking/tasks.md`.

## Component Architecture Constraint

Implementation must preserve high cohesion and low coupling. Any duplicated UI,
state, validation, formatting, or domain behavior that meets the extraction rule
must become a shared component, hook, utility, or service.
