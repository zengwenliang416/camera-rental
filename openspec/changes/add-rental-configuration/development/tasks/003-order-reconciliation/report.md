# Task Report: 003-order-reconciliation

## Status

DONE

## Files Changed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/reconciliation/**`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/integration/xianyu/service/XianyuOrderPersistenceServiceImpl.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/{RentalDeviceAssignmentServiceImpl.java,admin/XianyuOrderShipService.java}`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/dal/mysql/{rental/RentalScheduleAllocationMapper.java,xianyu/**}`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/controller/admin/{xianyu/**,rental/vo/RentalProductSkuReportRespVO.java}`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/resources/mapper/rental/RentalReportMapper.xml`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/{service,integration,dal}/**`
- Removed the obsolete `XianyuRentalConversionService`,
  `XianyuRentalConversionServiceImpl`, `RentalConversionResult`, and
  `XianyuProductMappingMapper` runtime paths and their superseded tests.

## What Changed

- Added one transactional `RentalChannelOrderReconciliationService` as the
  authoritative path for skip evaluation, internal-order creation, exact model
  resolution, preparation status, review resolution, and retry updates.
- Added `RentalChannelOrderEligibilityPolicy`: only paid normal statuses `12`,
  `21`, and `22` may create internal orders; pending-payment, refunded, closed,
  fully-refunded, canceled, null-amount, and negative-amount details remain
  durable channel evidence without creating an internal order.
- `RentalChannelOrderReconciliationTrigger` now only publishes a tenant-scoped
  event. `RentalChannelOrderReconciliationWorker` consumes it asynchronously
  after the source transaction commits, pages candidates by ascending
  `xianyu_order.id` in batches of 500, and isolates individual order failures
  from the originating rule or synchronization write.
- Rule updates compare the persisted previous identity with the accepted new
  identity. Moving a rule across shop/item boundaries publishes precise
  reconciliation events for both the old and new identities.
- Normal durable order details now create exactly one internal rental order and
  item immediately. Missing model, remark, or dates produce
  `WAITING_MODEL`/`WAITING_REMARK` instead of blocking creation; `pay_amount`
  is retained as the order and item rent amount.
- `CONFIG_SKIPPED` is evaluated by exact shop plus Xianyu item before remark
  parsing and creates no internal order, item, schedule, or conversion review.
  A later skip rule cannot clear or reverse an already-created internal order.
- Single-model rules use only the exact shop/item rule. Multi-model rules
  require an enabled child mapping for the order's exact XianGuanJia SKU and
  never fall back to a product-level model.
- A Xianyu SKU is derived only when the synchronized product matches the same
  shop, Xianyu item, XianGuanJia product, and XianGuanJia SKU relationship.
- Existing items with a concrete device assignment are protected from
  automatic model/date mutation. Task 004 owns the fuller fulfillment-state
  update matrix and remark-history policy.
- Existing `rentalOrderId` links are validated against `sourceType = XIANYU`,
  exact `shopId:externalOrderId`, and the current `channelOrderId` before any
  mutation. A mismatch preserves both records and opens
  `RENTAL_ORDER_LINK_CONFLICT` review instead of updating an unrelated order.
- Resolved or closed conversion reviews are reopened when reconciliation fails
  again, and changed reasons replace stale details while clearing prior
  resolution metadata.
- Shipment no longer creates a mapping or performs conversion. Shipment and
  device assignment both use `RentalOrderPreparationPolicy.requireReady`, and
  pending-allocation list/count SQL includes `preparation_status = 'READY'`.
- Server order query/response and product/SKU reporting now expose
  `xgjProductId`, `xianyuItemId`, `xgjSkuId`, and `xianyuSkuId`; current runtime
  reads no longer use the ambiguous legacy order product/SKU fields.

## TDD Evidence

- `RentalChannelOrderReconciliationServiceTest` covers immediate creation with
  missing data, same-order retry, exact skip behavior, no product fallback for
  multi-model rules, exact enabled SKU mapping, model-before-insert behavior,
  later readiness, protection of existing internal orders, and complete
  synchronized product/SKU derivation.
- `RentalOrderPreparationPolicyTest`,
  `RentalDeviceAssignmentServiceImplTest`,
  `RentalScheduleAllocationMapperContractTest`, and
  `XianyuOrderShipServiceTest` cover both query and transactional `READY`
  gates and removal of shipment-time conversion/mapping creation.
- Persistence, order query, manual review, reporting, order sync, and mapper
  tests cover the centralized reconciliation entrypoint and explicit
  identifiers.
- `RentalChannelOrderEligibilityPolicyTest` covers every accepted and rejected
  order-status boundary. `RentalChannelOrderReconciliationTriggerTest`,
  `RentalChannelProductRuleServiceTest`,
  `XianyuProductPersistenceServiceTest`, and
  `XianyuProductSkuPersistenceServiceTest` cover normalized event publication,
  old/new rule identity fan-out, exact SKU selection,
  disabled/non-matching cases, and same-order reuse without duplication.
- `RentalChannelOrderReconciliationWorkerTest` covers after-commit asynchronous
  dispatch, tenant-context restoration, continuation beyond 500 candidates,
  stable cursor advancement, exact SKU scope, and per-order failure isolation.
- `XianyuOrderReconciliationCandidateIntegrationTest` executes the production
  MyBatis queries against H2 MySQL mode and covers tenant/shop isolation,
  assigned/closed/unlinked exclusion, exact SKU selection, and paging beyond
  the first 500 records.
- Reconciliation tests additionally cover stateful two-call retry, linked-order
  identity conflict protection, and reopening resolved reviews. Shipment tests
  verify the preparation policy call and rejection behavior before assignment
  or remote shipment.
- The Spring/MyBatis/H2 transaction test uses `@MockitoBean` for the new trigger
  dependency and still proves parent and prior child mappings roll back when
  replacement insertion violates the database constraint.
- Focused Task 003 suite: 153 tests, 0 failures, 0 errors, 0 skipped.
- Full rental-biz suite: 592 tests, 0 failures, 0 errors, 6 existing
  environment-gated real-MySQL concurrency tests skipped.

## Verification Commands

- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -Dmaven.repo.local=/Volumes/zwl/maven-repository -pl yudao-module-rental/yudao-module-rental-biz -Dtest=RentalChannelProductRuleTransactionIntegrationTest test`
- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -Dmaven.repo.local=/Volumes/zwl/maven-repository -pl yudao-module-rental/yudao-module-rental-biz -Dtest=RentalChannelProductRuleServiceTest,RentalChannelProductRuleTransactionIntegrationTest,RentalChannelOrderReconciliationTriggerTest,RentalChannelOrderReconciliationWorkerTest,XianyuOrderReconciliationCandidateIntegrationTest test`
- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -Dmaven.repo.local=/Volumes/zwl/maven-repository -pl yudao-module-rental/yudao-module-rental-biz -Dtest=RentalChannelOrderReconciliationServiceTest,RentalOrderPreparationPolicyTest,RentalScheduleAllocationMapperContractTest,XianyuOrderMapperTest,XianyuOrderPersistenceServiceImplTest,RentalDeviceAssignmentServiceImplTest,XianyuOrderShipServiceTest,XianyuOrderSyncServiceTest,XianyuOrderAdminServiceTest,RentalManualReviewAdminServiceTest,RentalReportAdminServiceTest,RentalChannelOrderEligibilityPolicyTest,RentalChannelOrderReconciliationTriggerTest,RentalChannelOrderReconciliationWorkerTest,RentalChannelProductRuleServiceTest,XianyuProductPersistenceServiceTest,XianyuProductSkuPersistenceServiceTest,XianyuOrderReconciliationCandidateIntegrationTest test`
- `cd camera-rental-server/yudao-module-rental/yudao-module-rental-biz && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -Dmaven.repo.local=/Volumes/zwl/maven-repository test`
- `git diff --check`
- JSON/JSONL parse validation for the complete change directory
- `openspec validate add-rental-configuration --strict`
- Static searches for removed conversion/mapping services and ambiguous
  order/report runtime reads

## Concerns

- Six unrelated MySQL concurrency tests remain skipped behind their existing
  environment gate. No production database or 80-server test substituted for
  them.
- Task 003 uses an after-commit in-process event worker for immediate affected
  orders. Task 005 remains the owner of durable historical-job state, operator
  pause/resume, persisted counters, and restart recovery; this task does not
  claim those batch-operation capabilities.
- The server order/report contract now uses four explicit identifiers, while
  the admin client still uses the two legacy field names. Task 006 owns that
  coordinated UI/API-client adaptation; deploying this backend slice alone
  would temporarily break those admin filters and columns.
- Task 004 still owns remark-attempt history, invalid-remark preservation,
  conflict classification, and the full assigned/dispatched/returned/settled
  update matrix. Task 005 owns bounded historical reconciliation.

## Scope Deviations

- None recorded.

## Follow-up Needed

- Task 004 must extend the centralized reconciliation path with
  fulfillment-safe remark history and update classification.
- Task 005 must use the same idempotent reconciliation service for bounded
  historical processing.
- Task 006 must update the admin client and UI to the four explicit identifier
  fields before deployment.

## Adjudication

The implementation stays within the Task 003 server boundary and does not
select a concrete device, mutate fulfillment/financial facts, run a historical
batch, call a third-party write API, deploy, or alter production data. The
current evidence supports the task behavior and complete A4 order-side exact
matching; the remaining concerns are assigned to later graph nodes rather than
hidden compatibility fallbacks.
