# Task Brief: 006-admin-configuration-ui

## Goal

Deliver the approved three-tab Rental Configuration page for catalog, exact
channel mapping and remark guidance across locale, theme and narrow layouts,
including the minimal backend task reference and result query required to close
the asynchronous reconciliation flow.

## Vertical Slice

An authorized administrator can manage catalog entries, configure a single-model
or synchronized multi-SKU product rule after impact preview, and copy a concise
approved remark template from one page.

## In Scope

- Checklist items `6.1` through `6.6`.
- Typed API clients, page/route, three tabs, states, permissions, locales and tests.
- Removal of category/model quick-create controls from Rental Device.
- Configuration-scoped authorized-shop lookup.
- Rule-save reconciliation task references, persisted result counters and result query.
- Incremental migration `056` for reconciliation-run persistence.

## Out Of Scope

- Frontend-side authoritative matching, scheduling or third-party requests.
- A page-local theme or locale subsystem.
- Production migration execution, deployment, historical backfill or real shop writes.

## Files Allowed

- `camera-rental-admin/package.json`
- `camera-rental-admin/src/config/axios/service.ts`
- `camera-rental-admin/src/api/rental`
- `camera-rental-admin/src/views/rental`
- `camera-rental-admin/src/locales`
- `camera-rental-admin/tests`
- `camera-rental-server/yudao-module-rental`
- `camera-rental-server/sql/mysql/migrations/20260901_056_rental_channel_reconciliation_run.sql`
- `openspec/changes/add-rental-configuration/development/migrations/20260901_056_rental_channel_reconciliation_run.sql`
- `openspec/changes/add-rental-configuration/development/migrations/rollback-20260901_056_rental_channel_reconciliation_run.sql`
- `openspec/changes/add-rental-configuration/development/migrations/manifest.json`
- `openspec/changes/add-rental-configuration/development/migrations/README.md`

## Interfaces / Seams

- Backend `/admin-api/rental/configuration/**` APIs, configuration-scoped
  authorized-shop lookup, shared device catalog query and asynchronous
  reconciliation-run persistence.

## Components To Create

- Approved page, panels, dialogs, drawer, SKU table, identifier summary and impact dialog.
- Reconciliation result dialog and the minimal backend run record/query contract.

## Components To Reuse

- `ContentWrap`, `Pagination`, Element Plus, `v-hasPermi`, request client and existing theme/locale controls.

## Components To Extract

- Shared identifier presentation/copy behavior and category-to-model selection helpers.

## API / Data Flow Contracts

- All external identifiers are TypeScript strings.
- Multi-SKU selection contains only synchronized SKUs returned for the selected shop/item.
- Dangerous mutations require a fresh impact preview and backend version token.
- Rule saves return a reconciliation run ID; the page polls its tenant-scoped
  result and blocks overlapping rule writes until a terminal state.

## State / Error / Empty / Loading Behavior

- Loading uses skeleton/disabled mutation actions; error is distinct from empty.
- Empty explains synchronization prerequisites; 403 has an explicit permission state.
- Disabled rules remain readable; narrow layout preserves actions and identifier labels.

## TDD Requirement

- Add model/component tests before or alongside page implementation.
- Add focused backend tests for shop filtering, run persistence, worker counters
  and migration structure.

## Verification Commands

- `cd camera-rental-admin && pnpm ts:check`
- `cd camera-rental-admin && pnpm lint`
- `cd camera-rental-admin && pnpm build:prod`
- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -pl yudao-module-rental/yudao-module-rental-biz -Dmaven.repo.local=/Volumes/zwl/maven-repository test`

## Stop Conditions

- Stop if the backend contract is not finalized or exact SKU ownership is unavailable.
- Stop if a design change departs from the approved prototype variant.

## Unsafe Assumptions

- JavaScript numeric safety cannot be assumed for external identifiers; keep them as strings.
