# Task Report: 008-tenant-device-catalog

## Outcome

The existing admin device page now lets an authorized store administrator add
a tenant-scoped device category or model with a numbering prefix without a new
page or menu. For manual device creation, the administrator enters `1-999` and
the backend normalizes it to canonical `01-999` form before combining it with the selected
model's configured prefix.

## Implementation

- Added tenant-aware `rental_device_category` and `rental_device_model` tables,
  current-catalog seeds and `next_sequence` initialization in migration `050`.
- Added catalog query and category/model create endpoints using the existing
  `rental:device:query` and `rental:device:create` permissions.
- Added normalized per-tenant uniqueness checks for category code, model code
  and device-number prefix, including the explicit `支架` token.
- Added server-authoritative manual number composition and duplicate-device
  rejection without incrementing the model's `next_sequence`.
- Kept transactional `01-999` row-locked reservation for configured ERP inbound
  models. Unknown ERP models retain the existing legacy numbering path and
  remain uncategorized.
- Added category/model quick-create dialogs in the existing device-create
  dialog, catalog refresh, automatic selection of the saved record and a
  prefix-plus-number preview.
- Removed obsolete frontend static category labels and manual device-number
  validation copy so the backend catalog remains the runtime authority.

## Reuse

- Existing `Dialog` component and Element Plus select footer slots.
- Existing `TenantBaseDO`, MyBatis Plus mappers and tenant device-number unique
  constraint.
- Existing `RentalDeviceCode` normalization and canonical `01-999` formatting.
- Existing device query/create permissions and device page.

## Verification

- `/Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -pl yudao-module-rental/yudao-module-rental-biz -am -DskipTests compile`
- `/Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=RentalDeviceCatalogServiceTest,RentalDeviceAdminServiceTest,RentalDeviceInboundServiceTest,RentalDeviceInboundCategoryTest,RentalDeviceCodeStandTest,RentalDeviceCodeTest,ReturnSerialNormalizerTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Result: 27 tests, 0 failures, 0 errors.
- `node --test --experimental-strip-types tests/deviceCatalogModel.test.ts`
  - Result: 3 tests passed.
- `pnpm ts:check`
  - Result: passed.
- `VITE_BASE_URL=http://127.0.0.1:5173 pnpm build:local`
  - Result: passed; existing CSS `*zoom`, large-chunk and ineffective dynamic
    import warnings remain outside this task.
- `bun test tests/return-registration.test.ts`
  - Result: 7 tests passed.
- `bun run build`
  - Result: passed.
- `bash ops/github-deploy/tests/migration-runner-test.sh`
  - Result: passed.
- `bash -n ops/github-deploy/apply-migrations.sh`
  - Result: passed.
- Production and audit migration `050` are byte-identical with SHA-256
  `9ff67ccf71938b093fa6f7601381a3adbd00e1a50738a847f1aba0b3784a14fc`.
- Scoped `git diff --check` passed.

## Not Executed

- Migration `050` was not applied to any database.
- Browser/API integration was not run because the new catalog tables were not
  applied to a local runtime database.
- No commit, push, deployment or production verification was performed.

## Risks

- Manual creation accepts only `1-999` and rejects an already used short code.
  ERP automatic allocation remains limited to 999 device numbers; a
  model whose `next_sequence` exceeds 999 requires a reviewed numbering-policy
  change instead of silent rollover.
- The additive migration seeds every active system tenant and every tenant
  already present in `rental_device`; tenants created later need the normal
  application flow to add their catalog entries.
