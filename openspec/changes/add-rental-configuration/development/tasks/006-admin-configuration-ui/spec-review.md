# Spec Review: 006-admin-configuration-ui

## Verdict

approved

## Missing Requirements

- None within Task 006.
- `6.1` is satisfied by the typed `/rental/configuration/**` client, the
  standalone page component, and the migration-owned Rental Operations menu
  entry. Shop selection now uses the configuration-scoped
  `/rental/configuration/shops` endpoint rather than requiring the separate
  Xianyu shop permission
  (`configuration.ts:1-155`; `RentalConfigurationController.java:71-198`;
  `20260831_052_rental_configuration_foundation.sql:219-253`).
- `6.2` is satisfied by catalog create, edit and enable/disable flows with
  loading, empty, permission and optimistic-version behavior. Category and
  model edits fail closed when `lockVersion` is absent, and status requests do
  not substitute a synthetic version
  (`DeviceCatalogPanel.vue:1-434`; `index.vue:384-496`).
- `6.3` is satisfied by exact shop/item rule editing, mutually exclusive
  single-model and synchronized multi-SKU modes, fresh impact preview,
  optimistic locking, asynchronous reconciliation polling and persisted result
  counters. Multi-SKU requests are restricted to the currently synchronized
  internal SKU IDs, stale scope responses are discarded, and rule writes stay
  disabled while the accepted run is non-terminal
  (`configurationModel.ts:25-87`; `index.vue:299-304,498-686`;
  `RentalChannelProductRuleService.java:56-179`;
  `RentalChannelReconciliationRunService.java:28-97`;
  `RentalChannelOrderReconciliationWorker.java:32-109`).
- `6.4` is satisfied by three copyable base templates and all eight required
  special-case explanations (`RemarkConventionPanel.vue:56-133`).
- `6.5` is satisfied: Rental Device still reads the shared catalog for filters
  and device creation, but no longer contains category/model quick-create
  dialogs (`device/index.vue`; `configurationUiContract.test.ts:135-142`).
- `6.6` has complete matching `zh-CN` and `en` configuration copy and explicit
  responsive structures. The narrow SKU view renders XianGuanJia SKU ID,
  Xianyu SKU ID, display name, mapped model and text status rather than hiding
  the detailed mapping behind the desktop table
  (`zh-CN.ts:485-641`; `en.ts:489-649`;
  `ChannelProductRuleTable.vue:144-232`;
  `ChannelSkuMappingTable.vue:72-118`).

## Extra Behavior

- Migration `056` adds the tenant-scoped asynchronous reconciliation-run
  ledger needed by the corrected Task 006 brief. This is in the declared scope
  and does not execute a production migration or perform a third-party write.
- The production and development forward SQL copies are byte-identical with
  SHA-256
  `03882e854de674a06d1fd9d5afbe52ce3d7484e8ffeca464642a4fce1a791083`.
  The destructive rollback is separately identified with SHA-256
  `e42209a5eb7f2047a94510453f096435bb393b29404c22ec188f1c438f62aa84`.

## Misunderstood Requirements

- None. Missing channel identifiers remain visible with an explicit `—`
  marker; they are not hidden or inferred. Multi-SKU model selection is limited
  to synchronized rows, while SKU display text remains informational only
  (`ChannelIdentifierSummary.vue:1-50`;
  `ChannelSkuMappingTable.vue:1-168`).
- Reconciliation results are queried with the configuration query permission
  and resolved through the current tenant. The worker restores the event
  tenant, records every required counter, and reaches `SUCCEEDED`,
  `COMPLETED_WITH_ERRORS` or `FAILED`
  (`RentalConfigurationController.java:191-198`;
  `RentalChannelReconciliationRunMapper.java:12-15`;
  `RentalChannelReconciliationRunService.java:49-96`).

## Cannot Verify From Diff

- Browser-driven sensory verification of light/dark themes, `zh-CN`/`en`, and
  desktop/narrow viewports remains a Task 007 verification surface. Task 006
  supplies theme-token styling, locale dictionaries, responsive layouts and
  rendered component evidence, but this review does not claim an interactive
  browser session was performed.
- Production migration execution, deployment and real-shop writes are outside
  Task 006 and were not performed.
- The task report and quality-review files remain controller/independent-review
  artifacts and are not adjudicated by this specification verdict.

## Acceptance Assertions Verified

- A1 - Verified at the Task 006 implementation-contract level. An authorized
  administrator has one standalone page for catalog maintenance, exact channel
  rules and SKU mappings, and copyable remark templates. Backend permissions,
  tenant-scoped shop/run queries, impact preview and asynchronous result
  closure are present. Final browser E2E/sensory evidence remains correctly
  assigned to Task 007.

## Required Fixes

- None for Task 006 specification handoff.

## Validation Performed

- Independently ran `cd camera-rental-admin && pnpm
  test:rental-configuration`: 15 tests, 0 failures, 0 skipped.
- Independently ran the focused backend command for
  `RentalConfigurationControllerTest`,
  `RentalConfigurationShopServiceTest`,
  `RentalChannelReconciliationRunServiceTest`,
  `RentalChannelOrderReconciliationWorkerTest`, and
  `RentalChannelReconciliationRunMigrationTest`: 13 tests, 0 failures,
  0 errors, 0 skipped, `BUILD SUCCESS`.
- Independently verified the migration `056` hashes and byte identity with
  `shasum -a 256` and `cmp -s`.
- Controller-provided evidence, not independently rerun in this review:
  `pnpm ts:check`, targeted ESLint/Stylelint/Prettier checks and
  `pnpm build:prod` passed; the full rental-biz suite recorded 658 tests,
  0 failures, 0 errors and 8 environment-gated skips; strict OpenSpec
  validation passed.
