# Verification Receipt: xian-guanjia-data-integration-v1

## Covered Scope

- Authorized-shop/order synchronization, order conversion/manual review,
  physical-device assignment, schedule/sync-run queries, and basic revenue
  summary.
- Focused XianGuanJia regression and full backend/admin static verification
  executed on 2026-07-24, with additional after-sale/manual-review focused
  regression and admin TypeScript checks on 2026-07-25.
- Authenticated API-level runtime smoke on 2026-07-24 against current jar
  `48082` connected to real local XianGuanJia data: config `READY`, shops `7`,
  orders `730`, sync-runs `1841` first check / `1861` repeat check.
- Authenticated API-level runtime smoke on 2026-07-25 against rebuilt current
  jar `48082`: config `READY`, shops `7`, orders `732`, manual-review page
  total `1` without HTTP 500, express companies `94`, after-sale one-page sync
  `20` received / `20` persisted / `hasNextPage=true`.
- Authenticated browser E2E on 2026-07-25 against admin `5174` and backend
  `48082`: Xianyu integration, manual review, sync-run, and report pages
  rendered without 404 after migration 018/runtime menu repair; all observed
  `/admin-api/rental/**` calls returned HTTP 200.
- Migration 018 was added and verified twice on the local MySQL 8.4 verification
  container to repair schedule, sync-run, and report menu visibility for
  existing role grants.
- Runtime permission-denied API behavior was verified on 2026-07-25 against
  backend `48082` using a temporary role/user with no rental menu grants:
  permission info returned code `0`, while rental report overview returned
  business code `403`; the temporary role/user were deleted after verification.
- Permission-denied browser click-through was verified on 2026-07-25 against
  admin `5174` and backend `48082`: a temporary no-rental-grant user had no
  rental menu/permission, direct navigation to `/rental/report` rendered a
  denied/not-found state without rental content, the sidebar had no rental
  entry, and the temporary user/role were deleted after verification.
- Focused device assignment and assign-request validation regression passed on
  2026-07-25: 11 tests, 0 failures.
- Admin default channel-ID masking was added on 2026-07-25 for ordinary order,
  shop authorization, and after-sale list views; `pnpm ts:check` passed after
  the masking change.
- Admin order `sellerRemark` privacy masking was added on 2026-07-25 at the
  backend response boundary, with a defensive front-end display mask; focused
  backend privacy regression and admin `pnpm ts:check` passed.
- Backend-default external identifier masking was added on 2026-07-25 for
  ordinary order, shop authorization, and after-sale pagination responses;
  focused backend privacy regression passed and the rebuilt jar on `48083`
  verified masked real-data API responses.
- Local infra-job registration can now be disabled through the documented
  `XGJ_JOB_REGISTER_INFRA_JOBS=false` environment variable; the legacy
  `XGJ_JOB_REGISTER_INFRA` name remains supported for compatibility. Focused
  config tests, shell syntax check, jar rebuild, and a temporary `48084`
  startup-log probe verified that no `[xianyu][job-register]` lines are emitted
  when the new variable is false.
- Browser masking verification was repeated on 2026-07-25 against temporary
  admin `5175` and the current rebuilt backend jar on `48085`. `/rental/order`
  and `/rental/xianyu` both stayed authenticated, observed rental API HTTP 200
  responses, displayed masked markers, and had no mainland mobile-number or
  10+ continuous-digit pattern in visible table text. Retained screenshots blur
  table bodies.
- Real MySQL 8.4 concurrent device allocation was verified on 2026-07-25 in
  `camera_rental_verify`: two overlapping assignment attempts for the same
  device serialized on the device row lock; the second waited `1356.9 ms`,
  rechecked overlaps, and rejected as `schedule-conflict`. Final device counts
  were exactly one effective overlap schedule and one assigned row.
- Representative schedule query plans were recorded with `30001` schedule rows:
  the overlap query used `idx_rental_schedule_device_range`, and the default
  admin page query used `idx_rental_schedule_admin_default`.
- Deduplicated authorization-loss alerts were added on 2026-07-25 using the
  existing `xianyu_alert` table. Backend pagination/resolve APIs mask source
  identifiers and messages, authorized-shop sync creates or refreshes one alert
  per invalid/disappeared authorization, and the admin Xianyu page now displays
  and resolves open alerts. Focused backend tests and admin `pnpm ts:check`
  passed.
- Deduplicated sync-failure alerts were added on 2026-07-25. Order page sync
  and after-sale page sync now refresh one `SYNC_FAILED` alert per
  resource/shop/safe-error-code after writing the failed sync-run outcome.
  Focused backend regression covered order failure, after-sale failure, alert
  dedupe-key construction, alert masking, and resolve behavior.
- Deduplicated guarantee-health alerts were added on 2026-07-25. Authorized
  shop sync now parses documented `is_deposit_enough` values into
  `HEALTHY`, `DEPOSIT_INSUFFICIENT`, or `UNKNOWN`, persists the value on
  `xianyu_shop.guarantee_status`, and records one `GUARANTEE_HEALTH` alert for
  insufficient service deposit without calling any XianGuanJia write API.
  Focused backend regression passed: 12 tests, 0 failures.
- Restricted raw-payload access was added on 2026-07-25. Backend APIs under
  `/admin-api/rental/xianyu/raw-payload/**` require the separate
  `rental:xianyu:raw` permission, list metadata without payload content, return
  only re-redacted detail JSON, and keep response bodies out of access logs.
  Admin Xianyu page now exposes a permission-gated masked-payload dialog.
  Focused backend regression and admin `pnpm ts:check` passed.
- Manual order-push event replay was added on 2026-07-25. Backend API
  `/admin-api/rental/xianyu/replay/push-event` requires the separate
  `rental:xianyu:replay` permission, requeues only durable local order-push
  payloads, skips succeeded/processing events, does not advance sync cursors,
  and disables response-body access logging. Admin Xianyu page exposes a
  permission-gated replay form. Focused backend replay regression and admin
  `pnpm ts:check` passed.
- Order-detail raw-payload replay was added on 2026-07-25 under the same
  `rental:xianyu:replay` permission. Backend API
  `/admin-api/rental/xianyu/replay/raw-payload` accepts only local
  `ORDER_DETAIL` raw payload records, reuses the existing order-detail
  persistence path, rejects unsupported source types, and does not advance
  sync cursors or call XianGuanJia. Admin Xianyu page exposes a separate raw
  payload ID replay action. Focused backend regression and admin
  `pnpm ts:check` passed.
- Order-page raw-payload replay was added on 2026-07-25 under the same
  `rental:xianyu:replay` permission. Order list sync now persists local
  `ORDER_PAGE` raw payload evidence before parsing page metadata; raw payload
  replay accepts local `ORDER_PAGE` records, reuses the existing page parser
  and detail-refresh logic, records a `REPLAY` sync run, preserves partial
  success counts on failures, and never advances the order page cursor.
  Focused backend regression passed: 17 tests, 0 failures.
- Product-push ingestion and replay were added on 2026-07-25. Backend webhook
  `POST /xianyu/webhooks/product` verifies the documented push signature,
  strictly parses documented product push fields, stores only redacted
  `PRODUCT_PUSH` webhook payloads in the raw-payload table, persists an
  idempotent push event, and asynchronously refreshes the normalized
  `xianyu_product` row through the read-only `POST /api/open/product/detail`
  path. Manual push-event replay now routes both `ORDER_PUSH` and
  `PRODUCT_PUSH` durable events without advancing cursors or invoking any
  third-party write API. Focused backend regression passed: 20 tests,
  0 failures.
- Product-push runtime consumption was verified on 2026-07-25 against the
  rebuilt jar on isolated backend `48086` with Quartz in standby. A signed
  `PRODUCT_PUSH` webhook returned HTTP 200 / `success`, the event reached
  `SUCCEEDED`, `processing_token` was cleared, and the synthetic webhook raw
  payload plus push-event rows were soft-deleted after polling. The fixture did
  not print credentials, signatures, raw payloads, external product IDs, seller
  remarks, phone numbers, or addresses.
- Raw-payload access-log auditing was verified on 2026-07-25 against isolated
  backend `48086` with `yudao.access-log.enable=true` and Quartz in standby.
  Admin login and raw detail access both returned HTTP 200 / business code
  `0`; `infra_api_access_log` recorded one row for
  `/admin-api/rental/xianyu/raw-payload/get`; `response_body` was `NULL`; and
  `request_params` did not contain raw-payload fields or
  secret/token/phone/address-like markers.
- Browser transition and sensory evidence was verified on 2026-07-25 against
  isolated admin `5176` and rebuilt backend `48086`. Six V1 rental admin pages
  were checked across `zh-CN` / `en` and light / dark states. All 24 states
  settled on target routes, had no loading/denied/error state, all observed
  rental APIs returned HTTP 2xx, language/theme markers were observed, and
  blurred screenshots plus a structured JSON receipt were recorded under
  `verify/e2e/artifacts/2026-07-25T00-44-37-775Z-*`.
- The browser pass exposed a report-page privacy defect: product report
  `externalProductId` and `externalSkuId` were rendered raw. The report table
  now uses `maskChannelIdentifier` for both fields; `pnpm ts:check` passed and
  the repeated browser pass found no visible mainland mobile-number or
  10+ continuous-digit pattern.
- Product list/SKU page orchestration was added on 2026-07-25. The channel
  sync service now includes a read-only product job with a tenant-scoped
  product lock, optional `seller_id` query parameter, fixed six-month-safe
  `update_time` windows, count probes, one-ten-thousand-row window splitting,
  `PRODUCT_PAGE` raw evidence, stale product-detail refresh, multi-spec SKU
  reads chunked at 100 `product_id` values, and a separate `PRODUCT` cursor
  advanced only after all fixed-window pages succeed. Focused backend
  regression passed: 34 tests, 0 failures.
- Express-company raw evidence persistence was added on 2026-07-25. The
  existing read-only express-company admin lookup now stores the complete
  `POST /api/open/express/companies` response as `EXPRESS_COMPANIES` in the
  restricted raw-payload table before returning the parsed code/name/alias/hot
  list. Focused backend regression passed: 1 test, 0 failures.
- After-sale list/detail orchestration was added on 2026-07-25. Manual sync
  and scheduled sync now store `AFTER_SALE_PAGE`, `AFTER_SALE_LIST_ITEM`, and
  `AFTER_SALE_DETAIL` raw evidence, refresh the normalized after-sale row from
  the detail response, traverse both `apply_time` and `refund_time` windows,
  register `xianyuAfterSaleSyncJob`, expose `XGJ_JOB_AFTER_SALE_CRON`, and
  advance a separate `AFTER_SALE` cursor only after all configured pages
  succeed. Focused backend regression passed: 22 tests, 0 failures.
- Manual product sync was verified with real data on 2026-07-25. The admin-only
  `POST /admin-api/rental/xianyu/product/sync-page` path omits the optional
  `seller_id` query parameter for the current self-developed / third-party ERP
  integration mode, returned code `0` against the local backend, and persisted
  one PRODUCT raw payload plus one PRODUCT sync run without printing secrets,
  external IDs, raw payloads, seller remarks, phone numbers, or addresses.
- Scheduled product sync was verified with real data on 2026-07-25 after the
  manual product-sync fix. `xianyuProductSyncJob` was present and enabled in
  infra job config, `/admin-api/infra/job/trigger` returned code `0`, the
  latest infra job log completed with status `1`, scheduled PRODUCT sync-run
  total increased from `28` to `32`, and the latest scheduled PRODUCT run was
  `SUCCEEDED` with `3` received / `3` succeeded / `3` deduplicated /
  `0` failed. `PRODUCT_PAGE` raw evidence increased by one; PRODUCT_DETAIL and
  PRODUCT_SKUS remained unchanged because all received products were already
  current.
- Order detail backfill was hardened on 2026-07-25. Missing-detail candidates
  now require a non-blank external order id and are prioritized by external
  update time, while the service layer skips blank `order_no` candidates without
  blocking valid rows. Focused order sync regression passed: 25 tests,
  0 failures.
- Admin loading/error/retry/i18n states were completed on 2026-07-25. Device,
  order, schedule, sync-run, and Xianyu operation pages now expose page-level
  load-error alerts with retry actions, joining the existing manual-review and
  report states; the generic error copy is present in both Chinese and English
  locale files. Admin `pnpm ts:check` passed after the changes.
- Migration 019 was added as an audit-copied seed migration for the separate
  raw-payload and safe-replay menu permissions.

## Uncovered Scope

- After-sale scheduled/page orchestration beyond the existing manual page sync.
- Runtime raw/replay permission smoke and cross-tenant red-team scenarios.
- Independent visual/sensory review.
- Facticity, redteam, E2E, and sensory verification.

## Residual Risk

- Existing infra Job cron/status drift is detected but not automatically
  overwritten.
- Deployment scripts and runtime environments should prefer
  `XGJ_JOB_REGISTER_INFRA_JOBS`; older local setups using
  `XGJ_JOB_REGISTER_INFRA` remain compatible.
- Process-level tenant ownership is now mandatory configuration and requires
  deployment confirmation.
- Migrations 001-017 passed on disposable MySQL 8.4 and migration 018 passed a
  repeatability check on the local MySQL 8.4 verification container. Migration
  019 has file/SHA audit-copy evidence but still needs disposable MySQL and
  approved-environment rollout evidence. Local real MySQL allocation
  concurrency and representative query plans are now verified, but production
  data distribution checks remain unverified.

## Confidence

C (partial implementation; not release-ready)
