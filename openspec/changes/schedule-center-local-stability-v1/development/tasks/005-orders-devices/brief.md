# Task Brief: 005-orders-devices

## Goal

An operator can filter and inspect rental orders and registered physical
devices through a consistent responsive list/detail system, then enter only the
existing permitted scheduling, shipping, detail, or return workflow.

## Parent Artifacts

- `openspec/changes/schedule-center-local-stability-v1/requirements.md`
- `openspec/changes/schedule-center-local-stability-v1/acceptance.md`
- `openspec/changes/schedule-center-local-stability-v1/prototype/handoff.md`

## Vertical Slice

Open the order or device route, apply safe local filters, inspect stable
identifiers, status, server-derived dates and registration boundaries, then
emit a permitted existing workflow intent without recalculating business truth.

## In Scope

- Split order filtering, status/channel presentation, result cards, and page
  coordination into an orders feature.
- Split device model selection, status/search filtering, device cards, and page
  coordination into a devices feature.
- Use shared filter, identifier, range, status, empty, and responsive list
  components.
- Remove direct return action from general order cards; route return work to
  existing server-authoritative operational surfaces.
- Make the registered-asset boundary explicit and avoid sequential-number
  inventory claims.
- Add localized light/dark copy and pure filter/read-model tests.

## Out Of Scope

- Complete device detail drawer and QR internals, authentication, exception
  handling, global context decomposition, API or database changes.
- New order status, device status, return endpoint, inventory import, or
  customer-data exposure.

## Files Allowed

- `camera-rental-schedule-center/package.json`
- `camera-rental-schedule-center/src/components/OrdersView.tsx`
- `camera-rental-schedule-center/src/components/DevicesView.tsx`
- `camera-rental-schedule-center/src/features/orders/**`
- `camera-rental-schedule-center/src/features/devices/DevicesPage.tsx`
- `camera-rental-schedule-center/src/features/devices/deviceModel.ts`
- `camera-rental-schedule-center/src/features/devices/deviceModel.test.ts`
- `camera-rental-schedule-center/src/features/devices/components/DeviceCard.tsx`
- `camera-rental-schedule-center/src/shared/ui/FilterToolbar.tsx`
- `camera-rental-schedule-center/src/shared/ui/IdentifierText.tsx`
- `camera-rental-schedule-center/src/shared/ui/ResponsiveDataList.tsx`
- `camera-rental-schedule-center/src/features/preferences/messages.ts`
- `camera-rental-schedule-center/src/index.css`

## Interfaces / Seams

- `useApp()` supplies mapped order/device records and intent callbacks.
- Feature read-model functions own filtering and view-only labels.
- Pages compose shared presentation and emit route/overlay intents only.

## Components To Create

- `OrdersPage`
- `OrderCard`
- `DevicesPage`
- `DeviceCard`
- `FilterToolbar`
- `IdentifierText`
- `ResponsiveDataList`

## Components To Reuse

- Existing mapped order/device models, semantic tokens, shared status badges,
  feature headers, empty states, date-range display, and navigation intents.

## Components To Extract

- Order and device filtering.
- Channel, lifecycle, and action-state presentation.
- Identifier and responsive list behavior.

## API / Data Flow Contracts

- Preserve current snapshot APIs and command entry points.
- Search fields are local view filters and never persist customer-private
  values.
- Server status, dates, assignment identifiers, and action flags remain
  authoritative.

## State / Error / Empty / Loading Behavior

- Loading: retain filters and current route.
- Empty: distinguish no registered data from no filtered matches.
- Error: rely on safe shared sync feedback.
- Disabled: explain incomplete internal detail or permission.
- Permission: hide or disable assignment, shipping, and device-detail intents
  according to existing permission checks.

## TDD Requirement

- Add focused tests for order/device filters, status tones, action readiness,
  and registered-asset boundary copy.

## Verification Commands

- `pnpm test`
- `pnpm lint`
- `pnpm build`
- `git diff --check`
- Production source line-ceiling scan.
- Browser checks for orders and devices at 1440, 768, 390, and 360 CSS pixels.

## Stop Conditions

- Scope lock mismatch.
- A page needs new server fields or a new mutation.
- Filtering would persist private data or recompute authoritative state.

## Unsafe Assumptions

- Registered devices equal the physical fleet.
- Unit numbers are continuous.
- Every non-complete order permits an operation.
- A client-side status label can override backend action flags.
