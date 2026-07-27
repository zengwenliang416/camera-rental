# Development Basis: xian-guanjia-order-dispatch-ship-v1

## Requirements Reference

- `openspec/specs/ui-design/design.md`
- `openspec/specs/system-architecture/design.md`
- `openspec/specs/frontend-backend-data-flow/design.md`
- `openspec/specs/component-architecture/design.md`
- `openspec/changes/xian-guanjia-order-dispatch-ship-v1/requirements.md`
- `openspec/changes/xian-guanjia-order-dispatch-ship-v1/acceptance.md`
- `openspec/changes/xian-guanjia-order-dispatch-ship-v1/spec-map.json`
- `openspec/changes/xian-guanjia-order-dispatch-ship-v1/component-impact-map.json`

## Prototype Reference

- `openspec/changes/xian-guanjia-order-dispatch-ship-v1/prototype/artifact/index.html`
- `openspec/changes/xian-guanjia-order-dispatch-ship-v1/prototype/handoff.md`
- `openspec/changes/xian-guanjia-order-dispatch-ship-v1/prototype/decision.json`

## Handoff Reference

The approved handoff freezes the production behavior: the operator captures the
courier waybill code and device QR/serial first, searches pending-dispatch
XianGuanJia orders, confirms the device-order association, and only then ships
through `/api/open/order/ship`.

## External API Basis

The live XianGuanJia order ship documentation checked on 2026-07-26 defines the
write path as `POST https://open.goofish.pro/api/open/order/ship`. The
implementation must send documented required fields `order_no`, `waybill_no`,
`express_code`, and `express_name`, plus optional sender fields only if later
configured server-side.

## Component Architecture Constraint

Implementation must preserve high cohesion and low coupling. OCR/barcode
extraction, pending-order search, device-code resolution, and ship submission
must be separate typed services/hooks. UI components must not implement
authoritative pending-order, device availability, schedule, or tenant checks.

## Scope Reference

- `openspec/changes/xian-guanjia-order-dispatch-ship-v1/scope.json`
- First implementation packet:
  `openspec/changes/xian-guanjia-order-dispatch-ship-v1/development/tasks/001-backend-ship-core/brief.md`
