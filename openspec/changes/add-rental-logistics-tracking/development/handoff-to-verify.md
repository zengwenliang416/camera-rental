# Development Handoff To Verify: add-rental-logistics-tracking

## Implemented Slices

- 001 Delivery foundation, tenant-safe persistence, device relations, snapshots,
  Inbox/Outbox, and provider-neutral contracts.
- 002 Kuaidi100 subscription/query/callback adapters and bounded asynchronous
  workers.
- 003 Xianyu shipment-to-Delivery transaction integration and replay.
- 004 Schedule-center summaries, detail traces, refresh, polling, and risks.
- 005 Provider/mapping operations, failed tasks, reconcile, backfill, cleanup,
  metrics, permissions, and tenant-isolated callback routing.
- 006 Multiple encrypted Kuaidi100 credentials with stable tenant-safe Delivery
  assignment and legacy single-credential migration.

## Files Changed

- Additive MySQL migration 032 and logistics ADR.
- Rental backend logistics controllers, DOs, mappers, enums, provider adapter,
  services, jobs, tests, and Xianyu shipment response/linkage changes.
- Schedule-center tracking and logistics-operations features, shared providers,
  API client additions, localization, navigation, and tests.
- SpecNav task packets, independent reviews, migration manifest, validation
  ledger, drift evidence, and browser screenshots.

## Requirements Covered

- Physical Delivery is distinct from shipment audit, supports multiple packages
  and multiple devices, and preserves provider-neutral local authority.
- Provider work is asynchronous, leased, idempotent, bounded, and isolated from
  the Xianyu shipment transaction.
- Callback, configuration, operations, queries, unique keys, and workers are
  tenant-safe; sensitive fields remain encrypted or masked.
- Provider common configuration is separated from multiple named credential
  pairs. A Delivery reuses its bound credential while usable and safely
  reselects only after the binding becomes invalid.
- Schedule and exception views read local summaries and traces only.
- Operations are explicit, permissioned, bounded, dry-run safe, and do not
  automatically mutate historical business data.

## Prototype Decisions Implemented

- Existing schedule-center shell, semantic status styling, light/dark themes,
  zh-CN/en copy, desktop and narrow layouts.
- Single-package inline summary, multi-package selection, on-demand trace
  drawer, stable queued/throttled refresh feedback, and logistics risk surfaces.
- Independent operations panels with masked credentials and explicit
  confirmation for non-dry-run maintenance.

## Components Created / Reused / Extracted

- Created Delivery, relation, trace, Inbox, Outbox, carrier mapping, provider
  config, provider credential, provider adapter, tracking query/risk, and
  operations components.
- Reused TenantBaseDO, EncryptTypeHandler, transaction templates, OkHttp,
  existing Xianyu shipment orchestration, schedule-center auth/API/state shell,
  masking utilities, Provider gateway/signer, Outbox worker/completion, secret
  actions, and permission model.
- Extracted provider-neutral commands/results, retry policy, snapshot
  normalization, carrier resolution, callback signing, and local operations
  services.

## API / Data Flow Changes

- Xianyu shipment now creates/reuses and links a Delivery, then queues
  SUBSCRIBE/INITIAL_QUERY without waiting for Kuaidi100.
- Public Kuaidi100 callback resolves a unique exact token candidate, verifies
  the signature, persists encrypted Inbox data under the located tenant, and
  acknowledges before asynchronous processing.
- Admin APIs expose batched summaries, on-demand detail, asynchronous refresh,
  masked configuration, carrier mappings, failed tasks, reconcile, metrics,
  bounded backfill, and bounded cleanup.
- Credential CRUD and local verification reuse the existing logistics config
  permissions and never return plaintext customer codes or API keys.
- Schedule center polls local summaries every 60 seconds only while visible and
  never calls the Provider directly.

## Tests Added

- Delivery idempotency, relations, snapshot normalization, terminal protection,
  provider mapping, privacy, callback, worker lease/retry/dedupe, shipment
  integration, tracking queries/refresh/risks, operations/backfill/cleanup, and
  real MySQL concurrency tests.
- Multi-credential tests cover stable binding, ordered distribution, disabled
  credential fallback, incomplete and cross-tenant rejection, encryption,
  masking, and tenant-scoped update behavior.
- Frontend API, access, state, polling, tracking drawer, sibling actions,
  operations panels, localization, focus, error, and responsive behavior tests.

## Local Validation

- Backend logistics regression: 110 tests, 0 failures, 0 errors; six
  environment-gated database cases were then rerun separately on MySQL 8.4.10
  with 6 tests passed and no skips.
- Frontend: 98 tests passed, TypeScript lint passed, and Vite production build
  completed with 1796 modules transformed.
- Fresh schema, legacy single-credential upgrade, and repeated migration 032
  all passed on MySQL 8.4.10 with eight logistics tables. The encrypted legacy
  pair migrated once into the `default` credential.
- Browser sensory matrices passed for tracking and operations across required
  state, locale, theme, desktop, and narrow-screen combinations.
- Changed-text whitespace, JSON/JSONL integrity, provider boundary, and
  sensitive literal scans passed.

## Known Risks

- The active Change and `tasks.md` are not tracked by the current Git HEAD
  because no commit was authorized; this remains the expected baseline blocker.
- No production credentials were configured and no real Kuaidi100 network call
  was made. Provider enablement must remain off until an authorized deployment
  configures and verifies tenant credentials.
- Application rollback should retain the additive logistics tables; destructive
  schema rollback requires backup and explicit data-retention approval.

## Items Requiring Six-Domain Verification

- Run verification against the packaged application and a mock Provider,
  including the full shipment-to-Delivery-to-worker-to-callback-to-schedule
  closed loop.
- Confirm deployment configuration, permission assignment, worker scheduling,
  callback public routing, and observability in the target environment.
- Exercise an explicit multi-direction, multi-package, multi-device order
  scenario against the integrated database/API boundary.
