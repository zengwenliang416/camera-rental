# Domain
static

# Verdict
green

# Inputs Reviewed
- Backend XianGuanJia write client and shipment service.
- Admin/staff shipment UI and API modules.
- Migration SQL for shipment and permissions.
- Worktree status around neighboring OpenSpec changes.

# Evidence
- `XianyuWriteClient` and `XianyuWriteEndpoint` implement a closed write allowlist for `/api/open/order/ship`.
- `XianyuReadEndpoint` remains read-only.
- `RentalDeviceShipmentDO` and mapper follow tenant/soft-delete conventions.
- `XianyuOrderController` gates ship and OCR endpoints with `rental:xianyu:ship` and `rental:xianyu:ship:ocr`.
- Staff app reuses the existing resolve-qr API through `src/api/rental/device.ts`.
- `git status --short` shows neighboring `xian-guanjia-data-integration-v1` dirty artifacts are outside this change.

# Commands Run
- `rg` static checks over Xianyu write/read clients, controller permissions, shipment DO/mapper, migrations, and staff/admin screens.
- Focused Maven tests for shipment, OCR, QR, redteam, and tenant cases.
- `git status --short` to isolate neighboring dirty OpenSpec artifacts.

# Findings
- Static implementation boundaries match the acceptance assertions.
- OCR config naming is implementation-aligned under `yudao.ai.shipment-ocr`; the frozen A15 statement text remains unchanged.
- A25 is satisfied for assertion non-mutation: this change does not mutate the read-only change's acceptance assertions.

# Required Fixes
- None.

# Residual Risk
- Neighboring dirty OpenSpec generated/status files should be handled separately before archiving multiple changes together.

# Follow-up Domain Routing
- No further static verification required for this change.
