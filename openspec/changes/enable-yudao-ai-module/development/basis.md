# Development Basis: enable-yudao-ai-module

## Requirements Reference

- `openspec/changes/enable-yudao-ai-module/requirements.md`
- `openspec/changes/enable-yudao-ai-module/acceptance.md`
- `openspec/changes/enable-yudao-ai-module/spec-map.json`
- `openspec/changes/enable-yudao-ai-module/component-impact-map.json`

## Prototype Reference

- `openspec/changes/enable-yudao-ai-module/prototype/handoff.md`
- `openspec/changes/enable-yudao-ai-module/prototype/decision.json`

## Handoff Reference

Development is allowed only after the prototype handoff and decision are valid.

## Component Architecture Constraint

Implementation must preserve high cohesion and low coupling. Any duplicated UI,
state, validation, formatting, or domain behavior that meets the extraction rule
must become a shared component, hook, utility, or service.
