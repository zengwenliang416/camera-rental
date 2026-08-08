## ADDED Requirements

### Requirement: Device-lane schedule timeline
The system SHALL render the current tenant's single-warehouse schedule as
server-paginated device lanes across 14, 30, or 90 day windows.

#### Scenario: Operator switches the visible range
- **WHEN** an authorized operator selects a 14, 30, or 90 day view
- **THEN** the server returns the requested half-open window and complete schedule segments for each device on the current device page

#### Scenario: Long rental crosses the visible window
- **WHEN** an effective occupied period starts before or ends after the visible window
- **THEN** the client clips the visual bar and displays the corresponding left or right continuation marker without changing the authoritative dates

### Requirement: Server-side device search and pagination
The system SHALL search and paginate device lanes by the current tenant using
device number, serial number, model, device state, and logistics state.

#### Scenario: Search among hundreds of devices
- **WHEN** an operator enters a device number or serial keyword and selects a page size of 25, 50, or 100
- **THEN** the backend returns matching device lanes, a total device count, and the requested page without paginating individual schedule rows

### Requirement: Authoritative occupied lifecycle
The system SHALL treat outbound transit, customer possession, return transit,
and returned-pending-inspection as occupied and SHALL release availability only
after inspection completion or an explicit non-rentable maintenance outcome.

#### Scenario: Returned device is waiting for inspection
- **WHEN** a device has been received at the warehouse but inspection is not complete
- **THEN** the workbench shows the device as occupied and excludes it from available candidates

#### Scenario: Candidate interval overlaps effective occupancy
- **WHEN** a pending order item's occupied interval overlaps an effective device schedule
- **THEN** the backend excludes the device with a stable conflict reason and does not create another effective schedule

### Requirement: Pending allocation work queue
The system SHALL expose pending and partially allocated rental order items with
required, assigned, and remaining quantities.

#### Scenario: Order item is partially allocated
- **WHEN** at least one required device remains unassigned
- **THEN** the item remains in the pending queue with the authoritative remaining quantity

#### Scenario: Operator opens pending order detail
- **WHEN** an operator selects a pending order
- **THEN** the system opens a right-side drawer with internal order, item, assigned-device, date, logistics-risk, and review information allowed by permission

### Requirement: Candidate recommendation and assignment
The system SHALL calculate candidate eligibility on the backend and SHALL use
the existing transactional assignment service for final confirmation.

#### Scenario: Eligible candidates are requested
- **WHEN** an operator requests candidates for a pending order item
- **THEN** the backend returns same-model candidates with eligibility, reason codes, neighboring schedules, logistics state, and next available date

#### Scenario: Candidate becomes stale before confirmation
- **WHEN** another transaction occupies the selected device before confirmation
- **THEN** final assignment fails with a schedule conflict and the client refreshes candidates before another confirmation

### Requirement: Classified device locks
The system SHALL persist classified device locks for order holds, returned
devices awaiting inspection, maintenance isolation, and authorized manual holds.

#### Scenario: Supervisor creates an order hold
- **WHEN** an authorized supervisor reserves a candidate device for a pending order with a reason and planned end time
- **THEN** the backend creates an audited active order-hold record and excludes the device from other candidate results

#### Scenario: Returned device awaits inspection
- **WHEN** a device is registered as returned but inspection is incomplete
- **THEN** the backend creates or retains a system-managed return-inspection lock until the inspection workflow completes

#### Scenario: Operator attempts to release a system-managed lock
- **WHEN** an operator tries to manually release a return-inspection or maintenance lock
- **THEN** the backend rejects the request and requires the owning lifecycle workflow to release it

#### Scenario: Assignment races with a new lock
- **WHEN** a device becomes actively locked after candidate lookup but before final assignment
- **THEN** the transactional assignment rejects the device and returns a stable locked-device conflict

### Requirement: Schedule metrics and exception queue
The system SHALL return window-scoped operational metrics and scheduling-related
exceptions from authoritative server data.

#### Scenario: Workbench loads
- **WHEN** the workbench query succeeds
- **THEN** it shows total, available, occupied, in-transit, pending-allocation, exception, and utilization metrics using the same query scope

#### Scenario: Logistics delay threatens a later order
- **WHEN** return logistics or pending inspection can extend occupancy into a later requirement
- **THEN** the exception queue identifies the affected device, order, risk reason, expected release, and safe next action

### Requirement: Device and logistics detail drawers
The system SHALL provide right-side device and logistics details without
duplicating warehouse execution actions or exposing unnecessary customer data.

#### Scenario: Operator selects a device lane
- **WHEN** an authorized operator clicks a device or schedule segment
- **THEN** a drawer shows device identity, effective schedules, current assignment, outbound and return deliveries, logistics risk, inspection state, and expected release

#### Scenario: Operator refreshes tracking
- **WHEN** an authorized operator requests a logistics refresh from the drawer
- **THEN** the server uses the existing provider integration, stores the safe snapshot, and returns refreshed redacted tracking data

### Requirement: Management-client conformity
The system SHALL implement the workbench with the current Vue 3, Element Plus,
theme, locale, permission, loading, error, and responsive patterns.

#### Scenario: Theme and locale change
- **WHEN** the operator changes the existing admin theme or locale
- **THEN** the workbench renders equivalent light/dark and zh-CN/en content without raw locale keys

#### Scenario: Narrow viewport
- **WHEN** the workbench is used on a narrow viewport
- **THEN** controls remain operable and horizontal scrolling is contained within the timeline region
