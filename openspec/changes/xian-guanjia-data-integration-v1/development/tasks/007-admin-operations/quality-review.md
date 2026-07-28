# Quality Review: 007-admin-operations

## Verdict

needs-fix

## Separation Of Concerns

- Query, synchronization, conversion, OCR, and remote shipment operations are
  now combined under `XianyuOrderController`.

## Component Cohesion / Coupling

- The ordinary order response is coupled to schedule-center fulfillment needs,
  causing a low-privilege query endpoint to expose full recipient data.

## Test Quality

- Current tests cover rental periods, receiver snapshots, assignment data, and
  waybills, but they assert full PII in the ordinary page instead of protecting
  the parent privacy requirement.

## Error Handling

- Shipment permissions and the server write gate exist. The response boundary
  lacks a safe default for recipient privacy.

## Reuse / Duplication

- Existing domain services and UI infrastructure are reused. The shipment
  workflow is duplicated into a change whose admin task was designed to remain
  read-only.

## Complexity Delta

- The admin order surface now spans channel queries, local conversion,
  fulfillment contact, OCR, device assignment, and third-party shipment.

## Required Fixes

- Split privileged fulfillment contact/shipment operations from ordinary order
  querying and add backend masking, audit, and permission tests for the
  separate boundary.
