# Prototype Question: enable-yudao-ai-module

## Question

Can the existing full `yudao-module-ai` be enabled without startup-time
provider failures, plaintext API-key persistence, missing-table failures, or
regressions to the independent customer return service?

## Branch

`data-flow`

## Review Target

- Entry: `data-flow-map.md`
- Required reviewer decision: approve the database-backed dynamic-provider flow
  with all Spring AI auto-model providers disabled by default.

## Out of Scope

- Production implementation.
- Database writes.
- Deployment behavior.
