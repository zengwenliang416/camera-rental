# Domain Report: redteam

## Domain

redteam

## Verdict

blocked

## Inputs Reviewed

- requirements.md
- acceptance.md
- development/handoff-to-verify.md
- verify/command-results.md

## Evidence

- Static scans found no committed real XianGuanJia credential value and no
  outbound third-party write client in the rental integration.
- Unit tests cover selected redaction, safe-error, signature, idempotency, and
  tenant-isolation behavior.
- Runtime authorization-denied scenarios, raw-payload access-log inspection,
  and a signed product-push consumption fixture have evidence. Replay and
  raw-payload detail now explicitly pre-check current `tenant_id + id` before
  processing, with focused regression coverage.
- Browser-visible privacy red-team coverage now includes six rental admin
  pages across zh-CN/en and light/dark states on admin `5176` / backend
  `48086`: no visible mainland mobile-number pattern or 10+ continuous digit
  pattern remained after the report external product/SKU ID masking fix.
- Framework-level tenant security coverage now includes an authenticated tenant
  mismatch regression: an admin user from tenant `1001` attempted to access
  request tenant `2002`; `TenantSecurityWebFilter` returned business code
  `403` and did not call the downstream filter chain.

## Commands Run

- Maven tests and static security/write-client scans recorded in
  verify/command-results.md.

## Findings

- The delivered client remains read-only toward XianGuanJia.
- Internal admin write endpoints exist and require broader runtime permission
  and cross-tenant testing.
- Separately permissioned, audited raw-payload access is implemented, and
  manual order-push event replay, order-detail raw-payload replay, and
  order-page raw-payload replay are implemented for durable local evidence.
  Product-push replay is implemented; raw detail access-log auditing has DB
  evidence. The replay/raw detail entrypoints now use explicit current-tenant
  lookup before accepting an ID.
- Focused replay/raw regression passed on 2026-07-25:
  `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuReplayAdminServiceTest,XianyuRawPayloadAdminServiceTest,XianyuPushRetryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
  ran 17 tests with 0 failures.
- Focused tenant-sensitive ID regression passed on 2026-07-25:
  `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuPushRetryServiceTest,XianyuProductPushShopResolverTest,XianyuReplayAdminServiceTest,XianyuRawPayloadAdminServiceTest,XianyuAlertAdminServiceTest,XianyuOrderAdminServiceTest,XianyuProductAdminServiceTest,XianyuAfterSaleAdminServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
  ran 35 tests with 0 failures. It covers push retry, product-push shop
  fallback, replay, raw-payload detail, alert resolve, and manual
  order/product/after-sale sync entrypoints.
- Framework-level tenant mismatch regression passed on 2026-07-25:
  `mvn -pl yudao-framework/yudao-spring-boot-starter-biz-tenant -Dtest=TenantSecurityWebFilterTest -Dsurefire.failIfNoSpecifiedTests=false test`
  ran 1 test with 0 failures. It covers the authenticated cross-tenant request
  guard before service or mapper code can execute.
- Browser-visible raw identifier leakage in the report product/SKU table was
  found and fixed by routing external product/SKU IDs through
  `maskChannelIdentifier`.

## Required Fixes

- Execute production-style runtime cross-tenant penetration probes with
  separate authenticated tenants.
- Execute unauthorized, duplicate webhook, stale-signature, replay, and
  sensitive-error runtime scenarios.

## Residual Risk

- Focused service tests and the framework security filter test now cover the
  sensitive ID entrypoints plus authenticated tenant mismatch guard, but they
  still do not replace a running-system cross-tenant penetration pass with
  separate authenticated tenants.

## Follow-up Domain Routing

- Re-run redteam after missing controls and runtime scenarios are available.
