# Proposal: Complete Rental Logistics Tracking

## Why

The platform can dispatch Xianyu orders and audit device shipment, but it does
not own a physical-package model, durable Provider workflow, complete tracking
history, or schedule-center logistics view. Operators therefore cannot reliably
see multi-package state, callback/query progress, delivery risk, or failed
tracking tasks without consulting external systems.

## What Changes

- Add a tenant-scoped Delivery aggregate with multi-package and multi-device
  relationships, complete versioned tracking snapshots, callback Inbox, event
  Outbox, carrier mappings, and masked Provider configuration.
- Add a Provider-neutral logistics contract and a Kuaidi100 adapter for
  subscription, active query, verified callback parsing, bounded retry, query
  throttling, and reconciliation.
- Extend successful Xianyu shipment so the same local transaction creates or
  reuses the physical Delivery, binds the dispatched device, links the shipment,
  and enqueues post-commit tracking tasks.
- Add local tracking summary, detail, refresh, operations, backfill, cleanup, and
  risk APIs.
- Add schedule-center logistics summaries, a multi-package trace drawer,
  visibility-aware 60-second local polling, refresh feedback, operations panels,
  and logistics risk presentation.

## Impact

- Backend: `camera-rental-server/yudao-module-rental/yudao-module-rental-biz`.
- Database: one additive migration after `_031`.
- Frontend: `camera-rental-schedule-center`.
- Operations: new permissions, disabled-by-default Provider configuration,
  bounded asynchronous tasks, dry-run historical backfill, and technical-data
  retention.
- Compatibility: existing shipment fields and APIs remain compatible;
  `rental_device_shipment.delivery_id` is nullable.

## Non-Goals

- No customer-facing tracking page, SSE, map, automatic refund, or automatic
  device release after delivery.
- No Provider call from the browser, structural migration, or Xianyu shipment
  transaction.
- No real Kuaidi100 network call in CI or ordinary local tests.
