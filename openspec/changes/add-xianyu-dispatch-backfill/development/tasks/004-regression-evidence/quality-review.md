# Quality Review: 004-regression-evidence

## Verdict

needs-fix

## Separation Of Concerns

- The focused cases are added to the existing
  `XianyuOrderShipServiceTest`, reusing the established mocks and fixtures
  instead of creating a parallel production abstraction. This is an
  appropriate test seam for the service-level contract.
- The task still owns both regression cases and exact validation receipts; the
  latter are absent from the current development artifacts.

## Component Cohesion / Coupling

- The new helpers (`backfillReq`, `shippedOrder`, and existing fixture helpers)
  keep repeated state construction out of individual assertions. The tests
  exercise the service boundary rather than reaching into controller details.
- No production extraction is warranted for this evidence task, but the test
  class now mixes formal remote-shipping and local-backfill scenarios; an
  integration fixture is needed for cross-service transaction behavior rather
  than more Mockito setup.

## Test Quality

- The exact backend command passed: `XianyuOrderShipServiceTest` ran 28 tests
  with zero failures and the Maven reactor built successfully.
- `tasks.md` explicitly leaves 4.2, 4.3, and 4.4 unchecked
  (`tasks.md:32-35`). The current tests do not cover `refundStatus=5`,
  backfill non-shippable devices, persistence failure, or a database-backed
  rollback of assignment/schedule/device/Shipment/order changes.
- The Delivery rollback case only checks that a mock was called and that the
  method has a rollback annotation (`XianyuOrderShipServiceTest.java:351-384`);
  it does not observe post-rollback database state.
- No current system-executed admin type, ESLint, Prettier, or browser/sensory
  receipt is present in `development/validation-log.jsonl`.

## Error Handling

- Several rejection tests assert no assignment, Shipment, Delivery, or remote
  write interaction, which is useful negative-path coverage.
- The missing refund-state case and missing persistence-failure case leave
  important no-partial-write paths unguarded. The broad assignment error
  remapping identified in task 002 is also not caught by the regression suite.

## Reuse / Duplication

- Existing `validShop`, device, order, item, and Delivery result fixtures are
  reused. The additional tests do not duplicate production logic or add test
  dependencies.

## Complexity Delta

- The test-only delta is substantial but bounded at roughly 309 lines. It
  improves interaction coverage without changing runtime complexity; the
  missing integration layer is a coverage gap, not a reason to add more
  production abstractions.

## Required Fixes

- Complete 4.2 with refund-state, non-shippable, persistence-failure, and
  rollback assertions that observe real transactional state.
- Complete 4.3 and 4.4 with exact system-executed Maven and admin validation
  receipts after final edits; do not rely on the 28-test count alone.
- Add or record the required browser/sensory evidence for permission,
  validation, loading, conflict, success, locale, theme, and narrow layout
  behavior.
- Populate the task report, ledger, and validation entries before handoff.
