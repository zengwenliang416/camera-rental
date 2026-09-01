# Quality Review: 003-order-reconciliation

## Verdict

approved

The two previous HIGH blockers are closed. Configuration, product, and SKU
writes now publish tenant-scoped events only; reconciliation runs through an
asynchronous after-commit worker with stable paging, per-order transaction
isolation, and old/new rule-identity fan-out.

## Separation Of Concerns

- `RentalChannelOrderReconciliationTrigger` is limited to identifier
  normalization and `ApplicationEventPublisher.publishEvent(...)`. It no
  longer selects orders or calls the reconciliation service inside source
  transactions.
- `RentalChannelOrderReconciliationWorker` owns event consumption and candidate
  fan-out. Its listener is annotated with both `@Async` and
  `@TransactionalEventListener(phase = AFTER_COMMIT)`, while the rental module
  explicitly depends on the job starter whose auto-configuration enables
  Spring async processing.
- Each candidate delegates to the authoritative transactional
  `RentalChannelOrderReconciliationService.reconcile(channelOrderId)` rather
  than duplicating mapping, readiness, or review behavior.
- Task 003 remains correctly bounded to immediate re-evaluation after current
  rule, product, or SKU changes. It does not scan historical orders without an
  internal order or claim persisted job state, pause/resume, counters, restart
  recovery, or operational backfill controls; those remain Task 005
  responsibilities.

## Component Cohesion / Coupling

- The trigger, immutable event, worker, candidate queries, and reconciliation
  service form distinct components with explicit responsibilities.
- Rule updates read the persisted previous identity before mutation. When
  shop/item identity changes, the service publishes one event for the old
  exact scope and one for the accepted new scope; unchanged identity publishes
  only the current scope.
- Product and SKU persistence reuse the same trigger boundary. SKU events
  preserve exact shop, product, and normalized non-empty SKU identifiers.
- `RentalChannelOrderReconciliationService` remains a large orchestration
  component, but it is the single intended domain entrypoint and no competing
  conversion or shipment-time mapping path was reintroduced.

## Test Quality

- Receipt `007-003-order-reconciliation.log` records the current Task 003
  focused suite at 164 tests, 0 failures, 0 errors, and 0 skipped.
- The independently rerun Spring/MyBatis/H2 rule transaction test passed:
  1 test, 0 failures, 0 errors, and 0 skipped.
- `XianyuOrderReconciliationCandidateIntegrationTest` executes the production
  MyBatis SQL against H2 in MySQL mode. It proves that 502 valid item
  candidates page as 500 then 2, and excludes cross-tenant, cross-shop,
  assigned, closed, and unlinked rows. Its SKU case proves exact tenant, shop,
  product, SKU, and cursor filtering.
- Worker tests prove stable cursor advancement beyond 500 candidates, exact SKU
  scope forwarding, continuation after one reconciliation failure, listener
  annotations, and tenant-context restoration. Rule tests prove old/new
  identity fan-out.
- **MEDIUM, non-blocking:** the listener test constructs the worker directly
  and verifies `@Async` and `AFTER_COMMIT` by reflection. It does not execute a
  real Spring-proxied transaction to prove rollback suppression, commit-time
  dispatch, or execution on a different thread. The production dependency,
  async auto-configuration, source-only event publication, and listener
  annotations make the runtime boundary directly inspectable, so this is a
  test-depth risk rather than an unresolved implementation defect.
- Recorded `attestation: "system-executed"` evidence also shows the focused
  asynchronous-fix suite at 25 tests, the complete Task 003 suite at 153 tests,
  and the full rental-biz suite at 592 tests, all with 0 failures and 0 errors.
  Six pre-existing real-MySQL concurrency tests remained environment-gated.

## Error Handling

- The worker has no surrounding transaction. Each call into the proxied
  reconciliation service opens its own transaction, and `reconcileSafely`
  catches a single order's `RuntimeException`, logs only the channel-order ID
  and exception class, and continues with later candidates.
- Source rule/product/SKU writes therefore complete before reconciliation
  begins and are not rolled back by an affected-order failure.
- All three candidate queries explicitly constrain tenant and exact scope,
  require an existing internal order, exclude closed and assigned orders, use
  `id > afterId`, order by ascending ID, and limit each batch to 500.
- The worker advances from the final ID of each batch, stops only after a short
  or empty batch, and guards against a non-advancing cursor. This closes the
  previous first-500 starvation defect for the current affected set.
- Failure logging does not include raw payloads, credentials, customer contact
  details, or other sensitive order data.

## Reuse / Duplication

- Rule, product, and SKU mutation paths reuse one event trigger and one worker
  rather than implementing separate affected-order loops.
- Immediate reconciliation and future historical backfill share the same
  idempotent reconciliation service without conflating their orchestration
  lifecycles.
- Assignment and shipment continue to reuse
  `RentalOrderPreparationPolicy.requireReady`; the obsolete conversion service
  and shipment-time mapping path remain removed.
- Candidate filtering is expressed in three scope-specific Mapper queries.
  Their repeated safety predicates are straightforward and preserve explicit
  SQL boundaries rather than introducing an unnecessary dynamic-query
  abstraction.

## Complexity Delta

- The asynchronous fan-out adds an event record, listener, and three paged
  candidate queries, but removes synchronous reconciliation work from source
  transactions and makes failure isolation explicit.
- Stable ID paging is linear in the current candidate set and bounded to 500
  selected IDs per query. It avoids repeated first-page processing without
  introducing offset paging or an in-memory full-set load.
- The in-process listener intentionally does not provide durable orchestration.
  Adding persisted retry state here would duplicate Task 005's bounded,
  resumable historical-job design and increase coupling.
- The remaining reconciliation-service size is a maintainability concern, not
  a correctness, security, or release blocker in this review round.

## Acceptance Assertions Verified

- `A4` - Verified at the order-side exact-matching/no-fallback implementation
  layer. The order reconciliation path selects an enabled product rule only by
  exact `shopId + xianyuItemId`
  (`RentalChannelOrderReconciliationService.java:300-306`,
  `RentalChannelProductRuleMapper.java:25-30`). A single-model rule resolves
  only its configured `singleDeviceModelId`; a multi-model rule instead
  requires the order's non-empty XianGuanJia SKU and selects an enabled child
  mapping only by exact `productRuleId + xgjSkuId`
  (`RentalChannelOrderReconciliationService.java:308-330`,
  `RentalChannelProductSkuMappingMapper.java:24-29`). The multi-model branch
  never reads the product-level default. Synchronized Xianyu SKU enrichment is
  also rejected unless the same shop, Xianyu item, XianGuanJia product, and
  XianGuanJia SKU relationship is present
  (`RentalChannelOrderReconciliationService.java:341-358`).
  `RentalChannelOrderReconciliationServiceTest` proves no fallback when a
  product default exists, successful use of the exact enabled SKU mapping,
  rejection of a mismatched synchronized product, and enrichment only from the
  complete synchronized relationship (`:207-253,328-362`).
- No other acceptance assertion is claimed by this Task 003 quality review;
  the task packet's acceptance set is exactly `A4`.

## Required Fixes

- None. The previous asynchronous-boundary, failure-isolation, paging, and
  identity-change findings were verified as corrected in the current
  implementation and tests.

## Validation Evidence

- Independently reviewed the committed implementation diff from baseline
  `cea45c48` through implementation commit `c621976b`, scoped to the Task 003
  packet, report, production code, tests, and receipt 007.
- Receipt `007-003-order-reconciliation.log`: 164 focused Task 003 tests passed.
- Reviewer-run Spring/MyBatis/H2 rule transaction test: 1 test passed.
- System-executed full rental-biz suite: 592 tests passed, with 6 existing
  environment-gated MySQL concurrency tests skipped.
- Current review reran `git diff --check` successfully and
  `openspec validate add-rental-configuration --strict` returned valid.
