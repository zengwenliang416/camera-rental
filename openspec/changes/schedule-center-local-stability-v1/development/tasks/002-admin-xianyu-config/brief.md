# Task Brief: 002-admin-xianyu-config

## Goal

Make the management database the only runtime source for tenant XianGuanJia
configuration. Remove the environment-variable configuration path without a
compatibility fallback.

## Parent Artifacts

- `openspec/changes/schedule-center-local-stability-v1/requirements.md`
- `openspec/changes/schedule-center-local-stability-v1/acceptance.md`
- `openspec/changes/schedule-center-local-stability-v1/development/basis.md`

## Vertical Slice

An authorized tenant administrator configures the integration endpoint,
AppKey, one-way AppSecret replacement, webhook URL, read/write/job switches,
and synchronization parameters from `camera-rental-admin`. Backend clients,
webhooks, scheduled jobs, and shipment commands read the persisted
`xianyu_application` row for the active tenant.

## In Scope

- Extend `xianyu_application` with tenant-managed connection, credential,
  write, job, and synchronization fields.
- Encrypt AppSecret at rest with the existing `EncryptTypeHandler`.
- Add the protected configuration update API and disable request-body access
  logging for that endpoint.
- Preserve AppKey and AppSecret when their replacement inputs are blank.
- Resolve read/write client configuration for every request.
- Resolve webhook tenant configuration by AppKey before signature validation.
- Use `@TenantJob` plus a persisted tenant guard for all XianGuanJia jobs.
- Keep cron expressions and job lifecycle in infrastructure job management.
- Deny real shipment before local or remote side effects when persisted writes
  are disabled.
- Remove `rental.xianyu` YAML, `XGJ_*` startup wiring, the tracked environment
  template, Spring scheduling fallback, and startup synchronization runner.
- Update management UI, translations, integration documentation, and the
  prototype write-state hint.

## Out Of Scope

- Applying the migration to production.
- Importing credentials from environment variables.
- Compatibility fallback to old configuration names.
- Calling any real XianGuanJia write endpoint.
- Changing third-party API semantics or adding another write operation.

## Files Allowed

- `AGENTS.md`
- `CLAUDE.md`
- `camera-rental-admin/src/api/rental/xianyu.ts`
- `camera-rental-admin/src/locales/en.ts`
- `camera-rental-admin/src/locales/zh-CN.ts`
- `camera-rental-admin/src/views/rental/xianyu/**`
- `camera-rental-server/.env.xianyu.example`
- `camera-rental-server/scripts/setup-local.sh`
- `camera-rental-server/scripts/start-local.sh`
- `camera-rental-server/sql/mysql/migrations/20260729_029_xianyu_admin_managed_config.sql`
- `camera-rental-server/yudao-server/src/main/resources/application*.yaml`
- Rental module XianGuanJia controllers, VOs, persistence, integration, jobs,
  services, and focused tests.
- `docs/domain/xianyu-integration.md`
- `docs/integrations/xianyu/**`
- `openspec/changes/schedule-center-local-stability-v1/prototype/artifact/app.js`

## Interfaces / Seams

- `XianyuRuntimeConfigService` is the single runtime adapter from encrypted
  persistence to the existing typed runtime snapshot.
- Controllers accept validated request VOs and never return AppSecret.
- Business services and clients do not read Spring process properties.
- The management frontend calls only `/admin-api`; it never calls XianGuanJia.
- The generic database encryption master key remains infrastructure config.

## Components To Create

- `XianyuConfigPanel`
- `XianyuRuntimeConfigService`
- `XianyuTenantJobGuard`
- `XianyuConfigUpdateReqVO`

## Components To Reuse

- Existing Element Plus form, switch, password input, number input, and confirm
  dialog components.
- Existing MyBatis `EncryptTypeHandler`.
- Existing tenant context, `TenantUtils`, `@TenantJob`, API client, signer, and
  shipment command.

## Components To Extract

- Extract tenant runtime configuration mapping from clients and business
  services into `XianyuRuntimeConfigService`.
- Extract configuration editing from the large XianGuanJia operations page
  into `XianyuConfigPanel`.

## API / Data Flow Contracts

- Preserve `GET /rental/xianyu/config/get`.
- Add protected `PUT /rental/xianyu/config/update`.
- Accept replacement secrets but never return stored AppSecret.
- Resolve reads and writes through
  `view -> management API -> config service -> encrypted persistence`.
- Resolve webhook ownership by AppKey before entering tenant context.
- Preserve existing shipment and synchronization API routes.

## State / Error / Empty / Loading Behavior

- Missing configuration returns a disabled draft with safe defaults.
- Invalid HTTPS URLs, missing required credentials, and writes enabled while
  integration is disabled are rejected before persistence.
- The admin form retains loading/saving states and clears replacement
  credentials after a successful save.
- Disabled tenant jobs return an explicit skip result without invoking sync.

## TDD Requirement

- Add or update focused tests before or alongside configuration persistence,
  dynamic client resolution, webhook lookup, job gating, and shipment guards.

## Verification Commands

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am test`
- `pnpm ts:check`
- `pnpm build:prod`
- `pnpm test`
- `pnpm lint`
- `pnpm build`
- `cmp` and SHA-256 verification for migration copies
- `git diff --check`
- Repository scan for executable `XGJ_*`, `rental.xianyu`, and
  `.env.xianyu.example` entries

## Stop Conditions

- AppSecret appears in a response, access log, fixture, or browser storage.
- A disabled write path can reach assignment, network, dispatch, or shipment
  persistence.
- Webhook lookup cannot determine tenant ownership before entering tenant
  context.
- A legacy process property remains executable runtime configuration.

## Unsafe Assumptions

- Removing documentation is sufficient even if runtime code still reads the
  old property.
- A frontend switch is sufficient authorization for real third-party writes.
- A webhook can trust request tenant context before AppKey ownership and
  signature verification.
- Blank replacement credentials should erase persisted credentials.
