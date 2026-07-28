# Spec Review: 003-channel-persistence

## Verdict

needs-fix

## Missing Requirements

- The task packet states that private recipient fields remain only in the
  restricted raw payload. The current diff adds full receiver name, mobile,
  and address columns to `xianyu_order` and persists them as normalized data.
- No task-level contract defines the access-control and retention boundary for
  those newly duplicated private fields.

## Extra Behavior

- `XianyuOrderPersistenceServiceImpl` now parses rental periods, runs historical
  rental-period backfill, triggers conversion, and maintains receiver
  snapshots. Those behaviors exceed the durable order-detail/cursor slice
  described by this task.

## Misunderstood Requirements

- Keeping raw payload access restricted does not preserve the original privacy
  boundary when the same unmasked recipient values are copied into an ordinary
  normalized order table.

## Cannot Verify From Diff

- The local schedule-center artifact proves a masked browser presentation for
  one flow, but it does not prove that every API and export path containing the
  new normalized fields is separately permissioned and masked.
- A1 remains an unresolved placeholder with no substantive statement, so it
  cannot resolve this privacy-scope conflict.

## Required Fixes

- Reconcile the task and parent privacy contract with the receiver-snapshot
  design. Either keep full recipient values behind a separately permissioned
  restricted boundary or update the approved requirements before retaining
  them in `xianyu_order`.
- Update the task report to describe the current parsing, backfill, conversion,
  and receiver-snapshot responsibilities, then add current system-executed
  privacy tests for all exposed paths.
