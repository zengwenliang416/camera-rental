# Development Handoff To Verify

## Implemented Slices

- Backend shipment workflow exposes pending-order search, OCR, and idempotent ship submission behind server permissions and write switch.
- Admin Web contains the scan/upload/search/confirm/ship workbench using typed APIs.
- Staff mobile contains the scan/upload/search/confirm/ship screen and now uploads OCR images with base URL, tenant header, and bearer token.

## Files Changed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/admin/XianyuOrderShipService.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/service/admin/XianyuOrderShipServiceTest.java`
- `camera-rental-staff/src/api/rental/xianyu.ts`
- `openspec/changes/xian-guanjia-order-dispatch-ship-v1/tasks.md`
- `openspec/changes/xian-guanjia-order-dispatch-ship-v1/development/**`

## Requirements Covered

- A6, A8, A9, A10, A13, A18, A19, A20, A20b, A20c, and A24 have development/static/unit evidence.
- A1, A2, A3, A4, A5, A21, A22, and A23 still require verification-stage execution.

## Prototype Decisions Implemented

- The implemented flow keeps code capture first, pending-order search second, and manual association confirmation before ship submission.
- Frontends do not own authoritative inventory, schedule, tenant, or channel-write checks.

## Components Created / Reused / Extracted

- Reused `XianyuOrderShipService`, `ShipmentOcrService`, `OpenAiCompatibleShipmentOcrClient`, `XianyuWriteClient`, existing admin typed APIs, and staff uni-app transport conventions.
- Added focused test coverage for backend ship orchestration.
- No duplicate QR signing/decoding pipeline or XianGuanJia signing path was added.

## API / Data Flow Changes

- Staff OCR upload now calls `${getEnvBaseUrl()}/rental/xianyu/order/ship/ocr` and carries `tenant-id` plus `Authorization`.
- Idempotent ship replay returns the original shipment with resolved `deviceNo`.

## Tests Added

- `XianyuOrderShipServiceTest` covers write-disabled no mutation, remote-success local commit ordering, and idempotent replay response.

## Local Validation

- Backend focused Maven test passed with 7 tests.
- Admin `pnpm ts:check` passed.
- Staff `pnpm type-check`, `pnpm build:h5`, and `pnpm build:mp-weixin` passed.
- `git diff --check` passed.
- Migration checksum was recorded.

## Known Risks

- No real XianGuanJia write was executed.
- Mock browser E2E, sensory state review, and tenant/security red-team probes remain before archive.
- WeChat scan behavior still needs runtime confirmation on device or simulator.

## Items Requiring Six-Domain Verification

- Facticity: re-check `/api/open/order/ship` field contract against current docs before production write enablement.
- Static: confirm no secrets, no extra write endpoint, and no frontend business-rule authority.
- Unit: broaden backend failure-path tests for shop authorization, remote failure rollback, duplicate waybill, and tenant scoping.
- Redteam: run write-disabled, shop-not-authorized, and cross-tenant probes with no outbound channel write.
- E2E: run admin and staff mock shipment flows with OCR fixture and mock XianGuanJia endpoint.
- Sensory: review admin and staff loading, empty, error, permission, and success states.
