# Requirements: xian-guanjia-order-dispatch-ship-v1

## Summary

Add an authorized scan/search/bind/ship workflow that ships a single
XianGuanJia (闲管家) pending-dispatch channel order from admin Web and the
warehouse staff app:

1. Operator captures two codes first: the courier waybill code (a courier label
   QR/one-dimensional barcode photo or a courier mini-program barcode
   screenshot) and the physical device QR/serial-number code.
2. The system extracts the waybill number + express company, resolves the
   physical device, then lets the operator search the XianGuanJia "待发货" order
   pool.
3. The operator confirms the matched order, confirms "this device belongs to
   this order", then clicks ship. The backend binds the device and order and
   calls XianGuanJia `/api/open/order/ship`.

The backend only persists the shipment record and durable device-order binding
after a successful XianGuanJia ship response. The resulting device assignment is
created or reused as `ASSIGNED`, then advanced to `DISPATCHED` in the same
successful flow. Writes are off by default and gated by an explicit
write-enable switch plus per-shop authorization re-validation.

## Users & Actors

- Warehouse operator / equipment operator (staff app): captures the courier
  waybill code image and the device QR/serial-number code, searches pending
  XianGuanJia orders, confirms the order-device match, triggers shipment, and
  reads the result on the staff mobile screen.
- Rental operator (admin web): performs the same code-first flow in the admin
  shipment panel: courier code upload, device QR/serial resolution, pending
  order search, match confirmation, and ship.
- Admin (integration operator): turns on the write-enable switch and confirms
  the target shop is still authorized before enabling shipment on that shop.
- Backend scheduler: not involved; shipment is operator-triggered only.

## In Scope

- New admin HTTP endpoint `GET /admin-api/rental/xianyu/order/pending-ship/page`
  that searches the authorized shop's XianGuanJia pending-dispatch order pool by
  order number, product keyword, buyer nickname, or masked contact keyword. It
  returns only the candidate fields needed for operator confirmation; it does
  not expose full phone numbers or addresses.
- New admin HTTP endpoint `POST /admin-api/rental/xianyu/order/ship` that takes
  a channel order id, a device id or resolved device number, an idempotency key,
  an express code, an express name, a waybill number, and a source
  (`ADMIN`/`STAFF`). It validates that the order is still pending shipment,
  validates the device is shippable, creates or reuses the local
  order-device assignment, and performs the XianGuanJia write call.
- New admin HTTP endpoint `POST /admin-api/rental/xianyu/order/ship/ocr` that
  accepts an uploaded shipment photo or courier-app barcode screenshot and
  returns a structured best-effort extraction result (waybill number, express
  company code/name, confidence/source) without performing any third-party
  write.
- New backend OCR client that calls the OpenAI-compatible multimodal endpoint
  declared under `yudao.ai.shipment-ocr` (base-url + api-key from config), with the
  image sent as a base64 `image_url` content part and a fixed extraction prompt
  that asks for `waybill_no` / `express_name` only. The prompt must handle two
  common operator inputs: physical waybill photos and courier mini-program
  screenshots containing a one-dimensional barcode (for example Code128) plus a
  visible waybill number such as `SF5113560342626`. If both barcode-like content
  and text are visible, the service prefers the barcode/large printed waybill
  number and ignores unrelated order numbers in the background list.
- New backend write client (`XianyuWriteClient`) — NOT an addition to the
  existing `XianyuReadClient` / `XianyuReadEndpoint`, which are an explicit
  read-only closed allowlist ("outbound allowlist for the V1 XianGuanJia read
  client"). The write client reuses the same `XianyuRequestSigner`,
  `XianyuCanonicalJson` serializer, `OkHttpClient`, `ObjectMapper`, and `Clock`
  plumbing as the read client, but routes through a new
  `XianyuWriteEndpoint` enum listing only `/api/open/order/ship`. This keeps the
  read-only allowlist closed and the write path auditable per CLAUDE.md §21.2/§21.3.
- New error code constants in `ErrorCodeConstants` (range `1_040_001_020+`,
  continuing the existing XianGuanJia `1_040_001_xxx` segment):
  `XIANYU_WRITE_DISABLED`, `XIANYU_SHIP_IDEMPOTENT_KEY_REUSED`,
  `XIANYU_SHIP_REMOTE_ERROR`. Shop-authorization failure reuses the existing
  `XIANYU_SHOP_AUTHORIZATION_INVALID` (`1_040_001_006`), not a new code.
- Write-enable gate: a new `rental.xianyu.write-enabled` flag (env
  `XGJ_WRITE_ENABLED`, default `false`) plus per-shop authorization status
  re-validation before any ship call. The endpoint refuses with
  `XIANYU_WRITE_DISABLED` when the switch is off and
  `XIANYU_SHOP_AUTHORIZATION_INVALID` when the shop is not currently authorized.
- Idempotent shipment coordination: the ship endpoint requires a client
  idempotency key bound to the assignment. Repeating the same key returns the
  first successful result and does not call XianGuanJia a second time. A
  different key on the same assignment is rejected.
- Local state transition: on XianGuanJia ship success only, the selected device
  is durably linked to the selected channel order through
  `rental_device_assignment` and that assignment advances
  `ASSIGNED -> DISPATCHED` (reusing the existing
  `RentalDeviceOpsService.dispatch` lifecycle). On XianGuanJia failure no new
  durable binding, shipment row, or assignment state change is committed, and
  the failure reason is returned for retry.
- New persistent shipment record under the rental domain capturing channel
  order id, assignment id, device id, waybill number, express code/name, ship
  request body hash, ship response code/msg, the OCR-confirmed flag, source, and
  timestamps. The waybill number is stored in full internally and masked in
  normal kanban and export views per CLAUDE.md §17.
- Admin UI: a shipment panel that can be opened from the XianGuanJia shipment
  entry or an order row. The panel first accepts the courier waybill code image
  and the device QR/serial-number code, shows the extracted waybill/device
  draft, lets the operator search and select a "待发货" XianGuanJia order, then
  requires explicit confirmation of the device-order association before enabling
  the ship button.
- Staff app (`camera-rental-staff`) shipment screen: capture/scan the courier
  waybill code image, scan the device's signed QR (reusing
  `RentalDeviceQrCodec`), search pending XianGuanJia orders, show candidate
  orders for manual selection, confirm the device-order binding, and submit
  ship via the same `/rental/xianyu/order/ship` endpoint as admin. Scan
  validation, pending-order empty results, write-disabled, shop-not-authorized,
  OCR-failed, ship-failed, and cross-tenant states are first-class staff feedback
  per CLAUDE.md §13.
- QR precondition: a device is staff-shippable only after it has a printed QR
  produced by the existing `GET /admin-api/rental/device/get-qr` endpoint
  (returns the printable `CRD1|{deviceNo}|{modelCode}|{sig16}` `payload` via
  `RentalDeviceQrCodec`). Devices whose QR fails to decode are rejected at staff
  scan with `RENTAL_DEVICE_QR_INVALID` / `RENTAL_DEVICE_NOT_EXISTS`. This change
  reuses the existing QR generation and resolution endpoints and does not build a
  new QR pipeline. Note on signing: `RentalDeviceQrCodec` enforces the HMAC
  signature only when `rental.device.qr-secret` is configured; when the secret
  is empty (local/dev default, per `RentalDeviceProperties.isQrSigned()`) the
  codec accepts unsigned payloads. Production deployments MUST set
  `RENTAL_DEVICE_QR_SECRET` to enforce signed QRs; the unsigned local mode is the
  existing behavior and is not a new risk introduced by this change.
- Permission model: reuse a single backend permission code `rental:xianyu:ship`
  for both admin and staff, enforced server-side by
  `@PreAuthorize("@ss.hasPermission('rental:xianyu:ship')")` on the ship
  endpoint (and `rental:xianyu:ship:ocr` on the OCR endpoint) — matching the
  existing `rental:xianyu:query` / `:sync` / `:raw` / `:replay` convention.
  No new role is created; the code is assigned to whichever admin/staff role
  performs warehouse ops through normal role management. Two new `system_menu`
  rows (permissions, type=3, parent=7001 "闲管家集成状态") seeded by an
  incremental migration under `rental` using the existing
  `INSERT ... SELECT ... WHERE NOT EXISTS` idempotent template (column set
  matching `20260724_005_rental_admin_menus.sql`); menu ids `7070` and `7071`
  (existing rentals reach `7060`). The staff `menu.json` gets a new entry
  pointing at the shipment screen with `permission: "rental:xianyu:ship"`,
  filtered by the existing `useAccess().hasAccessByCodes` (matching the existing
  `system:user:list`-style entries).
- Reuse of the existing express-company list (`/api/open/express/companies`) as
  the controlled vocabulary for express code/name; OCR fills names but the
  submit validates against the express-company list.
- Pending-order search must only list orders whose channel status is eligible
  for XianGuanJia shipment. The backend rechecks that status at ship time, so a
  stale UI search result cannot ship an already shipped/canceled order.
- Audit logging of every ship write call (operator, shop, order, waybill masked
  in the log, response code/msg, source = admin or staff), keeping CLAUDE.md
  §17 redaction rules.

## Out of Scope

- Any other XianGuanJia write operation (改价, 同意/拒绝退款, 商品上下架,
  库存编辑, 商品创建/编辑). Only order ship is enabled.
- Automatic / scheduled shipment. Shipment is strictly operator-triggered.
- Automatic retry of failed ship calls. The operator re-submits with the same
  idempotency key to retry.
- Barcode/OCR extraction beyond waybill number and express company name. The
  extractor does not read recipient address, name, or any PII.
- Storing shipment photos as authoritative documents. The photo is uploaded for
  OCR only and may be discarded after OCR; the authoritative trace is the
  shipment record plus the XianGuanJia ship response.
- Customer-facing surfaces. The admin and staff flows do not surface in
  `uniapp` or `web` clients. Customer "已发货" / 物流展示 is out of this change
  and will be a separate change if needed.
- Editing the v1 read-only change. The read-only ingestion change
  `xian-guanjia-data-integration-v1` stays frozen; this change adds a write
  capability next to it without modifying its acceptance assertions.

## UI Design Impact

- Foundation spec: `openspec/specs/ui-design/design.md`
- New admin shipment panel reuses existing Element Plus dialogs, form items,
  upload component, status tags, and amount/identifier formatters. No new design
  tokens; follows the shared semantic colors and typography in the foundation
  spec.
- New staff shipment screen follows `camera-rental-staff` uni-app conventions:
  scan-first navigation, compact confirm form, mobile-network-aware upload, and
  platform-safe UI (no DOM-only APIs) per CLAUDE.md §13.
- Loading, empty, permission-denied, write-disabled, shop-not-authorized,
  OCR-failed, ship-failed, and scan-rejected states are first-class feedback
  states in both surfaces per the component-architecture foundation spec.

## Theme & Locale Capability Impact

- Theme support: `light-dark` on admin; staff uni-app inherits its existing
  platform theme handling (no new theme tokens on either surface).
- Theme toggle policy: show existing admin toggle; no new toggle.
- Internationalization: `enabled` (admin ships en + zh-CN; staff follows its
  existing locale handling).
- Supported locales: `en`, `zh-CN`.
- Default locale: `zh-CN`.
- Prototype coverage: prototype must show the admin `zh-CN` light-theme shipment
  panel and the staff `zh-CN` shipment screen in both the empty/scan-pending
  state and the OCR-confirmed-ready-to-submit state.

## Architecture & Database Impact

- Foundation spec: `openspec/specs/system-architecture/design.md`
- New backend write client `XianyuWriteClient` + `XianyuWriteEndpoint` enum
  (reusing `XianyuRequestSigner` + `XianyuCanonicalJson` + shared `OkHttpClient`/
  `ObjectMapper`/`Clock`); the existing `XianyuReadClient` / `XianyuReadEndpoint`
  read-only allowlist is NOT modified. A single write method `orderShip(...)`
  is used only by the ship service.
- New shipment persistence in the rental domain (`rental_device_shipment` table)
  modeled after `RentalDeviceAssignmentDO`: `extends TenantBaseDO`,
  `@TableName("rental_device_shipment")` / `@TableId` / `@KeySequence`, mapper
  `extends BaseMapperX<...>`, soft-delete via the inherited `deleted` bit column.
  Unique `(tenant_id, idempotency_key)` and unique
  `(tenant_id, channel_order_id, waybill_no, express_code)` prevent duplicate
  shipments of the same waybill for one order. A `source` column records whether
  the ship was triggered from admin or staff.
- No change to existing `rental_device_assignment` columns; the lifecycle
  transition reuses the existing `ASSIGNED -> DISPATCHED` semantics.
- New config block `yudao.ai.shipment-ocr` (base-url, api-key, model, enable) used only
  for OCR. No other AI model is touched.
- New config `rental.xianyu.write-enabled` (env `XGJ_WRITE_ENABLED`, default
  `false`); no change to the existing read credentials block.
- Incremental SQL migration only; additive columns/tables; no rewrite of
  historical migrations. A separate migration seeds the `rental:xianyu:ship`
  and `rental:xianyu:ship:ocr` permission menu rows into `system_menu` using the
  existing `INSERT ... SELECT ... WHERE NOT EXISTS` idempotent template (ids 7070,
  7071, parent 7001, type 3) so administrators can assign them to roles (CLAUDE.md
  §10 incremental-SQL rule).

## Frontend-Backend Data Flow Impact

- Foundation spec: `openspec/specs/frontend-backend-data-flow/design.md`
- New flow `FLOW-XIANYU-PENDING-ORDER-SEARCH`:
  trigger = operator enters a keyword after courier/device codes are captured;
  API = `/rental/xianyu/order/pending-ship/page`;
  persistence = none;
  user result = candidate pending-dispatch channel orders for manual selection.
- New flow `FLOW-XIANYU-ORDER-SHIP`:
  trigger = operator confirms courier waybill + physical device + selected
  pending channel order, then clicks ship;
  entry UI = admin shipment panel or staff scan/search/bind shipment screen;
  API = `/rental/xianyu/order/ship` (same endpoint from both surfaces, with a
  `source` discriminator);
  persistence = device-order assignment + `rental_device_shipment` insert +
  assignment DISPATCHED update, only after XianGuanJia ship returns success;
  user result = success summary with masked waybill and channel response msg,
  or a typed failure (pending-order stale/ineligible, device not shippable,
  write-disabled, shop-not-authorized, OCR-no-match, ship validation error, ship
  transport error, scan-rejected, idempotency conflict).
- New flow `FLOW-SHIPMENT-OCR`:
  trigger = operator uploads a courier label photo or courier-app one-dimensional
  barcode screenshot in the panel or staff screen;
  API = `/rental/xianyu/order/ship/ocr`;
  persistence = none (ephemeral);
  user result = structured barcode/OCR draft the operator must confirm or edit.
  No third-party write is performed by the OCR flow.
- New flow `FLOW-DEVICE-CODE-RESOLVE-FOR-SHIP`:
  trigger = admin or staff scans/pastes the device QR or serial-number code on
  the shipment screen;
  API = existing `POST /admin-api/rental/device/resolve-qr` (reused);
  persistence = none;
  user result = resolved physical device and shippability summary, or
  scan-rejected (invalid QR / model mismatch / device not found).

## Component Architecture Impact

- Foundation spec: `openspec/specs/component-architecture/design.md`
- New admin page section `RentalXianyuOrderShip` (domain component) composing
  the courier-code upload/OCR widget, device-code scan/resolve widget, pending
  order search picker, express selector, order-device association preview, and
  confirm-submit. No new raw API client inside the component; it uses typed
  `api/rental/xianyu` methods.
- New staff shipment screen `RentalStaffShip` in `camera-rental-staff`: this is
  a NEW screen (the staff app currently has NO rental pages and no
  `api/rental` module — verified by inspection of `camera-rental-staff/src/api`
  and a repo-wide search for `resolve-qr`/`rental` in staff source). The new
  screen uses `uni.scanCode` to capture the device QR, accepts a courier
  waybill code image, calls the existing `/admin-api/rental/device/resolve-qr`,
  searches pending shipment orders, and shows a compact association confirm form
  (selected order, device, express picker, waybill input, submit). No raw API
  client — it uses a new
  `api/rental/*` module following the staff `api/*` convention plus the same
  `/admin-api/rental/xianyu/order/ship` endpoint as admin.
- New backend services: `XianyuOrderShipService` (write orchestration) and
  `ShipmentOcrService` (OCR call only). The OCR adapter is a thin
  OpenAI-compatible client under `integration/ocr` and is not mixed into the
  XianGuanJia client package.
- Reused: `RentalDeviceQrCodec` + the existing `resolve-qr` endpoint (staff scan
  resolution), `RentalDeviceOpsService.dispatch` (called after a successful ship
  to flip the assignment to DISPATCHED), `XianyuRequestSigner` +
  `XianyuCanonicalJson` + the shared OkHttp/ObjectMapper plumbing (reused by the
  new write client), the existing express-company list, existing Element Plus
  form and upload components (`components/UploadFile`) on admin, and the staff
  `http`/`useAccess`/`menu.json` conventions on staff.
- Forbidden dependencies: the new OCR adapter must not live inside the
  XianGuanJia `client` package; the write client must NOT be added to
  `XianyuReadClient`/`XianyuReadEndpoint` (read-only closed allowlist);
  shipments must not be issued from the customer `uniapp` or `web` clients; the
  ship service must not call any XianGuanJia endpoint other than
  `/api/open/order/ship`; the staff shipment screen must not embed
  device-availability or schedule logic (backend remains authoritative); no new
  role is created — `rental:xianyu:ship` is assigned to existing roles.

## Unresolved Gaps

- None. All product, architecture, data-flow, and component-boundary decisions
  are recorded above.
