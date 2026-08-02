# Vertical Slices

## 1. Secure registration and persistence foundation

User outcome: An operator can issue a safe order-bound return link and the
system can persist a registration without exposing a plaintext token.

- [x] 1.1 Add the executable additive migration for registration, device and attachment tables, indexes, permissions and menu data, with validation and rollback documentation.
- [x] 1.2 Add tenant-aware DOs, Mappers, enums, error codes and token generation/hash services with global token lookup.
- [x] 1.3 Add unit tests for token entropy, hash-only persistence, expiry, revocation, tenant restoration and safe invalid-token responses.

## 2. Private RustFS photo upload

User outcome: A customer can upload required return photos to private storage,
retry failures and attach only files owned by the current return link.

- [x] 2.1 Extend the infra file API with registration-scoped presigned PUT, object confirmation and short-lived preview support without exposing a generic public uploader.
- [x] 2.2 Add attachment policy and endpoints for type, signature, size, count, category, ownership and removal validation.
- [x] 2.3 Add RustFS container, persistent-volume, health-check, private-console, least-privilege and backup configuration for the production deployment.
- [x] 2.4 Add unit and redteam tests for cross-token/cross-tenant file IDs, forged object keys, oversized/invalid files, anonymous access and signed URL leakage.

## 3. Idempotent customer submission and Return Delivery

User outcome: A customer can submit logistics, assigned device serials and
photos once, receiving either an accepted receipt or a clear review state.

- [x] 3.1 Implement safe public context and idempotent submission APIs with order confirmation, carrier, waybill, shipped date, issue and attachment validation.
- [x] 3.2 Implement serial normalization and assigned-order device matching for 1-8 devices, preserving mismatch evidence for review.
- [x] 3.3 Create or reuse one `RETURN` Delivery and device relations only when every validation is safe; otherwise persist `REVIEW_REQUIRED`.
- [x] 3.4 Add transaction, concurrency and no-side-effect tests proving duplicate requests do not duplicate rows and no device/order/inspection/schedule lifecycle changes occur.

## 4. Production Nuxt customer experience

User outcome: A customer opening the link in WeChat can complete the approved
five-step return form on mobile or desktop with reliable upload and status
feedback.

- [x] 4.1 Promote the approved prototype into SSR-safe Nuxt route `/return/[token]` using cohesive step components and reusable upload/draft hooks.
- [x] 4.2 Implement loading, expired, revoked, invalid, already-submitted, validation, upload-failure, review-required and success states.
- [x] 4.3 Add `zh-CN` and `en`, light/dark site preferences, accessible focus/touch behavior and responsive layouts without page overflow.
- [x] 4.4 Add component and browser tests for navigation, serial normalization, optional packaging omission, upload retry, duplicate submit and receipt rendering.

## 5. Admin link and review operations

User outcome: Authorized operations staff can create, find, inspect, revoke and
review customer return registrations from the management application.

- [x] 5.1 Add admin create, page, detail, revoke and review services/controllers with permission and tenant checks.
- [x] 5.2 Add typed admin API clients, paginated filter table, link creation/copy action and detail photo drawer.
- [x] 5.3 Add accept/reject review actions that revalidate all relationships before Delivery binding and preserve audit history.
- [x] 5.4 Add backend and frontend tests for permissions, pagination, full detail, expired/revoked links and review races.

## 6. Migration, release and production verification

User outcome: The approved feature is ready for GitHub deployment to
`154.9.235.80`, with production execution and end-to-end evidence owned by the
verification stage.

- [x] 6.1 Add idempotent numbered migration execution and release blocking to the GitHub server deployment path.
- [x] 6.2 Run focused Maven tests, admin type-check/build, Nuxt build, static/redteam checks and browser sensory checks.
- [x] 6.3 Prepare the feature-owned staging boundary for GitHub `main`, excluding `.serena/`, unrelated worktree changes and the deferred Gitee remote.
- [x] 6.4 Configure the production-80 GitHub workflow, migration 037 artifact gate and service health checks, then hand actual deployment and public E2E evidence to verification.

## 7. Fixed public entry without manually issued links

User outcome: A customer can open one permanent return page, verify an order
with the Xianyu order number and receiver mobile last four digits, and complete
the existing return workflow without an operator issuing a tokenized URL.

- [x] 7.1 Replace manual-link requirements with fixed-entry verification, automatic HttpOnly session, enumeration-resistant errors and rate limits.
- [x] 7.2 Add backend order verification, unique tenant restoration, automatic registration create/resume and session-scoped upload/submit APIs.
- [x] 7.3 Promote `/return` to the customer entry page, add order/mobile-last4 verification states, and redirect historical token routes safely.
- [x] 7.4 Update admin guidance from per-order link generation to one fixed public URL while preserving registration search, detail, revoke and review operations.
- [x] 7.5 Add migration/index support and unit, redteam, browser and build verification for the fixed-entry flow; production verification remains in the verification stage.
