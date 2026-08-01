# Task Brief: 005-admin-return-operations

## Goal

Provide authorized management workflows for issuing, finding, inspecting,
revoking and reviewing customer return registrations.

## Vertical Slice

Operations staff can create and copy a link from an order, page registrations,
inspect full authorized details and accept or reject a review-required submission.

## In Scope

- Admin service/controller APIs and typed Vue API client.
- Element Plus filter table, pagination, create action and detail drawer.
- Review validation, photo preview, permission and race handling tests.
- Create focused admin components; reuse existing table, drawer, upload preview and permission patterns.

## Files Allowed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz`
- `camera-rental-admin/src/api/rental/returnRegistration.ts`
- `camera-rental-admin/src/views/rental/return-registration`
- `openspec/changes/add-customer-return-registration`

## Verification Commands

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='*ReturnRegistrationAdmin*' test`
- `pnpm ts:check`
- `pnpm build:prod`

## Stop Conditions

- Stop if permissions rely only on hidden frontend buttons.
- Stop if review acceptance does not repeat backend relationship validation.
- Stop before editing existing dirty global locale files.
