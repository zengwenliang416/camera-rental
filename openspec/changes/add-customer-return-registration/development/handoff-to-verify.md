# Development Handoff To Verify: add-customer-return-registration

## Implemented Slices

- Secure order-bound link issuance with 256-bit one-time tokens and hash-only persistence.
- Private RustFS-backed photo upload with ownership, signature, size, count and content revalidation.
- Row-locked idempotent submission, assigned-device matching and conditional `RETURN` Delivery creation.
- Production Nuxt five-step customer form with retry, draft recovery, locale/theme and terminal states.
- Permissioned admin page for create, page, detail, revoke and review operations.
- Checksum-pinned migration runner and RustFS release integration for the production-80 deployment.
- Fixed `/return` entry with receiver mobile last-four verification and optional Xianyu order-number disambiguation.
- Automatic 24-hour `Secure`, `HttpOnly`, `SameSite=Lax` session with hash-only persistence and per-IP/verification-subject/session rate limits.
- One-page `/return` registration with Xianyu order number, receiver mobile last four digits, required machine code and required waybill number.
- Optional one-page return photos with local previews, upload progress, failure retry and a strict maximum of ten.
- Channel-order review fallback that preserves machine and waybill evidence when no internal rental order or safe device assignment exists.
- Two-digit customer machine codes such as `P4-01`, with normalized input,
  stable per-model migration and legacy device-number lookup compatibility.
- Backend-authoritative device categories and models with admin filtering,
  linked create selects, create-time validation and known-model ERP
  classification.

## Files Changed

- Backend rental return-registration controllers, services, DOs, Mappers, enums, tests and Delivery compatibility changes.
- Infra file presigned-upload confirmation, preview and deletion APIs.
- MySQL migrations 034, 036, 041, 049 and 050 plus exact SpecNav audit copies.
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
- Device creation accepts only a catalog-valid category/model pair and allocates
  the final device number from the model's configured prefix; unknown historical
  and ERP models remain unclassified rather than being deleted or guessed.

## Prototype Decisions Implemented

- Five-step order, logistics, devices, photos and review sequence.
- Customer machine-code example `P4-01`; prior device numbers remain legacy lookup aliases.
- Light/dark and `zh-CN`/`en` preferences with mobile-first responsive behavior.
- Required photo progress/retry and explicit accepted/review-required receipts.

## Components Created / Reused / Extracted

- Created token, resolver, attachment, submission, admin and public-context services.
- Reused tenant context, rental order/device assignment, Delivery aggregate and infra file configuration boundaries.
- Extracted Nuxt upload/draft/preferences composables and small progress/photo/status components.
- Reused the existing Element Plus management shell, table, pagination, drawer, permissions and message hooks.

## API / Data Flow Changes

- Customer opens `/return`, verifies a uniquely matched order with receiver mobile last four digits alone, and supplies the Xianyu order number only when disambiguation is needed.
- Public verification locates one channel order, restores tenant context, creates or resumes the registration and returns only safe order/form metadata.
- `/return` is a 捷租达-only public page and `simple-submit` always restores the
  configured 捷租达 tenant. Order number and mobile suffix are optional.
- When both optional fields are absent, `simple-submit` first attempts to locate
  the channel order through an active `ASSIGNED` or `DISPATCHED` assignment. If
  the machine code has not been registered, it creates an unbound standalone
  registration and submits the machine code and waybill in one transaction.
- An unregistered machine code persists as `REVIEW_REQUIRED` with null rental
  order, channel order, device and assignment references. It does not create or
  modify inventory, assignments or Return Delivery, and does not trigger
  check-in, inspection, schedule release or order completion.
- When photos are selected, the page verifies first, uploads each file through
  the existing private RustFS presigned PUT/confirm flow, and passes the
  confirmed attachment IDs to `simple-submit`; when no photos are selected,
  the page sends no verification or upload requests.
- `simple-submit` permits zero to ten `RETURN_PHOTO` attachments and rechecks
  ownership, confirmation state, category, object path hash, file ID, size,
  content type and SHA-256 while the registration row is locked.
- A channel order without an internal rental order is accepted for registration but persisted as `REVIEW_REQUIRED`; it never auto-binds an unverified device.
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
- Admin device catalog: 3 frontend model tests, TypeScript check and local build
  passed; same-origin browser interaction covered all seven categories,
  category/model linking, prefix defaults and category/model filtering.
- Backend device catalog: 7 focused tests passed for catalog membership, direct
  create validation, explicit stand codes and ERP known/unknown model handling.
- Shell/Compose: migration runner, incremental helper, Bash syntax and RustFS Compose validation passed.
- Browser: complete synthetic flow and required terminal/theme/locale/responsive states passed with zero application console errors.
- Fixed entry regression: 12 focused backend tests and 4 Playwright mobile flows passed for last-four-only verification, ambiguity rejection and historical-route redirect.
- One-page slice: 32 focused Maven tests passed with zero failures, the admin and Nuxt production builds passed, and the final executable JAR contains the public return controller with uncompressed nested rental and AI modules.
- Optional-photo slice: Nuxt unit tests, `vue-tsc --noEmit`, production build
  and 8 mobile Playwright flows passed, including the no-photo fast path,
  RustFS request order, failed-upload retry and the ten-photo browser limit.
- Production-80: release `manual-20260802-ai-return-v3` is active; backend, Web, Nginx, `/return`, the public `simple-submit` route and protected rental/AI routes were probed successfully.
- Production-80 optional-photo deployment: release
  `manual-20260803-return-photos-v1` is active as of 2026-08-03. The 25
  focused backend tests passed, the executable JAR contains `RETURN_PHOTO`,
  both systemd services and the public `/return` route are healthy, the
  `simple-submit` and `upload-authorizations` routes are registered, RustFS
  verification passed, and a 390 x 844 Playwright production check confirmed
  the optional multi-file photo control, JPEG/PNG/WebP acceptance and the
  ten-photo guidance. The only failed browser request was the unrelated
  Cloudflare Insights beacon; no business resource returned 4xx/5xx.
- Production-80 short-code deployment: release
  `manual-20260803-device-short-code-v1` is active as of 2026-08-03.
  Migration 041 is recorded with its pinned checksum, all four migrated
  devices have valid short codes and preserved legacy aliases, and both
  active assignments resolve through short codes. Public `/return` returned
  200 in 20 consecutive probes. A 390 x 844 Playwright production check
  loaded the four-field form, displayed `例如 P4-01`, rejected `P4-1`, and
  normalized `p4 － 01` to `P4-01`. The only failed browser request was the
  unrelated Cloudflare Insights beacon.
- Production-80 dispatched-return deployment: release
  `manual-20260803-return-dispatched-v2` is active as of 2026-08-03. A
  read-only production database check found two active `DISPATCHED`
  assignments; both linked devices remain `RENTED` and both resolve through
  two-digit short codes. The focused submission regression accepts a
  `DISPATCHED` assignment while asserting that return registration does not
  update the assignment or device lifecycle state. The public `/return` route
  opened with title `设备退回服务 · 捷租达`; authenticated interactive browser
  inspection remains separate evidence because the browser DOM channel timed
  out during this verification pass.
- Production-80 unregistered-machine deployment: release
  `manual-20260803-unregistered-return-v1` is active as of 2026-08-03. The
  public page is fixed to the 捷租达 tenant and a 390 x 844 production browser
  check displayed the unregistered-machine and manual-review guidance. A
  synthetic machine-code-only submission returned `REVIEW_REQUIRED`; database
  evidence showed tenant `1`, null rental order, channel order, Delivery,
  device and assignment references, plus zero matching inventory, assignment
  and Delivery rows. The synthetic registration and device-detail row were
  deleted after verification.

## Known Risks

- RustFS is pre-1.0 and requires health monitoring plus off-host backup.
- The S3 public endpoint requires DNS, TLS and Nginx while port 9001 must remain private.
- The application must use the generated least-privilege `RUSTFS_APP_*` credentials, never RustFS root credentials.
- The production deployment was performed directly over SSH because GitHub Actions quota was unavailable; GitHub source-of-truth publication remains a separate repository operation.
- A real customer submission was not fabricated during deployment verification; production route, validation, persistence schema and focused duplicate/review tests provide the current evidence.

## Items Requiring Six-Domain Verification

- Prove the same committed SHA on GitHub and `/release-info.json`.
- Prove migrations 034, 036 and 041 and their checksums in the production schema migration table.
- Prove migrations 049 and 050 and their checksums in the production schema
  migration table.
- Execute authenticated admin device catalog/page/create E2E against the real
  backend; the development browser evidence used a same-origin Mock API.
- Prove RustFS health, private bucket policy, application service account and non-public console.
- Configure the private S3 file configuration in the management application.
- Execute a synthetic public upload-to-database-to-admin review flow and verify `infra_file`, all return-registration tables and `rental_delivery.direction=RETURN`.
- Prove `camera-rental-server.service` and `camera-rental-web.service` remain disabled/inactive on 211.
