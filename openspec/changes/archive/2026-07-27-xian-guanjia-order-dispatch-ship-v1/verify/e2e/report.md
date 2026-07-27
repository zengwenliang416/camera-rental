# Domain
e2e

# Verdict
green

# Inputs Reviewed
- Live admin and staff browser runtime evidence.
- Local mock XianGuanJia `/api/open/order/ship` server.
- Local backend restarted with write enabled only against the mock base URL.
- SPECNAV_E2E isolated database rows.

# Evidence
- `verify/e2e/artifacts/live-browser-runtime-after-qr-fix.json`
- `verify/e2e/artifacts/staff-ui-search-candidate.png`
- `verify/e2e/artifacts/staff-ui-ready-to-submit.png`
- `verify/e2e/artifacts/staff-ui-submit-success.json`
- `verify/e2e/artifacts/staff-ui-after-submit.png`
- `verify/e2e/artifacts/mock-ship-success-db-proof-20260727.json`

# Commands Run
- Started a local mock server at `http://127.0.0.1:18089`.
- Restarted backend with `XGJ_WRITE_ENABLED=true` and `XGJ_BASE_URL=http://127.0.0.1:18089`.
- Inserted isolated `SPECNAV_E2E` rental devices, rental orders, order items, and pending XianGuanJia orders.
- Submitted ADMIN and STAFF source shipments through `/admin-api/rental/xianyu/order/ship`.
- Ran Playwright against staff H5 to search a pending order, confirm the dialog, and submit through the browser.
- Queried local MySQL with pymysql for shipment, assignment, order, and device postconditions.

# Findings
- Successful submits return masked waybill, `remoteMsg=SPECNAV_MOCK_SHIP_OK`, and `assignmentStatus=DISPATCHED`.
- Browser staff submit records `source=STAFF` and leaves the pending-order list empty after success.
- Database proof shows `rental_device_shipment` rows, xianyu order waybill fields, `DISPATCHED` assignments, and `RENTED` devices.
- The mock captured only the documented write body fields: `order_no`, `waybill_no`, `express_code`, `express_name`.

# Required Fixes
- None.

# Residual Risk
- This is a mock-server success E2E. It intentionally avoids a real XianGuanJia write because no controlled test-shop write authorization was granted.

# Follow-up Domain Routing
- No further E2E verification required for the mock-server verification scope.
