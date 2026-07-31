# Task Report: 006-multiple-provider-credentials

## Status

DONE

## Files Changed

- Added the credential table and Delivery credential binding to migration 032,
  together with the matching migration manifest and upgrade evidence.
- Added credential persistence, mapper, selection service, operations API,
  provider-command binding, tests, and masked frontend management.
- Updated requirements, design, acceptance, task, handoff, and review artifacts
  for the approved multi-credential requirement.

## What Changed

- Added `rental_logistics_provider_credential` for multiple named
  `customerCode + apiKey` pairs per tenant and Provider.
- Kept Provider-wide switches, callback configuration, result version, and
  query interval in `rental_logistics_provider_config`.
- Added nullable `rental_delivery.provider_credential_id`. The Outbox lease
  selects and persists one usable credential before creating a provider-neutral
  work item.
- Reused a valid existing binding. Disabled, deleted, incomplete, cross-tenant,
  or wrong-Provider credentials are ignored and safely reselected from the
  ordered usable pool.
- Added masked credential CRUD, enable/disable, ordering, delete, and local
  verification to the existing operations API and page.
- Added a guarded pre-release migration that copies encrypted single-credential
  values into one `default` credential without decrypting them.
- Fixed tenant isolation for credential updates: a supplied ID that is not
  present in the current tenant now returns `PROVIDER_CREDENTIAL_NOT_FOUND`
  instead of creating a new row.

## Reuse Review

- Encryption reuses `EncryptTypeHandler` and `@ToString.Exclude`.
- Tenant isolation reuses `TenantBaseDO`, `TenantContextHolder`, and
  tenant-scoped mapper methods.
- Secret editing reuses `SecretAction` KEEP/REPLACE/CLEAR and the existing
  masked response pattern.
- Provider execution reuses `LogisticsProvider`, `Kuaidi100Gateway`,
  `Kuaidi100Signer`, `Kuaidi100Converter`, and the existing registry.
- Async execution reuses the existing Outbox lease, worker, completion, retry,
  throttling, and redaction components.
- The frontend reuses the authenticated API client, operations permissions,
  `SecretField`, status badges, theme, locale, and panel layout.
- Static review found no duplicate HTTP client, signing, encryption, retry,
  tenant, masking, or permission implementation introduced by this task.

## TDD Evidence

- `RentalLogisticsProviderCredentialServiceTest`: stable bound reuse, ordered
  distribution, disabled-binding fallback, incomplete filtering, and
  tenant-scoped lookup.
- `RentalLogisticsOperationsConfigurationServiceTest`: multiple masked
  credentials, no plaintext response, complete-enabled credential requirement,
  tenant-safe updates, and credential persistence.
- `RentalDeliveryOutboxLeaseServiceTest` and
  `RentalDeliveryOutboxWorkerTest`: credential binding and provider-neutral
  credential ID propagation.
- `Kuaidi100LogisticsProviderTest`: credential ID resolution for subscribe and
  query without changing the gateway or signer boundary.
- `RentalLogisticsSensitiveFieldTest`: credential fields use the existing
  encryption TypeHandler and do not enter generated `toString`.
- Frontend operations model tests prove multiple credential rows keep
  independent IDs and secret drafts without storing masked values.

## Verification Commands

- `mvn clean` focused multi-credential and Provider tests: 24 passed.
- Complete rental logistics Maven regression: 110 passed with no failures or
  errors; six environment-gated cases were rerun separately.
- `RentalLogisticsMysqlConcurrencyTest` against MySQL 8.4.10: 6 passed with no
  skips.
- `pnpm test && pnpm lint && pnpm build`: 98 tests passed, lint passed, and
  Vite transformed 1796 modules.
- Disposable MySQL 8.4.10 fresh and legacy upgrade paths passed, including
  repeated migration 032 and ciphertext-preserving conversion.
- `git diff --check` and JSON/JSONL integrity checks passed.

## Concerns

- Multiple credentials provide stable distribution, not automatic quota or
  billing failover. A Provider-reported quota policy remains out of scope.
- No production credential was configured and no real Kuaidi100 request was
  made.

## Scope Deviations

- The original design assumed one credential pair per Provider configuration.
  The user explicitly expanded the requirement to multiple credential pairs,
  so task 006 added a separate credential aggregate without changing Provider
  transport, worker, retry, or transaction boundaries.

## Follow-up Needed

- Verification must exercise the packaged application with a mock Provider and
  confirm credential CRUD, stable Delivery binding, and disabled-binding
  reselection through the deployed API boundary.
