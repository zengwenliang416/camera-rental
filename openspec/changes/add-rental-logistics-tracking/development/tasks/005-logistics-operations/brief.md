# Task Brief: 005-logistics-operations

## Goal

Tenant administrators can safely operate Provider configuration, carrier
mappings, failures, reconcile, backfill, cleanup, and logistics metrics.

## Parent Artifacts

- `openspec/changes/add-rental-logistics-tracking/requirements.md`
- `openspec/changes/add-rental-logistics-tracking/acceptance.md`
- `openspec/changes/add-rental-logistics-tracking/prototype/handoff.md`

## Vertical Slice

From permission-controlled operations pages and commands through masked local
configuration, task state changes, bounded maintenance services, and metrics,
deliver the complete operational lifecycle.

## In Scope

- Config and mapping CRUD/verify APIs, failed task page/retry, reconcile,
  dry-run and bounded backfill, retention cleanup, metrics, explicit permissions,
  operations page/panels, safe copy, and focused tests.

## Out Of Scope

- No production credential setup, deployment, automatic historical subscription,
  destructive order/shipment deletion, or unbounded maintenance job.

## Files Allowed

- `camera-rental-server/sql/mysql/migrations`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz`
- `camera-rental-schedule-center`
- `openspec/changes/add-rental-logistics-tracking`

## Interfaces / Seams

- Provider config, carrier mapping, task query/retry, reconcile, backfill,
  cleanup, metrics services and corresponding operations API/client components.

## Components To Create

- Operations controllers/VOs/services, retention and backfill policies,
  `LogisticsOperationsPage`, Provider config, carrier mapping, task queue, and
  metrics panels.

## Components To Reuse

- Encrypted config DO, mapping DO, Inbox/Outbox services, PageResult, permission
  annotations, API client, `StatusBadge`, `PermissionAwareAction`, theme/locale.

## Components To Extract

- Secret replacement/masking, task status presentation, safe retry commands,
  bounded batch policy, retention eligibility, and metrics aggregation.

## API / Data Flow Contracts

- Responses never return plaintext credentials or callback content; backfill
  defaults to dry-run and does not call Provider unless an explicit later action
  is enabled; cleanup only removes eligible technical records.

## State / Error / Empty / Loading Behavior

- Loading: panels load independently and preserve successful sections.
- Empty: no failures or mappings render explicit safe empty states.
- Error: raw Provider errors are replaced with stable safe categories.
- Disabled: config and mapping states show why Provider calls are blocked.
- Permission: each query and mutation has a distinct backend permission.

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='*LogisticsOperations*,*Backfill*,*Cleanup*' test`
- `pnpm test`
- `pnpm lint`
- `pnpm build`
- `git diff --check`

## Stop Conditions

- Scope lock mismatch.
- Missing product, architecture, data-flow, or component decision.
- Component duplication that should be extracted.
- An operation would expose plaintext secrets or callback content.
- Backfill or cleanup would be unbounded, destructive, or call Provider by default.

## Unsafe Assumptions

- Do not assume config verification means features should be enabled.
- Do not assume historical shipment rows are complete or safe to subscribe.
- Do not delete Delivery, shipment, order, or active audit history.
