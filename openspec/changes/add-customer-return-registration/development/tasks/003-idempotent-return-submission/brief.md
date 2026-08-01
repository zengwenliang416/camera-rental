# Task Brief: 003-idempotent-return-submission

## Goal

Implement safe public context, device matching, idempotent final submission and
conditional Return Delivery creation.

## Vertical Slice

A customer submits logistics, assigned device serials and confirmed photos and
receives one stable receipt or a review-required result without changing
warehouse lifecycle state.

## In Scope

- Public context and submission controllers, VOs and services.
- Serial normalization, assigned-order matching and mismatch persistence.
- Return Delivery create/reuse adapter and transaction boundary.
- Extract matching and normalization services; reuse existing order, assignment, device and Delivery services.

## Files Allowed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz`
- `openspec/changes/add-customer-return-registration`

## Verification Commands

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='*ReturnRegistration*' test`
- `git diff --check -- camera-rental-server/yudao-module-rental`

## Stop Conditions

- Stop if a customer submission would mutate device, order, inspection or schedule state.
- Stop if Delivery creation requires bypassing existing tenant/order validation.
- Stop if duplicate concurrent submissions can create duplicate business rows.
