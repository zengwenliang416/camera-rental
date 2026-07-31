# Task Brief: 006-multiple-provider-credentials

## Goal

Tenant administrators can configure multiple encrypted Kuaidi100 credential
pairs, and every Delivery uses one stable tenant-safe credential assignment.

## Parent Artifacts

- `openspec/changes/add-rental-logistics-tracking/requirements.md`
- `openspec/changes/add-rental-logistics-tracking/acceptance.md`
- `openspec/changes/add-rental-logistics-tracking/design.md`

## Vertical Slice

From masked credential CRUD in the existing logistics operations page through
tenant-scoped encrypted persistence and Outbox credential binding, execute
Kuaidi100 subscription and query with the selected credential without changing
the existing Provider, worker, retry, or transaction boundaries.

## In Scope

- Separate Provider common configuration from named credential pairs.
- Support credential create, update, enable/disable, ordering, delete, and local
  completeness verification.
- Bind a usable credential to a Delivery on its first Provider task.
- Reuse the bound credential while it remains usable; reselect only after it is
  disabled, deleted, incomplete, cross-tenant, or for another Provider.
- Migrate pre-release single-credential config rows without decrypting values.
- Update operations UI, tests, migration evidence, and OpenSpec contracts.

## Out Of Scope

- No production credential setup, automatic quota failover, provider billing
  policy, deployment, or real Kuaidi100 network call.

## Files Allowed

- `camera-rental-server/sql/mysql/migrations`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz`
- `camera-rental-schedule-center`
- `docs/decisions`
- `openspec/changes/add-rental-logistics-tracking`

## Interfaces / Seams

- `RentalLogisticsProviderCredentialService` owns tenant-safe credential
  selection.
- `RentalDeliveryOutboxLeaseService` persists the selected credential ID.
- `Kuaidi100LogisticsProvider` resolves authentication by credential ID.
- Existing operations permissions protect credential CRUD and verification.

## Components To Reuse

- `TenantBaseDO`, `TenantContextHolder`, `BaseMapperX`, `EncryptTypeHandler`,
  `SecretAction`, `RentalOutboxWorkItem`, Outbox lease/worker/completion,
  `Kuaidi100Gateway`, `Kuaidi100Signer`, `SensitiveValueRedactor`, shared API
  client, operations panels, theme, locale, and permission model.

## Components To Create

- Provider credential DO, mapper, selection service, operations models and
  endpoints, credential management UI, and focused tests.

## Components To Extract

- None. Credential selection is one cohesive service and must not duplicate
  Provider transport, signing, encryption, retry, tenant, or masking logic.

## Verification Commands

- `mvn clean -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='*RentalLogistics*,*RentalDeliveryOutbox*,*Kuaidi100*' -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='*RentalDelivery*,*RentalTracking*,*RentalLogistics*,*Kuaidi100*,XianyuOrderShipServiceTest,*ShipmentDelivery*,*LogisticsOperations*,*Backfill*,*Cleanup*,*LogisticsRisk*,WaybillPrivacyTest' -Dsurefire.failIfNoSpecifiedTests=false test`
- `cd camera-rental-schedule-center && pnpm test && pnpm lint && pnpm build`
- Disposable MySQL 8.4 fresh, legacy single-credential upgrade, and repeated
  migration verification.
- `git diff --check`

## Stop Conditions

- A credential can be read or updated outside the current tenant.
- Plaintext credentials reach logs, errors, ordinary UI state, or responses.
- Provider calls move into a database transaction.
- Multi-key support duplicates transport, signing, retry, encryption, tenant,
  masking, or permission mechanisms.
