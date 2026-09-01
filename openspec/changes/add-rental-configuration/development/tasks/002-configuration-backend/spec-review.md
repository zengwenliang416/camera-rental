# Spec Review: 002-configuration-backend

## Verdict

approved

## Missing Requirements

- None within Task 002 items `2.1` through `2.4`.
- The configuration controller exposes the standalone backend route and guards
  every catalog and product-rule endpoint with
  `rental:configuration:query` or `rental:configuration:update`.
- Catalog mutations use tenant-scoped optimistic writes for categories and
  models. Rule mutations use tenant-scoped optimistic writes, stable conflict
  errors, bounded impact counts, and transactional parent/child replacement.
- The controlled seed contains the approved 29 item IDs and refuses zero,
  duplicate, expired-authorization, missing-confirmation, and partial-write
  cases before accepting shop-scoped rules.

## Extra Behavior

- No unrelated third-party write behavior was found in the reviewed backend
  slice.
- The seed remains separately controlled, requires explicit confirmation, and
  does not execute as part of the normal migration.

## Misunderstood Requirements

- None. Rule validation requires the current tenant's internal shop and owning
  application, a valid unexpired authorization, the exact synchronized Xianyu
  item, synchronized child SKU ownership for multi-model rules, and enabled
  device models.
- Single-model and multi-model persistence remain mutually exclusive.
  `CONFIG_SKIPPED` normalizes to `mapping_mode=NONE` with no model or child
  mappings. No title, remark, product-ID, SKU-text, or cross-shop fallback is
  introduced.

## Cannot Verify From Diff

- The complete administrator experience is not verified by this task because
  page rendering, remark templates, responsive states, themes, and locales are
  owned by the admin-UI and verification tasks.
- The complete order-side matching assertion is not verified by this task
  because applying the persisted rule during order reconciliation is owned by
  Task 003.
- Production execution remains outside Task 002. The controlled seed was
  prepared and tested only and was not applied to a real tenant or shop.

## Acceptance Assertions Verified

- `A1:backend-configuration` - Verified only for the authorized backend
  configuration subclaim: tenant-scoped catalog and exact rule APIs,
  query/update permissions, impact preview, optimistic locking, standard
  persistence audit fields, and structured rule-change logs are present.
- `A4:rule-persistence` - Verified only for the persistence subclaim:
  single-model rules persist an exact shop/item model, while multi-model rules
  accept only synchronized child SKUs for that exact product and persist the
  XianGuanJia SKU plus available Xianyu SKU without fallback.

## Required Fixes

- None.
- System-executed receipt `005` is bound to Git head `c621976b` and records the
  MySQL 8.4 migration/seed fixture passing optimistic-lock, exact 29-item seed,
  tenant/shop isolation, authorization guard, conflict atomicity, rollback,
  and cleanup checks.
- System-executed receipt `006` is bound to the same Git head and records the
  focused Task 002 Maven suite passing 40 tests with 0 failures, 0 errors, and
  0 skipped.
