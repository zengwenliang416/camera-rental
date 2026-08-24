# Quality Review: 002-transactional-dispatch-backfill

## Verdict

approved

## Separation Of Concerns

- The controller remains a thin permission-gated entry point
  (`XianyuOrderController.java:88-95`); aggregate coordination is kept in
  `XianyuOrderShipService.backfillDispatch` rather than duplicating assignment
  or scheduling rules in the controller.
- The service does not call `XianyuRuntimeConfigService` or `XianyuWriteClient`
  from the backfill branch (`XianyuOrderShipService.java:201-270`), preserving
  the required local-only side-effect boundary.

## Component Cohesion / Coupling

- Reuse of conversion, assignment, device dispatch, Shipment, and Delivery
  services is directionally correct. The new mapper helper also keeps the
  business-key lock at the persistence boundary.
- The aggregate method is coupled to several service/mapper contracts, but that
  is the approved domain boundary. No controller or frontend code reaches into
  those mutable contracts.

## Test Quality

- Independent execution of the focused Maven command passed
  `XianyuOrderShipServiceTest`: 35 tests, 0 failures, 0 errors, 0 skipped.
  The current class covers success, refund/closed/cancelled and pending
  rejection, tenant isolation, non-shippable devices, business-key conflicts,
  conversion, idempotency replay/conflict, assignment-state reuse,
  persistence/Delivery failure propagation, and zero remote/config calls
  (`XianyuOrderShipServiceTest.java:190-644`).
- The Mockito rollback assertions do not replace a database-backed rollback or
  concurrency oracle. That limitation is explicitly assigned to Verification
  2.0 in the handoff, so it is not treated as a development component-quality
  defect here.

## Error Handling

- `requireBackfillEligible` now rejects cancellation, closed statuses `23/24`,
  and persisted successful refund status `5` before device lookup
  (`XianyuOrderShipService.java:340-348`).
- Assignment conflicts preserve the typed backfill conflict code while
  idempotency reuse retains its dedicated error
  (`XianyuOrderShipService.java:432-443`). Existing `DISPATCHED` assignments
  are reused only with a `RENTED` device; `ASSIGNED` assignments go through the
  existing dispatch service (`XianyuOrderShipService.java:403-429`).
- The method is annotated with `@Transactional(rollbackFor = Exception.class)`;
  the remaining proof obligation is runtime rollback, not an unhandled error
  path in this diff.

## Reuse / Duplication

- The implementation reuses the existing conversion, assignment, dispatch, and
  Delivery services and factors the shared Delivery command builder. No second
  inventory or scheduling implementation was introduced.
- The backfill-specific state checks are kept in helpers and reuse the existing
  typed assignment and device-ops contracts.

## Complexity Delta

- The change adds a bounded local branch and focused helpers to an already
  domain-specific shipping service. The orchestration is non-trivial, but its
  responsibilities match the approved aggregate and the 35-test suite covers
  the principal decision paths.

## Acceptance Assertions Verified

- `A4`, `A5`, `A6`, `A7`, `A8`, `A9`, `A10`, and `A11` are covered by the
  current backend source review and the 35-test managed receipt. `A11` still
  requires a database-backed rollback oracle in Verification 2.0; the unit
  evidence is not promoted as persisted rollback proof.

## Required Fixes

- No implementation-quality fix is required for this development task.
- Verification 2.0 must still run the real database rollback/concurrency and
  red-team cases already listed in `handoff-to-verify.md`; the current unit
  result must not be reported as database proof.
