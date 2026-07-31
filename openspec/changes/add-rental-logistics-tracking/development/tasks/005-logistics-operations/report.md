# Task Report: 005-logistics-operations

## Status

DONE

## Files Changed

- Logistics operations controller, request models, configuration, task,
  metrics, backfill, cleanup, and transaction services with focused tests.
- Additive migration permissions for tracking and twelve distinct logistics
  operations without automatic role grants.
- Schedule-center operations API client, permission model, navigation, page,
  Provider configuration, carrier mapping, failed-task/retry, reconcile,
  metrics, backfill, and cleanup panels with focused tests and localized copy.
- Shared API client `DELETE` support so mapping deletion uses the same token
  refresh, tenant headers, and safe error path as other authenticated requests.
- Task report, validation log, drift check, and task ledger for slice 005.
- Tenant-scoped callback token uniqueness, safe public-webhook candidate
  matching, and real MySQL cross-tenant reuse coverage required by A12.

## What Changed

- Added masked Provider configuration query and explicit KEEP/REPLACE/CLEAR
  secret updates. Plaintext credentials and callback content are not returned,
  logged, or stored in frontend state after save.
- Added local configuration verification that does not call the Provider and
  does not automatically enable query or subscription features.
- Added tenant-scoped carrier mapping CRUD with independent query, update, and
  delete permissions.
- Added masked failed Inbox/Outbox inspection, safe retry, bounded reconcile,
  privacy-safe metrics, dry-run historical backfill, and dry-run technical-data
  cleanup.
- Backfill defaults to dry-run, is bounded to 100 rows, does not call the
  Provider, skips incomplete historical rows safely, and atomically binds the
  created/reused Delivery back to the shipment.
- Cleanup defaults to dry-run and deletes only bounded old snapshots and
  successfully processed Inbox/Outbox technical rows; it never deletes
  Delivery, shipment, order, or active audit history.
- Added twelve independent backend operation permissions and one tracking
  permission to the additive migration without granting them to any role.
- Added a responsive operations page whose panels load independently and retain
  successful data when another panel fails. Navigation is available when the
  account owns any operations permission; every panel and mutation remains
  independently permission-gated.
- Non-dry-run Backfill and Cleanup require an explicit confirmation dialog.
- Replaced the global callback-token hash constraint with
  `tenant_id + callback_token_hash`, retained a non-unique hash lookup index,
  and made the public callback path constant-time match the decrypted exact
  token before entering exactly one tenant context. Missing, mismatched, or
  cross-tenant ambiguous tokens fail without an Inbox write.

## TDD Evidence

- Backend configuration tests cover tenant isolation, masking, KEEP/REPLACE/
  CLEAR semantics, local-only verification, and safe errors.
- Task operation tests cover tenant-scoped failed-task reads, retry eligibility,
  bounded reconcile, and safe task projections.
- Backfill tests cover dry-run defaults, limits, incomplete/invalid waybills,
  local-only Delivery creation, no Provider enqueue by default, idempotent reuse,
  and rollback on shipment binding conflict.
- Cleanup tests and mapper contract tests cover retention bounds and deletion of
  technical data only.
- Controller and migration tests cover all distinct permissions and the absence
  of automatic role grants.
- Frontend tests cover independent permissions, secret-free drafts, bounded
  safe commands, independent panel loading/error/empty states, and authenticated
  tenant-aware `DELETE`.
- Callback tests cover exact-token selection from same-hash cross-tenant
  candidates and fail-closed handling when the same token is ambiguous.
- Real MySQL tests prove callback hashes can repeat across tenants, remain
  unique inside one tenant, and preserve the existing Inbox/Outbox concurrency
  contracts.

## Verification Commands

- `cd camera-rental-server && mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='*LogisticsOperations*,*Backfill*,*Cleanup*' -Dsurefire.failIfNoSpecifiedTests=false test`
- `cd camera-rental-server && mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='RentalDeliveryServiceImplTest,XianyuOrderShipServiceTest,*ShipmentDelivery*,*TrackingQuery*,*TrackingRefresh*,*TrackingController*,*LogisticsRisk*,*LogisticsOperations*,*Backfill*,*Cleanup*' -Dsurefire.failIfNoSpecifiedTests=false test`
- `cd camera-rental-schedule-center && pnpm test`
- `cd camera-rental-schedule-center && pnpm lint`
- `cd camera-rental-schedule-center && pnpm build`
- `cd camera-rental-server && mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='Kuaidi100CallbackServiceTest,Kuaidi100TrackingWebhookControllerTest' -Dsurefire.failIfNoSpecifiedTests=false test`
- `cd camera-rental-server && RENTAL_LOGISTICS_MYSQL_* mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='RentalLogisticsMysqlConcurrencyTest' -Dsurefire.failIfNoSpecifiedTests=false test`
- Disposable MySQL 8.4 migration execution twice with table, column, and
  permission assertions.
- Disposable MySQL 8.4 fresh and current-upgrade replay, including legacy
  global-index repair, repeated migration 032, shipment fixture preservation,
  tenant-scoped callback uniqueness, and non-unique callback lookup index.
- `git diff --check`
- Credential literal scan across the working tree excluding generated
  dependencies and build output.
- Browser sensory inspection of the running operations page in light/dark,
  zh-CN/en, desktop, and 390px layouts, including the expected internal task
  table scroller and no page-level horizontal overflow.

## Concerns

- The operations page intentionally does not configure production credentials,
  execute deployment, or subscribe historical shipments automatically.
- SpecNav entry remains blocked by `git-baseline:tasks-not-tracked`; no staging
  or commit was performed.

## Scope Deviations

- None recorded.

## Follow-up Needed

- Independent re-review must verify the A12 tenancy fix and retain the previous
  A11, A15, and A16 evidence.
- Final six-domain verification must rerun MySQL concurrency, fresh/upgrade
  migration, full logistics regression, frontend checks, and security scans.

## Adjudication

The independent review's A12 blocker has been fixed and directly verified.
Independent re-review files remain authoritative for approval.
