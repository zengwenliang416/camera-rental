# Spec Review: 002-configuration-backend

## Verdict

approved

## Missing Requirements

- None within the corrected Task 002 contract. The reviewed backend slice
  provides the authorized catalog, exact shop/item rule configuration,
  optimistic conflict handling, permissions, impact preview, migration, and
  separately controlled seed required by this task.
- The authorization-expiry repair is complete. Both controlled seed copies
  require a null expiry or an `authorization_expires_at` later than the current
  Asia/Shanghai time.
- The final diff also closes the quality-review gaps: shop validation requires
  an application owned by the current tenant and rejects expired runtime
  authorization; `CONFIG_SKIPPED` accepts an omitted mapping mode and persists
  `NONE` without child mappings; update collisions return stable domain errors
  before child replacement; model-code and device-prefix collisions remain
  distinguishable; and a proxied database test proves parent and child rollback
  when replacement insertion fails.

## Extra Behavior

- No unrelated behavior was confirmed in the reviewed Task 002 files.
- The seed remains separately controlled, requires explicit confirmation, and
  does not execute as part of the normal migration.

## Misunderstood Requirements

- None. Exact configuration remains scoped by tenant, internal shop, item, and
  synchronized SKU identifiers without title, remark, item, SKU, or cross-shop
  fallback.
- The seed uses
  `TIMESTAMPADD(HOUR, 8, UTC_TIMESTAMP())` for the same Asia/Shanghai
  current-time boundary used by the runtime validator.

## Cannot Verify From Diff

- Complete A1 remains owned by Task 006 because it requires the standalone
  administration page and remark-template experience.
- Complete A4 remains owned by Task 003 because it requires the order-side exact
  matcher and no-fallback reconciliation behavior.
- Production execution remains outside Task 002. The controlled seed was
  prepared and tested only and was not applied to a real tenant or shop.

## Acceptance Assertions Verified

- Not applicable. Task 002 has no independently bound acceptance assertions
  after the corrected task split. This review does not claim A1 or A4 as
  verified.

## Required Fixes

- None.
- The independently rerun focused Task 002 suite passed 37 tests with no
  failures, errors, or skips. Current system-executed evidence also records 561
  full rental-biz tests with no failures or errors, both disposable MySQL
  fixtures passing, and successful static validation.
- The production and development seed copies are byte-identical with SHA-256
  `26f9bfccb8bc3c50b09f489b3837be90bca957c3ab3823522870329a054cbc46`;
  the independently rerun MySQL 8.4 fixture produced
  `EXPIRED_AUTHORIZATION_ZERO_WRITE_PASS`, `CLEANUP_PASS`, and
  `DISPOSABLE_MYSQL_053_PASS`.
