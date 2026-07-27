# Acceptance Criteria: xian-guanjia-order-dispatch-ship-v1

## User-Visible Criteria

- An authorized warehouse operator can open the admin shipment panel, upload or
  scan a courier waybill code image, scan/paste the physical device QR or serial
  number, search the XianGuanJia "待发货" order pool, select the intended order,
  confirm the device-order association, and submit. Submit is disabled until the
  operator confirms the courier values and the order-device match.
- On submit, the system shows a success summary with masked waybill number and
  the channel response message, and the order/assignment row reflects
  "已发货 / DISPATCHED".
- On submit failure, the system shows a typed error (write-disabled,
  shop-not-authorized, OCR-no-match, ship validation error, ship transport
  error, scan-rejected, idempotency conflict) without changing the assignment
  state, and the operator can retry by re-submitting with the same idempotency
  key.
- An authorized warehouse worker can open the staff shipment screen, capture the
  courier waybill code image, scan the device's signed QR, see the resolved
  device, search and select a pending-dispatch XianGuanJia order, confirm that
  this device is being shipped for that order, and submit. The same
  success/failure/retry semantics as admin apply.
- A device whose QR fails to decode is rejected at staff scan with
  `RENTAL_DEVICE_QR_INVALID` / `RENTAL_DEVICE_NOT_EXISTS`; the QR generation and
  resolution endpoints are reused as-is, with no new QR pipeline. (Note:
  `RentalDeviceQrCodec` enforces the HMAC only when `rental.device.qr-secret` is
  set; production must set `RENTAL_DEVICE_QR_SECRET` to enforce signed QRs — the
  unsigned local fallback is existing behavior, not a new risk.)
- The staff app ships a NEW `RentalStaffShip` screen and a new `api/rental/*`
  module (the staff app has NO existing rental pages or `api/rental` module —
  verified). The screen uses `uni.scanCode`, the existing `resolve-qr` endpoint,
  the staff `http` interceptor, and `useAccess` filtering; it does not embed raw
  HTTP or device-availability/schedule logic.
- `rental:xianyu:ship` (and `rental:xianyu:ship:ocr`) are real assignable
  permission codes seeded by an incremental migration into `system_menu` (ids
  7070/7071, parent 7001, type 3, `INSERT ... WHERE NOT EXISTS` template); the
  same single code gates both admin and staff, enforced server-side by
  `@PreAuthorize`. The staff `menu.json` exposes the shipment screen only when
  the user's permission codes include `rental:xianyu:ship`.
- Both the admin shipment panel and the staff shipment screen surface
  first-class states for loading, empty pending-order search results, device not
  shippable, permission denied, write disabled, shop not authorized, OCR failed,
  scan rejected, stale/ineligible order, and ship failed.

## System Criteria

- The backend refuses any `/rental/xianyu/order/ship` call when
  `rental.xianyu.write-enabled` is false (`XIANYU_WRITE_DISABLED`) or when the
  target shop is not currently authorized (reusing the existing
  `XIANYU_SHOP_AUTHORIZATION_INVALID` error code, not a new one).
- The backend calls XianGuanJia `/api/open/order/ship` through a NEW
  `XianyuWriteClient` (not via `XianyuReadClient`) using the same self-developed
  MD5 signing contract as the read endpoints, with canonical UTF-8 JSON bytes.
  The existing `XianyuReadClient` / `XianyuReadEndpoint` read-only allowlist is
  NOT modified. The write call is logged with operator, shop id, channel order
  id, masked waybill, response code/msg, and source (admin or staff) without
  exposing AppSecret or the full phone.
- New error code constants live in `ErrorCodeConstants` at `1_040_001_020+`:
  `XIANYU_WRITE_DISABLED`, `XIANYU_SHIP_IDEMPOTENT_KEY_REUSED`,
  `XIANYU_SHIP_REMOTE_ERROR`. Shop-authorization failure reuses the existing
  `XIANYU_SHOP_AUTHORIZATION_INVALID`.
- The local device-order assignment is created or reused, advances
  `ASSIGNED -> DISPATCHED`, and the shipment record is persisted **only after**
  XianGuanJia ship returns success. On failure no new durable binding,
  assignment state change, or shipment record is committed.
- Repeating a ship call with the same idempotency key returns the first
  successful result and does not call XianGuanJia again. Repeating with a
  different key on the same assignment is rejected (`XIANYU_SHIP_IDEMPOTENT_KEY_REUSED`).
- The ship endpoint only accepts a selected device and channel order that belong
  to the operator's tenant, where the channel order is still pending shipment
  and the device is shippable. Any stale order status, cross-tenant reference,
  or non-shippable device is rejected with a typed error and no XianGuanJia call
  is made.
- Every successful shipment produces exactly one `rental_device_shipment` row
  and one `DISPATCHED` assignment; duplicate waybills for the same channel
  order are rejected by a unique constraint.
- Both admin and staff ship calls hit the same `/rental/xianyu/order/ship`
  endpoint; the request carries a `source` value (`ADMIN` / `STAFF`) recorded on
  the shipment row. The backend does not branch business rules on `source`.
- `rental:xianyu:ship` (and `rental:xianyu:ship:ocr`) are enforced server-side
  by `@PreAuthorize("@ss.hasPermission('rental:xianyu:ship')")` and seeded into
  `system_menu` by an incremental migration (ids 7070/7071, parent 7001, type 3)
  using the existing `INSERT ... SELECT ... WHERE NOT EXISTS` idempotent
  template, so role managers can assign them. No new role is created.

## Data Criteria

- `rental_device_shipment` stores channel order id, assignment id, device id,
  waybill number, express code/name, ship request body hash, ship response
  code/msg, OCR-confirmed flag, source (admin/staff), created/updater
  timestamps, and tenant id. The DO follows `RentalDeviceAssignmentDO`:
  `extends TenantBaseDO`, `@TableName`/`@TableId`/`@KeySequence`, mapper
  `extends BaseMapperX`, soft-delete via the inherited `deleted` bit column.
  Waybill is masked in non-detail kanban/export views.
- OCR config `yudao.ai.shipment-ocr` (base-url, api-key, model, enable) is read from
  config only; no api-key is ever written to a log, frontend payload, or
  fixture. OCR failures degrade gracefully to an empty draft and never block
  shipment (the operator can still type values manually).
- The OCR endpoint accepts both physical courier-label photos and courier-app
  screenshots containing a one-dimensional barcode plus visible waybill number
  (for example `SF5113560342626`). It returns the waybill number from the
  barcode/large printed text instead of unrelated background order numbers, and
  exposes an extraction source/confidence so the operator can judge whether to
  edit before confirmation.
- No change to `rental_device_assignment` columns; the lifecycle reuses the
  existing `ASSIGNED -> DISPATCHED` transition.

## Component Criteria

- The admin shipment panel is one domain component composing courier-code
  upload/OCR, device-code scan/resolve, pending-order search, association
  preview, express selector, and confirm-submit; no new design tokens and no raw
  API client inside the component.
- The staff shipment screen is one uni-app screen reusing the staff scan flow,
  uni-app file upload, pending-order search, and a compact association confirm
  form; no raw API client and no device-availability/schedule logic inside the
  screen.
- `ShipmentOcrService` and its OpenAI-compatible adapter live outside the
  XianGuanJia `client` package (under `integration/ocr`); they expose a single
  OCR method and never ship.
- The ship service calls only `/api/open/order/ship` via a NEW
  `XianyuWriteClient` (reusing `XianyuRequestSigner` + `XianyuCanonicalJson` +
  shared OkHttp/ObjectMapper/Clock) — NOT by adding a method to
  `XianyuReadClient`/`XianyuReadEndpoint`; the read-only allowlist stays closed.
  No new signing code is written.
- Device scan resolution on admin and staff reuses the existing `resolve-qr`
  endpoint and `RentalDeviceQrCodec`; no duplicate QR decode is implemented.
- Reusable components, hooks, utilities, or services named in
  `component-impact-map.json` are extracted instead of duplicated.

## Verification Surfaces

- Facticity: the live XianGuanJia `/api/open/order/ship` request/response
  contract documented at `docs/integrations/xianyu/` is quoted in the change
  design and the request body shape matches the documented fields exactly.
- Static: the OCR adapter, ship service, and controller compile under
  `yudao-module-rental`; the admin page passes the type check; the staff
  shipment screen builds for both H5 and WeChat Mini Program per CLAUDE.md §16.
- Unit: `XianyuOrderShipService` has tests for write-disabled refusal,
  shop-not-authorized refusal, idempotent replay (same key returns first
  result, no second XianGuanJia call), ship-then-local ordering (no local
  mutation on channel failure), pending-order stale rejection, non-shippable
  device rejection, unique-waybill rejection, and cross-tenant rejection —
  including a `source=STAFF` call exercising the same path.
  `ShipmentOcrService` has a test pinning the multimodal prompt and base64
  `image_url` shape against a mocked OpenAI-compatible endpoint, including a
  fixture of a courier app screenshot with a one-dimensional barcode and a
  visible `SF...` waybill number.
- Redteam: a dual-tenant probe asserts a ship call referencing another tenant's
  assignment is rejected without any XianGuanJia write call; a write-disabled
  probe asserts no XianGuanJia write request is emitted when the switch is off;
  a shop-not-authorized probe asserts the same for a de-authorized shop.
- E2E: an authorized admin opens the panel, uploads a fixture courier label or
  mini-program barcode screenshot, scans/pastes a device code, searches and
  selects a pending-dispatch order, confirms the association, and submits
  against a mock XianGuanJia ship endpoint; the row reflects DISPATCHED and the
  shipment record exists with the masked waybill. A second E2E drives the staff
  one: courier code image -> device QR -> resolve -> pending-order search ->
  confirm association -> ship.
- Sensory: the `zh-CN` light-theme admin shipment panel and the staff shipment
  screen both show loading, empty, permission-denied, write-disabled,
  shop-not-authorized, OCR-failed, scan-rejected (staff), and ship-failed states
  per the design-system spec.

## Unresolved Gaps

- None.
