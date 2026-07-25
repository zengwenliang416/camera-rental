# Domain Report: unit

## Domain

unit

## Verdict

green

## Inputs Reviewed

- requirements.md
- acceptance.md
- development/handoff-to-verify.md
- verify/command-results.md

## Evidence

- System-executed full 21-project Maven Reactor for
  yudao-module-rental-biz and its dependencies.
- Rental module result: 109 tests, 0 failures, 0 errors, 0 skipped.
- Focused XianGuanJia and tenant-isolation regression tests.
- Focused device assignment and assign-request validation regression passed on
  2026-07-25: 11 tests, 0 failures, 0 errors, 0 skipped.
- Focused guarantee-health parser/shop-sync/alert regression passed on
  2026-07-25: 12 tests, 0 failures, 0 errors, 0 skipped.
- Focused order-page persistence/replay regression passed on 2026-07-25:
  17 tests, 0 failures, 0 errors, 0 skipped.
- Focused product-push and product-detail persistence regression passed on
  2026-07-25: 20 tests, 0 failures, 0 errors, 0 skipped.
- Focused product list/SKU orchestration regression passed on 2026-07-25:
  34 tests, 0 failures, 0 errors, 0 skipped.
- Focused express-company raw evidence regression passed on 2026-07-25:
  1 test, 0 failures, 0 errors, 0 skipped.
- Focused after-sale list/detail orchestration regression passed on 2026-07-25:
  22 tests, 0 failures, 0 errors, 0 skipped.
- Admin V1 loading/error/retry/i18n state changes passed `pnpm ts:check` on
  2026-07-25.

## Commands Run

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am test`
- Focused XianGuanJia regression test selection.
- Focused XianyuPropertiesTest and
  XianyuScheduledJobTenantIsolationTest selection.
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=RentalDeviceAssignReqVOTest,RentalDeviceAssignmentServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuAuthorizedShopListParserTest,XianyuShopAdminServiceTest,XianyuAlertAdminServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuOrderSyncServiceTest,XianyuReplayAdminServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuProductPushPayloadParserTest,XianyuProductDetailPayloadParserTest,XianyuProductPersistenceServiceTest,XianyuProductWebhookPersistenceServiceTest,XianyuProductPushConsumerTest,XianyuPushRetryServiceTest,XianyuOrderWebhookControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuProductSyncServiceTest,XianyuChannelSyncServiceTest,XianyuPropertiesTest,XianyuProductListPageParserTest,XianyuProductSkuPayloadParserTest,XianyuProductSkuPersistenceServiceTest,XianyuReadClientTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuExpressCompanyAdminServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuAfterSaleAdminServiceTest,XianyuChannelSyncServiceTest,XianyuPropertiesTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `pnpm ts:check` in `camera-rental-admin`

## Findings

- The implemented rental and XianGuanJia backend slices pass their current
  automated unit suite.

## Required Fixes

- No required fixes remain for this domain.

## Residual Risk

- Real MySQL concurrency and migration behavior are outside this unit domain
  and remain blocked in facticity/E2E.

## Follow-up Domain Routing

- Keep the aggregate receipt partial until all required domains are green.
