# Task Report: 005-orders-devices

## Status

DONE

## Files Changed

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

## What Changed

- Replaced legacy order and device pages with thin compatibility entries and
  focused feature pages, read models, filters, and responsive cards.
- Order filtering uses status, channel, order number, and device model without
  searching or persisting customer-private values.
- Order cards consistently show masked customer snapshots, billable and
  occupied ranges, server-returned status, requirements, and bound device IDs.
- Removed the direct return mutation from general order cards. Return-ready
  records explain that the existing device return/inspection operational flow
  is required.
- Device cards use only management-returned records and identifiers. The page
  explicitly states that unit numbers may be discontinuous and never infers a
  sequential fleet total.
- Device-detail intents now require `rental:device:query`; non-idle devices
  without a concrete server date no longer claim immediate availability.
- Mapper-created availability and warehouse fragments are normalized before
  locale rendering, so populated English cards do not leak Chinese UI copy.
- Assignment, shipping, and device-detail intents remain permission gated and
  use the existing commands or overlays.
- Added complete light/dark-compatible `zh-CN` and `en` copy for both routes.

## TDD Evidence

- Added pure tests for order/device filtering, semantic status/channel tones,
  permission-aware action readiness, direct-return exclusion, and registered
  asset counts without sequential-number assumptions.
- The full schedule-center suite now contains 61 passing tests.

## Verification Commands

- `pnpm test`: 61 tests passed, 0 failed.
- `pnpm lint`: `tsc --noEmit` exited 0.
- `pnpm build`: Vite production build exited 0 with separate order and device
  route chunks.
- `git diff --check`: exited 0.
- Browser checks in `zh-CN` and `en`: both empty routes rendered complete
  localized copy, no raw translation keys, and no page-level overflow at the
  active 1280px viewport.

## Concerns

- The local management snapshot contains no order or device records, so
  populated cards and action intents could not be exercised in the browser
  without writing fixture data.
- The preference dictionary is 599 lines and remains below the hard ceiling.
  Its single responsibility is static locale lookup; task 007 removes the
  oversized mutable application context instead of expanding this dictionary.

## Scope Deviations

- The implementation remained inside the approved frontend files and did not
  add customer fields, APIs, mutations, inventory assumptions, or persistence.

## Follow-up Needed

- Run populated order/device browser scenarios and the full 360/390/768/1440
  matrix during final change-level verification.

## Adjudication

Independent spec and quality reviews are approved. The task-level SpecNav
contract returned `ok: true`.
