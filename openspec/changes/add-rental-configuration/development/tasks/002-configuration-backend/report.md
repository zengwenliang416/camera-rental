# Task Report: 002-configuration-backend

## Status

DONE

## Files Changed

- `camera-rental-server/sql/mysql/migrations/20260831_053_rental_configuration_backend.sql`
- `camera-rental-server/sql/mysql/seeds/20260831_rental_configuration_skipped_items.sql`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-api/src/main/java/cn/iocoder/yudao/module/rental/enums/ErrorCodeConstants.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/controller/admin/rental/configuration/**`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/dal/{dataobject,mysql}/rental/**`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/{configuration,device}/**`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/{controller,service}/**`
- `openspec/changes/add-rental-configuration/development/migrations/**`

## What Changed

- Added a complete configuration catalog read model that includes disabled
  categories and models plus their optimistic `lockVersion`.
- Added category/model create, edit and status operations. Every update uses an
  `id + tenant_id + lock_version` conditional write and returns a stable
  configuration conflict error when no row is updated.
- Added exact channel product rules keyed by current tenant, internal shop ID
  and Xianyu item ID. Single-model rules store one enabled device model;
  multi-model rules accept only synchronized SKU rows belonging to that exact
  product and store their XianGuanJia SKU plus optional Xianyu SKU identifiers.
- Added `CONFIG_SKIPPED` normalization to `mapping_mode=NONE` with no model or
  child mappings. No item, SKU, title, remark or cross-shop fallback was added.
- Added bounded impact preview counts for orders without an internal order,
  mutable internal orders, protected assigned orders and review-required rows.
- Added fourteen standalone `/admin-api/rental/configuration/**` endpoints,
  each guarded by `rental:configuration:query` or
  `rental:configuration:update`.
- Added migration 053 for catalog optimistic versions and a separately
  controlled, tenant-scoped seed for the approved 29 skipped items. The seed
  requires a fixed confirmation sentinel and exactly one active, unexpired
  `VALID` `小疆` shop plus one active, unexpired `VALID` `发发` shop before
  any insert.

## TDD Evidence

- Frozen catalog tests require tenant-scoped conditional optimistic updates and
  prove stale versions do not fall back to `updateById`.
- Product-rule tests cover exact shop/item lookup, single-model save,
  synchronized multi-SKU ownership, cross-tenant shop/application rejection,
  expired authorization, no identifier fallback, `CONFIG_SKIPPED` normalization
  without a placeholder mapping mode, stable update-collision errors, bounded
  impact output, and parent version success before child replacement.
- A real Spring/MyBatis/H2 transaction test forces the replacement child insert
  to fail after the parent update and old-child deletion, then proves the
  parent fields/version and both previous child mappings were rolled back.
- Catalog tests cover the concurrent model-code collision path where the
  device-number prefix is unchanged, preventing a model-code conflict from
  being misreported as a prefix conflict.
- Controller tests inspect every endpoint and require the standalone route plus
  the correct query/update permission expression.
- Migration tests require both `lock_version` columns and exactly 29 quoted
  approved item identifiers.
- Focused Task 002 suite: 37 tests, 0 failures, 0 errors, 0 skipped.
- Full rental-biz suite: 561 tests, 0 failures, 0 errors, 6 existing
  environment-gated MySQL concurrency tests skipped.
- Disposable MySQL 8.4.10 fixtures passed migration 052 regression and all 053
  catalog-lock, seed success, zero/duplicate/expired-authorization rejection,
  atomicity, rollback and cleanup cases.

## Verification Commands

- `openspec/changes/add-rental-configuration/development/migrations/verify-20260831_052-disposable-mysql.sh`
- `openspec/changes/add-rental-configuration/development/migrations/verify-20260831_053-disposable-mysql.sh`
- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=RentalDeviceCatalogServiceTest,RentalChannelProductRuleServiceTest,RentalChannelProductRuleTransactionIntegrationTest,RentalConfigurationControllerTest,RentalConfigurationBackendMigrationTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=/Volumes/zwl/maven-repository test`
- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -pl yudao-module-rental/yudao-module-rental-biz -am -DskipTests -Dmaven.repo.local=/Volumes/zwl/maven-repository install`
- `cd camera-rental-server/yudao-module-rental/yudao-module-rental-biz && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -Dmaven.repo.local=/Volumes/zwl/maven-repository test`
- `openspec validate add-rental-configuration --strict`
- `git diff --check`
- `jq empty` for current change and SpecNav JSON artifacts
- `sh -n` for all development migration fixture scripts
- `cmp -s` and `shasum -a 256` for production/development SQL copies

## Concerns

- Six unrelated MySQL concurrency tests remain skipped behind their existing
  environment gate; the task's disposable database coverage ran separately.
- Task 003 still owns order reconciliation and automatic model application.
  This task returns impact counts but intentionally does not mutate historical
  orders.
- This backend slice contributes to A1 and A4 but does not independently claim
  either complete assertion. Task 006 owns complete A1 verification after the
  admin page and remark-template UI exist; Task 003 owns complete A4
  verification after the order-side exact matcher exists.

## Scope Deviations

- None recorded.

## Follow-up Needed

- Task 003 must consume these exact rules in the centralized reconciliation
  flow without adding product- or text-based fallback.
- Task 006 must consume the standalone APIs and finish A1, including remark
  templates and page state coverage.

## Adjudication

The independent spec review's authorization-expiry finding was fixed by making
the controlled seed use the same current-authorization boundary as the runtime
validator and by adding a zero-write expired-authorization MySQL case. The
implementation remains within Task 002 allowed files, does not call a
third-party write API, and has not been applied to production or the 80 server.
The controlled seed is prepared and tested only; deployment, tenant selection
and production execution still require separate approval and backup evidence.

The independent quality review's five findings were also addressed: update
collisions now return stable domain errors before child replacement; model-code
and prefix conflicts are classified without treating the current model as a
duplicate; shop validation requires an application owned by the current
tenant; skipped rules no longer require a meaningless mapping mode; and a real
database transaction test proves parent/child rollback when replacement
insertion fails.
