# Vertical Slices

## 1. Secure registration and persistence foundation

User outcome: An operator can issue a safe order-bound return link and the
system can persist a registration without exposing a plaintext token.

- [ ] 1.1 Add the executable additive migration for registration, device and attachment tables, indexes, permissions and menu data, with validation and rollback documentation.
- [ ] 1.2 Add tenant-aware DOs, Mappers, enums, error codes and token generation/hash services with global token lookup.
- [ ] 1.3 Add unit tests for token entropy, hash-only persistence, expiry, revocation, tenant restoration and safe invalid-token responses.

## 2. Private RustFS photo upload

User outcome: A customer can upload required return photos to private storage,
retry failures and attach only files owned by the current return link.

- [ ] 2.1 Extend the infra file API with registration-scoped presigned PUT, object confirmation and short-lived preview support without exposing a generic public uploader.
- [ ] 2.2 Add attachment policy and endpoints for type, signature, size, count, category, ownership and removal validation.
- [ ] 2.3 Add RustFS container, persistent-volume, health-check, private-console, least-privilege and backup configuration for the 211 deployment.
- [ ] 2.4 Add unit and redteam tests for cross-token/cross-tenant file IDs, forged object keys, oversized/invalid files, anonymous access and signed URL leakage.

## 3. Idempotent customer submission and Return Delivery

User outcome: A customer can submit logistics, assigned device serials and
photos once, receiving either an accepted receipt or a clear review state.

- [ ] 3.1 Implement safe public context and idempotent submission APIs with order confirmation, carrier, waybill, shipped date, issue and attachment validation.
- [ ] 3.2 Implement serial normalization and assigned-order device matching for 1-8 devices, preserving mismatch evidence for review.
- [ ] 3.3 Create or reuse one `RETURN` Delivery and device relations only when every validation is safe; otherwise persist `REVIEW_REQUIRED`.
- [ ] 3.4 Add transaction, concurrency and no-side-effect tests proving duplicate requests do not duplicate rows and no device/order/inspection/schedule lifecycle changes occur.

## 4. Production Nuxt customer experience

User outcome: A customer opening the link in WeChat can complete the approved
five-step return form on mobile or desktop with reliable upload and status
feedback.

- [ ] 4.1 Promote the approved prototype into SSR-safe Nuxt route `/return/[token]` using cohesive step components and reusable upload/draft hooks.
- [ ] 4.2 Implement loading, expired, revoked, invalid, already-submitted, validation, upload-failure, review-required and success states.
- [ ] 4.3 Add `zh-CN` and `en`, light/dark site preferences, accessible focus/touch behavior and responsive layouts without page overflow.
- [ ] 4.4 Add component and browser tests for navigation, serial normalization, optional packaging omission, upload retry, duplicate submit and receipt rendering.

## 5. Admin link and review operations

User outcome: Authorized operations staff can create, find, inspect, revoke and
review customer return registrations from the management application.

- [ ] 5.1 Add admin create, page, detail, revoke and review services/controllers with permission and tenant checks.
- [ ] 5.2 Add typed admin API clients, paginated filter table, link creation/copy action and detail photo drawer.
- [ ] 5.3 Add accept/reject review actions that revalidate all relationships before Delivery binding and preserve audit history.
- [ ] 5.4 Add backend and frontend tests for permissions, pagination, full detail, expired/revoked links and review races.

## 6. Migration, release and production verification

User outcome: The approved feature is deployed on the 211 server and can be
verified from public form through private object storage to admin review.

- [ ] 6.1 Add idempotent numbered migration execution and release blocking to the GitHub server deployment path.
- [ ] 6.2 Run focused Maven tests, admin type-check/build, Nuxt build, static/redteam checks and browser sensory checks.
- [ ] 6.3 Commit feature-owned paths in logical batches and push the same `main` SHA to GitHub and Gitee without staging unrelated logistics changes.
- [ ] 6.4 Observe the GitHub production workflow, verify RustFS and service health, then execute public upload-to-database-to-admin end-to-end checks on 211.
