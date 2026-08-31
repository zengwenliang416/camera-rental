# Task Brief: 004-fulfillment-safe-remarks

## Goal

Reparse later seller remarks safely while preserving the last valid plan and all
assigned, dispatched, returned, inspected and financially settled facts.

## Vertical Slice

A later remark can extend, shorten or reschedule an eligible order; the system
classifies the change, updates only mutable plan fields, and creates a review
instead of overwriting immutable fulfillment facts when conflicts exist.

## In Scope

- Checklist items `4.1`, `4.2`, `4.3`, and `4.4`.
- Parse history, effective-plan preservation, change classification and lifecycle guards.

## Out Of Scope

- Remark-driven device assignment, physical swap completion, refund or settlement.

## Files Allowed

- `camera-rental-server/yudao-module-rental`
- `camera-rental-server/sql/mysql`

## Interfaces / Seams

- Seller remark resolver, reconciliation service, schedules, assignments and manual reviews.

## Components To Create

- `XianyuOrderRemarkHistoryService`
- `RentalFulfillmentUpdateGuard`
- Remark plan change classifier and redteam fixtures.

## Components To Reuse

- Existing assignment, delivery, inspection, schedule and manual-review services.

## Components To Extract

- Model and date updates share one lifecycle-aware fulfillment guard.

## API / Data Flow Contracts

- Invalid parses are recorded but do not replace the previous effective plan.
- Replacement intent records history and review; it does not directly replace a device.

## State / Error / Empty / Loading Behavior

- Unassigned orders may update valid plan fields transactionally.
- Assigned or later orders preserve facts and emit an explicit conflict/review reason.
- Early-return text never releases occupancy before confirmed return/inspection facts.

## TDD Requirement

- Add focused and adversarial tests for all named change classes and lifecycle states.

## Verification Commands

- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -pl yudao-module-rental -am test -Dmaven.repo.local=/Volumes/zwl/maven-repository`

## Stop Conditions

- Stop if a remark path can mutate assignment, delivery, inspection, refund or settlement.
- Stop if schedule updates are not protected by the same transaction and lifecycle policy.

## Unsafe Assumptions

- Seller intent is not proof that a physical return, swap or financial action occurred.
