# Task Report: 004-fulfillment-safe-remarks

## Status

DONE

## Files Changed

- `camera-rental-server/sql/mysql/migrations/20260831_054_rental_fulfillment_facts.sql`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/dal/dataobject/{rental,xianyu}/**`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/dal/mysql/{rental,xianyu}/**`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/integration/xianyu/service/{XianyuOrderPersistenceServiceImpl,XianyuOrderRemarkHistoryService}.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/reconciliation/**`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/{RentalDeviceAssignmentServiceImpl,admin/RentalDeviceOpsService,logistics/RentalDeliveryServiceImpl}.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/**`
- `openspec/changes/add-rental-configuration/development/migrations/{20260831_054_*,fixture-20260831_054-fulfillment-base.sql,rollback-20260831_054_*,verify-20260831_054-disposable-mysql.sh,manifest.json,README.md}`

## What Changed

- Every current order-detail remark parse attempt now creates a
  `xianyu_order_remark_history` snapshot with the restricted source remark,
  parser result, parsed dates, source update time and classified change type.
  Empty, incomplete, invalid and ambiguous attempts remain audit history and
  cannot replace the previous effective plan.
- A successful candidate becomes effective only after the same transactional
  reconciliation returns `planApplied = true`. Review-required reconciliation
  no longer advances the channel order's effective dates or marks the candidate
  history row effective.
- Added explicit classification for initial plans, unchanged plans, extensions,
  early returns, reschedules, replacements, damage, loss, overdue and logistics
  delay. Contradictory or multiple special-case suffixes are classified as
  ambiguous and require review.
- Added one `RentalFulfillmentUpdateGuard` for both remark-driven plan changes
  and configuration-driven model changes. Unassigned orders may update their
  model and valid plan; assigned orders require the configured model, order-item
  model and every physical device model to match exactly.
- Assigned and dispatched schedule extensions use locked effective schedules
  and overlap checks. Conflicts preserve the current schedules and open an
  explicit fulfillment review.
- Early-return remarks update only `expected_send_back_date`; they do not
  shorten an effective occupied schedule or synthesize return or inspection
  completion.
- Replacement, damage, loss, overdue and logistics-delay suffixes create
  operational reviews. They do not replace a device, close an assignment,
  release a schedule, issue a refund or settle an order.
- Returned/inspected, canceled and financially settled orders are immutable to
  this reconciliation path. Mixed active/returned assignments, missing devices,
  inconsistent device states, active device locks, changed assignments and
  missing/mismatched schedules all fail closed into review.
- Reconciliation and assignment now share the stable lock order:
  channel order, rental order, order item, device, assignment, schedule and
  overlap rows. Migration 054 adds separate expected-return, inspection and
  settlement facts so seller intent is not confused with physical or financial
  completion.
- Authoritative warehouse return time, inspection-completion time and schedule
  narrowing now share one injected `Clock.system(Asia/Shanghai)`. The persisted
  timezone-less timestamps therefore use Shanghai business wall time even when
  the JVM default timezone differs.

## TDD Evidence

- `RentalRemarkPlanChangeClassifierTest` covers every approved suffix, inferred
  extension/early-return/reschedule changes, invalid candidates and
  contradictory or multiple suffixes.
- `XianyuOrderPersistenceServiceImplTest` proves every current parse is
  recorded, invalid updates retain the previous effective plan, successful
  candidates update the same order only after reconciliation accepts them,
  review-required candidates do not advance effective dates, old snapshots do
  not overwrite newer facts, and configured skipped orders do not parse or
  write remark history.
- `RentalFulfillmentUpdateGuardTest` has 19 cases covering unassigned updates,
  assigned/dispatched extensions, overlap conflicts, early-return occupancy
  preservation, operational review suffixes, returned/inspected and settled
  immutability, exact model/device consistency, mixed lifecycle state, missing
  devices, active device locks and stable lock order.
- Assignment, delivery, device-ops and reconciliation tests cover the shared
  fact fields and transaction ordering. Guard plus assignment coverage passed
  29 tests with no failures.
- `RentalFulfillmentFactsMigrationTest` verifies the five additive columns,
  byte-identical production/development migration copies and the limited 054
  rollback.
- The disposable MySQL 8.4 fixture passed nullable defaults, value round trips,
  destructive rollback, base-row retention and cleanup. Its two-thread
  assign-vs-reconcile test invokes the real
  `RentalDeviceAssignmentServiceImpl.assign(...)` and
  `RentalChannelOrderReconciliationService.reconcile(...)` through Spring
  transaction proxies and production MyBatis Mappers. A test aspect pauses the
  actual `RentalScheduleMapper.insert(...)` after assignment has acquired its
  earlier locks, proving reconciliation waits and then both transactions commit
  without deadlock or timeout. The final database state has exactly one
  assignment, one effective schedule, the extended schedule end, updated
  order/item plan dates and `CONVERTED/READY` channel state.
- `RentalDeviceOpsServiceTest` uses a fixed Shanghai clock while changing the
  JVM default timezone to `America/Los_Angeles`. It asserts exact
  `returnedAt` and `inspectionCompletedAt` values of `2026-09-01T00:30` and an
  exclusive schedule end of `2026-09-02`.
- Focused Task 004 regression: 129 tests, 0 failures, 0 errors, 0 skipped.
- Full rental-biz regression: 631 tests, 0 failures, 0 errors, 7 skipped.
  Six skips are existing environment-gated logistics MySQL tests; the seventh
  is the fulfillment MySQL test that passed separately in the disposable
  fixture.

## Verification Commands

- `openspec/changes/add-rental-configuration/development/migrations/verify-20260831_054-disposable-mysql.sh`
- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -pl yudao-module-rental/yudao-module-rental-biz -am test -Dmaven.repo.local=/Volumes/zwl/maven-repository -Dtest=XianyuOrderPersistenceServiceImplTest,XianyuOrderShipServiceTest,RentalDeliveryServiceImplTest,RentalChannelOrderReconciliationServiceTest,RentalOrderPreparationPolicyTest,RentalRemarkPlanChangeClassifierTest,RentalFulfillmentUpdateGuardTest,RentalDeviceAssignmentServiceImplTest,RentalDeviceOpsServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
- `cd camera-rental-server/yudao-module-rental/yudao-module-rental-biz && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -Dmaven.repo.local=/Volumes/zwl/maven-repository test`
- `cmp -s` and `shasum -a 256` for the production/development 054 migration,
  rollback, fixture and disposable verification script
- `git diff --check`
- JSON/JSONL parsing for the complete change directory
- `sh -n` for every development migration verification script
- `openspec validate add-rental-configuration --strict`

## Concerns

- Migration 054 is additive but has not been applied to production or the 80
  server. Its rollback discards any expected-return, inspection and settlement
  values written after deployment and therefore requires backup and separate
  approval.
- This task records replacement intent and opens review only. A future
  operational replacement command must preserve the old assignment and create
  a new assignment rather than overwrite `device_id`.
- Full end-to-end A6/A7 verification remains part of the final change
  Verification stage; this task supplies the server-side unit, redteam and real
  MySQL concurrency evidence.

## Scope Deviations

- None recorded.

## Follow-up Needed

- Task 005 must reuse this reconciliation and fulfillment guard behavior while
  adding persisted historical-job state, bounded batches, pause/resume,
  counters and restart recovery.
- Task 006 must expose the approved remark conventions and reconciliation
  results on the standalone admin page.
- Task 007 must complete cross-repository documentation and final Verification
  preparation.

## Adjudication

The implementation satisfies Task 004 and contributes complete server-side
evidence for A6 and A7 without treating seller intent as physical or financial
completion. It does not perform device replacement, return confirmation,
inspection, refund, settlement, third-party writes, production migration or
historical batch execution.

## Online Facticity Evidence

- On `2026-09-01`, the public `llms.txt` index was fetched and hashed as
  `d14e677ee86bc7c0b02737fe627b5444e361a68921537ba326ab205eabbb11ea`.
- The current access guide was fetched and hashed as
  `52601f044346cdd43f9687d4a09662bc88b089111e618b8bdf165324ce1ea72e`.
  It still documents `https://open.goofish.pro`, strict parameter types and an
  MD5 signature over the application key, MD5 of the exact POST body,
  second-based timestamp and application secret.
- The current order-detail Markdown was fetched and hashed as
  `efc0e1c28be34b0514c53c1ab1e4304f21f0fb1469c3b786fc58fca3b35b8813`.
  It documents JSON `POST /api/open/order/detail`, query authentication with
  `appid`, a timestamp valid for five minutes and `sign`, plus a string
  `order_no` request body.
- The response still contains `seller_remark` and separates
  `goods.product_id` as the XianGuanJia product ID, `goods.item_id` as the
  Xianyu item ID and `goods.sku_id` as the XianGuanJia SKU ID. The order-detail
  schema does not provide a Xianyu SKU ID, so the implementation does not invent
  one from this response.
- The published schema exposes business `code`, `msg` and `data`, with a
  documented failed response example using code `500`; no broader error catalog
  or additional endpoint-specific rate limit is stated in this Markdown.
- This evidence refresh used public documentation only. No merchant credential,
  real order-detail request, third-party write, production database or 80-server
  access was used.
