# Release Notes: xian-guanjia-order-dispatch-ship-v1

## Summary

- This release adds the first complete XianGuanJia order dispatch workflow for
  warehouse/admin operators. Operators confirm a waybill, device, and pending
  channel order before shipment. The backend performs the authoritative checks
  and keeps write operations disabled unless explicitly configured.

## Verification

- SpecNav aggregate verification is green across facticity, static, unit,
  redteam, e2e, and sensory domains.
- Focused backend tests passed: 16 tests, 0 failures.
- Mock-server E2E covered successful ADMIN and STAFF shipment paths without real
  external mutation.
- Staff H5 Playwright covered candidate search, enabled submit, confirmation,
  browser POST source `STAFF`, and success response.

## Known Limitations

- No real XianGuanJia write was executed against a controlled test shop.
- Physical handset testing was not executed; staff flow was verified in H5
  mobile viewport.
- Production write enablement still requires explicit approval and controlled
  credentials.
