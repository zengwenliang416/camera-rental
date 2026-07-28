# Quality Review: 003-channel-persistence

## Verdict

needs-fix

## Separation Of Concerns

- Parsing, hashing, and mapper boundaries remain separate, but the persistence
  service now also owns rental-period parsing, historical backfill, receiver
  snapshot policy, and automatic conversion dispatch.

## Component Cohesion / Coupling

- The service has become coupled to both channel evidence and rental
  conversion. That broadens a transaction originally scoped to raw plus
  normalized order persistence.

## Test Quality

- Tests cover receiver preservation, period parsing, later remark completion,
  and bounded backfill. They do not prove a restricted access boundary for the
  newly normalized full recipient fields.

## Error Handling

- Older source snapshots are rejected and blank receiver updates do not erase
  known values. Privacy failure modes are not represented as a separate
  boundary.

## Reuse / Duplication

- The shared rental-period parser is reused correctly. Full recipient values
  are duplicated from immutable raw evidence into another persistence surface.

## Complexity Delta

- The persistence service now coordinates several lifecycle concerns beyond
  its original durable-ingestion responsibility.

## Required Fixes

- Extract or explicitly define the receiver-snapshot and rental-period
  backfill boundaries, and add permission/masking tests for every API or export
  that can read the normalized private fields.
