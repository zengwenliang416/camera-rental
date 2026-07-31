# Task Brief: 004-schedule-tracking

## Goal

Schedule operators can view local single-package or multi-package summaries,
open complete traces, request asynchronous refresh, and see logistics risks.

## Parent Artifacts

- `openspec/changes/add-rental-logistics-tracking/requirements.md`
- `openspec/changes/add-rental-logistics-tracking/acceptance.md`
- `openspec/changes/add-rental-logistics-tracking/prototype/handoff.md`

## Vertical Slice

From the visible schedule order IDs through one batch summary request, shared
tracking state, compact schedule presentation, detail API, trace drawer, refresh
command, and risk presentation, deliver the approved responsive experience.

## In Scope

- Backend summary/detail/refresh/risk APIs and VOs.
- Frontend API types, summary model, visible 60-second polling, local state,
  schedule integration, multi-package drawer, timeline, refresh, risk extension,
  theme/locale copy, responsive behavior, and tests.

## Out Of Scope

- No SSE, direct Kuaidi100 call, frontend status mapping/throttle/risk authority,
  customer PII, map, or device availability transition.

## Files Allowed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz`
- `camera-rental-schedule-center`
- `openspec/changes/add-rental-logistics-tracking`

## Interfaces / Seams

- Tracking batch, detail, and refresh APIs; `trackingByOrderId`; reusable
  summary, drawer, timeline, status, refresh, risk, and polling components.

## Components To Create

- Delivery tracking controller/VO/query service, `DeliveryTrackingSummary`,
  `DeliveryTrackingDrawer`, `DeliveryTrackingTimeline`,
  `DeliveryTrackingStatusBadge`, polling/refresh hooks, and risk presentation.

## Components To Reuse

- `SchedulePage`, `ScheduleDeviceTable`, `ExceptionsPage`, `StatusBadge`,
  `DetailDrawerShell`, `EmptyState`, `PermissionAwareAction`, API client,
  preference context, semantic tokens, and responsive shell.

## Components To Extract

- Grouping/summarizing packages, localized status presentation, business-time
  formatting, visibility-aware polling, stale-result suppression, and refresh
  reason presentation.

## API / Data Flow Contracts

- Batch summaries use order IDs once per visible window; detail loads on demand;
  refresh enqueues and returns accepted/reason/nextAllowedAt immediately.

## State / Error / Empty / Loading Behavior

- Loading: schedule remains usable while logistics summary or detail loads.
- Empty: unshipped orders show no tracked package without false errors.
- Error: partial summary failures preserve schedule and safe retry.
- Disabled: mapping-required, Provider-disabled, stale, and throttled are explicit.
- Permission: no logistics request or detail is exposed without permission.

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='*TrackingQuery*,*LogisticsRisk*' test`
- `pnpm test`
- `pnpm lint`
- `pnpm build`
- `git diff --check`

## Stop Conditions

- Scope lock mismatch.
- Missing product, architecture, data-flow, or component decision.
- Component duplication that should be extracted.
- The frontend would need Provider credentials, raw errors, or authoritative
  risk/throttle logic.
- Complete trace data would be copied into every schedule block.

## Unsafe Assumptions

- Do not assume all orders have Delivery records or all package details load.
- Do not assume browser visibility, network timing, or response order.
- Do not infer device availability from `DELIVERED`.
