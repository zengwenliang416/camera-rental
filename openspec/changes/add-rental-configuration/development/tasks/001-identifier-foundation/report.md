# Task Report: 001-identifier-foundation

## Status

DONE

## Files Changed

- `camera-rental-server/sql/mysql/migrations/20260831_052_rental_configuration_foundation.sql`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/**`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/**`
- `openspec/changes/add-rental-configuration/development/migrations/**`

## What Changed

- Added explicit string fields for XianGuanJia product/SKU identifiers and
  Xianyu item/SKU identifiers on channel shops, products, SKUs and orders.
- Parsed order `goods.product_id`, `goods.item_id` and `goods.sku_id` into
  separate fields without cross-field fallback.
- Persisted product `publish_shop.item_id` only from the uniquely matching
  synchronized shop and persisted SKU `xy_sku_id` beside its XianGuanJia
  `sku_id`.
- Kept missing identifiers null, preserved an existing exact Xianyu SKU ID when
  a later response omitted it, and removed ambiguous legacy-field lookup paths
  from product synchronization.
- Added the additive configuration foundation migration, guarded legacy
  backfills, exact-match indexes, menu/permission seeds and destructive
  pre-production rollback documentation.
- Made rollback restore the proven XianGuanJia product/SKU values into nullable
  legacy columns before reapplying their original `NOT NULL` definitions.

## TDD Evidence

- `RentalConfigurationFoundationMigrationTest` checks explicit columns,
  configuration tables, permission seeds, JSON-source backfill and rejection
  of direct ambiguous order legacy-field backfill.
- Parser and persistence tests cover numeric identifiers serialized as strings,
  missing fields remaining null, exact shop ownership, ambiguous/missing shop
  rejection, nullable Xianyu item IDs and exact SKU-ID preservation.
- The disposable MySQL fixture executes the real forward migration and rollback
  while asserting unambiguous backfill, ambiguous-row preservation, numeric ID
  precision, all new unique constraints, nullable legacy inserts, menu seeds
  and rollback restoration.
- Focused suite result: 57 tests, 0 failures, 0 errors, 0 skipped.
- Full rental-biz result: 537 tests, 0 failures, 0 errors, 6 skipped
  environment-gated MySQL concurrency tests.
- Disposable database result: MySQL 8.4.10, forward and rollback assertions
  passed; temporary container and volume removed.

## Verification Commands

- `git diff --check`
- `shasum -a 256 camera-rental-server/sql/mysql/migrations/20260831_052_rental_configuration_foundation.sql`
- `openspec/changes/add-rental-configuration/development/migrations/verify-20260831_052-disposable-mysql.sh`
- `/Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=RentalConfigurationFoundationMigrationTest,XianyuAuthorizedShopListParserTest,XianyuOrderPayloadParserTest,XianyuOrderPersistenceServiceImplTest,XianyuProductDetailPayloadParserTest,XianyuProductListPageParserTest,XianyuProductPersistenceServiceTest,XianyuProductSkuPayloadParserTest,XianyuProductSkuPersistenceServiceTest,XianyuProductSyncServiceTest,XianyuProductPushShopResolverTest,XianyuChannelSyncServiceTest,XianyuShopAdminServiceTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=/Volumes/zwl/maven-repository test`
- `/Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -pl yudao-module-rental/yudao-module-rental-biz -am -DskipTests -Dmaven.repo.local=/Volumes/zwl/maven-repository install`
- `/Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -pl yudao-module-rental/yudao-module-rental-biz -Dmaven.repo.local=/Volumes/zwl/maven-repository test`

## Concerns

- Six unrelated MySQL concurrency tests were skipped by their existing
  environment gate.

## Scope Deviations

- None recorded.

## Follow-up Needed

- Task 003 must remove runtime conversion reads of ambiguous legacy order
  fields; that behavior is intentionally outside this task.

## Adjudication

The isolated-MySQL execution concern is resolved by the disposable MySQL
8.4.10 run, and both independent task reviews are approved. Task-local signed
acceptance is generated for all vertical slices together at the final
Development snapshot; it is not synthesized early for one task.
