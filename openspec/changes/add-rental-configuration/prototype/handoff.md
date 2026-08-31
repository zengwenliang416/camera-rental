# Prototype Handoff: add-rental-configuration

## Approved Branch Variant

- Approved branch: `ui-html`.
- Approved variant: `admin-three-tab-precise-mapping-v1`.
- Explicit user approval recorded on 2026-08-31.

## Screens Or Flows

- Rental Configuration admin page under Rental Operations.
- Device Catalog tab for category, model, number-prefix, sort and enable state.
- Channel Product Rules tab for shop-scoped handling policy and exact model mapping.
- Xianyu Remark Guide tab for three base formats and eight exception suffixes.
- Single-model mapping by shop and Xianyu item ID.
- Multi-model mapping by shop, Xianyu item ID and synchronized XianGuanJia SKU ID.
- Impact preview before rule mutation and asynchronous order reconciliation.

## Components To Create

- `RentalConfigurationPage`
- `DeviceCatalogConfigurationPanel`
- `DeviceCategoryFormDialog`
- `DeviceModelFormDialog`
- `ChannelProductRuleTable`
- `ChannelProductRuleFormDrawer`
- `ChannelSkuMappingTable`
- `ChannelIdentifierSummary`
- `RemarkConventionPanel`
- `RuleImpactPreviewDialog`

## Components To Reuse

- Existing admin shell, `ContentWrap`, `Pagination`, Element Plus table/form/tag/alert/dialog/drawer.
- Existing `v-hasPermi`, request client, theme control and locale control.
- Existing device catalog types and selection utilities.
- Existing rental device catalog, order persistence, seller remark parser, assignment,
  schedule and manual-review services.

## Extraction Targets

- Shared channel identifier labels, string formatting and copy behavior.
- Shared catalog model labels and category-to-model selection.
- One reconciliation service reused by order persistence, remark replay, product/SKU
  synchronization, configuration changes and historical backfill.
- One fulfillment guard reused by model and date updates.
- Remark template copy and exception help as a configuration-owned component.

## API Contracts

- Catalog page, create, update and enable APIs under `/admin-api/rental/configuration/catalog/**`.
- Product rule page, detail, impact preview, create/update and enable APIs under
  `/admin-api/rental/configuration/product-rule/**`.
- Synchronized SKU query scoped by tenant, shop and Xianyu item ID.
- Reconciliation result query returning scanned, skipped, created, updated, conflict,
  failed and review counts.
- External product and SKU identifiers are strings in all frontend contracts.

## Data Flows

- Order detail persists XianGuanJia product ID, Xianyu item ID and XianGuanJia SKU ID
  without fallback.
- Product and SKU synchronization persists shop-specific Xianyu item ID and Xianyu
  SKU ID, with exact ownership validation.
- Ordinary channel order persistence idempotently creates an internal rental order
  before remark/model readiness is complete.
- Product, SKU, remark or mapping changes call the same reconciliation service.
- `CONFIG_SKIPPED` retains channel/raw/payment evidence but skips parsing, internal
  order creation, manual conversion review and scheduling.
- Rule mutations preview affected orders, then reconcile asynchronously after the
  backend accepts the change.

## State Behavior

- Loading: skeletons replace the business surface and mutation actions remain unavailable.
- Empty: explains synchronization prerequisites and offers only an allowed sync action.
- Error: does not masquerade as an empty table and provides safe retry.
- Disabled: keeps read and impact-preview access while reconciliation blocks mutation.
- Permission: displays a 403 state and relies on backend permission enforcement.
- Missing multi-SKU mapping: remains `WAITING_MODEL` and never falls back to a product
  default model.
- Assigned or dispatched mismatch: preserves the current device and enters manual review.

## Theme And Locale Policy

- Theme support: `light-dark`.
- Theme modes shown in prototype: `light`, `dark`.
- Theme toggle: the prototype mirrors the existing admin user control; production reuses
  the current admin toggle and does not add a page-local theme system.
- Internationalization: enabled.
- Locales shown in prototype: `zh-CN`, `en`.
- Default locale: `zh-CN`.
- Locale switcher: the prototype mirrors the existing admin control; production uses
  existing `vue-i18n` dictionaries.

## Out Of Scope Items

- No model inference from title, SKU text, merchant code or seller remark.
- No automatic selection of a concrete device.
- No remark-driven device swap, inbound confirmation, inspection, refund, compensation
  or settlement.
- No deletion or reversal of fulfilled historical records.
- No third-party write operation or production data mutation during development tests.

## Required Tests

- Migration structure, indexes, permissions and string identifier contracts.
- Explicit identifier persistence without product/SKU fallback.
- Single-model and synchronized multi-SKU exact mapping.
- Immediate idempotent internal-order creation with missing remark/model/dates.
- `CONFIG_SKIPPED` behavior and shop-scoped initial rules.
- Valid remark snapshot preservation and invalid-reminder non-destructive behavior.
- Assigned, dispatched, extended, early-return and replacement-device guards.
- Reconciliation idempotency and bounded historical backfill counters.
- Admin permissions, loading/empty/error/disabled/permission states, narrow layout,
  light/dark and `zh-CN`/`en`.

## Open Risks

- Initial skip rules must resolve authoritative shop records uniquely before insertion.
- Existing ambiguous external columns must remain historical evidence but must not be
  read or written by new runtime paths.
- Historical reconciliation must be bounded and resumable to avoid long transactions.
- The change spans database, backend and admin contracts, so interface consistency must
  be validated before deployment.
