# Prototype Handoff: add-rental-delivery-foundation

## Approved Branch Variant

- Branch: `logic-state`
- Candidate variant: `deterministic-snapshot-v1`
- Approval status: pending explicit user approval.

## Screens Or Flows

- Normalize every event in a complete provider-independent tracking snapshot.
- Build a stable event fingerprint from canonical event fields.
- Sort events deterministically and build a stable snapshot hash.
- Suppress exact snapshot replay without advancing versions.
- Advance snapshot and tracking versions when complete history changes.
- Derive the current delivery summary from the latest business-time event.
- Resolve equal-time events by platform status precedence.
- Preserve a terminal summary when late non-terminal data arrives.

## Components To Create

- `RentalTrackingSnapshotAggregator` as a pure domain component.
- Typed normalized trace and snapshot result models.
- Platform tracking-status precedence and terminal-state policy.

## Components To Reuse

- Existing rental module persistence, tenancy, MyBatis, transaction, audit, and
  test infrastructure.
- Existing project hashing and JSON utilities only when they preserve the
  canonical behavior proven by this prototype.

## Extraction Targets

- Event normalization.
- Canonical fingerprint construction.
- Complete-snapshot hashing.
- Summary candidate selection.
- Terminal-regression protection.

## API Contracts

- No HTTP API is introduced by this Change.
- No provider SDK type may enter the domain contract.
- The aggregator accepts provider-independent normalized event input and returns
  snapshot metadata, ordered trace rows, and the current summary.

## Data Flows

- Complete normalized event list -> deterministic event ordering -> event
  fingerprints -> snapshot hash.
- Existing snapshot hash equals incoming hash -> duplicate result with no trace
  replacement and no version advance.
- Existing snapshot hash differs -> new snapshot version and full trace snapshot
  persistence by the later production service.
- Candidate summary -> terminal and event-time guards -> delivery current
  tracking summary.

## State Behavior

- Initial: first non-empty complete snapshot starts at version 1.
- Duplicate: reordered or identical complete input does not advance versions.
- Corrected history: a changed complete history advances versions.
- Terminal: `DELIVERED` and `RETURNED` cannot regress to non-terminal states.
- Unknown status: maps to platform `UNKNOWN`.
- Missing ETA: accepted as `null`.
- Invalid: an empty complete snapshot or invalid event time is rejected.

## Theme And Locale Policy

- Theme support: none; this Change has no UI.
- Theme modes shown in prototype: none.
- Theme toggle: none.
- Internationalization: not applicable to the pure logic harness.
- Locales shown in prototype: none.
- Locale switcher: none.

## Out Of Scope Items

- 快递100 SDK, subscription, query, callback signature verification, webhook,
  inbox worker, outbox worker, controller, job, and network calls.
- XianGuanJia shipment-flow modification.
- Schedule-center API or UI.
- Historical delivery backfill.
- Device availability changes after delivery.

## Required Tests

- Unit tests for deterministic fingerprints and snapshot hashes.
- Unit tests for reordered replay suppression and corrected-history versioning.
- Unit tests for equal-time precedence and missing-business-time handling.
- Unit tests for terminal-state regression protection.
- Unit tests for unknown status and nullable ETA.
- Service tests proving duplicate snapshots do not replace traces.
- Transaction tests proving outbox rows are written with delivery changes while
  provider calls remain outside the transaction.
- Mapper tests for tenant isolation, unique constraints, and encrypted sensitive
  configuration fields.

## Open Risks

- Provider timestamps may omit offsets or use provider-specific formats; parsing
  belongs to the later provider adapter, not this foundation aggregator.
- Canonical field changes alter fingerprints and hashes, so the production
  canonicalization contract must be versioned or kept backward-compatible.
- Terminal-state protection must not prevent an explicit future platform
  correction policy; such a policy requires a separate approved change.
- This handoff is not development approval until the user explicitly approves
  `deterministic-snapshot-v1` and `decision.json` is updated.
