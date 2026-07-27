# Prototype Handoff: xian-guanjia-order-dispatch-ship-v1

## Approved Branch Variant

- Branch: `ui-html`
- Variant: `balanced` (populated default; both light+dark; both admin panel + staff screen reviewable)
- Approval status: **APPROVED** — user asked to start development after confirming
  the required flow: capture courier waybill code + machine serial/device QR,
  search pending XianGuanJia orders, confirm the device-order association, then
  click ship through XianGuanJia's order ship API.

## Screens Or Flows

- `admin-ship-panel` — Admin captures/uploads courier waybill code image → scans
  or enters machine serial/device QR → searches "待发货" orders → confirms
  selected order + device association → submit → success (masked waybill,
  DISPATCHED) or typed failure.
- `staff-ship-screen` — Staff captures courier waybill code image → scans device
  signed QR → `resolve-qr` → searches pending shipment orders → confirms the
  selected order-device association → submit (same endpoint, `source=STAFF`).
- Data flows mapped: `FLOW-XIANYU-PENDING-ORDER-SEARCH`,
  `FLOW-XIANYU-ORDER-SHIP`, `FLOW-SHIPMENT-OCR`,
  `FLOW-DEVICE-CODE-RESOLVE-FOR-SHIP`.

## Components To Create

- Admin: `RentalXianyuOrderShipPanel` domain component (dialog: courier-code
  upload/OCR + device-code resolve + pending-order search + association preview
  + express selector + confirm-submit).
- Staff: `RentalStaffShip` screen + `src/api/rental` module (staff has neither
  today — verified).
- Backend: `XianyuWriteClient` + `XianyuWriteEndpoint` (NEW; reuses `XianyuRequestSigner` + `XianyuCanonicalJson` + shared OkHttp/ObjectMapper/Clock; does NOT touch `XianyuReadClient`/`XianyuReadEndpoint` read-only allowlist).
- Backend: `XianyuOrderShipService` (write orchestration), `ShipmentOcrService` + `integration/ocr` adapter (multimodal OCR only).
- Database: `rental_device_shipment` table + DO (`extends TenantBaseDO`, `@TableName`/`@TableId`/`@KeySequence`, mapper `extends BaseMapperX`, soft-delete via `deleted` bit).
- API module: `ErrorCodeConstants` additions at `1_040_001_020+` (`XIANYU_WRITE_DISABLED`, `XIANYU_SHIP_IDEMPOTENT_KEY_REUSED`, `XIANYU_SHIP_REMOTE_ERROR`).

## Components To Reuse

- `RentalDeviceOpsService.dispatch` (ASSIGNED→DISPATCHED transition after
  successful ship).
- `RentalDeviceQrCodec` + existing `/rental/device/get-qr` +
  `/rental/device/resolve-qr` endpoints (admin/staff device code resolution; no
  duplicate decode).
- `XianyuRequestSigner` + `XianyuCanonicalJson` + shared `OkHttpClient`/`ObjectMapper`/`Clock` (reused by the new write client).
- Existing express-company list (`/api/open/express/companies`) as controlled vocabulary.
- Admin: existing Element Plus `dialog`/`form`/upload components (`components/UploadFile`), `request` from `@/config/axios`, `v-hasPermi`.
- Staff: existing `http` interceptor, `useAccess().hasAccessByCodes`, `menu.json` menu-visibility model, `uni.scanCode`.
- Existing error code `XIANYU_SHOP_AUTHORIZATION_INVALID` (`1_040_001_006`) reused for shop-not-authorized (no duplicate code).
- `system_menu` `INSERT ... SELECT ... WHERE NOT EXISTS` idempotent template (migrations `20260724_005` / `019`).

## Extraction Targets

- `ShipmentOcrService` + adapter extracted under `integration/ocr` (not in `integration/xianyu/client`), reusable by future OCR needs.
- Shared typed `api/rental/xianyu` methods (`searchPendingShipOrders`,
  `shipXianyuOrder`, `ocrShipment`) on admin; `api/rental/*` module on staff
  (new).

## API Contracts

- `GET /admin-api/rental/xianyu/order/pending-ship/page` — query pending
  shipment orders by order number, product keyword, buyer nickname, or masked
  contact keyword; → candidate order list for manual confirmation.
- `POST /admin-api/rental/xianyu/order/ship` — body: `channelOrderId`,
  `deviceId` or `deviceNo`, `idempotencyKey`, `expressCode`, `expressName`,
  `waybillNo`, `source` (`ADMIN`|`STAFF`); → masked ship result.
- `POST /admin-api/rental/xianyu/order/ship/ocr` — multipart courier-label photo or courier-app one-dimensional barcode screenshot → `{waybillNo, expressName, confidence, extractionSource}` (no persistence, no third-party write).
- `POST https://open.goofish.pro/api/open/order/ship` — per `docs/integrations/xianyu/` (`order_no`, `waybill_no`, `express_code`, `express_name`, optional sender fields); same MD5 self-developed-app signing contract as read endpoints.

## Data Flows

- `FLOW-XIANYU-PENDING-ORDER-SEARCH`: admin/staff keyword search → `/rental/xianyu/order/pending-ship/page` → candidate pending-dispatch channel orders (no persistence, no third-party write).
- `FLOW-XIANYU-ORDER-SHIP`: admin/staff confirm courier waybill + device + selected pending order → `/rental/xianyu/order/ship` (carries `source`) → check `rental.xianyu.write-enabled` + shop authorization + pending-order status + device shippability → `XianyuWriteClient.orderShip` → on success persist device-order binding, `rental_device_shipment`, and assignment `ASSIGNED→DISPATCHED` → masked result. On failure: typed error, no local mutation.
- `FLOW-SHIPMENT-OCR`: admin/staff upload a courier-label photo, courier QR, or courier-app one-dimensional barcode screenshot → `/rental/xianyu/order/ship/ocr` → `ShipmentOcrService` → `yudao.ai.mimo` multimodal (base64 `image_url`) → structured barcode/OCR draft (no persistence, no third-party write).
- `FLOW-DEVICE-CODE-RESOLVE-FOR-SHIP`: admin/staff scan or paste device QR/serial-number code → existing `/rental/device/resolve-qr` → resolved physical device and shippability summary (no persistence, no third-party write).

## State Behavior

- Loading: spinner in dialog / staff body while loading assignable devices or parsing QR.
- Empty: "没有匹配的待发货订单" / "未识别到有效设备二维码或序列号".
- Error (ship-failed): `XIANYU_SHIP_REMOTE_ERROR` example "express_name is not set" — local state unchanged, retry with same idempotency key.
- Disabled (write-disabled): `XIANYU_WRITE_DISABLED` — `rental.xianyu.write-enabled=false`.
- Permission: no `rental:xianyu:ship` code → `@PreAuthorize` rejects.
- Shop-not-authorized: reuses `XIANYU_SHOP_AUTHORIZATION_INVALID`.
- OCR-failed: degrades to empty draft; manual entry always available (never blocks shipment).
- Scan-rejected (staff only): `RENTAL_DEVICE_QR_INVALID` / `RENTAL_DEVICE_NOT_EXISTS`.
- Ship-success: masked waybill (e.g. `SF****4432`), `source` shown, assignment `DISPATCHED`.

## Theme And Locale Policy

- Theme support: `light-dark` (approved).
- Theme modes shown in prototype: light + dark via 🌓 toggle (`data-theme` on `<body>`; both admin and staff buttons wired).
- Theme toggle: `data-specnav-theme-control` present on both surfaces; manifest `toggle_in_prototype=true`.
- Internationalization: enabled; locales `zh-CN`, `en`; default `zh-CN`.
- Locales shown in prototype: `zh-CN` only (manifest `locale_switch_in_prototype=false` — no in-prototype locale switcher).
- Locale switcher: none in prototype (per manifest).

## Out Of Scope Items

- Other XianGuanJia write operations (改价 / 同意·拒绝退款 / 商品上下架 / 库存编辑 / 商品创建·编辑).
- Automatic / scheduled shipment (operator-triggered only).
- Automatic retry (operator re-submits with same idempotency key).
- OCR beyond waybill + express company (no recipient PII).
- Storing shipment photos as authoritative (OCR-only; authoritative trace is the shipment record + XianGuanJia ship response).
- Customer-facing surfaces in `uniapp` / `web` (customer "已发货" / 物流展示 is a separate future change).
- Any modification to the frozen v1 read-only change (`xian-guanjia-data-integration-v1`).

## Required Tests

- `XianyuOrderShipService` unit: write-disabled refusal, shop-not-authorized refusal (reuses existing error code), idempotent replay (same key returns first result, no second XianGuanJia call), ship-then-local ordering (no local mutation on channel failure), unique-waybill rejection, cross-tenant rejection — covering `source=ADMIN` and `source=STAFF`.
- `ShipmentOcrService` unit: pinned multimodal prompt + base64 `image_url` shape against a mocked OpenAI-compatible endpoint; fixture for a courier-app one-dimensional barcode screenshot with visible `SF...` waybill number; degraded empty-draft on transport error.
- `XianyuWriteClient` unit: canonical-JSON bytes + MD5 signing match the read-endpoint contract; only `/api/open/order/ship` is callable.
- Admin sensory: dialog submit disabled until confirm; all typed failure states shown in story fixtures.
- Staff static: `pages-rental/ship` builds for H5 + WeChat Mini Program (CLAUDE.md §16); scan-rejected, empty search, stale-order, write-disabled, shop-not-authorized, OCR-failed, ship-failed states visible.
- Redteam: dual-tenant ship probe (admin + staff), write-disabled probe, shop-not-authorized probe (none emit a XianGuanJia write request).
- Facticity: live `/api/open/order/ship` request/response contract quoted in change design; implemented body shape matches docs exactly.
- Static: `v1` change assertions not mutated.

## Open Risks

- **Direct verification basis**: verifier-report.json records direct Codex
  inspection of the static ui-html prototype, including admin Web upload-image
  shipment, staff scan shipment, confirm-before-submit, typed failures, theme
  controls, and shipment success masking.
- **User approval basis**: the user asked to start development after explicitly
  adding that the Web admin must support uploading an image for shipment,
  clarifying the courier input is a one-dimensional barcode screenshot with
  visible waybill number, and correcting the production flow to capture courier
  code + machine QR first, then search pending orders, bind, and ship.
- **OCR model is `*-free`**: `mimo-v2.5-free` stability/quota unguaranteed; mitigated by OCR-failure → manual-entry degradation (never blocks shipment).
- **QR signing default off**: `rental.device.qr-secret` empty by default → unsigned local fallback; production MUST set `RENTAL_DEVICE_QR_SECRET` to enforce signed QRs (existing behavior, not a new risk, but a deployment precondition).
- **dispatch ↔ ship sequencing**: existing `RentalDeviceOpsService.dispatch` flips `DISPATCHED` without calling XianGuanJia; this change invokes it only after XianGuanJia ship succeeds, while legacy direct dispatch remains a separate operational concern.
