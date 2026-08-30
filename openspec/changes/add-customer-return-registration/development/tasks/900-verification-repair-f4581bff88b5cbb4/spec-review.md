# Spec Review

## Verdict

needs-fix

## Missing Requirements

- The repair is not executable by the managed Verification Kernel. The Kernel
  serializes only the `return-success-idempotent` scenario function and
  recompiles it in an isolated VM, but the function calls the module-scoped
  `installReturnRegistrationMock` at
  `tests/specnav/customer-return-registration.js:220`. Recompiling the
  scenario exactly as the Kernel does produces
  `ReferenceError: installReturnRegistrationMock is not defined` before any
  assertion runs.

## Extra Behavior

- The shared module-level mock helper works when the CommonJS module is loaded
  and the exported scenario is invoked directly, but that execution shape is
  outside the Kernel's serialized-scenario contract. No product behavior was
  changed in this commit.

## Misunderstood Requirements

- Removing the denied `page.route` call is necessary but not sufficient. A
  project-owned Playwright scenario must also be self-contained after
  `Function.prototype.toString()` serialization. The report's direct module
  invocation through the API guard does not exercise that isolation boundary.

## Cannot Verify From Diff

- Formal Verification rerun and regression evidence at commit
  `1ffb55fc8169325a9c0cd42e629f4642271a6258` are not present.
- The direct Chromium probe through the installed Kernel API guard passed all
  four targeted assertions with an empty denied-method list, but the actual
  serialized Kernel path fails before navigation and therefore cannot confirm
  the repair.

## Acceptance Assertions Verified

- `vc01-fixed-entry-visible`
- `vc01-normalized-machine-code`
- `vc01-receipt-accepted`
- `vc01-submit-count`

These are the complete frozen failure assertion ids. They were observable as
passing in a direct Chromium probe through the same Kernel guard, but they are
not accepted as verified for task approval because the Kernel's required
serialized execution cannot reach them.

## Required Fixes

- Make `return-success-idempotent` self-contained under Kernel serialization,
  for example by defining the mock installer inside the scenario or otherwise
  embedding all required code in the serialized function.
- Rerun the scenario through the actual Kernel worker/serialization path and
  record all four assertion results plus an empty Playwright denied-method
  list. Update the repair report with that executed evidence.
