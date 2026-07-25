# Spec Review: 005-rental-conversion

## Verdict

approved

## Missing Requirements

No missing requirements were identified within this slice.

## Extra Behavior

- The task also widens local converted-rental amount columns to `BIGINT`. This is required to prevent truncating the documented `int64` source cents during conversion.

## Misunderstood Requirements

- None found. The parser only derives dates from the approved explicit or receipt/return rules and returns a review reason otherwise; it never creates a schedule.

## Cannot Verify From Diff

- Unit tests cannot prove production MySQL lock behavior or real source order line-item shape. The service uses source-row and source-identity mapper lock seams; multi-item conversion remains explicitly deferred.

## Acceptance Assertions Verified

- not applicable: this change has no `acceptance.json`; the prose channel-derived rental-order assertion was reviewed against the transaction service, explicit mapping condition, parsed-date result, unique source schema, migration, and focused test receipts.

## Required Fixes

No required fixes remain for this review.
