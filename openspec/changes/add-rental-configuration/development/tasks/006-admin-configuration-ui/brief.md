# Task Brief: 006-admin-configuration-ui

## Goal

Deliver the approved three-tab Rental Configuration page for catalog, exact
channel mapping and remark guidance across locale, theme and narrow layouts.

## Vertical Slice

An authorized administrator can manage catalog entries, configure a single-model
or synchronized multi-SKU product rule after impact preview, and copy a concise
approved remark template from one page.

## In Scope

- Checklist items `6.1` through `6.6`.
- Typed API clients, page/route, three tabs, states, permissions, locales and tests.
- Removal of category/model quick-create controls from Rental Device.

## Out Of Scope

- Frontend-side authoritative matching, scheduling or third-party requests.
- A page-local theme or locale subsystem.

## Files Allowed

- `camera-rental-admin/src/api/rental`
- `camera-rental-admin/src/views/rental`
- `camera-rental-admin/src/locales`
- `camera-rental-admin/tests`

## Interfaces / Seams

- Backend `/admin-api/rental/configuration/**` APIs and shared device catalog query.

## Components To Create

- Approved page, panels, dialogs, drawer, SKU table, identifier summary and impact dialog.

## Components To Reuse

- `ContentWrap`, `Pagination`, Element Plus, `v-hasPermi`, request client and existing theme/locale controls.

## Components To Extract

- Shared identifier presentation/copy behavior and category-to-model selection helpers.

## API / Data Flow Contracts

- All external identifiers are TypeScript strings.
- Multi-SKU selection contains only synchronized SKUs returned for the selected shop/item.
- Dangerous mutations require a fresh impact preview and backend version token.

## State / Error / Empty / Loading Behavior

- Loading uses skeleton/disabled mutation actions; error is distinct from empty.
- Empty explains synchronization prerequisites; 403 has an explicit permission state.
- Disabled rules remain readable; narrow layout preserves actions and identifier labels.

## TDD Requirement

- Add model/component tests before or alongside page implementation.

## Verification Commands

- `cd camera-rental-admin && pnpm ts:check`
- `cd camera-rental-admin && pnpm lint`
- `cd camera-rental-admin && pnpm build:prod`

## Stop Conditions

- Stop if the backend contract is not finalized or exact SKU ownership is unavailable.
- Stop if a design change departs from the approved prototype variant.

## Unsafe Assumptions

- JavaScript numeric safety cannot be assumed for external identifiers; keep them as strings.
