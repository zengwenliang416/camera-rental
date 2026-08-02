# Development Handoff To Verify: add-customer-return-registration

## Implemented Slices

- Secure order-bound link issuance with 256-bit one-time tokens and hash-only persistence.
- Private RustFS-backed photo upload with ownership, signature, size, count and content revalidation.
- Row-locked idempotent submission, assigned-device matching and conditional `RETURN` Delivery creation.
- Production Nuxt five-step customer form with retry, draft recovery, locale/theme and terminal states.
- Permissioned admin page for create, page, detail, revoke and review operations.
- Checksum-pinned migration runner and RustFS release integration for the production-80 deployment.
- Fixed `/return` entry with Xianyu order number plus receiver mobile last-four verification.
- Automatic 24-hour `Secure`, `HttpOnly`, `SameSite=Lax` session with hash-only persistence and per-IP/order/session rate limits.

## Files Changed

- Backend rental return-registration controllers, services, DOs, Mappers, enums, tests and Delivery compatibility changes.
- Infra file presigned-upload confirmation, preview and deletion APIs.
- MySQL migrations 034 and 036 plus exact SpecNav audit copies.
- Nuxt customer form route, components, composables, styles, types and tests.
- Admin typed API and return-registration operations page.
- GitHub deployment migration runner and RustFS Compose/install/bootstrap/backup assets.

## Requirements Covered

- Public links persist no plaintext token and restore the located tenant only after global hash lookup.
- Required exterior and serial-label photos are private and registration-owned; packaging and damage photos are optional.
- Safe submissions create or reuse one local-only `RETURN` Delivery; mismatch or issue evidence enters `REVIEW_REQUIRED`.
- Duplicate submissions return the original receipt without duplicate business writes.
- Admin operations are backend-permissioned and show authorized full customer details.
- Migrations block release activation on failure or checksum drift.

## Prototype Decisions Implemented

- Five-step order, logistics, devices, photos and review sequence.
- Physical serial example `A6-08-4L5H`.
- Light/dark and `zh-CN`/`en` preferences with mobile-first responsive behavior.
- Required photo progress/retry and explicit accepted/review-required receipts.

## Components Created / Reused / Extracted

- Created token, resolver, attachment, submission, admin and public-context services.
- Reused tenant context, rental order/device assignment, Delivery aggregate and infra file configuration boundaries.
- Extracted Nuxt upload/draft/preferences composables and small progress/photo/status components.
- Reused the existing Element Plus management shell, table, pagination, drawer, permissions and message hooks.

## API / Data Flow Changes

- Customer opens `/return`, verifies the Xianyu order number and receiver mobile last four digits, and receives a cookie-bound session.
- Public verification locates one channel order, restores tenant context, creates or resumes the registration and returns only safe order/form metadata.
- Historical `/return/{token}` routes redirect to the fixed entry; old public
  token APIs and admin issue/reissue APIs are closed while existing hash data
  remains available for authorized audit.
- Upload authorization returns a short-lived PUT URL; confirmation creates or reuses `infra_file` metadata after content validation.
- Submission locks the registration, revalidates attachments, matches assigned devices and either binds one `RETURN` Delivery or persists review evidence.
- Admin review repeats attachment and device validation before accepting a review-required registration.

## Tests Added

- Token entropy/hash privacy, admin hash-only creation, file size/content validation, confirmed/unconfirmed cleanup.
- Serial normalization, safe submission, review fallback, duplicate submission and missing-photo rejection.
- Nuxt utility tests for physical serials, optional packaging and both locales.
- Migration runner replay/checksum tests and incremental deployment helper tests.

## Local Validation

- Backend: 12 tests passed with zero failures/errors.
- Nuxt: 3 tests, typecheck and production build passed.
- Admin: TypeScript check and production build passed.
- Shell/Compose: migration runner, incremental helper, Bash syntax and RustFS Compose validation passed.
- Browser: complete synthetic flow and required terminal/theme/locale/responsive states passed with zero application console errors.

## Known Risks

- GitHub push and production deployment are not yet complete, so this file is not final production evidence.
- RustFS is pre-1.0 and requires health monitoring plus off-host backup.
- The S3 public endpoint requires DNS, TLS and Nginx while port 9001 must remain private.
- The application must use the generated least-privilege `RUSTFS_APP_*` credentials, never RustFS root credentials.

## Items Requiring Six-Domain Verification

- Prove the same committed SHA on GitHub and `/release-info.json`.
- Prove migrations 034, 036 and 037 and their checksums in the production schema migration table.
- Prove RustFS health, private bucket policy, application service account and non-public console.
- Configure the private S3 file configuration in the management application.
- Execute a synthetic public upload-to-database-to-admin review flow and verify `infra_file`, all return-registration tables and `rental_delivery.direction=RETURN`.
- Prove `camera-rental-server.service` and `camera-rental-web.service` remain disabled/inactive on 211.
