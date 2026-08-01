# Development Basis: add-customer-return-registration

## Requirements Reference

- `openspec/specs/ui-design/design.md`
- `openspec/specs/system-architecture/design.md`
- `openspec/specs/frontend-backend-data-flow/design.md`
- `openspec/specs/component-architecture/design.md`
- `openspec/changes/add-customer-return-registration/requirements.md`
- `openspec/changes/add-customer-return-registration/acceptance.md`
- `openspec/changes/add-customer-return-registration/spec-map.json`
- `openspec/changes/add-customer-return-registration/component-impact-map.json`

## Prototype Reference

- `openspec/changes/add-customer-return-registration/prototype/handoff.md`
- `openspec/changes/add-customer-return-registration/prototype/decision.json`
- `openspec/changes/add-customer-return-registration/prototype/artifact/index.html`

## Handoff Reference

The user explicitly approved the warm mobile-first five-step prototype and
requested complete development and deployment on August 1, 2026. Production
code must reimplement the flow under the typed Nuxt, Spring and Element Plus
boundaries rather than copying demo fixtures or simulated submission behavior.

## Component Architecture Constraint

Implementation preserves backend authority for token, tenant, file ownership,
device matching, registration state and Delivery creation. Repeated upload,
normalization, validation, state presentation and review behavior is extracted
into focused services, hooks, utilities and components. New or refactored source
files are split before 600 physical lines.

## Worktree Constraint

The repository contains unrelated logistics, schedule-center, admin locale and
system-user edits. Only paths allowed by `scope.json` may be staged, and existing
dirty files under review roots must be inspected before modification.
