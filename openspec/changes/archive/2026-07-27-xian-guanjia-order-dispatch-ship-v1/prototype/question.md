# Prototype Question: xian-guanjia-order-dispatch-ship-v1

## Question

Two new user-facing surfaces are introduced in this change — an admin web
shipment panel and a staff uni-app shipment screen — and the riskiest
uncertainty is **visual + interaction**: can the operator/walker stride
(order/device pick -> upload label -> OCR draft -> confirm -> submit -> result
or typed failure) be reviewed in the real project shell with the right labels,
fields, density, and all the typed failure states before any production code is
written? Equally, does the staff scan-first mobile surface feel native to
`camera-rental-staff`'s existing scan/upload conventions and platform-safe UI?

## Branch

`ui-html`

## Review Target

- Entry: `artifact/index.html` (open in browser; light + dark; admin panel and
  staff screen shown as two switchable screens).
- Required reviewer decision: approve the two surfaces, their states (loading,
  empty, permission-denied, write-disabled, shop-not-authorized, OCR-failed,
  scan-rejected, ship-failed, populated), and the confirm-before-submit gate,
  with project-real labels and chrome — before implementation.

## Out of Scope

- Production implementation (no `.vue`, no Java, no migration).
- Database writes.
- Deployment behavior.
- Real OCR / real XianGuanJia calls (static fixtures only).
