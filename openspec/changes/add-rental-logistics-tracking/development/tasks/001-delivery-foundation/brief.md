# Task Brief: 001-delivery-foundation

## Goal

Operators can create or reuse one tenant-safe physical package, bind multiple
devices, retain complete versioned tracking data, and enqueue safe local work
without changing existing shipment behavior.

## Parent Artifacts

- `openspec/changes/add-rental-logistics-tracking/requirements.md`
- `openspec/changes/add-rental-logistics-tracking/acceptance.md`
- `openspec/changes/add-rental-logistics-tracking/prototype/handoff.md`

## Vertical Slice

From a local Delivery command through validation, persistence, device relation,
snapshot application, and Outbox creation, produce a package state that later
Provider and UI slices can consume.

## In Scope

- Add the seven logistics tables and nullable shipment `delivery_id`.
- Add logistics enums, encrypted DOs, Mappers, provider-neutral models, Delivery
  creation, device relation, snapshot aggregation, Inbox persistence, Outbox
  enqueue, carrier mapping, Provider config, masking, and focused tests.

## Out Of Scope

- No Kuaidi100 network adapter, Worker, webhook, Xianyu shipment integration,
  admin HTTP API, schedule-center UI, backfill, cleanup, or device release.

## Files Allowed

- `camera-rental-server/sql/mysql/migrations`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz`
- `docs/decisions`
- `openspec/changes/add-rental-logistics-tracking`

## Interfaces / Seams

- `RentalDeliveryService`, `RentalTrackingSnapshotService`,
  `RentalDeliveryOutboxService`, `RentalDeliveryInboxService`, and
  `LogisticsProvider` define provider-neutral boundaries.

## Components To Create

- Logistics DOs, Mappers, enums, commands/results/events, snapshot aggregator,
  Delivery/Inbox/Outbox services, carrier mapping service, config service, and
  an ADR.

## Components To Reuse

- `TenantBaseDO`, `BaseMapperX`, `EncryptTypeHandler`, rental order/item/device/
  assignment entities, Snowflake IDs, Spring transactions, and test starter.

## Components To Extract

- Waybill normalization/masking, sensitive-value redaction, event fingerprint,
  snapshot hash, stable event sorting, and terminal-state protection.

## API / Data Flow Contracts

- Internal flows only: Delivery create or reuse, device bind, snapshot apply,
  Inbox persist, and Outbox enqueue. No HTTP or Provider network flow.

## State / Error / Empty / Loading Behavior

- Loading: not applicable to the internal service slice.
- Empty: an empty snapshot does not advance `tracking_version`.
- Error: relationship mismatches roll back atomically with safe messages.
- Disabled: missing mapping or disabled config creates an explicit local state.
- Permission: tenant isolation is enforced by persistence and service queries.

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -DskipTests compile`
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='*RentalDelivery*,*RentalTracking*,*RentalLogistics*' test`
- `git diff --check`

## Stop Conditions

- Scope lock mismatch.
- Missing product, architecture, data-flow, or component decision.
- Component duplication that should be extracted.
- A migration number is already occupied or an existing migration would need
  modification.
- Any design would require a Provider call or historical backfill in migration.

## Unsafe Assumptions

- Do not assume database foreign keys exist; enforce relations with service
  validation and unique indexes.
- Do not assume Provider config or carrier mapping exists.
- Do not log full waybills, phones, addresses, tokens, callback content, or
  credentials.
