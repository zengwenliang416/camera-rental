# Spec Review: 006-device-allocation

## Verdict

approved

## Missing Requirements

- No missing requirement was found in the assignment slice.

## Extra Behavior

- Rejecting reuse of an idempotency key with different device, item, or dates
  is a necessary strengthening of the idempotency contract.

## Misunderstood Requirements

- None. The service stores and compares occupied half-open ranges and does not
  reinterpret them as billable dates.

## Cannot Verify From Diff

- Unit tests alone cannot prove InnoDB lock scheduling. The existing real-MySQL
  concurrency report independently shows one overlapping assignment succeeds,
  the competing transaction waits on the device lock, and then receives a
  schedule conflict.

## Acceptance Assertions Verified

- A1

## Required Fixes

- No blocking fixes were identified.
