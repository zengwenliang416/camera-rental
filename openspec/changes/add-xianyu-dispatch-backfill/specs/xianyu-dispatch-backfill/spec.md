## ADDED Requirements

### Requirement: Eligible shipped orders expose a Web-admin backfill action
The system SHALL expose the dispatch backfill action only for uncancelled Xianyu orders whose channel status is `21` or `22`, and SHALL require the existing `rental:xianyu:ship` permission.

#### Scenario: Eligible shipped order
- **WHEN** an authorized administrator views an uncancelled Xianyu order with status `21` or `22`
- **THEN** the order row exposes the dispatch backfill action

#### Scenario: Ineligible order
- **WHEN** an administrator views a pending, refunded, cancelled, or closed Xianyu order
- **THEN** the order row does not expose the dispatch backfill action

#### Scenario: Missing permission
- **WHEN** an administrator without `rental:xianyu:ship` views an otherwise eligible order
- **THEN** the backfill action is unavailable and the backend endpoint rejects direct access

### Requirement: Administrators can enter actual dispatch facts without scanning hardware
The system SHALL provide a Web dialog that accepts the actual device number, existing waybill number, carrier code and name, actual ship time, and a required backfill reason without requiring a physical scanner, camera, or staff mobile application.

#### Scenario: Complete keyboard-entry form
- **WHEN** an authorized administrator opens the dialog and enters all required values using the Web interface
- **THEN** the client submits a typed dispatch-backfill request

#### Scenario: Invalid form
- **WHEN** a required field is empty, the waybill is invalid, or the reason exceeds the accepted length
- **THEN** the client prevents submission and presents a localized field error

### Requirement: Backfill never performs a remote shipment write
The system MUST treat dispatch backfill as a local historical-fact repair and MUST NOT read the Xianyu write-enabled runtime configuration, call `XianyuWriteClient`, or invoke any XianGuanJia write endpoint.

#### Scenario: Successful local backfill
- **WHEN** a valid dispatch backfill request succeeds
- **THEN** local rental and logistics records are updated and no remote shipment request is emitted

#### Scenario: Xianyu write configuration is disabled
- **WHEN** the Xianyu write-enabled configuration is disabled or unavailable
- **THEN** an otherwise valid local backfill remains eligible because the path does not read that configuration

#### Scenario: Shop authorization has expired
- **WHEN** the channel order belongs to a shop in the current tenant but its external authorization is expired
- **THEN** the local historical backfill may proceed without an external authorization check

### Requirement: Backend revalidates order, tenant, device, and assignment state
The backend SHALL lock and validate the channel order, current-tenant shop, physical device, rental-order conversion, order item, active assignment, and occupied schedule before accepting a backfill.

#### Scenario: Pending order rejected
- **WHEN** the selected channel order is still pending shipment
- **THEN** the backend returns `XIANYU_DISPATCH_BACKFILL_ORDER_NOT_SHIPPED` before any local mutation

#### Scenario: Refunded or closed order rejected
- **WHEN** the selected channel order is cancelled, refunded, or closed
- **THEN** the backend returns `XIANYU_DISPATCH_BACKFILL_ORDER_CLOSED` before any local mutation

#### Scenario: Device is not shippable
- **WHEN** a new or assigned device is disabled or not `AVAILABLE`
- **THEN** the backend rejects the request without changing assignment, schedule, shipment, or delivery state

#### Scenario: Cross-tenant shop
- **WHEN** the channel order references a shop outside the current tenant
- **THEN** the backend rejects the request without local or remote side effects

### Requirement: Successful backfill restores the local dispatch aggregate
The system SHALL transactionally create or reuse the internal rental conversion, first rental order item, device assignment, occupied schedule, local dispatch state, device shipment evidence, outbound Delivery, and channel-order logistics values.

#### Scenario: New assignment and dispatch
- **WHEN** an eligible order has no active assignment for the selected available device
- **THEN** the system creates the assignment and occupied schedule, transitions the device to `RENTED`, transitions the assignment to `DISPATCHED`, creates `ADMIN_BACKFILL` shipment evidence, and creates or reuses an outbound Delivery

#### Scenario: Existing assigned device
- **WHEN** the same device is already actively assigned to the selected order item with status `ASSIGNED`
- **THEN** the system reuses the assignment and dispatches it exactly once

#### Scenario: Existing dispatched device
- **WHEN** the same device is already `RENTED` and its active assignment is `DISPATCHED`
- **THEN** the system reuses that state without invoking dispatch again and still restores missing shipment and Delivery evidence

#### Scenario: Unmapped channel order
- **WHEN** the eligible channel order has no internal rental order
- **THEN** the system converts it using the selected device model and continues only after a valid first rental order item with occupied dates exists

### Requirement: Backfill is idempotent and conflict-safe
The system SHALL normalize request identifiers and logistics strings, bind the complete request to an idempotency hash, and reject incompatible replays or business-key collisions.

#### Scenario: Matching idempotent replay
- **WHEN** the same idempotency key is submitted with the same order, device, waybill, carrier, ship time, and reason
- **THEN** the system returns the existing result without duplicate assignment, schedule, shipment, Delivery, or dispatch writes

#### Scenario: Reused idempotency key with different request
- **WHEN** an existing idempotency key is submitted with a different order, device, logistics value, ship time, or reason
- **THEN** the system returns `XIANYU_SHIP_IDEMPOTENT_KEY_REUSED`

#### Scenario: Same business waybill bound to another device
- **WHEN** the same channel order, waybill number, and carrier code already identify a shipment for another device
- **THEN** the system returns `XIANYU_DISPATCH_BACKFILL_CONFLICT`

### Requirement: Backfill writes are atomic
The system SHALL execute the local backfill in one transaction and SHALL leave no partial assignment, schedule, device, shipment, Delivery, or channel-order update after failure.

#### Scenario: Delivery creation fails
- **WHEN** assignment and local dispatch succeed but outbound Delivery creation fails
- **THEN** the transaction rolls back every mutation made by the backfill request

#### Scenario: Shipment persistence fails
- **WHEN** shipment evidence cannot be persisted
- **THEN** the transaction rolls back conversion, assignment, schedule, device, and channel-order changes

### Requirement: The Web experience is localized and theme-compatible
The system SHALL provide complete `zh-CN` and `en` copy and SHALL remain usable in the existing admin light and dark themes on desktop and narrow layouts.

#### Scenario: Theme and locale matrix
- **WHEN** the administrator switches between light/dark and `zh-CN`/`en`
- **THEN** the action, dialog labels, remote-call warning, validation, loading, conflict, error, and success states remain readable and operable
