# Task Report: 001-shipping-workbench

## Status

DONE

## Files Changed

- `camera-rental-schedule-center/src/App.tsx`
- `camera-rental-schedule-center/src/components/Header.tsx`
- `camera-rental-schedule-center/src/components/QuickBindingView.tsx`
- `camera-rental-schedule-center/src/components/QuickBindingModal.tsx`
- `camera-rental-schedule-center/src/features/shipping/**`
- `camera-rental-schedule-center/src/api/mappers.test.ts`
- Rental module management order controller, VOs, services, mapper, and focused tests.
- `docs/integrations/xianyu/order-sync.md`
- `docs/integrations/xianyu/field-mapping.md`

## What Changed

- Replaced duplicated shipping page/modal implementations with thin entries and
  one feature workbench.
- Preserved the `waybill -> device -> pending-order search -> confirm binding
  -> ship` sequence and the existing server-authoritative command.
- Extended authorized management order responses to return complete receiver
  name, mobile, address, and seller remark while excluding raw payloads, goods
  blobs, and payment numbers.
- Extended pending-shipment search to match order number, receiver name, or
  exact full receiver mobile and return complete verification fields.
- Updated the shipping UI to use the complete search contract and removed the
  former backend-capability warning.

## TDD Evidence

- Added failing backend tests before implementation for complete management
  customer fields, complete pending-shipment fields, and the three keyword
  predicates.
- Added frontend read-model assertions for the complete short-lived shipment
  candidate.
- Retained tests proving ordinary dashboard mapping remains masked and private
  shipment data is not persisted in the global order model.

## Verification Commands

- `mvn -o -pl yudao-module-rental/yudao-module-rental-biz -Dtest=XianyuOrderAdminServiceTest,XianyuOrderShipServiceTest,XianyuOrderMapperTest test`
- `mvn -o -pl yudao-module-rental/yudao-module-rental-biz -am test`
- `bun run test`
- `bun run lint`
- `bun run build`
- `git diff --check`
- Production source line-ceiling scan.

## Concerns

- No live production customer record was queried during this task.
- Real shipment submission, production deployment, and third-party writes were
  intentionally not executed.
- Existing complete receiver snapshots are required for name/mobile lookup;
  historical rows without those persisted columns remain searchable by order
  number only until the normal detail backfill populates them.

## Scope Deviations

- The user explicitly expanded the approved frontend slice to the existing
  rental backend management order query contract. Scope and requirements were
  updated before production code edits.

## Follow-up Needed

- Verify one controlled authorized order-number, receiver-name, and full-phone
  query against a non-production or approved production session before release.
- Continue the remaining schedule-center route redesign as separate vertical
  slices; this task only completes the shipping workbench slice.
