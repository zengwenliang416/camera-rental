# Development Handoff To Verify: add-rental-configuration

## Implemented Slices

- Explicit XianGuanJia/Xianyu product and SKU identifier persistence without fallback.
- Shop-scoped device catalog and channel product-rule configuration.
- Immediate idempotent internal-order creation and centralized reconciliation.
- Remark history, fulfillment-safe updates and preparation-state gating.
- Resumable historical reconciliation and persisted asynchronous rule-change runs.
- Standalone three-tab admin configuration UI and shared device catalog behavior.
- Deployment, dry-run, monitoring, stop-condition and rollback documentation.

## Files Changed

- `camera-rental-server/yudao-module-rental/**`
- `camera-rental-server/sql/mysql/migrations/20260831_052_*.sql` through
  `20260901_056_*.sql`
- `camera-rental-admin/src/api/rental/**`
- `camera-rental-admin/src/views/rental/configuration/**`
- `camera-rental-admin/src/views/rental/device/**`
- `camera-rental-admin/src/locales/{zh-CN,en}.ts`
- `docs/domain/xianyu-integration.md`
- `docs/integrations/xianyu/**`
- `openspec/changes/add-rental-configuration/**`

## Requirements Covered

- Exact string identifier sources and no product/SKU fallback.
- `shop_id + xianyu_item_id` single-model rules and exact synchronized SKU rules.
- `CONFIG_SKIPPED` channel preservation without remark conversion or scheduling.
- Immediate internal-order creation with later remark/model completion.
- Non-destructive extension, early-return, reschedule and replacement handling.
- Bounded historical reconciliation with durable counters and failure boundaries.
- Admin catalog/rule/remark UI with impact preview, optimistic locking and async results.

## Prototype Decisions Implemented

- Approved `admin-three-tab-precise-mapping-v1` structure.
- Existing admin theme/locale controls, Element Plus, `ContentWrap` and permissions.
- Desktop and narrow responsive layouts, explicit four-ID labels and copy actions.
- Single-model and synchronized multi-SKU editors with no inferred fallback.
- Fresh impact preview before mutation and persisted result polling afterward.

## Components Created / Reused / Extracted

- Created configuration Controller/VOs, rule services, centralized reconciliation,
  fulfillment guard, historical backfill, reconciliation-run ledger and UI panels.
- Reused `RentalDeviceCatalogService`, seller remark parser, assignment/schedule
  services, admin request client, permissions, i18n and Element Plus primitives.
- Shared catalog types/model labels and channel identifier display helpers are used
  across the configuration and rental-device surfaces.

## API / Data Flow Changes

- Added `/admin-api/rental/configuration/**` catalog, shop, rule, impact, synced-SKU,
  reconciliation-result and historical-reconciliation APIs.
- Order detail now persists explicit product/item/SKU identifiers, evaluates exact
  skip rules, creates ordinary internal orders immediately, then reconciles remark,
  model and preparation state.
- Product/SKU sync, order detail, rule changes, manual re-evaluation and historical
  backfill converge on the same reconciliation policy.
- Assignment and scheduling require authoritative `READY`; protected fulfillment
  facts are never inferred from remarks or overwritten by configuration changes.

## Tests Added

- Identifier parser/persistence, migration and no-fallback tests.
- Rule validation, optimistic locking, tenant/shop/SKU isolation and transaction tests.
- Immediate creation, idempotency, preparation, fulfillment guard and conflict tests.
- Historical backfill state-machine, lease/checkpoint and disposable-MySQL tests.
- Admin configuration model/UI contract tests and migration `056` coverage.

## Local Validation

- Task 006 checkpoint: admin configuration tests `16/16`, `pnpm ts:check`, targeted
  ESLint/Prettier and `pnpm build:prod` passed.
- Task 006 checkpoint: focused backend tests `20/20`; full `rental-biz` tests
  `660` with `0` failures, `0` errors and `8` environment-gated skips.
- Migrations `052` through `055` passed their disposable MySQL fixtures; `056`
  forward copies are byte-identical and its static migration test passed.
- `git diff --check`, JSON/JSONL parsing, shell syntax checks and strict OpenSpec
  validation passed at the Task 006 checkpoint.
- Task 007 current-HEAD static close-out passed in signed evidence `016`; the
  pre-case adapter execution in evidence `015` remains append-only and is
  adjudicated as a superseded lifecycle-ordering test defect.

## Known Risks

- No migration, controlled seed, deployment, real historical reconciliation or
  third-party write has been executed against production or the 80 server.
- The controlled seed requires unique valid unexpired shops for “小疆” and “发发”.
- Destructive rollback SQL is not a normal production rollback path.
- Formal Verification 2.0 now has reviewer `Zengwenliang0416`, immutable
  implementation commit `c621976b210ba78278a25455d156e061f70e6057` and a
  ready user-scoped Runtime, but still requires an approved case snapshot, six
  domains and current runtime/browser evidence.
- Browser light/dark, `zh-CN`/`en`, desktop and 320/375px interaction evidence is
  not yet established by the development checks.

## Items Requiring Six-Domain Verification

- Facticity: online API field sources and sanitized fixture provenance.
- Static: Java/TypeScript contracts, migrations, permissions, i18n and no-fallback search.
- Unit: identifiers, exact mapping, immediate creation, skip policy, update guards,
  idempotency and historical reconciliation.
- Redteam: cross-tenant/shop/SKU attacks, stale writes, replay, protected facts and privacy.
- E2E: catalog/rule administration, immediate order creation, later completion,
  skip behavior and conflict handling.
- Sensory: light/dark, `zh-CN`/`en`, desktop/320/375px, loading, empty, error,
  permission, SKU expansion, confirmations and copy feedback.
