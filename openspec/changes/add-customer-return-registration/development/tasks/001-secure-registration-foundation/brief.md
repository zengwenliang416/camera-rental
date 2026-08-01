# Task Brief: 001-secure-registration-foundation

## Goal

Create the additive schema, persistence model and secure token boundary required
to issue an order-bound customer return link.

## Vertical Slice

An authorized operator can create a link for one eligible rental order and the
backend persists only a globally unique token hash with tenant-safe order
references and expiry.

## In Scope

- Migration, menu permissions, DOs, Mappers, enums, error codes and token service.
- Global token lookup followed by explicit tenant restoration.
- Focused tests for entropy, hash-only persistence, expiry, revocation and safe errors.
- Create and extract the token service; reuse current tenant base, order Mapper and test patterns.

## Files Allowed

- `camera-rental-server/sql/mysql/migrations`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz`
- `openspec/changes/add-customer-return-registration`

## Verification Commands

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='*ReturnRegistration*' test`
- `git diff --check -- camera-rental-server openspec/changes/add-customer-return-registration`

## Stop Conditions

- Stop if global token lookup cannot restore tenant context without leaking tenant or order identifiers.
- Stop before modifying an existing dirty logistics implementation without direct review.
- Stop if schema changes would rewrite an executed migration.
