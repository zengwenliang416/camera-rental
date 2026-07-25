# Quality Review: 006-device-allocation

## Verdict

approved

## Separation Of Concerns

- The assignment service coordinates one domain transaction. Mappers own query shape and row locks; data objects own persistence representation; command/result/exception types form the future API seam.

## Component Cohesion / Coupling

- The service has one responsibility: atomically assign one device and create one occupied schedule. It does not own controller authorization, device lifecycle transitions, schedule mutation, or third-party integration.

## Test Quality

- Tests assert persisted IDs, lock call order, replay behavior, conflict boundaries, no-partial-write behavior, and typed failures. Adjacent and overlapping ranges are independently asserted rather than inferred from UI behavior.

## Error Handling

- Input, device, item, order, capacity, overlap, and idempotency mismatches are rejected before writes. The method rolls back on `Exception`; if schedule persistence fails, assignment persistence is not invoked.

## Reuse / Duplication

- Reuses tenant/audit base DOs, MyBatis Plus mapper helpers, existing rental order/item boundaries, the foundation schema, and Spring transaction support. No signing, HTTP client, controller, or duplicate scheduling utility is introduced.

## Complexity Delta

- The slice adds only three persistence entities, three mappers, and one local transactional service. Device CRUD, API/UI, lifecycle transitions, full allocation state, reporting, and MySQL integration harness remain deferred.

## Required Fixes

No required fixes remain for this review.
