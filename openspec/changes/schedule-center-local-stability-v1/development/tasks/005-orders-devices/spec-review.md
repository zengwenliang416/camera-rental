# Spec Review: 005-orders-devices

## Verdict

approved

## Missing Requirements

- No blocking task-level requirement is missing from the current allowed-files
  implementation.
- Ordinary order filtering uses only status, channel, order number, and device
  model. Customer name and phone remain masked mapped values and are not
  included in search, URL state, or browser persistence.
- General order cards expose no direct return mutation. Assignment, shipment,
  and device-detail intents combine backend action flags or assigned identity
  with their existing permissions, including `rental:device:query`.
- Registered and per-model device totals derive from management-returned device
  records. The UI explicitly rejects continuous-number and unimported-inventory
  assumptions.
- Device availability is status aware: only `IDLE` may use localized immediate
  availability, non-idle states use a safe unavailable presentation unless a
  concrete server date exists, and mapper-created warehouse/availability copy
  is normalized before locale-specific rendering.

## Extra Behavior

- No new API, database field, inventory mutation, return command, or browser
  persistence was introduced.
- General order cards add navigation into existing assignment, shipment, and
  device-detail surfaces only. The direct return mutation was removed and
  replaced with explanatory operational-flow copy.
- The device read model recognizes the current mapper's Chinese and English
  warehouse prefixes solely to separate the warehouse value from localized UI
  copy; it does not change device state or infer availability.

## Misunderstood Requirements

- None found. The current implementation preserves masked ordinary-order
  presentation, existing operational workflow entry points, backend action
  authority, permission separation, and the registered-asset boundary.

## Cannot Verify From Diff

- The current local management snapshot has no orders or devices. Populated
  card rendering, action navigation, responsive behavior, and light/dark
  sensory checks remain assigned to task 007/change-level verification. No
  database fixture or invented business record is required for this task
  review.
- Successful populated assignment, shipment, and device-detail navigation
  against live backend records remains a final change-level E2E concern. Static
  inspection and executed tests verify the intent and permission gates without
  claiming a live business operation.

## Acceptance Assertions Verified

- A3

## Required Fixes

- No blocking fixes remain for task 005 before development handoff.
