# Task Brief: 001-shipping-workbench

## Goal

An authorized warehouse operator can complete the approved outbound review
flow with a coherent responsive UI while every business transition remains
server-authoritative.

## Parent Artifacts

- `openspec/changes/schedule-center-local-stability-v1/requirements.md`
- `openspec/changes/schedule-center-local-stability-v1/acceptance.md`
- `openspec/changes/schedule-center-local-stability-v1/prototype/handoff.md`

## Vertical Slice

Open the shipping module, review or edit a waybill draft, search and select an
AVAILABLE registered device, explicitly search pending orders by receiver name,
full phone, or order number, inspect all fields returned by the backend, review
readiness, and submit through the existing shipment command only when all gates
pass. Authorized management-order queries also return complete persisted
receiver snapshots and seller remarks.

## In Scope

- Reimplement `QuickBindingView` as a thin feature entry.
- Extract shipping state and pure search/readiness logic.
- Build the approved waybill, device, order, confirmation, and history panels.
- Preserve OCR as an editable draft and retain entered waybill data when order
  selection changes.
- Search devices by device ID, SN, model, or warehouse/location text.
- Search orders by complete order number, receiver name, or full receiver
  phone using ephemeral in-memory input.
- Show only backend-returned order fields and a safe incomplete-data state.
- Extend the existing management order response with complete receiver name,
  mobile, address, and seller remark under `rental:xianyu:query`.
- Extend the protected pending-shipment query to search order number, receiver
  name, or receiver mobile and return complete verification fields under
  `rental:xianyu:ship`.
- Continue excluding detail JSON, goods JSON, payment numbers, credentials, and
  signatures from management responses.
- Preserve existing permission, config, write-enabled, submit, and refresh
  behavior.

## Out Of Scope

- Database, API route, permission, authentication, tenant, or state-machine
  changes.
- Other schedule-center routes.
- New dependencies or prototype fixture data.
- Real shipment submission during automated or browser tests.

## Files Allowed

- `camera-rental-schedule-center/src/components/QuickBindingView.tsx`
- `camera-rental-schedule-center/src/components/QuickBindingModal.tsx`
- `camera-rental-schedule-center/src/components/Header.tsx`
- `camera-rental-schedule-center/src/App.tsx`
- `camera-rental-schedule-center/src/features/shipping/**`
- `camera-rental-schedule-center/src/api/mappers.ts`
- `camera-rental-schedule-center/src/api/mappers.test.ts`
- `camera-rental-schedule-center/src/types.ts`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/controller/admin/xianyu/XianyuOrderController.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/controller/admin/xianyu/vo/XianyuOrderRespVO.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/controller/admin/xianyu/vo/XianyuPendingShipOrderPageReqVO.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/controller/admin/xianyu/vo/XianyuPendingShipOrderRespVO.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/dal/mysql/xianyu/XianyuOrderMapper.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/admin/XianyuOrderAdminService.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/admin/XianyuOrderShipService.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/dal/mysql/xianyu/XianyuOrderMapperTest.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/service/admin/XianyuOrderAdminServiceTest.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/service/admin/XianyuOrderShipServiceTest.java`
- `docs/integrations/xianyu/order-sync.md`
- `docs/integrations/xianyu/field-mapping.md`

## Interfaces / Seams

- Existing `useApp()` supplies server records, permission checks, configuration,
  shipment command, preselected order intent, and device-detail intent.
- Existing `recognizeXianyuShipmentImage`, `resolveRentalDeviceQr`, and
  `fetchXianyuExpressCompanies` remain typed service seams.
- New presentational panels receive read models and intent callbacks only.

## Components To Create

- `ShippingWorkbench`
- `ShippingWorkflowStepper`
- `WaybillPanel`
- `DeviceSelectionPanel`
- `OrderSelectionPanel`
- `ShipmentDocket`
- `ShipmentHistory`

## Components To Reuse

- Existing `lucide-react` icons, API services, `AppContext`, `DeviceInstance`,
  `RentalOrder`, express-company vocabulary, and shipment command.

## Components To Extract

- `useShippingWorkbench` for UI/controller state and async drafts.
- `shippingModel.ts` for device/order search, readiness, and safe visible
  result derivation.

## API / Data Flow Contracts

- Preserve `/rental/xianyu/express-company/list`,
  `/rental/device/resolve-qr`, `/rental/xianyu/order/ship/ocr`, and
  `/rental/xianyu/order/ship`.
- Extend `/rental/xianyu/order/page` and
  `/rental/xianyu/order/pending-ship/page` in place; do not create a parallel
  private-data route.
- No direct XianGuanJia call from the browser.
- Search input and private result data never enter URL state, persistence,
  analytics, ordinary exports, fixtures, or client/server logs.
- Shipment submit remains non-optimistic and refreshes through the existing
  `bindDeviceWithOrderAndLogistics` command.

## State / Error / Empty / Loading Behavior

- Loading: panel-scoped progress while retaining current draft.
- Empty: distinguish no device match, no order match, and zero eligible order.
- Error: safe localized OCR or shipment error; no raw stack/transport text.
- Disabled: explain missing waybill, device, order, rental period, permission,
  configuration, or server write enablement.
- Permission: unmasked order details and submit intent are unavailable without
  the existing shipment permission.

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `bun run test`
- `bun run lint`
- `bun run build`
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -Dtest=XianyuOrderAdminServiceTest,XianyuOrderShipServiceTest,XianyuOrderMapperTest test`
- `mvn -pl yudao-module-rental -am test`
- Browser checks at 1440, 1087, 768, 390, and 360 CSS pixels.
- Source line ceiling and page-level overflow checks.

## Stop Conditions

- Scope lock mismatch.
- Missing product, architecture, data-flow, or component decision.
- Component duplication that should be extracted.
- Existing persisted receiver snapshots are unavailable or would require a
  persistent schema change.
- Existing command cannot preserve the approved waybill-first sequence.

## Unsafe Assumptions

- Do not assume every channel order is eligible merely because status is 12.
- Do not assume frontend masking or hiding is a security boundary; preserve
  backend permission and tenant checks.
- Do not assume prototype counts represent current production data.
- Do not infer receiver values from seller nickname or remarks.
- Do not expose raw detail/goods payloads or payment numbers merely because
  management customer fields are complete.
