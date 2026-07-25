# Spec Review: 006-device-allocation

## Verdict

approved

## Missing Requirements

No missing requirements were identified within this slice.

## Extra Behavior

- The service rejects idempotency-key reuse with different command values. This is required to avoid returning an accepted identifier with altered caller dates or device data.

## Misunderstood Requirements

- None. The service stores only `occupy_start_date` and `occupy_end_date_exclusive`; it does not reinterpret them as billable dates or a closed range.

## Cannot Verify From Diff

- Unit tests cannot prove MySQL `FOR UPDATE` behavior, query-plan index selection, or competing transactions in a production-like schema. The mapper locks the device row first and then locks effective overlap rows; the existing device-range index supports the query, but this needs real-MySQL concurrency verification before rollout.

## Acceptance Assertions Verified

- not applicable: this change has no `acceptance.json`. The Rental Operations prose acceptance was reviewed against the service transaction, device/item/order locks, overlap query, idempotency uniqueness constraints in the foundation schema, tests, and documented interval invariant.

## Required Fixes

No required fixes remain for this review.
