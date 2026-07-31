# Task Report: 004-schedule-tracking

## Status

DONE

## Files Changed

- Delivery tracking admin controller, request/response VOs, local query,
  asynchronous refresh, and server-derived risk services.
- Schedule-center tracking API types, shared tracking provider/model/copy,
  visibility-aware polling, summary panel, detail drawer, timeline, schedule
  table integration, and exception-center integration.
- Focused backend and frontend tests plus task evidence for slice 004.

## What Changed

- Added one tenant-scoped batch summary endpoint for the visible rental-order
  IDs, one on-demand Delivery detail endpoint, and one asynchronous refresh
  command using the existing `rental:delivery:tracking` permission.
- Kept complete traces out of schedule summaries; details and timeline data load
  only after an operator opens a package.
- Added stable refresh results for accepted, throttled, mapping-required, and
  Provider-disabled states without making a synchronous Provider query.
- Added server-derived stale, exception, mapping, and return-risk presentation
  without changing order, schedule, assignment, or device lifecycle state.
- Added shared frontend tracking state that batch-loads local summaries, ignores
  stale responses, polls every 60 seconds only while visible, pauses while
  hidden, and refreshes immediately when the page becomes visible again.
- Rendered single-package and multi-package summaries in the schedule center,
  exposed a responsive detail drawer with devices, risks, and full trace
  timeline, and merged logistics risks into the exception center.
- Added zh-CN/en copy and semantic status presentation that does not rely on
  color alone.

## TDD Evidence

- `RentalDeliveryTrackingQueryServiceTest` covers tenant-scoped batch summaries,
  package grouping, full on-demand detail, masked waybills, traces, and stale
  state.
- `RentalDeliveryTrackingRefreshServiceTest` covers accepted asynchronous
  enqueue, throttle with `nextAllowedAt`, mapping-required, and disabled
  Provider outcomes.
- `RentalDeliveryTrackingControllerTest` covers the tracking permission and
  response boundary.
- `RentalLogisticsRiskServiceTest` covers server-derived logistics risk without
  device lifecycle mutation.
- Frontend tracking model tests cover deterministic single/multi-package
  mapping, nullable masked waybills, normalized statuses, risks, devices, and
  traces.
- Polling tests prove the 60-second visible-only timer, hidden pause, and
  immediate visible-resume refresh.
- Exception tests prove tracking risks merge into the exception center with
  tracking-specific actions.
- Provider behavior regression proves opening a Delivery detail keeps the
  loader callback stable, performs one detail request, and renders the trace.
- Schedule table regression proves the device-detail and tracking actions are
  sibling buttons rather than invalid nested interactive controls.

## Verification Commands

- `cd camera-rental-server && mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='*TrackingQuery*,*TrackingRefresh*,*TrackingController*,*LogisticsRisk*' -Dsurefire.failIfNoSpecifiedTests=false test`
- `cd camera-rental-schedule-center && pnpm test`
- `cd camera-rental-schedule-center && pnpm lint`
- `cd camera-rental-schedule-center && pnpm build`
- `git diff --check`
- Credential literal scan across the working tree excluding generated
  dependencies and build output.
- Browser sensory matrix using a local fictional API fixture for normal,
  delivered, multi-package, risk, async refresh, exception-center, loading,
  empty, permission, and failure states across light/dark, zh-CN/en, desktop,
  and 390px layouts.

## Concerns

- SpecNav entry remains blocked by `git-baseline:tasks-not-tracked`; no staging
  or commit was performed.

## Scope Deviations

- None recorded.

## Follow-up Needed

- Independent specification and quality review must verify A7, A8, A9, A10,
  A11, and A16.
- Final six-domain verification must retain the no-Provider-call and no-device-
  lifecycle-mutation boundaries.

## Adjudication

Implementation and direct verification are complete. Independent review files
remain authoritative for approval.
