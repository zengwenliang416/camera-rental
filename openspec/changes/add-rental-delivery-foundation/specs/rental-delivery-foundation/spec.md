## ADDED Requirements

### Requirement: Delivery represents one physical package

The system SHALL represent each physical logistics package as one tenant-scoped
`rental_delivery` independent from the channel shipment audit record.

#### Scenario: One order has multiple packages
- **WHEN** an order uses different waybills, package sequences, or logistics directions
- **THEN** the system stores independent Delivery records without overwriting an earlier package

#### Scenario: Shipment remains an audit record
- **WHEN** a shipment is associated with a Delivery
- **THEN** `rental_device_shipment` retains its channel-write audit fields and references the package through nullable `delivery_id`

### Requirement: Delivery supports all approved directions

The system SHALL support `OUTBOUND`, `RETURN`, `EXCHANGE_OUT`, and
`EXCHANGE_RETURN` as platform-owned Delivery directions.

#### Scenario: Customer return reaches the warehouse
- **WHEN** a customer return package is delivered to the warehouse
- **THEN** its direction remains `RETURN` and its tracking status can be `DELIVERED`

#### Scenario: Carrier returns an abnormal package
- **WHEN** the carrier returns a package to its sender after a delivery exception
- **THEN** the tracking status uses `RETURNING` or `RETURNED` without changing the Delivery direction

### Requirement: Delivery creation is idempotent

The system MUST use tenant, rental order, direction, source carrier code, and
normalized waybill number as the Delivery creation identity.

#### Scenario: Duplicate create request
- **WHEN** the same tenant-scoped Delivery identity is submitted more than once
- **THEN** the system returns or reuses the existing Delivery and does not insert a duplicate package

#### Scenario: Same waybill in another tenant
- **WHEN** another tenant submits the same order-relative carrier and waybill values
- **THEN** the second tenant receives an isolated Delivery without reading or conflicting with the first tenant

### Requirement: One package can bind multiple devices

The system SHALL allow one Delivery to reference multiple order-item,
assignment, and physical-device relationships.

#### Scenario: Multi-device package
- **WHEN** valid assignments for multiple devices of the same rental order are bound to one Delivery
- **THEN** the system stores one Delivery and one unique relation per device

#### Scenario: Duplicate device relation
- **WHEN** the same device is bound to the same Delivery more than once
- **THEN** the system reuses the existing relation and does not insert a duplicate

#### Scenario: Mismatched relationship
- **WHEN** an order item, assignment, device, or Delivery belongs to a different tenant or incompatible order relationship
- **THEN** the system rejects the operation atomically without partial Delivery, relation, or Outbox writes

### Requirement: Missing carrier mapping does not block local Delivery

The system SHALL create a local Delivery even when a carrier mapping or enabled
Provider configuration is unavailable.

#### Scenario: Carrier mapping is missing
- **WHEN** a Delivery is created with an unmapped source carrier code
- **THEN** the Delivery is stored with `MAPPING_REQUIRED`, no Provider call occurs, and the package remains available for later reconciliation

#### Scenario: Carrier mapping exists
- **WHEN** an enabled mapping exists for the source type and carrier code
- **THEN** the Delivery stores canonical and Provider carrier codes and uses `MAPPED`

### Requirement: Tracking uses complete versioned snapshots

The system MUST normalize and version complete tracking snapshots rather than
treating each callback or query result as an incremental append.

#### Scenario: First snapshot
- **WHEN** a non-empty complete snapshot is applied to a Delivery
- **THEN** the system stores a new snapshot version, event fingerprints, the snapshot hash, the latest summary, and increments `tracking_version`

#### Scenario: Identical snapshot replay
- **WHEN** the normalized complete snapshot hash equals the current snapshot hash
- **THEN** the system does not insert another effective snapshot and does not increment `tracking_version`

#### Scenario: Provider corrects historical events
- **WHEN** a complete snapshot changes historical event content while remaining valid
- **THEN** the system stores a new snapshot version and preserves the older snapshot for audit

### Requirement: Tracking summary cannot regress

The system MUST select the current tracking summary using business event time,
platform status precedence, and terminal-state protection.

#### Scenario: Late in-transit event after delivery
- **WHEN** the current status is `DELIVERED` and a late snapshot ends with `IN_TRANSIT`
- **THEN** the current summary remains `DELIVERED` while the different historical snapshot can still be retained

#### Scenario: Older event arrives later
- **WHEN** a newly received event has a business event time before the current latest trace time
- **THEN** the event cannot replace the current latest summary

#### Scenario: ETA is absent
- **WHEN** a valid snapshot contains no estimated arrival time
- **THEN** snapshot persistence and summary advancement still succeed

### Requirement: Outbox tasks are durable, deduplicated, and safe

The system SHALL persist tenant-scoped `SUBSCRIBE`, `INITIAL_QUERY`,
`REFRESH_QUERY`, and `RECONCILE` tasks using a stable event-type and dedupe-key
identity.

#### Scenario: Duplicate Outbox request
- **WHEN** the same tenant, event type, and dedupe key are enqueued repeatedly
- **THEN** the system returns the existing task and does not insert another active task

#### Scenario: Outbox is created in the foundation Change
- **WHEN** an Outbox task is stored before Provider integration exists
- **THEN** the task remains pending and no Worker or network call consumes it

#### Scenario: Outbox data is inspected
- **WHEN** an Outbox row, log, or test fixture is reviewed
- **THEN** it contains no phone number, address, complete waybill, credential, callback token, salt, or callback body

### Requirement: Callback Inbox supports verified persistence later

The system SHALL provide a tenant-scoped Callback Inbox model with Provider,
Delivery, payload hash, encrypted callback content, processing lease, retry, and
result metadata.

#### Scenario: Duplicate verified callback payload
- **WHEN** a later verified callback entry attempts to persist the same Provider, Delivery, and payload hash
- **THEN** the Inbox model can identify it as an idempotent duplicate

#### Scenario: Foundation has no public callback
- **WHEN** Change 1 is deployed
- **THEN** no public logistics callback Controller, signature verifier, ACK handler, or Inbox Worker exists

### Requirement: Sensitive logistics values are encrypted or redacted

The system MUST use the project encryption TypeHandler for tracking phone,
callback token, callback salt, callback parameter, customer code, API key, and
API secret fields.

#### Scenario: Sensitive DO is rendered as a string
- **WHEN** a Delivery, Inbox, or Provider configuration object is logged through `toString`
- **THEN** encrypted sensitive fields are excluded from the output

#### Scenario: Operational error is recorded
- **WHEN** a local logistics operation fails
- **THEN** logs and error summaries omit complete waybills, phones, addresses, credentials, tokens, salts, and callback bodies

### Requirement: Provider contracts remain supplier independent

The system SHALL expose subscription, query, and verified-callback parsing
through platform Command, Result, and Event models only.

#### Scenario: Domain module is compiled
- **WHEN** the rental logistics domain, Service, DO, Mapper, and tests are compiled
- **THEN** no Kuaidi100 SDK type appears outside a future Provider adapter

#### Scenario: Ordinary tests run
- **WHEN** CI or rental module unit tests execute
- **THEN** no real logistics Provider endpoint is contacted

### Requirement: Migration is additive and backward compatible

The system MUST create the approved logistics tables and shipment reference
through a new timestamped additive MySQL migration.

#### Scenario: Fresh database
- **WHEN** the complete migration chain runs on an empty supported database
- **THEN** all seven logistics tables and nullable shipment `delivery_id` are created with required indexes and audit fields

#### Scenario: Upgrade from current migration
- **WHEN** the new migration runs after the current `_031` migration
- **THEN** existing rental orders, devices, assignments, shipments, and waybill values remain unchanged

#### Scenario: Migration executes
- **WHEN** the structural migration is applied
- **THEN** it performs no external request, historical backfill, historical subscription, DROP, or bulk business-status update

### Requirement: Logistics delivery does not release a device

The system MUST keep logistics tracking state independent from device
availability, return inspection, and schedule release.

#### Scenario: Return package is delivered
- **WHEN** a return Delivery reaches `DELIVERED`
- **THEN** no device, assignment, inspection, maintenance, or schedule state is automatically changed by the logistics foundation
