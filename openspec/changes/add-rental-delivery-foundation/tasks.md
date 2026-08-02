## 1. Architecture and persistence foundation

- [ ] 1.1 A maintainer can review the logistics ADR and apply one additive migration that creates the seven tenant-scoped logistics tables plus nullable `rental_device_shipment.delivery_id` without external calls or historical backfill.
- [ ] 1.2 A rental developer can use typed logistics enums, encrypted DOs, and tenant-aware Mappers for every new persistence entity without introducing Provider SDK types.

## 2. Delivery and reliability services

- [ ] 2.1 A rental service can idempotently create or reuse one physical Delivery, tolerate missing carrier mapping, validate order-assignment-device relationships, and bind multiple devices atomically.
- [ ] 2.2 A logistics integration can enqueue deduplicated PII-free Outbox tasks and compile against supplier-independent subscribe, query, callback, result, and tracking-event contracts.

## 3. Complete tracking snapshots

- [ ] 3.1 A tracking consumer can normalize, fingerprint, hash, sort, and persist complete tracking snapshots while suppressing identical replays and preserving historical versions.
- [ ] 3.2 A late or out-of-order snapshot cannot regress a terminal tracking summary, and an absent ETA does not block snapshot advancement.

## 4. Validation and handoff

- [ ] 4.1 A maintainer can run focused rental-module tests and static checks proving Delivery idempotency, multi-device relations, Outbox dedupe, snapshot versioning, encryption annotations, tenant isolation, migration safety, and the absence of real Provider access.
