# Handoff To Verify

## Implemented Slices

- The foundation, canonical read client, order persistence/synchronization,
  durable order-push ingestion/retry, rental conversion/manual review,
  physical-device assignment, schedule query, sync-run query, basic revenue
  summary, and admin localization/theme reuse are implemented.
- The earlier 001-008 task packets are historical implementation records, not
  evidence that the complete V1 acceptance set is finished.

## Files Changed

- camera-rental-server/yudao-module-rental/**
- camera-rental-server/yudao-server/src/main/resources/application*.yaml
- camera-rental-server/sql/mysql/migrations/**
- camera-rental-admin/src/api/rental/**
- camera-rental-admin/src/views/rental/**
- camera-rental-admin/src/locales/**
- camera-rental-admin/src/router/modules/remaining.ts
- openspec/changes/xian-guanjia-data-integration-v1/**

## Requirements Covered

- Read-only XianGuanJia integration with runtime credentials and a default-off
  integration switch.
- Bounded authorized-shop/order synchronization, conversion/review, and
  transactional device assignment.
- Signed order-push reception with strict field validation, redacted raw
  persistence, idempotent event state, after-commit detail refresh, and
  tenant-scoped infra Job retry.
- Admin order, device, schedule, review, sync-run, and basic revenue surfaces
  with zh-CN/en and light/dark reuse.

## Prototype Decisions Implemented

- data-flow read-only sync through transactional device allocation.

## Components Created / Reused / Extracted

- Created rental admin APIs/UI and reused Yudao permissions, tenancy,
  dictionaries, formatting, configuration, and infra Job/Quartz capabilities.

## API / Data Flow Changes

- /admin-api/rental/** config, shop, order, device, review, report endpoints.
- Implemented XianGuanJia admin routes use `/admin-api/rental/xianyu/**`,
  including `/admin-api/rental/xianyu/sync-run/page`.

## Tests Added

- Rental domain, synchronization, redaction, cursor, conversion, scheduling,
  assignment, and admin-service tests are present. Current counts must come
  from a fresh full-module run rather than the stale 42-test snapshot.

## Local Validation

- Focused Xianyu regression on 2026-07-24: 16 tests passed.
- Full 21-project Maven Reactor on 2026-07-24: BUILD SUCCESS; rental module
  109 tests passed with no failures, errors, or skips.
- Full Maven Reactor, admin type/lint/style checks, shell syntax, migration
  audit-copy integrity, and static security scans pass against the current
  worktree.
- Authenticated API-level runtime smoke passed on current jar `48082` against
  real local XianGuanJia data with scheduler/infra jobs disabled: config
  `READY`, shops `7`, orders `730`, sync-runs `1841` first check / `1861`
  repeat check.
- Browser click-through, real MySQL allocation concurrency, restricted
  raw-payload access, manual order-push replay, and order-detail raw-payload
  replay now have implementation or focused verification evidence. Product
  push ingestion/replay and order-page replay now have focused backend
  verification. Sensory review and full runtime red-team verification remain
  blocked.

## Known Risks

- Product push ingestion/replay and order-page replay are implemented. Full
  Product/SKU list/page orchestrators remain pending.
- Raw-payload access, manual order-push replay, and order-detail raw-payload
  replay are implemented, but runtime raw/replay abuse tests and broader
  red-team coverage are still pending.
- Browser E2E and live MySQL allocation concurrency now have partial evidence;
  approved-environment migration verification has not been run.
- The running `48080` service has real data but is an older process missing the
  current sync-run route. Use current jar `48082` or restart/deploy `48080`
  after approval before judging the admin UI.
- Infra Job registration detects existing cron/status drift but deliberately
  does not overwrite an administrator's stopped state or cron.
- Process-level XianGuanJia tenant identity must now be explicitly configured
  when the integration is enabled; deployment must supply and verify it.

## Items Requiring Six-Domain Verification

- Unit and static are current and green.
- Facticity, redteam, browser E2E, and sensory remain blocked or partial as
  described in their reports; API runtime routing is now covered.
