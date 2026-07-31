# Prototype Handoff: add-rental-logistics-tracking

## Approved Branch Variant

- Branch: `ui-html`
- Candidate variant: `schedule-tracking-command-center-v1`
- Approval status: awaiting explicit user approval

## Screens Or Flows

- `schedule-tracking`: dense physical-device schedule with local package summaries, multi-package status, risk badges, and an on-demand trace drawer.
- `shipment-result`: Xianyu shipment success followed by Delivery creation, device binding, Outbox creation, asynchronous subscription, and initial query.
- `logistics-operations`: tenant Provider config, carrier mappings, Outbox and Inbox failures, safe retry, reconcile, backfill, and cleanup entry points.
- `logistics-risks`: server-derived delivery risks tied to rental dates, occupied intervals, and the next booking.

## Components To Create

- Backend Delivery aggregate, delivery-device relation, trace snapshot, callback Inbox, event Outbox, Provider config, carrier mapping, risk calculation, workers, and admin APIs.
- Schedule-center delivery summary, multi-package tracking drawer, trace timeline, Provider config panel, carrier mapping panel, async task panel, and logistics risk detail.
- Shipping result extension that displays locally committed Delivery and asynchronous tracking state without coupling Xianyu shipment success to Kuaidi100.

## Components To Reuse

- `ScheduleCenterAppShell`, `ScheduleDeviceTable`, `DetailDrawerShell`, `StatusBadge`, `EmptyState`, and `PermissionAwareAction`.
- Existing Xianyu shipment service, shipment audit entity, device assignment and dispatch rules, tenant and permission conventions, encryption handler, job infrastructure, and unified API response conventions.
- Existing semantic CSS variables, light/dark theme policy, zh-CN/en locale policy, responsive top navigation, and mobile bottom navigation.

## Extraction Targets

- Provider-neutral logistics domain interfaces must be separate from Kuaidi100 SDK and payload types.
- Delivery read-model assembly must be reusable by schedule summaries, tracking detail, shipping result, and risk views.
- Async lease, retry, idempotency, masking, and safe-error utilities should be shared across Outbox and Inbox processing where current project patterns allow.
- Carrier normalization must be centralized in the mapping service instead of duplicated in shipment, callback, and query paths.

## API Contracts

- Schedule summary API returns local package summaries only and supports the current visible date window.
- Delivery detail API returns one order's multiple packages, related devices, current normalized status, full stored trace, last sync time, optional ETA, and risk.
- Manual refresh API enqueues work and returns queued, throttled, disabled, mapping-required, or not-found results without waiting for Kuaidi100.
- Operations APIs manage masked Provider configuration, carrier mappings, failed-task inspection and retry, reconcile, historical backfill, and cleanup.
- Kuaidi100 callback endpoint verifies authenticity, persists the callback Inbox first, responds quickly, and processes trace changes asynchronously.

## Data Flows

- Xianyu shipment transaction: remote shipment result -> local shipment audit -> device dispatch -> Delivery -> delivery-device relation -> Outbox -> commit.
- Post-commit worker: claim Outbox in a short transaction -> call Provider outside the transaction -> persist normalized result and next retry state.
- Callback: verify -> persist Inbox idempotently -> acknowledge -> async normalize -> append immutable trace snapshot -> update Delivery summary -> recalculate risk.
- Schedule page: poll local summary every 60 seconds while visible -> open detail on demand -> read complete local trace.
- Manual refresh: validate permission and throttle -> enqueue query -> return immediately -> later summary polling observes the new snapshot.

## State Behavior

- Loading: preserve shell and filters while showing a dedicated local-summary loading state.
- Empty: explain that unshipped orders do not create tracked packages.
- Error: preserve local-first behavior and never fall back to direct browser calls to Kuaidi100.
- Disabled: distinguish Provider disabled, carrier mapping required, and query throttled without turning them into shipment failures.
- Permission: hide logistics data and actions according to server authorization and tenant data scope.
- Populated: support one order with multiple packages and one package with multiple devices.

## Theme And Locale Policy

- Theme support: required.
- Theme modes shown in prototype: `light`, `dark`.
- Theme toggle: present in desktop and mobile shells.
- Internationalization: required for schedule-center business copy.
- Locales shown in prototype: `zh-CN`, `en`.
- Default locale: `zh-CN`.
- Locale switcher: present and verified.

## Out Of Scope Items

- SSE, maps, route visualization, and hard dependency on ETA.
- Direct Provider calls from the browser or schedule-page rendering path.
- Automatic device release or availability change when a package is delivered.
- Migration-time network calls or automatic historical subscription during schema migration.
- Real Provider calls in CI and ordinary unit tests.

## Required Tests

- Migration forward and rollback validation for all new tables, indexes, constraints, tenant columns, encryption columns, and `rental_device_shipment.delivery_id`.
- Domain and service tests for multi-package, multi-device, directions, idempotency, state mapping, trace ordering, masking, risk, throttle, retry, and tenant isolation.
- Provider adapter contract tests using fixtures for subscribe, query, callback authentication, malformed payloads, duplicate callbacks, and unknown status codes.
- Transaction tests proving Kuaidi100 failures cannot roll back a successful Xianyu shipment and proving Provider calls occur outside the shipment transaction.
- Worker concurrency tests for leases, retries, stale claims, duplicate processing, and cleanup.
- API authorization, validation, response-shape, and masked-secret tests.
- React component, interaction, responsive, theme, locale, multi-package drawer, state, and no-direct-Provider-request tests.
- End-to-end local-fixture flow from shipment to Delivery, Outbox, callback or query snapshot, schedule summary, trace detail, and risk.

## Open Risks

- Kuaidi100 callback signature and exact Java SDK behavior must be confirmed against the selected official SDK version before implementing the adapter.
- Existing Xianyu carrier codes and phone-number requirements need fixture coverage before enabling subscription per carrier.
- Historical shipment data quality may require a dry-run report and bounded backfill batches.
- Outbox and Inbox retention periods, retry ceilings, and operational alert thresholds need production defaults during the operations slice.
