# Task Report: 007-admin-device-category-catalog

## Status

DONE

## Files Changed

- Rental backend catalog, controller, VO, persistence field, create/page/inbound
  services, device-code validation and focused tests.
- Admin typed API, device page, extracted catalog model helper, locales and
  focused model tests.
- Additive migration 049, deployment migration list and SpecNav migration audit
  metadata.

## What Changed

- Added seven stable device categories and 24 approved model codes in one
  backend-authoritative catalog.
- Added catalog retrieval, category page filtering, create-time
  category/model/device-number validation and known-model ERP classification.
- Added a nullable category column with known-model backfill and a category/model
  query index while preserving unknown historical rows.
- Added category filtering and display plus linked category/model selects in the
  create dialog. Model selection defaults the device number to `MODEL-01`.

## TDD Evidence

- `RentalDeviceModelCatalogTest` covers catalog membership and invalid pairs.
- `RentalDeviceAdminServiceTest` covers valid creation and invalid category,
  model and device-number combinations.
- `RentalDeviceInboundCategoryTest` covers known and unknown ERP model handling.
- `RentalDeviceCodeStandTest` proves explicit `支架-01` support without arbitrary
  Chinese prefixes.
- `deviceCatalogModel.test.ts` covers backend-provided option filtering,
  membership and default device-number generation.

## Verification Commands

- Backend focused Maven suite: 7 tests passed, 0 failures/errors.
- `node --test --experimental-strip-types tests/deviceCatalogModel.test.ts`: 3
  tests passed.
- `pnpm ts:check`: passed.
- `VITE_BASE_URL=http://127.0.0.1:5173 pnpm build:local`: passed with existing
  CSS and chunk-size warnings.
- Scoped `git diff --check`: passed.

## Concerns

- Migration 049 has not been applied to a database.
- Browser verification used a same-origin catalog/device Mock API because the
  local backend was not running; real backend E2E remains unverified.

## Scope Deviations

- None recorded.

## Follow-up Needed

- Apply migration 049 only through the approved deployment target and verify the
  real catalog/page/create APIs with an authorized administrator.

## Adjudication

The implementation and local validation are complete. Database execution,
authenticated backend E2E, commit, push and deployment remain outside this
task.
