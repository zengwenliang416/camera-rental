# Quality Review: 005-rental-conversion

## Verdict

needs-fix

## Separation Of Concerns

- Pure seller-remark parsing remains separated, but the conversion service now
  also creates shipment-derived mappings and resolves shipment reviews.

## Component Cohesion / Coupling

- `XianyuRentalConversionServiceImpl` is coupled to both the durable conversion
  lifecycle and a later shipment workflow, increasing the number of reasons
  the transaction changes.

## Test Quality

- Tests cover stored periods, high-value integer cents, replay, pending
  remarks, explicit review, and shipment-selected mapping. The latter tests
  confirm the scope coupling rather than isolating it.

## Error Handling

- Conversion failures in `autoConvertAfterPersist` are swallowed to preserve
  ingestion, but logging `exception.toString()` can expose implementation or
  database detail instead of a safe classified code.

## Reuse / Duplication

- Shared parsers and mapper locks are reused. Shipment mapping policy is
  duplicated into the core conversion transaction instead of remaining in the
  shipment boundary.

## Complexity Delta

- The service now handles conversion, pending/review state, item occupancy
  repair, shipment mapping, and review resolution.

## Required Fixes

- Separate shipment-specific mapping/review behavior from core conversion and
  log only safe classified failure codes from automatic conversion.
