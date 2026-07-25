## ADDED Requirements

### Requirement: Idempotent rental order conversion

The system SHALL map one XianGuanJia channel order to at most one internal
rental order and retain the source identity, amount snapshot, conversion
version, and review state.

#### Scenario: The same channel order is processed again

- **WHEN** list sync, detail refresh, or push processing repeats conversion
- **THEN** the existing rental order is updated or returned without duplication

### Requirement: Manual review without revenue loss

The system SHALL preserve paid channel revenue when product mapping or rental
dates cannot be determined.

#### Scenario: Seller remark is invalid

- **WHEN** a paid channel order has an empty, conflicting, or unparseable remark
- **THEN** no schedule is fabricated and an actionable review record is created

### Requirement: Physical-device identity

The system SHALL manage every rentable physical device with a unique device
number and optional unique serial number independently from SKU quantity.

#### Scenario: Operator registers a duplicate device

- **WHEN** a device number already exists in the same tenant
- **THEN** registration is rejected without changing the existing device

### Requirement: Occupied schedule conflict protection

The system SHALL use `Asia/Shanghai` half-open occupied intervals and reject
overlapping effective schedules for one physical device.

#### Scenario: Concurrent assignments overlap

- **WHEN** competing transactions assign the same device to overlapping ranges
- **THEN** at most one transaction commits its assignment and schedule

### Requirement: Atomic device assignment

The system SHALL create the device assignment, occupied schedule, and related
order transition in one transaction.

#### Scenario: Schedule creation fails

- **WHEN** conflict validation or persistence fails during assignment
- **THEN** no partial assignment or order-state transition remains

### Requirement: Explicit product mapping

The system SHALL maintain auditable mappings from channel product/SKU identity
to an internal equipment model.

#### Scenario: No mapping exists

- **WHEN** a channel order item cannot be mapped uniquely
- **THEN** conversion enters manual review and does not invent a device or SKU

### Requirement: Source-linked operational reports

The system SHALL report rent revenue from channel `pay_amount`, refunds as a
separate metric, order source, product/SKU, utilization, idle time, and
assigned-device income.

#### Scenario: Paid order has invalid rental dates

- **WHEN** revenue exists but utilization dates cannot be parsed
- **THEN** revenue remains included while utilization stays review-required
