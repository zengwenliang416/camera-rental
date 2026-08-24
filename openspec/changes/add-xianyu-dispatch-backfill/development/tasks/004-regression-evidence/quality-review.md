# Quality Review: 004-regression-evidence

## Verdict

approved

## Separation Of Concerns

- The focused cases are added to the existing
  `XianyuOrderShipServiceTest`, reusing the established mocks and fixtures
  instead of creating a parallel production abstraction. This is an
  appropriate test seam for the service-level contract.
- The task owns both the regression cases and their exact managed receipts; the
  current evidence is bound to the final reviewed Git baseline.

## Component Cohesion / Coupling

- The new helpers (`backfillReq`, `shippedOrder`, and existing fixture helpers)
  keep repeated state construction out of individual assertions. The tests
  exercise the service boundary rather than reaching into controller details.
- No production extraction is warranted for this evidence task. The test class
  intentionally covers both the existing remote-shipping contract and the
  local-backfill branch at the shared service boundary; a database-backed
  fixture belongs to Verification 2.0 rather than more production code.

## Test Quality

- The current managed receipt records `XianyuOrderShipServiceTest` at 35 tests
  with zero failures, errors, or skips and a successful Maven reactor.
- `tasks.md` records 4.1-4.4 complete. The focused suite covers
  `refundStatus=5`, closed/cancelled/pending rejection, non-shippable devices,
  tenant isolation, persistence/Delivery failure propagation, idempotency,
  conflicts, conversion, and no remote/config calls.
- The current validation log contains system-executed receipts for the admin
  type, ESLint, Prettier, and diff checks, all bound to the same HEAD.
- Mockito rollback assertions do not replace a database-backed rollback or
  concurrency oracle; that limitation is explicitly assigned to Verification
  2.0.

## Error Handling

- Several rejection tests assert no assignment, Shipment, Delivery, or remote
  write interaction, which is useful negative-path coverage.
- The negative-path suite now covers the previously missing refund, closed,
  non-shippable, tenant, conflict, and persistence/Delivery cases. Persisted
  rollback remains a runtime proof obligation rather than an unhandled test
  error path.

## Reuse / Duplication

- Existing order, device, item, shop, and Delivery fixtures are reused. The
  additional tests do not duplicate production logic or add test dependencies.

## Complexity Delta

- The test-only delta is substantial but bounded. It improves interaction
  coverage without changing runtime complexity, and does not justify new
  production abstractions.

## Acceptance Assertions Verified

- `A5`, `A6`, `A7`, `A8`, `A9`, `A10`, `A11`, and `A12` are covered by the
  current source review, 35-test backend receipt, and managed Admin
  type/lint/format/diff receipts. Persisted rollback, concurrency, red-team,
  E2E, and sensory execution remain Verification 2.0 work.

## Required Fixes

- No development regression-evidence fix is required. Verification 2.0 must
  still run the database-backed rollback/concurrency and red-team cases, plus
  the admin E2E and sensory matrix; the current 35-test and static receipts
  must not be promoted as those runtime proofs.
