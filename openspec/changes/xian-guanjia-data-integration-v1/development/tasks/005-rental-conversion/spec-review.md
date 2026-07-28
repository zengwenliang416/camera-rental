# Spec Review: 005-rental-conversion

## Verdict

needs-fix

## Missing Requirements

- The task and parent acceptance require missing rental dates to create an
  actionable manual-review record. Current conversion instead leaves missing
  remarks/dates in `PENDING` and explicitly does not create a review.
- The task requires an existing explicit `MAPPED` product/SKU mapping before
  conversion. `convertForShipment` can create or overwrite that mapping from a
  selected device model.

## Extra Behavior

- Shipment-specific mapping creation and shipment-review resolution are now
  implemented inside the conversion service even though device shipment is an
  explicit non-goal for this task.

## Misunderstood Requirements

- The newer business decision to keep missing remarks pending until a later
  sync may be valid, but the active change requirements, acceptance text,
  brief, and report were not updated to authorize that behavior.

## Cannot Verify From Diff

- The report still describes every incomplete conversion as a reusable manual
  review, which is false for the current `PENDING` path.
- A1 does not state the expected pending-versus-review behavior and therefore
  cannot adjudicate the conflict.

## Required Fixes

- Update the approved requirements, acceptance, brief, and report to define
  the new pending-until-refresh behavior, or restore manual review for missing
  date prerequisites.
- Move shipment-selected mapping creation and review resolution into the
  separately scoped shipment change/service, leaving this conversion slice
  dependent on an explicit pre-existing mapping.
