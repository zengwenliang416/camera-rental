# Task Brief: 003-order-reconciliation

## Goal

Create exactly one internal rental order immediately for every eligible durable
channel order, then re-evaluate readiness through one idempotent service.

## Vertical Slice

When order details arrive, an eligible order is persisted and linked to one
internal order even if model, remark or rental dates are missing; later mapping
or remark data updates that same order and its preparation status.

## In Scope

- Checklist items `3.1`, `3.2`, `3.3`, and `3.4`.
- Exact skip-rule evaluation, idempotent internal-order creation and preparation policy.
- Removal of ambiguous runtime identifier reads and shipment-time mapping creation.

## Out Of Scope

- Fulfillment fact mutation, concrete device selection and historical batch orchestration.

## Files Allowed

- `camera-rental-server/yudao-module-rental`
- `camera-rental-server/sql/mysql`

## Interfaces / Seams

- Order detail persistence, product rule service, remark parser, schedule and assignment services.

## Components To Create

- `RentalChannelOrderReconciliationService`
- `RentalOrderPreparationPolicy`
- Reconciliation reason/status model and idempotency tests.

## Components To Reuse

- Existing channel order persistence, rental order services and transaction conventions.

## Components To Extract

- All persistence, mapping, remark replay and backfill callers use one reconciliation entrypoint.

## API / Data Flow Contracts

- `CONFIG_SKIPPED` retains channel order, raw payload and payment evidence but
  creates no internal order, review or schedule.
- Multi-model products require exact synchronized XianGuanJia SKU mapping.

## State / Error / Empty / Loading Behavior

- Missing model yields `WAITING_MODEL`; missing valid dates yields `WAITING_REMARK`.
- Assignment/scheduling remain blocked until the preparation policy reports ready.
- Retry updates the existing internal order rather than inserting another.

## TDD Requirement

- Cover duplicate detail delivery, missing fields, skip rules and exact SKU behavior.

## Verification Commands

- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -pl yudao-module-rental -am test -Dmaven.repo.local=/Volumes/zwl/maven-repository`

## Stop Conditions

- Stop if the current external order field source is still ambiguous.
- Stop if immediate creation would violate a current unique or tenant constraint.
- Stop if another runtime mapping source remains active.

## Unsafe Assumptions

- A paid order can be complete financially while still incomplete for scheduling.
