# Spec Review: 007-admin-operations

## Verdict

needs-fix

## Missing Requirements

- The ordinary order page is documented as containing no sensitive phone or
  address data, but `XianyuOrderAdminService` now returns full receiver name,
  mobile, and address. Its test explicitly asserts those unmasked values.
- The task requires admin operations without third-party write capabilities.
  The current controller exposes `/order/ship`, and the module contains a
  `XianyuWriteClient` allowlisting `/api/open/order/ship`.

## Extra Behavior

- Shipment OCR, pending-shipment search, device binding, and remote order
  shipment are present in the same admin/controller surface even though this
  V1 task explicitly excludes fulfillment writes.

## Misunderstood Requirements

- A masked browser presentation is not equivalent to masking at the ordinary
  API boundary. Any caller with `rental:xianyu:query` currently receives full
  recipient data.

## Cannot Verify From Diff

- The local schedule-center regression shows masked visible customer
  information and a disabled write gate, but it does not prove that the
  ordinary admin order API is masked; the current unit test proves the
  opposite.
- A1 has no substantive statement and cannot authorize the write-scope or
  privacy changes.

## Required Fixes

- Restore backend-default masking for the ordinary order page and expose full
  fulfillment contact only through a separately permissioned, audited endpoint
  if operationally required.
- Keep the shipment write client/controller/workbench in its separately scoped
  approved change, or update this active change's requirements and acceptance
  before treating them as part of task 007.
- Update the task report and run current API privacy and permission tests.
