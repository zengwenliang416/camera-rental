# Quality Review: 002-configuration-backend

## Verdict

approved

## Separation Of Concerns

- The controller remains thin and delegates catalog and channel-rule behavior
  to services rather than accessing Mappers directly.
- Exact tenant, shop, application, product, SKU, and model validation remains
  centralized in `RentalChannelProductRuleValidator` and is reused by create,
  update, impact preview, synchronized-SKU reads, and rule re-enable paths.
- Rule persistence and child replacement remain within one service transaction.
  The independently inspected Spring/MyBatis integration test invokes the
  service through an AOP proxy and proves that a child constraint failure rolls
  back the parent update, version increment, child deletion, and replacement.
- The controlled 29-item seed remains separate from normal migrations and does
  not introduce a production or third-party write path.

## Component Cohesion / Coupling

- `RentalChannelProductRuleService` remains cohesive around rule persistence,
  projections, bounded impact preview, optimistic versioning, and child
  mappings. Exact ownership policy remains in the validator.
- `XianyuApplicationMapper.selectByTenantIdAndId` now makes the application
  ownership boundary explicit. A shop whose application is missing or belongs
  to another tenant is rejected before product lookup.
- `CONFIG_SKIPPED` no longer requires an artificial mapping mode in the request.
  The validator accepts a null mode and normalizes the persisted rule to
  `NONE`, a null single model, and no child mappings.
- No title, remark, merchant-code, XianGuanJia-product, SKU-text, cross-shop, or
  product-default fallback was introduced.

## Test Quality

- The focused tests now cover the five previous findings: stable rule-update
  collision handling, current-model exclusion during duplicate
  classification, application ownership, expired runtime shop authorization,
  null-mode skipped normalization, and real parent/child transaction rollback.
- `RentalChannelProductRuleTransactionIntegrationTest` uses a Spring AOP proxy,
  actual MyBatis Mappers, H2 in MySQL mode, and a named database constraint. It
  verifies that the failure comes from the replacement child insert and then
  reads back the original parent fields/version and both original child rows.
- The independently rerun focused command completed with 37 tests, 0 failures,
  0 errors, and 0 skipped.
- Recorded `attestation: "system-executed"` evidence also shows the complete
  rental-biz suite at 561 tests with 0 failures and 0 errors, with six
  pre-existing environment-gated MySQL concurrency tests skipped.
- The disposable MySQL 8.4 evidence continues to cover catalog locks, the exact
  29-row seed, tenant/shop isolation, zero/duplicate/expired authorization
  rejection, conflict atomicity, rollback, and cleanup.

## Error Handling

- `updateRule` now converts a parent shop/item unique-key collision to
  `RENTAL_CHANNEL_PRODUCT_RULE_DUPLICATE` inside the parent update block and
  before any child deletion or insertion.
- `updateModel` now excludes the current model when rechecking the prefix after
  a database uniqueness exception. A concurrent model-code collision with an
  unchanged prefix is therefore reported as
  `RENTAL_DEVICE_MODEL_DUPLICATE`.
- Optimistic-lock misses remain stable
  `RENTAL_CONFIGURATION_VERSION_CONFLICT` responses, and stale updates stop
  before child mutation.
- Missing/cross-tenant shops or applications, invalid or expired shop
  authorization, unsynchronized exact items, foreign SKU rows, and
  missing/disabled models are rejected before persistence with stable domain
  errors.

## Reuse / Duplication

- Exact ownership and no-fallback checks are shared through the validator
  rather than copied across rule operations.
- The new application lookup extends the existing Xianyu application Mapper
  instead of adding a second persistence path.
- Catalog updates continue to reuse the existing normalization and duplicate
  validation helpers. The collision fix changes only the ambiguous exception
  classification branch.
- The H2 integration fixture is narrowly scoped to the transaction guarantee
  that Mock tests cannot prove; it does not duplicate the disposable MySQL
  migration fixture.

## Complexity Delta

- The fixes are localized: one guarded parent-update block, one current-row
  exclusion, one application ownership lookup, one relaxed request constraint,
  focused unit tests, and one transaction integration test.
- The additional application dependency is justified by the explicit
  shop/application ownership seam and does not add a new service cycle.
- The transaction integration test adds test infrastructure complexity, but it
  is proportional to the data-loss risk and verifies the exact multi-table
  rollback boundary.
- No new oversized component, deep nesting, fallback branch, or speculative
  abstraction was introduced by the fixes.

## Required Fixes

- None. All five findings from the previous quality review were verified as
  corrected in the current implementation and tests.
