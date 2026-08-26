# Task Brief: 008-tenant-device-catalog

## Goal

Allow a store administrator with `rental:device:create` permission to add
tenant-scoped device categories and models with numbering prefixes from the
existing device-create dialog.

## In Scope

- Additive tenant-aware category/model tables and current-catalog seed data.
- Backend catalog query and category/model create endpoints.
- Transactional device-number allocation from a model prefix.
- Existing-page quick-create dialogs with refresh and auto-selection.
- Focused backend/frontend tests, type-check and build.

## Out Of Scope

- A separate catalog page or menu.
- Applying migrations to any database.
- Editing or deleting existing catalog rows.
- Order, assignment, schedule, dispatch, return or inspection changes.
- Commit, push, deployment or production verification.

## Reuse

- Existing `Dialog`, Element Plus `el-select` footer slots and
  `open('create')`/`success` form pattern.
- Existing `TenantBaseDO`, MyBatis Plus mapper and row-lock patterns.
- Existing `RentalDeviceCode` normalization and two-digit formatting.
- Existing `rental:device:create` permission.

## Verification

- Focused Maven tests for catalog normalization, uniqueness, tenant ownership,
  automatic numbering and ERP inbound classification.
- Admin catalog model tests, `pnpm ts:check` and `pnpm build:local`.
- Scoped `git diff --check`.
