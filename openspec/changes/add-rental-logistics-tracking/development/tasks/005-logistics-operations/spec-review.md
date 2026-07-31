# Spec Review: 005-logistics-operations

## Verdict

approved

## Missing Requirements

- None found in the re-review scope. The A12 blocker from the previous review is fixed in the actual implementation:
  - `camera-rental-server/sql/mysql/migrations/20260731_032_rental_delivery_tracking.sql` now scopes callback-token uniqueness to `tenant_id + callback_token_hash`, preserves a separate non-unique hash lookup index, and repairs pre-release databases that still carry the old global unique index.
  - `RentalDeliveryMapper.selectCallbackCandidatesByTokenHash(...)` now returns all same-hash candidates instead of assuming one global match.
  - `Kuaidi100CallbackService.receive(...)` resolves candidates outside tenant context, performs constant-time exact-token matching, and only enters one tenant context when there is exactly one token match and the signature is valid.

## Extra Behavior

- The callback path now fails closed when the same raw callback token is reused across multiple tenants. That is stricter than "same hash may repeat across tenants", but it is security-preserving and does not contradict the accepted contract because the public callback endpoint still avoids cross-tenant routing and does not require ambiguous tokens to succeed.

## Misunderstood Requirements

- None remain after the fix. The previous misunderstanding was the global `callback_token_hash` uniqueness assumption; the migration, mapper, service flow, and MySQL regression now implement tenant-isolated uniqueness instead.

## Cannot Verify From Diff

- I could not re-run the database-backed MySQL concurrency suite in this shell because `RENTAL_LOGISTICS_MYSQL_*` is not exported here. I instead verified those cases from `development/validation-log.jsonl` entries with `attestation: "system-executed"` covering:
  - legacy global-index upgrade plus repeated migration 032,
  - MySQL 8.4 concurrency for cross-tenant callback-hash reuse and same-tenant uniqueness,
  - fresh-schema and current-upgrade migration replay.
- Everything else in this review was rechecked directly from the current files and local focused test execution.

## Acceptance Assertions Verified

- A11
- A12
- A15
- A16

## Required Fixes

No required fixes remain in the re-review scope.
