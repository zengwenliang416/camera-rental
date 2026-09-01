# Spec Review: 003-order-reconciliation

## Verdict

approved

## Missing Requirements

- None. The previous identity-change defect is corrected:
  `RentalChannelProductRuleService.updateRule` reads the persisted rule before
  mutation and publishes exact item-scope reconciliation events for both the
  previous and accepted new `shopId + xianyuItemId` when either identity field
  changes (`RentalChannelProductRuleService.java:74-102`).

## Extra Behavior

- `updateRuleStatus` publishes the same exact item event for both enable and
  disable operations (`RentalChannelProductRuleService.java:105-125`). This is
  consistent with re-evaluating mutable orders when an applicable rule appears
  or disappears.
- The event listener uses `fallbackExecution = true`
  (`RentalChannelOrderReconciliationWorker.java:29-33`), so a valid event
  published without a surrounding transaction is still processed. All current
  production publishers are transactional; this fallback does not weaken the
  after-commit behavior of those paths.

## Misunderstood Requirements

- None. The trigger is now only a tenant-scoped event publisher rather than an
  inline order-query/reconciliation executor
  (`RentalChannelOrderReconciliationTrigger.java:20-47`). The worker owns
  candidate discovery and invokes the same authoritative reconciliation
  service for every selected order.

## Cannot Verify From Diff

- No blocking claims remain unverifiable. The worker's actual thread handoff is
  not exercised through a complete Spring asynchronous integration test, but
  the production listener is annotated with both `@Async` and
  `@TransactionalEventListener(AFTER_COMMIT)`, the rental module includes the
  project's `yudao-spring-boot-starter-job` async configuration, and the worker
  test verifies both annotations and tenant-context restoration.
- The H2 integration test executes the production item and product-plus-SKU
  candidate statements. It does not separately invoke the pure-product
  statement; that statement's tenant, shop, product, linked-order, non-closed,
  assignment, cursor, ordering, and limit predicates were verified directly
  from `XianyuOrderMapper.java:261-290`. This is a non-blocking coverage gap
  because the shared database boundaries are executed by the item query, the
  product/SKU specificity is executed by the SKU query, and the worker test
  verifies the pure-product dispatch signature.

## Acceptance Assertions Verified

- A4 - Verified. Single-model resolution selects only an enabled rule by exact
  `shopId + xianyuItemId`. Multi-model resolution requires an enabled child
  mapping by exact `productRuleId + xgjSkuId` and never reads
  `singleDeviceModelId` as a fallback
  (`RentalChannelOrderReconciliationService.java:241-279`,
  `RentalChannelProductRuleMapper.java:25-30`,
  `RentalChannelProductSkuMappingMapper.java:24-29`). Synchronized Xianyu SKU
  enrichment additionally requires the same shop, Xianyu item, XianGuanJia
  product, and XianGuanJia SKU relationship
  (`RentalChannelOrderReconciliationService.java:282-300`).
  `RentalChannelOrderReconciliationServiceTest` covers the populated product
  default without an exact SKU, the exact enabled SKU mapping, disabled rule
  behavior, a mismatched synchronized product, and the complete synchronized
  relationship (`:161-207,232-316`).

## Required Fixes

- No blocking fixes remain for Task 003 development handoff.

## Validation Performed

- Independently reran the directly related suite:
  `RentalChannelProductRuleServiceTest`,
  `RentalChannelProductRuleTransactionIntegrationTest`,
  `RentalChannelOrderReconciliationTriggerTest`,
  `RentalChannelOrderReconciliationWorkerTest`, and
  `XianyuOrderReconciliationCandidateIntegrationTest`: 25 tests, 0 failures,
  0 errors, 0 skipped.
- Independently reran the focused Task 003 suite: 153 tests, 0 failures,
  0 errors, 0 skipped.
- Verified the system-executed full-module evidence recorded at
  `2026-08-31T21:49:04+08:00`: 592 tests, 0 failures, 0 errors, 6 existing
  environment-gated MySQL concurrency tests skipped.
- Verified the worker pages by ascending `xianyu_order.id` with
  `id > afterId`, a fixed batch size of 500, a non-advancing cursor guard, and
  per-order `RuntimeException` isolation
  (`RentalChannelOrderReconciliationWorker.java:37-77`).
- Verified all three production candidate SQL statements explicitly constrain
  tenant and shop; apply their exact item, product, or product-plus-SKU scope;
  require an existing internal order; exclude `CLOSED` and assigned orders;
  apply the optional exclusive cursor; order by ascending id; and enforce the
  supplied limit (`XianyuOrderMapper.java:230-326`).
