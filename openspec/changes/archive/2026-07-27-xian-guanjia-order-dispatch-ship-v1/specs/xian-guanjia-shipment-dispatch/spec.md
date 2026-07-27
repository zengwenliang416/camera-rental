## ADDED Requirements

### Requirement: Confirmed XianGuanJia shipment dispatch

The system SHALL ship a pending XianGuanJia channel order only after an operator
confirms the waybill, courier, local device, and selected pending order.

#### Scenario: Confirmed shipment succeeds

- **WHEN** an authorized operator submits a confirmed shipment for a pending
  channel order with a resolvable rentable device and idempotency key
- **THEN** the backend sends the documented XianGuanJia shipment request and
  persists local shipment, assignment, and device state changes only after the
  channel call succeeds

### Requirement: Shipment write safety

The system SHALL block XianGuanJia shipment writes unless the integration write
switch is explicitly enabled.

#### Scenario: Write switch is disabled

- **WHEN** an operator submits an otherwise valid shipment while writes are
  disabled
- **THEN** no remote shipment request is sent and no local shipment state is
  committed

### Requirement: Shipment access isolation

The system SHALL enforce tenant, permission, shop authorization, and device
availability checks on every shipment request.

#### Scenario: Device or shop does not belong to the operator tenant

- **WHEN** a shipment request references a cross-tenant device, order, or shop
- **THEN** the request is rejected before any remote write or local state change

### Requirement: Staff shipment screen

The system SHALL provide a staff mobile shipment screen that captures waybill
and device codes, searches pending order candidates, requires manual selection,
and submits through the backend shipment endpoint.

#### Scenario: Staff confirms a candidate

- **WHEN** staff enters waybill and device data, searches pending candidates,
  selects one candidate, and confirms shipment
- **THEN** the request source is recorded as `STAFF` and the backend remains the
  authority for binding and dispatch state changes
