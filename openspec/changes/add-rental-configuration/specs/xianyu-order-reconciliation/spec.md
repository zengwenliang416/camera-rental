## ADDED Requirements

### Requirement: Channel identifiers are stored with exact semantics
The system MUST persist XianGuanJia product ID, Xianyu item ID, XianGuanJia SKU ID, and Xianyu SKU ID in separate fields without fallback.

#### Scenario: Complete order detail
- **WHEN** order detail contains `goods.product_id`, `goods.item_id`, and `goods.sku_id`
- **THEN** each value is stored in its corresponding explicit field

#### Scenario: Missing item id
- **WHEN** order detail omits `goods.item_id`
- **THEN** Xianyu item id remains empty and is not filled from `goods.product_id`

#### Scenario: Xianyu SKU association
- **WHEN** synchronized product data uniquely links the order's XianGuanJia SKU to `xy_sku_id`
- **THEN** the order may store the linked Xianyu SKU id as derived evidence

### Requirement: Normal durable channel orders immediately create internal orders
The system SHALL create exactly one internal rental order and item after a non-filtered order detail is durably persisted, regardless of remark, date, or model completeness.

#### Scenario: Order without remark or mapping
- **WHEN** a paid order detail has no seller remark and no model mapping
- **THEN** one rental order and item are created with the pay amount and a waiting preparation status

#### Scenario: Replayed order detail
- **WHEN** the same shop and external order is persisted again
- **THEN** the existing internal order is updated and no duplicate order or item is created

### Requirement: Preparation readiness gates allocation
The system SHALL compute a preparation status from identifiers, effective remark plan, exact model mapping, occupied dates, and conflicts, and SHALL require `READY` before allocation.

#### Scenario: Incomplete order
- **WHEN** model or occupied dates are missing
- **THEN** allocation and schedule creation are rejected with a stable preparation reason

#### Scenario: Complete order
- **WHEN** model and valid plan are complete and no conflict exists
- **THEN** the order becomes `READY` and may enter allocation

### Requirement: Valid remarks incrementally update the same order
The system SHALL store every remark parse attempt and SHALL apply only valid plan snapshots to the existing rental order.

#### Scenario: Later valid remark
- **WHEN** a previously incomplete order receives a valid remark
- **THEN** the same order item receives the parsed plan and readiness is recomputed

#### Scenario: Later invalid remark
- **WHEN** an order with a valid effective plan receives an empty, incomplete, or invalid remark
- **THEN** the failed parse is audited and the previous effective plan remains unchanged

### Requirement: Remark updates respect fulfillment facts
The system MUST NOT derive or overwrite actual allocation, dispatch, replacement, return, inspection, refund, or settlement facts from seller remarks.

#### Scenario: Early return remark
- **WHEN** a dispatched order's expected return date moves earlier
- **THEN** only the expected return plan changes and the occupied schedule remains effective until actual return inspection completes

#### Scenario: Extension conflict
- **WHEN** a dispatched order's expected return date moves later and overlaps a future effective schedule
- **THEN** the original schedule remains and a conflict review is opened

#### Scenario: Replacement suffix
- **WHEN** a remark contains the replacement suffix
- **THEN** the system creates an operational review and does not overwrite the current assignment device

### Requirement: Mapping changes protect assigned orders
The system SHALL automatically apply mapping changes only to orders without a concrete device assignment.

#### Scenario: Unassigned order
- **WHEN** an exact mapping is added for an unassigned waiting order
- **THEN** the order item model is updated and readiness is recomputed

#### Scenario: Assigned order mismatch
- **WHEN** a mapping change disagrees with an assigned or dispatched order
- **THEN** the existing model and device facts remain and the mismatch is reported for review

### Requirement: Historical reconciliation is resumable and non-destructive
The system SHALL reconcile historical orders in bounded idempotent batches and SHALL never delete or reverse existing fulfillment history.

#### Scenario: Historical normal order
- **WHEN** a non-filtered channel order lacks an internal rental order
- **THEN** reconciliation creates the missing order and item exactly once

#### Scenario: Historical eligible filtered order
- **WHEN** a matching skipped product has no internal order or fulfillment fact
- **THEN** reconciliation marks the channel order `CONFIG_SKIPPED`

#### Scenario: Historical fulfilled order matches skip rule
- **WHEN** a matching order already has an internal order, assignment, schedule, dispatch, return, inspection, or settlement fact
- **THEN** reconciliation preserves all history and reports the order instead of skipping or reversing it

### Requirement: Reconciliation remains observable and private
The system SHALL record bounded task counts and safe reason codes without logging raw payloads, credentials, complete phone numbers, or addresses.

#### Scenario: Batch completes
- **WHEN** a reconciliation batch finishes
- **THEN** scanned, skipped, created, updated, conflict, review, and failed counts are available

#### Scenario: Record fails
- **WHEN** a record cannot be reconciled safely
- **THEN** the failure is retained with a safe code and the resumable cursor does not skip past the unresolved record
