# Prototype Question: schedule-center-local-stability-v1

## Question

Can one integrated rental-operations product combine unified admin login, a
light SN-level schedule, a top-navigation handoff workbench, and an
availability matrix while honestly separating the four currently registered
device instances from the user's much larger physical inventory that has not
yet been imported?

## Branch

`ui-html`

## Selected Variant

`integrated-rental-operations-v6-outbound-command-center`

## Review Target

- Entry: `artifact/index.html`; use `?screen=login` or the prototype-state
  control to review the login screen.
- Review the light unified-admin login surface, tenant/account/password flow,
  unsupported SMS/QR states, privacy statement, and server-authority boundary.
- Review the white top-navigation shell and today's four-lane handoff workbench.
- Review the model-grouped physical-device timeline with frozen device and SN
  context, ten-day occupied periods, billable/occupied distinction, and action
  rail.
- Review the model/date availability matrix, shortage causes, replacement
  suggestions, and physical-SN timeline.
- Review rental orders, device ledger, outbound handoff, and exception desk as
  complete supporting product areas.
- Review light/dark themes, `zh-CN`/`en`, desktop/tablet/mobile behavior, and
  populated/loading/empty/error/permission/disabled states.
- Confirm the outbound sequence remains `waybill -> device -> pending-order
  search -> confirm binding -> ship`, with OCR as an editable review draft.
- Confirm operators can search available registered devices by device ID, SN,
  model, or location, with the shipment docket following the active selection.
- Confirm the pending-order search contract supports receiver name, full phone,
  and order number. Authorized production results are unmasked and audit-logged,
  while this prototype deliberately copies no real customer values.
- Confirm every inventory metric says "registered" where appropriate. The four
  production device rows are not presented as the user's actual total assets.
- Review `device-import-field-model.md` and the device-ledger import field table
  before bulk-import implementation.
- Confirm the displayed order, review, alert, shop, schedule, assignment, and
  device counts match the masked production snapshot captured at
  `2026-07-29 17:12 Asia/Shanghai`.

## Out of Scope

- Production implementation or deployment.
- Database, API, order, device, schedule, or shipment writes.
- New backend endpoints, permissions, scheduling algorithms, or status
  transitions.
- Live API requests, private customer values, complete order identifiers,
  complete waybills, or production writes.
- Claiming that the four registered devices represent the user's actual
  inventory.
- Prototype approval, promotion, or handoff before explicit reviewer approval.
