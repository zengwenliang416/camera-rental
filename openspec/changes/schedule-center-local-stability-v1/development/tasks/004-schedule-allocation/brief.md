# Task Brief: 004-schedule-allocation

## Goal

An operator can inspect a responsive SN-level schedule, distinguish billable
and occupied ranges, and confirm a physical-device assignment only after all
existing permission, rental-period, device, and server command gates pass.

## Parent Artifacts

- `openspec/changes/schedule-center-local-stability-v1/requirements.md`
- `openspec/changes/schedule-center-local-stability-v1/acceptance.md`
- `openspec/changes/schedule-center-local-stability-v1/prototype/handoff.md`

## Vertical Slice

Open the schedule route, select a registered model, filter schedule lanes,
inspect server-derived ranges and conflicts, open an order allocation dialog,
review provisional candidates, and submit the existing backend assignment
command without optimistic acceptance.

## In Scope

- Split the legacy Gantt page into schedule read-model, filters, legend,
  timeline, responsive table, and page coordinator.
- Label billable and occupied ranges in text and accessible legend content.
- Keep dense lanes inside an explicitly labeled horizontal scroller.
- Split allocation into a focus-managed dialog, requirement summary, device
  candidate list, progress, permission state, conflict guidance, and submit
  guard.
- Retain existing provisional recommendation behavior while describing it as
  non-authoritative.
- Add localized light/dark copy and focused model tests.

## Out Of Scope

- New scheduling algorithms, API routes, database changes, automatic
  assignment, optimistic assignment, or changes to occupancy semantics.
- Order list, device ledger, authentication, exceptions, shipping internals, or
  final global context decomposition.

## Files Allowed

- `camera-rental-schedule-center/package.json`
- `camera-rental-schedule-center/src/components/GanttScheduleView.tsx`
- `camera-rental-schedule-center/src/components/OrderAllocationModal.tsx`
- `camera-rental-schedule-center/src/features/schedule/**`
- `camera-rental-schedule-center/src/shared/ui/DateRangeDisplay.tsx`
- `camera-rental-schedule-center/src/shared/ui/BillableOccupiedRangeLegend.tsx`
- `camera-rental-schedule-center/src/shared/ui/ConfirmDialogShell.tsx`
- `camera-rental-schedule-center/src/features/preferences/messages.ts`
- `camera-rental-schedule-center/src/index.css`
- `camera-rental-schedule-center/src/lib/scheduleEngine.ts`

## Interfaces / Seams

- `useApp()` supplies mapped models, devices, schedules, orders, navigation,
  overlay selection, permission checks, and the existing assignment command.
- Pure schedule model functions own date-window generation, filtering, status
  presentation, and allocation progress.
- Presentational components receive read models and emit select, inspect,
  close, recommend, and confirm intents.

## Components To Create

- `SchedulePage`
- `ScheduleFilters`
- `ScheduleTimeline`
- `ScheduleDeviceTable`
- `AllocationDialog`
- `DateRangeDisplay`
- `BillableOccupiedRangeLegend`
- `ConfirmDialogShell`

## Components To Reuse

- Existing mapped device, schedule, and order models.
- Existing `recommendDevicesForOrder`, `checkDeviceAvailability`, assignment
  command, semantic tokens, shared badges, headers, and empty states.

## Components To Extract

- Schedule date window and filtering.
- Range/status presentation.
- Dialog focus and dismissal behavior.
- Allocation progress and submit readiness.

## API / Data Flow Contracts

- Preserve `GET /rental/device/page`, `GET /rental/schedule/page`, and
  `POST /rental/device/assign`.
- The frontend candidate calculation is review assistance only.
- A successful UI state appears only after the backend command resolves and the
  server snapshot refreshes.

## State / Error / Empty / Loading Behavior

- Loading: retain filters and selected model while the shared sync state runs.
- Empty: distinguish no registered model, no matching devices, and no schedule
  blocks.
- Error: use safe shared sync or command copy, never raw transport text.
- Disabled: explain missing rental detail, period, permission, or complete
  device selection.
- Permission: allocation confirmation is unavailable without
  `rental:device:assign`.

## TDD Requirement

- Add pure tests for date windows, filtering, range labels, allocation progress,
  and submit readiness before or alongside implementation.

## Verification Commands

- `pnpm test`
- `pnpm lint`
- `pnpm build`
- `git diff --check`
- Production source line-ceiling scan.
- Browser checks at 1440, 768, 390, and 360 CSS pixels.

## Stop Conditions

- Scope lock mismatch.
- Required behavior needs a new API or scheduling algorithm.
- A presentational component would import raw transport.
- Billable and occupied ranges cannot be represented from current server data.

## Unsafe Assumptions

- A client recommendation proves availability.
- Billable dates and occupied dates are interchangeable.
- A colored block is an accessible status label.
- Inclusive display dates can replace backend end-exclusive command dates.
