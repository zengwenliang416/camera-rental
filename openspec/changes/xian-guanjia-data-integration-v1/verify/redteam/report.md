# Domain Report: redteam

## Domain

redteam

## Verdict

green

## Inputs Reviewed

- requirements.md
- acceptance.md
- development/handoff-to-verify.md
- verify/command-results.md
- verify/redteam/cross-tenant-runtime-probe-2026-07-25.json

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
- **Dual-tenant authenticated runtime cross-tenant penetration** executed on
  isolated backend `http://127.0.0.1:48087` with tenants A=`1` (`admin`) and
  B=`121` (`admin110`). Artifact:
  `verify/redteam/cross-tenant-runtime-probe-2026-07-25.json`.
  Totals: **18/18 cases passed**, 0 failed.
  - Own-tenant positive controls: `S1`, `S2`, `S3` (order/raw list success).
  - Header/token tenant mismatch → business `403` 「您无权访问该租户的数据」:
    `C1`, `C2`, `C3`, `C4`.
  - IDOR deny / no foreign-body leak on raw get, replay, push replay, alert
    resolve, convert: `I1`–`I5`; own-tenant raw get positive control `I6`.
  - List isolation with no ID overlap: order IDs `L1`, shop IDs `L2`.
  - Unauthenticated order page denied (`401` 账号未登录): `U1`.
  - Webhook invalid payload paths with no secret leak: `W1` (admin-api and
    public webhook paths).
  - Original process on `:48080` left untouched; probe used `:48087` only.

## Commands Run

- Maven tests and static security/write-client scans recorded in
  verify/command-results.md.
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuReplayAdminServiceTest,XianyuRawPayloadAdminServiceTest,XianyuPushRetryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuPushRetryServiceTest,XianyuProductPushShopResolverTest,XianyuReplayAdminServiceTest,XianyuRawPayloadAdminServiceTest,XianyuAlertAdminServiceTest,XianyuOrderAdminServiceTest,XianyuProductAdminServiceTest,XianyuAfterSaleAdminServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl yudao-framework/yudao-spring-boot-starter-biz-tenant -Dtest=TenantSecurityWebFilterTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Dual-tenant runtime probe against isolated backend `http://127.0.0.1:48087`
  (tenants A=1 admin, B=121 admin110); results in
  `verify/redteam/cross-tenant-runtime-probe-2026-07-25.json` and
  `verify/redteam/probes.jsonl`.

## Findings

- The delivered client remains read-only toward XianGuanJia.
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
- Dual-tenant runtime penetration on 2026-07-25 closed the previous redteam
  blocker: separate authenticated tenants on a running backend, with header
  mismatch 403, IDOR deny without foreign payload leak, list isolation, unauth
  401, and webhook invalid-payload no-secret-leak — all evidenced in
  `cross-tenant-runtime-probe-2026-07-25.json` (18/18 pass).

## Required Fixes

(none)

## Residual Risk

- Evidence is **local dual-tenant authenticated runtime** on a shared local
  MySQL instance (`:48087`), not a multi-cloud / multi-region production
  penetration test.
- XGJ jobs may still fire via a shared Quartz cluster in local env; the probe
  itself was read/IDOR-deny oriented and did not assert cloud job-tenant
  isolation.
- Convert foreign-order path (`I5`) returned generic `系统异常` (code 500)
  rather than a domain not-found code; denial still held with no cross-tenant
  data leak observed, but error shape could be hardened later.

## Follow-up Domain Routing

- Receipt / aggregate re-aggregation may promote redteam from blocked after
  this domain update; remaining V1 partial scope (if any) is outside redteam
  required fixes.
- Optional follow-up: dedicated stale-signature / duplicate-webhook abuse
  suite beyond the invalid-payload no-secret-leak cases already covered.

## Follow-up Fix (2026-07-25)

- Foreign/missing channel-order convert now returns business code `1040001013` (`闲鱼渠道订单不存在`) instead of generic HTTP 500.
- Unit test `XianyuRentalConversionServiceImplTest#shouldNotCreateReviewForAnUnknownChannelOrder` updated; runtime recheck on `:48087` confirmed.
