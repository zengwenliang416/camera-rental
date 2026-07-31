# Task Report: 001-delivery-foundation

## Status

DONE

## Files Changed

- `camera-rental-server/sql/mysql/migrations/20260731_032_rental_delivery_tracking.sql`
- `docs/decisions/0003-rental-logistics-tracking-model.md`
- Rental logistics DOs, Mappers, enums, provider-neutral contracts, services, utilities, and focused tests under `yudao-module-rental-biz`.
- `RentalDeviceShipmentDO` gains nullable `deliveryId`.
- SpecNav migration manifest, audit wrapper, validation log, drift check, and task reviews.

## What Changed

- Added seven tenant-scoped logistics tables and an idempotent guarded shipment alteration.
- Added one-order-many-packages and one-package-many-devices domain persistence.
- Added encrypted Provider configuration, callback Inbox, tracking phone, callback token, and callback parameter fields.
- Added Delivery creation/reuse with two-phase relationship validation and PII-free SUBSCRIBE/INITIAL_QUERY Outbox tasks.
- Added Provider-neutral subscribe, query, callback, event, snapshot, and result contracts.
- Added stable waybill normalization/masking, sensitive metadata redaction, event fingerprinting, complete snapshot hashing, immutable trace versions, and terminal-state protection.
- Centralized source/carrier normalization in `RentalCarrierMappingService`.
- Added explicit missing-mapping, tracking-phone-required, and provider-disabled states without any Provider network call.

## TDD Evidence

- `RentalDeliveryServiceImplTest`: create/reuse, mapping-required degradation, task enqueue, relationship mismatch, duplicate-device validation, and required-phone blocking.
- `RentalTrackingSnapshotServiceImplTest`: changed snapshot versioning, duplicate suppression, and terminal-state protection.
- `TrackingSnapshotNormalizerTest`: stable ordering/hash, full-key tie ordering, and history changes.
- `RentalCarrierMappingServiceTest`: normalized lookup and fallback carrier identity.
- Inbox/Outbox tests: payload/task dedupe and PII-free metadata.
- Sensitive-field test: EncryptTypeHandler and generated `toString()` exclusion.
- Provider config test: minimum query interval cannot be below 1,800 seconds.

## Verification Commands

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -DskipTests compile`
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='*RentalDelivery*,*RentalTracking*,*RentalLogistics*,RentalCarrierMappingServiceTest,WaybillPrivacyTest' -Dsurefire.failIfNoSpecifiedTests=false test`
- `git diff --check`
- Static count of seven migration tables.
- Static scan for Provider SDK imports outside the future Kuaidi100 adapter.
- Disposable MySQL 8.4 fresh-schema execution: baseline plus migrations 001-032.
- Disposable MySQL 8.4 current-upgrade execution: baseline plus migrations 001-031, fixture shipment, migration 032 twice, then schema and data assertions.

## Concerns

- Task 001 intentionally does not include Provider workers, callback HTTP verification, Xianyu shipment integration, admin APIs, UI, or operations.

## Scope Deviations

- None recorded.

## Follow-up Needed

- Task 002 must implement the official Kuaidi100 protocol only after reading current official documentation.
- Final six-domain verification must still cover the integrated application and mock Provider flow.

## Adjudication

The first independent reviews returned `needs-fix`. The implementation now validates every duplicate-device
relationship before insert, uses complete stable snapshot ordering, centralizes carrier normalization, blocks
Provider tasks when a required tracking phone is missing, and includes replayable MySQL 8.4 migration evidence.
Task 001 is ready for independent re-review.
