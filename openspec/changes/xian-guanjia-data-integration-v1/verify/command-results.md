# Current Verification Command Results

Recorded on 2026-07-24 and updated on 2026-07-25 against the current
worktree.

## Passed

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am install -DskipTests`
  - Full 21-project Reactor completed with `BUILD SUCCESS`.
- `mvn -pl yudao-module-rental/yudao-module-rental-biz test`
  - Rental module: 112 tests, 0 failures, 0 errors, 0 skipped.
  - Total time: 9.580 seconds.
- Focused XianGuanJia sync/webhook regression: 18 tests, 0 failures.
- Tenant configuration and scheduled-job isolation regression: 6 tests,
  0 failures.
- `pnpm ts:check` in `camera-rental-admin`.
- `pnpm build:local` in `camera-rental-admin`.
  - Build completed successfully in 42.23 seconds.
  - Vite emitted an existing Lightning CSS minify warning for `*zoom: 1`.
- Targeted ESLint and rental Vue Stylelint checks.
- `bash -n camera-rental-server/scripts/setup-local.sh`.
- `mvn -pl yudao-framework/yudao-spring-boot-starter-web -Dtest=ApiAccessLogSanitizerTest test`
  - API access-log sanitizer: 4 tests, 0 failures, 0 errors, 0 skipped.
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -Dtest=XianyuChannelSyncServiceTest,XianyuOrderSyncServiceTest,XianyuOrderPersistenceServiceImplTest,XianyuSyncCursorAdvancerTest test`
  - Order-window, cursor, idempotent persistence, and fixed-window count-drift
    regression: 24 tests, 0 failures, 0 errors, 0 skipped.
- `mvn -pl yudao-server -am package -DskipTests`
  - Full 23-project packaging completed with `BUILD SUCCESS`; tests skipped by
    command design for jar rebuild.
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuAfterSalePageParserTest,XianyuAfterSaleAdminServiceTest,RentalManualReviewAdminServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Focused after-sale parser/service and manual-review regression: 10 tests,
    0 failures, 0 errors, 0 skipped.
- `pnpm ts:check` in `camera-rental-admin` after after-sale and express-company
  UI/API wiring.
  - TypeScript check completed successfully.
- `mvn -pl yudao-server -am package -DskipTests`
  - Rebuilt the current `yudao-server.jar` with after-sale, express-company,
    and manual-review fixes; 23-project Reactor completed with `BUILD SUCCESS`.
- Production migration and SpecNav audit-copy SHA-256 comparison for
  migrations 001 through 019.
- SpecNav migration manifest and README contract checks.
- Authenticated runtime smoke against the current jar on `48082`, connected to
  the real local XianGuanJia data store with scheduler/infra jobs disabled:
  config `READY`, shops total `7`, orders total `730`, sync-run total `1841`
  on first check and `1861` on repeat check. The sync-run table is live data.
- Authenticated runtime smoke against the rebuilt current jar on `48082`,
  connected to the real local XianGuanJia data store with scheduler/infra jobs
  disabled:
  - config `READY`, app secret configured, masked app key only.
  - shops total `7`, with `4` valid and `3` invalid authorizations.
  - orders total `732`.
  - manual review page returned code `0`, total `1`; this verifies the open
    review with `resolved_by = NULL` no longer returns HTTP 500.
  - express-company read-only query returned `94` companies, `12` hot.
  - after-sale local page was initially empty; one authorized-shop read-only
    sync page received and persisted `20` records with `hasNextPage=true`, then
    after-sale page returned total `20`.
- After access-log sanitization, current jar `48082` logged
  `/system/auth/login` request parameters as
  `{"username":"admin","captchaVerification":""}` and
  `/system/auth/refresh-token` request parameters as `{}`; the submitted
  password and test refresh token were not printed.
- Runtime comparison showed `48080` has real XianGuanJia data but is an old
  process missing `/admin-api/rental/xianyu/sync-run/page`; `48081` has current
  code but only isolated verification data.
- Admin dev server `5174` was restarted with
  `VITE_BASE_URL=http://127.0.0.1:48082`, so local browser traffic now targets
  the current jar instead of the old `48080` process.
- Disposable `mysql:8.4` verification:
  - Imported the base and Quartz schemas.
  - Applied migrations 001 through 017 successfully.
  - Verified checksum-based second-run skips for all 17 migrations.
  - Verified required token/index schema and one-row webhook dedupe behavior.
- `20260725_018_rental_menu_role_grants.sql` was applied twice against
  `camera_rental_verify` in the local MySQL 8.4 verification container.
  - Both executions succeeded, proving the migration is rerunnable.
  - Verified menu ids `7040`, `7041`, `7050`, `7051`, and `7060` exist with
    the expected schedule, sync-run, and report paths/components.
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=RentalDeviceAssignReqVOTest,RentalDeviceAssignmentServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Focused device assignment and admin assign-request validation regression:
    11 tests, 0 failures, 0 errors, 0 skipped.
- Authenticated browser E2E using a temporary Chrome profile against admin
  `5174` and backend `48082`:
  - `/rental/xianyu`: rendered config/shop/after-sale/express sections; all
    observed rental API calls returned HTTP 200.
  - `/rental/review`: rendered the open manual-review row without HTTP 500.
  - `/rental/sync-run`: initially returned 404 because the current DB lacked
    the dynamic menu; after runtime menu repair matching migration 018, rendered
    and called `/admin-api/rental/xianyu/sync-run/page` with HTTP 200.
  - `/rental/report`: initially returned 404 for the same menu drift; after
    repair, rendered and called overview/product/device report APIs with HTTP
    200.
  - Screenshots were saved under `verify/e2e/artifacts/`; Xianyu table bodies
    were blurred in retained screenshots to avoid persisting external order or
    after-sale identifiers.
- Runtime permission-denied API verification against backend `48082`:
  - Created a temporary role and temporary user through official admin APIs.
  - The temporary user could login and call `/system/auth/get-permission-info`
    with code `0`.
  - The same user had no rental menu grants and calling
    `/admin-api/rental/report/overview` returned business code `403`.
  - The script deleted the temporary user and role afterward; no token, secret,
    order id, after-sale id, or PII was printed.
- `pnpm ts:check` in `camera-rental-admin` after adding default channel-ID
  masking.
  - Management order, Xianyu shop authorization, and after-sale list views now
    render external order/shop/authorization/after-sale identifiers through
    `maskChannelIdentifier` instead of showing full channel identifiers by
    default.
  - TypeScript check completed successfully.
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuOrderAdminServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Focused admin order privacy regression: 2 tests, 0 failures, 0 errors,
    0 skipped.
  - Ordinary admin order pagination now redacts `sellerRemark` before returning
    the response VO, including phone/address/name/long identifier patterns.
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuOrderAdminServiceTest,XianyuShopAdminServiceTest,XianyuAfterSaleAdminServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Focused admin list privacy regression: 6 tests, 0 failures, 0 errors,
    0 skipped.
  - Ordinary order, shop authorization, and after-sale pagination responses now
    mask external channel identifiers at the backend VO boundary.
- `pnpm ts:check` in `camera-rental-admin` after adding front-end defensive
  seller-remark masking.
  - TypeScript check completed successfully.
- `mvn -pl yudao-server -am package -DskipTests`
  - Rebuilt the current `yudao-server.jar` after backend VO masking changes;
    23-project Reactor completed with `BUILD SUCCESS`; tests skipped by command
    design for runtime jar rebuild.
- Runtime API masking verification against current rebuilt jar on `48083`:
  - Started a foreground verification backend on `48083`, connected to the real
    local XianGuanJia data store.
  - Quartz reported `NOT STARTED`, so scheduled jobs were not executed during
    the probe.
  - Authenticated as the admin user without printing the token.
  - `/admin-api/rental/xianyu/order/page` returned business code `0` with
    10 rows; sampled `sellerRemark` values contained no mainland mobile-number
    pattern and no 10+ digit identifier pattern, and sampled external order ids
    were masked.
  - `/admin-api/rental/xianyu/shop/page` returned business code `0` with
    7 rows; sampled external shop and authorization ids were masked.
  - `/admin-api/rental/xianyu/after-sale/page` returned business code `0` with
    10 rows; sampled external after-sale and order ids were masked.
  - The probe printed only counts and boolean checks; no token, external order
    id, after-sale id, shop id, phone number, address, or seller remark was
    printed.
- Final safety checks after the masking verification-record update:
  - `python3 -m json.tool` succeeded for `verify/runtime-evidence.json`,
    `verify/receipt.json`, and `verify/e2e/report.json`.
  - `git diff --check` completed with no whitespace errors.
  - A credential-literal scan found no XianGuanJia test AppKey/AppSecret values
    outside ignored local env and build directories.
  - `pnpm ts:check` in `camera-rental-admin` completed successfully.
- Final safety checks after the `48083` API runtime masking probe:
  - `python3 -m json.tool` succeeded for `verify/runtime-evidence.json`,
    `verify/receipt.json`, and `verify/e2e/report.json`.
  - `git diff --check` completed with no whitespace errors.
  - A credential-literal scan found no XianGuanJia test AppKey/AppSecret values
    outside ignored local env and build directories.
  - The temporary foreground backend on `48083` was stopped after the probe;
    port `48083` was no longer listening.
- Local XianGuanJia infra-job config alias verification:
  - `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuPropertiesTest -Dsurefire.failIfNoSpecifiedTests=false test`
    passed: 4 tests, 0 failures, 0 errors, 0 skipped.
  - `bash -n camera-rental-server/scripts/start-local.sh` completed with no
    syntax errors.
  - `mvn -pl yudao-server -am package -DskipTests` rebuilt the current jar
    after the local config alias fix; 23-project Reactor completed with
    `BUILD SUCCESS`; tests skipped by command design for jar rebuild.
  - A temporary backend was started on `48084` with only
    `XGJ_JOB_REGISTER_INFRA_JOBS=false` for the infra-job registration toggle
    plus scheduler/startup sync disabled. The app reached started state,
    listened on `48084`, emitted zero `[xianyu][job-register]` log lines, then
    was stopped and `48084` was no longer listening.
- Authenticated browser masking verification against the current rebuilt jar:
  - Started backend `48085` from the current jar with XianGuanJia scheduler,
    startup sync, and infra-job registration disabled.
  - Started admin `5175` with `VITE_BASE_URL=http://127.0.0.1:48085`.
  - Launched an isolated headless Chrome CDP session on `9226`.
  - Logged in through `/admin-api/system/auth/login` without printing the token,
    injected only browser localStorage auth state, and visited `/rental/order`
    and `/rental/xianyu`.
  - `/rental/order` stayed on the target route, observed 4 rental API HTTP 200
    responses, visible table text had no mainland mobile-number pattern and no
    10+ digit identifier pattern, and displayed masked markers.
  - `/rental/xianyu` stayed on the target route, observed 6 rental API HTTP 200
    responses, visible table text had no mainland mobile-number pattern and no
    10+ digit identifier pattern, and displayed masked markers.
  - Blurred screenshots were saved under `verify/e2e/artifacts/`:
    `2026-07-24T17-49-42-075Z-rental-order-masked.png` and
    `2026-07-24T17-49-42-075Z-rental-xianyu-masked.png`.
  - Temporary backend, admin, and Chrome sessions were stopped; ports `48085`,
    `5175`, and `9226` were no longer listening.
- Final safety checks after the infra-job config-alias verification-record
  update:
  - `python3 -m json.tool` succeeded for `verify/runtime-evidence.json`,
    `verify/receipt.json`, and `verify/e2e/report.json`.
  - `git diff --check` completed with no whitespace errors.
  - A credential-literal scan found no XianGuanJia test AppKey/AppSecret values
    outside ignored local env and build directories.
- Final safety checks after the browser masking verification-record update:
  - `python3 -m json.tool` succeeded for `verify/runtime-evidence.json`,
    `verify/receipt.json`, and `verify/e2e/report.json`.
  - `git diff --check` completed with no whitespace errors.
  - A credential-literal scan found no XianGuanJia test AppKey/AppSecret values
    outside ignored local env and build directories.
- Real MySQL concurrent device allocation and schedule query-plan verification
  against `camera-rental-mysql-runtime-v17` / `camera_rental_verify`:
  - Used isolated tenant `990025` and deterministic `cg-v1-20260725` fixture
    prefixes; no XianGuanJia credentials or third-party writes were involved.
  - Two PyMySQL worker threads attempted overlapping assignment for the same
    device and occupied period. Worker A assigned successfully; Worker B waited
    `1356.9 ms` on the same device row lock, then saw one overlap and returned
    `schedule-conflict`.
  - Final counts for that device were exactly one effective overlap schedule and
    one assigned row.
  - Expanded the verification tenant to `30001` `rental_schedule` rows.
  - `EXPLAIN ANALYZE` for the assignment overlap query used
    `idx_rental_schedule_device_range`.
  - `EXPLAIN ANALYZE` for the default schedule page query used
    `idx_rental_schedule_admin_default`.
  - Detailed evidence is recorded in `verify/mysql-concurrency-report.md`.
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuProductPushPayloadParserTest,XianyuProductDetailPayloadParserTest,XianyuProductPersistenceServiceTest,XianyuProductWebhookPersistenceServiceTest,XianyuProductPushConsumerTest,XianyuPushRetryServiceTest,XianyuOrderWebhookControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Focused product-push, product-detail persistence, webhook controller, and
    push replay regression: 20 tests, 0 failures, 0 errors, 0 skipped.
  - Verified `/xianyu/webhooks/product` dispatch, documented product-push
    field validation, redacted `PRODUCT_PUSH` raw payload persistence, safe
    unmapped-shop handling, product-push manual replay, read-only
    `/api/open/product/detail` consumer wiring, and old product detail snapshots
    not overwriting newer normalized product rows.
- Permission-denied browser click-through against admin `5174` and backend
  `48082`:
  - Created a temporary role and temporary user with no rental menu grants
    through official admin APIs.
  - Logged in as the temporary user through the backend login API without
    printing the token.
  - Injected only the temporary browser auth state into an isolated headless
    Chrome CDP profile and navigated directly to `/rental/report`.
  - Verified no rental menu, no rental permissions, denied/not-found route
    rendering, no rental content, and no rental sidebar entry.
  - Deleted the temporary user and role after verification.
  - Screenshot and sanitized summary were saved as
    `verify/e2e/artifacts/2026-07-24T18-10-49-302Z-rental-permission-denied.png`
    and
    `verify/e2e/artifacts/2026-07-24T18-10-49-302Z-rental-permission-denied.json`.
- Deduplicated authorization-loss alert implementation:
  - Added backend alert pagination and resolve APIs over the existing
    `xianyu_alert` table.
  - Authorized-shop sync now records one deduplicated alert when a remote shop
    is invalid or an existing authorization disappears from a successful
    snapshot.
  - Admin Xianyu page now shows open alerts with masked source/message fields
    and supports resolving them for users with `rental:xianyu:sync`.
  - `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuAlertAdminServiceTest,XianyuShopAdminServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
    passed: 5 tests, 0 failures, 0 errors, 0 skipped.
  - `pnpm ts:check` in `camera-rental-admin` completed successfully after the
    alert UI/API wiring.
- Deduplicated order/after-sale page sync-failure alert implementation:
  - Order page sync and after-sale page sync now refresh a `SYNC_FAILED`
    operational alert after writing a failed sync-run outcome.
  - The dedupe key is resource/shop/safe-error-code scoped, preventing repeated
    failures from creating duplicate rows while still preserving latest seen
    time and message.
  - `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuAlertAdminServiceTest,XianyuOrderSyncServiceTest,XianyuAfterSaleAdminServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
    passed: 15 tests, 0 failures, 0 errors, 0 skipped.
- Manual order-push event replay implementation:
  - Backend replay endpoint requires `rental:xianyu:replay`, requeues only
    durable local order-push events, skips succeeded/processing events, masks
    response messages, and does not advance page cursors or perform third-party
    writes.
  - Admin Xianyu page exposes the replay form only when the user has
    `rental:xianyu:replay`.
  - `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuPushRetryServiceTest,XianyuPushEventStateServiceTest,XianyuReplayAdminServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
    passed: 14 tests, 0 failures, 0 errors, 0 skipped.
  - `pnpm ts:check` in `camera-rental-admin` completed successfully after the
    replay UI/API/i18n wiring.
- Order-detail raw-payload replay implementation:
  - Backend replay endpoint `/admin-api/rental/xianyu/replay/raw-payload`
    requires `rental:xianyu:replay`, accepts only local `ORDER_DETAIL` raw
    payloads, rejects unsupported source types, reuses the existing
    order-detail persistence path, returns safe failure codes for persistence
    errors, does not advance sync cursors, and does not perform third-party
    writes.
  - Admin Xianyu page now exposes a separate raw payload ID replay action under
    the existing safe-replay permission.
  - `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuReplayAdminServiceTest,XianyuPushRetryServiceTest,XianyuPushEventStateServiceTest,XianyuOrderPersistenceServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
    passed: 22 tests, 0 failures, 0 errors, 0 skipped.
  - `pnpm ts:check` in `camera-rental-admin` completed successfully after the
    raw-payload replay UI/API/i18n wiring.
- Final safety checks after the permission-denied browser evidence update:
  - `python3 -m json.tool` succeeded for `verify/runtime-evidence.json`,
    `verify/receipt.json`, `verify/e2e/report.json`, and the new
    permission-denied browser artifact JSON.
  - `git diff --check` completed with no whitespace errors.
- Final safety checks after the manual replay and migration-019 evidence
  update:
  - All production migrations `001` through `019` matched their OpenSpec audit
    copies with `cmp`.
  - SHA-256 for both production and audit copies of
    `20260725_019_rental_xianyu_raw_and_replay_permissions.sql` was
    `2e316af1c778c3bffe0169155db56d3e5e888844cb565f40feb64558062aafec`.
  - `python3 -m json.tool` succeeded for `verify/runtime-evidence.json`,
    `verify/receipt.json`, `verify/e2e/report.json`,
    `verify/redteam/report.json`, and `development/migrations/manifest.json`.
  - `git diff --check` completed with no whitespace errors.
  - A credential-literal scan found no XianGuanJia test AppKey/AppSecret values
    outside ignored local env and build directories.
  - A credential-literal scan found no XianGuanJia test AppKey/AppSecret values
    outside ignored local env and build directories.
  - Temporary Chrome CDP ports `9231`, `9232`, and `9233` were not listening.
- Final safety checks after the MySQL concurrency and query-plan verification
  record update:
  - `python3 -m json.tool` succeeded for `verify/runtime-evidence.json`,
    `verify/receipt.json`, and `verify/e2e/report.json`.
  - `git diff --check` completed with no whitespace errors.
  - A credential-literal scan found no XianGuanJia test AppKey/AppSecret values
    outside ignored local env and build directories.
- Restricted raw-payload access implementation:
  - Added separately permissioned backend endpoints for raw-payload metadata and
    masked detail: `/admin-api/rental/xianyu/raw-payload/page` and
    `/admin-api/rental/xianyu/raw-payload/get`.
  - Detail access is annotated for API access-log auditing with response bodies
    disabled, and the service re-redacts JSON keys and values before returning
    any payload content.
  - Admin Xianyu page now has a `rental:xianyu:raw` gated table and masked
    detail dialog, with `zh-CN`/`en` copy.
  - Added migration `20260725_009_rental_xianyu_raw_permission.sql` for the
    separate `rental:xianyu:raw` button permission.
  - First focused backend test run exposed that sensitive JSON key names such as
    `AppSecret` were still visible after value masking; implementation was
    tightened to rename sensitive keys to `redacted_N`.
  - `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuRawPayloadAdminServiceTest,XianyuAlertAdminServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
    passed: 7 tests, 0 failures, 0 errors, 0 skipped.
  - `pnpm ts:check` in `camera-rental-admin` completed successfully after the
    raw-payload UI/API wiring.
- Guarantee-health alert implementation:
  - Re-read the online XianGuanJia `llms.txt` and opened the current
    `POST /api/open/user/authorize/list` Markdown document before editing.
  - Added conservative parsing for documented `is_deposit_enough` values:
    missing/unknown values remain `UNKNOWN`, `true` becomes `HEALTHY`, and
    `false` becomes `DEPOSIT_INSUFFICIENT`.
  - Authorized-shop sync now persists `xianyu_shop.guarantee_status` from the
    remote snapshot and records one deduplicated `GUARANTEE_HEALTH` alert for
    insufficient service deposit.
  - No real XianGuanJia API call and no third-party write operation was
    executed for this implementation.
  - `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuAuthorizedShopListParserTest,XianyuShopAdminServiceTest,XianyuAlertAdminServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
    passed: 12 tests, 0 failures, 0 errors, 0 skipped.
- Order-page raw-payload replay implementation:
  - Re-read the online XianGuanJia `llms.txt` and opened the current
    `POST /api/open/order/list` and `POST /api/open/order/detail` Markdown
    documents before editing.
  - Order list sync now persists local `ORDER_PAGE` raw payload evidence before
    page metadata parsing, using the existing restricted raw-payload table and
    hash-based idempotency.
  - Backend raw-payload replay now accepts local `ORDER_PAGE` records under the
    existing `rental:xianyu:replay` permission, reuses the existing order-page
    parser and detail-refresh logic, creates a `REPLAY` sync run, preserves
    partial success counts when later detail refresh fails, and never calls
    `advanceOrderCursor`.
  - This implementation may perform documented read-only order-detail requests
    for missing or stale orders during replay; it does not perform any
    XianGuanJia write operation and does not advance page cursors.
  - `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuOrderSyncServiceTest,XianyuReplayAdminServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
    first failed because the refactored detail-refresh helper lost partial
    success counts on a later detail failure; this was fixed by carrying the
    partial count through the failure path.
  - Re-run command:
    `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuOrderSyncServiceTest,XianyuReplayAdminServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
    passed: 17 tests, 0 failures, 0 errors, 0 skipped.
- Combined replay/guarantee regression:
  - `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuOrderSyncServiceTest,XianyuReplayAdminServiceTest,XianyuAuthorizedShopListParserTest,XianyuShopAdminServiceTest,XianyuAlertAdminServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
    passed: 29 tests, 0 failures, 0 errors, 0 skipped.
  - Touched-file trailing-whitespace check completed with no output.
  - A credential-literal scan found no XianGuanJia test AppKey/AppSecret values
    outside ignored local env and build directories.
- Product list/SKU page orchestration implementation:
  - Re-read the online XianGuanJia `llms.txt` and opened the current
    `POST /api/open/product/list` and `POST /api/open/product/sku/list`
    Markdown documents before editing.
  - Product incremental sync now runs through `xianyuProductSyncJob`, optional
    Spring fallback, startup sync, and infra Job registration, using
    `XGJ_JOB_PRODUCT_CRON` for runtime configuration.
  - Product sync persists `PRODUCT_PAGE` raw evidence before parsing,
    refreshes missing/stale product details through read-only
    `/api/open/product/detail`, chunks multi-spec SKU reads at 100
    `product_id` values, and advances a separate `PRODUCT` cursor only after
    all fixed-window pages succeed.
  - The read client now supports the documented optional `seller_id` query
    parameter for product reads; no XianGuanJia write operation was added.
  - `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuProductSyncServiceTest,XianyuChannelSyncServiceTest,XianyuPropertiesTest,XianyuProductListPageParserTest,XianyuProductSkuPayloadParserTest,XianyuProductSkuPersistenceServiceTest,XianyuReadClientTest -Dsurefire.failIfNoSpecifiedTests=false test`
    passed: 34 tests, 0 failures, 0 errors, 0 skipped.
- Express-company raw evidence implementation:
  - Re-read the online XianGuanJia `llms.txt` and opened the current
    `POST /api/open/express/companies` Markdown document before editing.
  - The existing admin express-company lookup now preserves the full read-only
    response as `EXPRESS_COMPANIES` in `xianyu_raw_payload` before returning
    the parsed list. No XianGuanJia write operation was added.
  - `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuExpressCompanyAdminServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
    passed: 1 test, 0 failures, 0 errors, 0 skipped.
- After-sale list/detail orchestration implementation:
  - Re-read the online XianGuanJia `llms.txt` and opened the current
    `POST /api/open/trade/refund/list` and
    `POST /api/open/trade/refund/detail` Markdown documents before editing.
  - After-sale manual sync now stores `AFTER_SALE_PAGE`,
    `AFTER_SALE_LIST_ITEM`, and `AFTER_SALE_DETAIL` raw evidence, and updates
    the normalized row from the detail response.
  - Scheduled orchestration now runs through `xianyuAfterSaleSyncJob`, optional
    Spring fallback, startup sync, and infra Job registration, using
    `XGJ_JOB_AFTER_SALE_CRON` for runtime configuration.
  - Incremental after-sale sync traverses VALID unexpired shops, queries both
    `apply_time` and `refund_time` windows, and advances a separate
    `AFTER_SALE` cursor only after all configured pages succeed. If
    `has_next_page` remains true after `max-pages-per-shop`, the window fails
    without cursor advancement.
  - The first focused Maven run failed at test compile because
    `XianyuAfterSaleSyncReqVO` was missing from the channel-sync test imports;
    the production module had already compiled. The import was fixed and the
    same focused suite was rerun.
  - `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuAfterSaleAdminServiceTest,XianyuChannelSyncServiceTest,XianyuPropertiesTest -Dsurefire.failIfNoSpecifiedTests=false test`
    passed: 22 tests, 0 failures, 0 errors, 0 skipped.
- Admin loading/error/retry/i18n state completion:
  - Device, order, schedule, sync-run, and Xianyu operation pages now expose
    page-level network/permission load-error alerts with retry actions, matching
    the already-covered manual-review and report pages.
  - The new generic loading-error copy is present in both `zh-CN` and `en`
    locale files under `rental.common.loadError`.
  - `pnpm ts:check` in `camera-rental-admin` passed after the front-end state
    changes.
- Runtime migration 020 and focused XianGuanJia API smoke:
  - Applied
    `camera-rental-server/sql/mysql/migrations/20260725_020_xianyu_raw_payload_version_width.sql`
    to the active local `ruoyi-vue-pro` MySQL database used by backend `48082`.
  - Verified `xianyu_raw_payload.schema_version` and
    `xianyu_raw_payload.redaction_version` are `varchar(64)`, and
    `xianyu_push_event.processing_token` exists as `varchar(64)`.
  - Production migration and SpecNav audit-copy SHA-256 both matched
    `b5cf0cc4139161c50dda0a6dbd43fde07147bcd6fc921c128e3debb4f43990b1`.
  - Authenticated API smoke against current backend `48082` returned code `0`
    for config, shop page, order page, after-sale page, express-company list,
    alert page, raw-payload page, manual-review page, sync-run page, and report
    overview with required dates.
  - Counts observed without printing tokens, raw payloads, external IDs, seller
    remarks, or PII: shops `7`, orders `733`, after-sales `20`,
    express companies `94`, raw payloads `815`, manual reviews `1`, sync-runs
    `2167`, report overview order count `702` for the current month.
  - No-token raw-payload page returned business code `401`; invalid raw-payload
    replay returned safe business code `1040001009`; invalid push-event replay
    returned safe business code `1040001010`. The previous express-company and
    invalid push replay HTTP 500 failures were not reproduced.
  - A bare `/rental/report/overview` request without `startDate/endDate`
    returned business code `400` from request validation; the page-shaped
    request with required dates returned business code `0`.
  - Final checks after the migration 020/runtime-smoke verification-record
    update:
    - `python3 -m json.tool` succeeded for the migration manifest,
      `verify/runtime-evidence.json`, `verify/receipt.json`, and
      `verify/e2e/report.json`.
    - `git diff --check` completed with no whitespace errors.
    - `pnpm ts:check` in `camera-rental-admin` completed successfully.
    - A non-printing credential-literal scan found zero test AppKey/AppSecret
      literals in non-ignored files.
- Runtime migration 021, raw/replay permission smoke, and product-job probe:
  - Before migration 021, the active local database had `xianyuShopSyncJob`,
    `xianyuOrderSyncJob`, and `xianyuPushRetryJob`, but was missing
    `xianyuProductSyncJob` and `xianyuAfterSaleSyncJob`; product tables and
    product raw evidence counts were all `0`.
  - Added and applied
    `camera-rental-server/sql/mysql/migrations/20260725_021_xianyu_product_after_sale_jobs.sql`
    twice to the active local `ruoyi-vue-pro` database. The migration is
    repeatable: exactly two active rows exist for `xianyuProductSyncJob` and
    `xianyuAfterSaleSyncJob`, both status `1`, cron `0 0/10 * * * ?`.
  - Production migration and SpecNav audit-copy SHA-256 both matched
    `eb7689e1b3589f134dd9b81bc853e3acc7b1a5f884c2b6a6b5c426d632d86c9f`.
  - `/admin-api/infra/job/sync` returned business code `0` and backend logs
    showed `xianyuProductSyncJob` and `xianyuAfterSaleSyncJob` synced into
    Quartz.
  - `/admin-api/infra/job/trigger` for `xianyuProductSyncJob` returned business
    code `0`, and the product job became visible in `/infra/job/page`.
  - Product runtime smoke did not pass: after repeated polling,
    `infra_job_log` still had `status=0`, `end_time=NULL`, `duration=NULL`, and
    no result for the product job; `xianyu_product`, `xianyu_product_sku`,
    `PRODUCT_PAGE`, `PRODUCT_DETAIL`, `PRODUCT_SKUS`, and `PRODUCT` sync-run
    counts remained `0`. `jcmd Thread.print` showed Quartz worker threads
    waiting and no active Xianyu product/OkHttp stack, so this is recorded as a
    product sync completion gap rather than a green product smoke.
  - Raw-payload access smoke passed for an admin token: raw page returned code
    `0` with total `815`, the sampled source identifier was masked, and raw
    detail returned a `maskedPayload` with no mainland mobile-number pattern
    and no unmasked address-keyword pattern detected by the smoke.
  - Runtime replay/raw permission smoke passed with a temporary role/user that
    had no rental grants: the temporary user could login, raw-payload page
    returned business code `403`, raw replay returned business code `403`, and
    the temporary user and role were deleted through official admin APIs.
- Product manual sync endpoint and real-data smoke:
  - Added a management-only read endpoint
    `POST /admin-api/rental/xianyu/product/sync-page`, guarded by
    `rental:xianyu:sync`, so operators can verify one bounded product page
    without waiting on Quartz.
  - Re-read the live XianGuanJia product docs on 2026-07-25. The product list,
    detail, and SKU endpoints document `seller_id` as business-integration-only;
    the product sync path now omits `seller_id` for the current self-developed /
    third-party ERP integration mode.
  - Focused backend regression passed for manual product sync, product page
    persistence, scheduled product orchestration, and cursor advancement:
    `XianyuProductAdminServiceTest`, `XianyuProductSyncServiceTest`, and
    `XianyuChannelSyncServiceTest` ran 21 tests with 0 failures.
  - Frontend `pnpm ts:check` passed after adding the Xianyu product-sync API
    type and the read-only product-sync form on the existing Xianyu ops page.
  - Rebuilt `yudao-server.jar` and started the corrected jar locally on
    `48080`. Authenticated real-data smoke returned code `0`: config `READY`,
    shops `7`, selected shop valid, product sync received `1`, succeeded `1`,
    deduplicated `0`, SKU count `0`, PRODUCT raw-payload total `1`, and PRODUCT
    sync-run total `1`. The smoke did not print tokens, secrets, external
    product IDs, raw payloads, seller remarks, phone numbers, or addresses.
  - Follow-up quality pass on 2026-07-25 confirmed manual and scheduled product
    sync calls pass `null` for the optional `seller_id` query parameter. The
    misleading backend test name was corrected, and the Xianyu operation page
    now handles cleared product/after-sale date ranges through the existing
    required-field warning instead of throwing in the click handler.
  - Follow-up verification passed:
    `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuProductAdminServiceTest,XianyuProductSyncServiceTest,XianyuChannelSyncServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
    ran 21 tests with 0 failures; `pnpm ts:check` passed in
    `camera-rental-admin`; `git diff --check` passed; a focused literal scan
    found zero occurrences of the provided test AppKey/AppSecret in non-ignored
    source files.
- Scheduled product job real-data completion:
  - Re-read the live XianGuanJia index and product-list docs on 2026-07-25
    before runtime verification. The product list endpoint is still
    `POST /api/open/product/list`; `seller_id` remains documented as
    business-integration-only and ignored for self-developed / third-party ERP
    mode.
  - Authenticated against the local admin API without printing tokens,
    passwords, AppKey, AppSecret, external product IDs, raw payloads, seller
    remarks, phone numbers, or addresses.
  - `/admin-api/infra/job/page` showed `xianyuProductSyncJob` present, status
    `1`, cron `0 0/10 * * * ?`.
  - Before trigger, scheduled PRODUCT sync-run total was `28`;
    `PRODUCT_PAGE`, `PRODUCT_DETAIL`, and `PRODUCT_SKUS` raw-payload totals
    were `21`, `261`, and `12`.
  - `/admin-api/infra/job/trigger` returned code `0` and accepted the
    `xianyuProductSyncJob` trigger.
  - The latest infra job log completed on the first poll with status `1` and
    duration `1246` ms.
  - After trigger, scheduled PRODUCT sync-run total was `32`. The latest
    scheduled PRODUCT run was `SUCCEEDED`, received `3`, succeeded `3`,
    deduplicated `3`, failed `0`.
  - `PRODUCT_PAGE` raw-payload total increased from `21` to `22`;
    `PRODUCT_DETAIL` and `PRODUCT_SKUS` totals remained `261` and `12` because
    all three products were deduplicated against current detail state.
- Order detail backfill hardening:
  - Tightened the missing-detail candidate query to select only rows with
    missing `detail_json` and a non-blank `external_order_id`, ordered by
    `source_updated_at` and local `id` descending so newer changed orders are
    not starved by older abnormal rows.
  - Added a service-layer guard to skip blank external order IDs even if a
    legacy or mocked mapper result includes them, without blocking valid rows
    in the same batch.
  - Focused regression passed:
    `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuOrderSyncServiceTest,XianyuChannelSyncServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
    ran 25 tests with 0 failures.
- Product-push runtime webhook and replay-path hardening:
  - Re-read the live XianGuanJia index and product webhook/detail docs on
    2026-07-25 before touching webhook/replay behavior.
  - Replaced Spring event publication with direct post-commit dispatch through
    `XianyuPushEventPublisher`, and added an outer async-consumer failure guard
    so dispatch/tenant-context failures are recorded with safe error codes
    instead of silently leaving retryable rows unchanged.
  - Fixed nullable push-event fields by setting MyBatis-Plus
    `updateStrategy = ALWAYS` for `processing_token`, `last_error_code`,
    `last_error_message`, and `processed_at`; runtime evidence had shown a
    successful product push could otherwise retain its old processing token.
  - Focused backend regression passed:
    `mvn -pl yudao-framework/yudao-spring-boot-starter-web,yudao-module-rental/yudao-module-rental-biz -am -Dtest=ApiAccessLogSanitizerTest,XianyuOrderWebhookControllerTest,XianyuPushRetryServiceTest,XianyuPushEventPublisherTest,XianyuProductPushConsumerTest,XianyuOrderPushConsumerTest,XianyuPushEventStateServiceTest,XianyuOrderWebhookPersistenceServiceTest,XianyuProductWebhookPersistenceServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
    ran 28 tests with 0 failures.
  - Rebuilt the server jar with `mvn -pl yudao-server -am package -DskipTests`.
  - Runtime fixture against isolated backend `48086` used configured
    credentials without printing them, posted a signed `PRODUCT_PUSH` webhook,
    and observed HTTP 200 / `success`, final event status `SUCCEEDED`,
    `processing_token` cleared, and cleanup of 1 synthetic push-event row plus
    1 synthetic raw-payload row.
  - 48086 startup logs confirmed Quartz stayed in standby and product webhook
    access logs recorded `无参数`, not the request body.

## Blocked Or Not Executed

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am test` failed before
  reaching the rental module because `yudao-module-system`
  `OAuth2TokenServiceImplTest` hit Redis `NOAUTH Authentication required`.
- A repeated `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dsurefire.failIfNoSpecifiedTests=false test`
  still failed before reaching `yudao-module-rental` for the same local Redis
  `NOAUTH Authentication required` issue in `yudao-module-system`
  `OAuth2TokenServiceImplTest`.
- Permission-denied API behavior and permission-denied browser click-through
  were executed; independent visual/sensory review was not executed.
- Browser verification was not needed for the local infra-job config alias
  because the covered behavior is backend startup wiring and logs.
- Product-sync form browser smoke was attempted on 2026-07-25 against
  frontend `5174` and backend `48080`, both of which responded on their local
  ports. Local Playwright had no bundled browser installed, and the fallback
  system Chrome headless run timed out twice before producing a reliable DOM
  result or screenshot, so no browser UI evidence is claimed for the new form.
- Runtime raw-payload access and replay permission smoke were executed for
  admin and a temporary no-rental-grant user. Raw detail access-log inspection
  was executed on isolated backend `48086` with `yudao.access-log.enable=true`:
  login and raw detail both returned HTTP 200 / business code `0`,
  `infra_api_access_log` recorded one raw detail row, `response_body` was
  `NULL`, and `request_params` did not contain raw-payload fields or
  secret/token/phone/address-like markers. Replay/raw services now also have
  explicit current-tenant ID lookup tests, now broadened to push retry,
  product-push shop fallback, alert resolve, and manual order/product/after-sale
  sync entrypoints. Production-style cross-tenant penetration with separate
  authenticated tenants remains unexecuted.
- Browser sensory/transition evidence was attempted on 2026-07-25 with a
  temporary Chrome DevTools port `9227`. Both headless and non-headless
  launches reached `DevTools listening` and then exited before CDP target
  creation, so no browser screenshot, DOM, transition, or sensory evidence is
  claimed from this attempt.
- A later browser sensory/transition pass succeeded on 2026-07-25 after
  holding Chrome as a foreground CDP session and using isolated current
  runtime ports: backend `48086` with Quartz in standby and admin `5176`
  pointing to `48086`. The pass covered six rental admin pages across
  `zh-CN` / `en` and light / dark states: all 24 states settled on target
  routes, had no loading/denied/error state, all observed rental APIs returned
  HTTP 2xx, zh-CN/en UI markers and light/dark document classes were observed,
  visible text had no mainland mobile-number or 10+ continuous-digit pattern,
  and blurred screenshots plus structured JSON were recorded under
  `verify/e2e/artifacts/2026-07-25T00-44-37-775Z-*`.
- During that pass, the report page exposed raw external product/SKU IDs in
  visible text. `camera-rental-admin/src/views/rental/report/index.vue` now
  renders `externalProductId` and `externalSkuId` through
  `maskChannelIdentifier`; `pnpm ts:check` passed and the repeated browser
  pass found no remaining visible 10+ continuous-digit pattern.
- Replay/raw tenant pre-check hardening:
  - `XianyuReplayAdminService` and `XianyuRawPayloadAdminService` now resolve
    sensitive replay/detail IDs through explicit current `tenant_id + id`
    mapper lookups before processing.
  - Added mapper helpers for `xianyu_push_event` and `xianyu_raw_payload`
    current-tenant ID lookup.
  - Focused regression passed:
    `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuReplayAdminServiceTest,XianyuRawPayloadAdminServiceTest,XianyuPushRetryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
    ran 17 tests with 0 failures.
- Tenant-sensitive ID entrypoint hardening:
  - `XianyuPushRetryService`, `XianyuProductPushShopResolver`,
    `XianyuAlertAdminService`, `XianyuOrderAdminService`,
    `XianyuProductAdminService`, and `XianyuAfterSaleAdminService` now use
    explicit current-tenant lookup before accepting sensitive event, raw
    payload, shop, or alert IDs.
  - Added current-tenant mapper helpers for `xianyu_shop` and `xianyu_alert`,
    and reused the existing current-tenant helpers for push events and raw
    payloads.
  - Focused regression passed:
    `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuPushRetryServiceTest,XianyuProductPushShopResolverTest,XianyuReplayAdminServiceTest,XianyuRawPayloadAdminServiceTest,XianyuAlertAdminServiceTest,XianyuOrderAdminServiceTest,XianyuProductAdminServiceTest,XianyuAfterSaleAdminServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
    ran 35 tests with 0 failures.
- Framework tenant security filter regression:
  - Added `TenantSecurityWebFilterTest` for an authenticated user from tenant
    `1001` attempting to access request tenant `2002`.
  - The filter returned business code `403`, did not call the downstream
    `FilterChain`, and did not call `TenantFrameworkService.validTenant`.
  - Focused regression passed:
    `mvn -pl yudao-framework/yudao-spring-boot-starter-biz-tenant -Dtest=TenantSecurityWebFilterTest -Dsurefire.failIfNoSpecifiedTests=false test`
    ran 1 test with 0 failures.
- Dual-tenant runtime cross-tenant penetration 2026-07-25:
  - Isolated backend `http://127.0.0.1:48087` (original `:48080` left untouched).
  - Tenants: A=`1` username `admin`; B=`121` username `admin110`.
  - Setup (no secrets recorded): extended tenant `121` expire_time; granted
    rental menus (incl. raw/replay) to role/package for tenant B; cleared
    redis menu/permission/role caches; restarted isolated server on `:48087`.
  - Artifact:
    `verify/redteam/cross-tenant-runtime-probe-2026-07-25.json`
    and case log `verify/redteam/probes.jsonl`.
  - Totals: **18/18 passed**, 0 failed.
  - Case IDs:
    - Own-tenant positive: `S1`, `S2`, `S3`.
    - Header/token mismatch business `403` 「您无权访问该租户的数据」:
      `C1`, `C2`, `C3`, `C4`.
    - IDOR deny / no foreign-body leak (raw get, replay, push replay, alert
      resolve, convert) plus own raw get: `I1`–`I6`.
    - List isolation no ID overlap: order `L1`, shop `L2`.
    - Unauthenticated deny `401`: `U1`.
    - Webhook invalid payload no secret leak: `W1` (admin-api and public paths).
  - Scope note: local dual-tenant authenticated runtime on shared MySQL; not
    multi-cloud / multi-region production penetration. Probe was read /
    IDOR-deny oriented.

## Evidence Rule

Static source checks and unit tests do not substitute for browser, database,
or authenticated runtime evidence. Domain reports must cite concrete command
or runtime artifacts before a non-blocked verdict.


## I5 convert not-found fix (2026-07-25)

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuRentalConversionServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` → 5 tests, 0 failures.
- Runtime on `:48087`: tenant B convert foreign/missing order → code `1040001013` / 闲鱼渠道订单不存在.
