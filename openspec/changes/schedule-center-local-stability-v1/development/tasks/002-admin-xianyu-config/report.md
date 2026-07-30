# Task Report: 002-admin-xianyu-config

## Status

DONE

## Files Changed

- Management API types, locales, XianGuanJia operations page, and extracted
  configuration panel.
- Rental API error constants plus XianGuanJia configuration controller, VOs,
  persistence, runtime service, clients, webhook services, jobs, sync service,
  shipment service, and focused tests.
- Migration 029, application YAML, local scripts, integration documentation,
  project rules, and prototype status copy.
- Deleted `.env.xianyu.example`, `XianyuChannelSyncScheduler`, and
  `XianyuChannelSyncStartupRunner`.

## What Changed

- Added tenant-owned XianGuanJia connection, credential, webhook, write, job,
  and synchronization fields to `xianyu_application`.
- Added an authorized management form and update API. AppSecret is a one-way
  encrypted replacement and is never returned.
- Made read/write clients, webhooks, synchronization services, and shipment
  commands resolve the current persisted configuration dynamically.
- Added `@TenantJob` execution and a database-backed per-tenant job guard.
- Removed the Spring scheduler fallback, startup sync runner, XianGuanJia YAML
  properties, startup-script environment loading, and tracked environment
  template. No compatibility fallback remains.
- Kept real writes disabled by default and checked the persisted switch before
  any assignment, network call, device transition, or shipment persistence.

## TDD Evidence

- Configuration tests cover encrypted persistence metadata, tenant ownership,
  blank credential preservation, missing state, and invalid transitions.
- Client tests prove dynamic resolution and zero network requests when writes
  are disabled.
- Webhook, job, sync, and shipment tests cover tenant resolution and
  pre-side-effect denial.
- Full Reactor testing exposed an unnecessary shared Mockito stub; the fixture
  was narrowed and the affected test plus full Reactor then passed.

## Verification Commands

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuPushRetryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am test`
- `cd camera-rental-admin && pnpm ts:check && pnpm build:prod`
- `cd camera-rental-schedule-center && pnpm test && pnpm lint && pnpm build`
- Migration `cmp` and `shasum -a 256`
- Legacy runtime-entry scan
- `git diff --check`

## Concerns

- Production must provide the generic MyBatis encryption master key before
  storing AppSecret.
- Existing tenants remain disabled until migration 029 is applied and an
  authorized administrator saves their configuration.
- Live webhook and shipment integration are intentionally not exercised.

## Scope Deviations

- The user explicitly required removal of environment-variable compatibility,
  so the tracked environment template and legacy scheduler/startup paths were
  deleted rather than retained as fallback.

## Follow-up Needed

- Apply migration 029 during an approved deployment.
- Configure each production tenant from the management page.
- Run controlled read-only sync and webhook verification before separately
  authorizing real shipment writes.

## Migration

- Required migration:
  `camera-rental-server/sql/mysql/migrations/20260729_029_xianyu_admin_managed_config.sql`
- Existing tenants default to disabled write and disabled automatic sync.
- The migration was prepared and checksum-verified but not applied to a live
  database in this task.

## Verification Summary

- Focused runtime/config/write/webhook/job/shipment suite: 42 tests passed.
- `XianyuPushRetryServiceTest`: 6 tests passed after removing an unnecessary
  Mockito stub.
- Full rental Reactor: 22 modules built successfully; rental business module
  243 tests passed with 0 failures and 0 errors.
- Schedule center: 19 tests passed; lint and production build passed.
- Admin: TypeScript check and production build passed.
- Migration audit and production copies have identical SHA-256
  `4896848909c75c8d08fba173ece0658380cf26002471794b79edd65f7c78e41b`.
- `git diff --check` passed.

## Not Executed

- No migration was applied to production or local business data.
- No real XianGuanJia API write, shipment, deployment, commit, or push was
  executed.

## Residual Notes

- The generic MyBatis encryption master key remains external infrastructure
  configuration by design.
- A local ignored `.env.xianyu` file may still exist on a developer machine,
  but no application code or startup script reads it.
