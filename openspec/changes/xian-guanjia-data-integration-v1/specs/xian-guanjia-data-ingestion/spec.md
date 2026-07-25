## ADDED Requirements

### Requirement: Exact authenticated read requests

The system SHALL serialize each request once and use the identical UTF-8 bytes
for XianGuanJia signing and HTTP transmission.

#### Scenario: Signed request succeeds

- **WHEN** a configured read request is sent
- **THEN** its query authentication uses the runtime AppKey, Unix-second
  timestamp, and signature derived from the transmitted body

### Requirement: Durable idempotent synchronization

The system SHALL persist raw and normalized records and advance a shop-scoped
cursor only after durable processing.

#### Scenario: A page is replayed

- **WHEN** a list page, detail response, or push event is received more than once
- **THEN** the same channel identity is updated without creating a duplicate

### Requirement: Bounded synchronization

The system SHALL enforce documented six-month boundaries, pagination limits,
fixed upper windows, and splitting before the 10,000-row cap.

#### Scenario: A window is too large

- **WHEN** a query window can reach the documented row cap
- **THEN** the system splits the window and preserves stable timestamp/id
  ordering without skipping records

### Requirement: Read-only integration

The system SHALL expose no client or admin action for XianGuanJia write
operations.

#### Scenario: Operator uses the admin UI

- **WHEN** an authorized operator views or synchronizes channel data
- **THEN** only local writes and documented third-party reads can occur

### Requirement: Safe admin access

The system SHALL provide permission-controlled admin query and sync monitoring
with private-field masking, light/dark mode, and `zh-CN`/`en`.

#### Scenario: Ordinary order list access

- **WHEN** an operator without raw-payload permission views an order
- **THEN** private fields and unrestricted raw JSON are not returned

### Requirement: Authorization and after-sale alerts

The system SHALL create deduplicated, source-linked alerts for authorization
expiry/invalid state, documented guarantee health failures, after-sale timeout,
and repeated synchronization failures.

#### Scenario: Shop authorization expires

- **WHEN** a shop changes from authorized to expired or invalid
- **THEN** synchronization is stopped safely and one actionable alert is shown

### Requirement: Safe replay

The system SHALL replay failed durable events or pages without duplicating
records or advancing an unsafe cursor.

#### Scenario: Operator retries a failed sync page

- **WHEN** the same stored page is processed again
- **THEN** accepted records are idempotently reused and the run audit records
  the replay result

### Requirement: Channel-derived rental order

The system SHALL convert an eligible channel order into at most one internal
rental order while preserving the source record and conversion version.

#### Scenario: Remark parsing fails

- **WHEN** a paid order has missing or invalid rental dates
- **THEN** its paid amount remains available for reporting and the order enters
  manual review without a fabricated schedule

### Requirement: Physical-device scheduling

The system SHALL schedule uniquely identified device instances using
`Asia/Shanghai` half-open occupied intervals.

#### Scenario: Device assignment conflicts

- **WHEN** two transactions attempt overlapping effective schedules for the
  same device
- **THEN** at most one assignment succeeds and no partial schedule remains
