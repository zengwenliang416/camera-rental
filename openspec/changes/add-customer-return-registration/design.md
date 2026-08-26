# Design: Customer Return Registration

## Context

The approved customer flow is a public, order-bound service page. It must accept
customer evidence without giving public users management access and without
confusing a submitted parcel notice with a completed warehouse return.

The platform already owns rental orders, physical devices, assignments,
Deliveries and infrastructure file metadata. The feature adds a registration
aggregate and reuses those authorities instead of creating a parallel order,
device or file system.

## Architecture

```text
Admin order operator
  -> create order-bound random token
  -> store SHA-256 hash only
  -> share /return/{token}

Customer Nuxt page
  -> read safe context by token
  -> request registration-scoped upload authorization
  -> upload directly to private RustFS S3 API
  -> confirm object and receive infra_file.id
  -> submit logistics + serials + confirmed file IDs

Submission transaction
  -> lock registration by token hash
  -> validate status, expiry and order confirmation
  -> validate attachment ownership and required categories
  -> normalize and match assigned devices
  -> create or reuse RETURN Delivery when safe
  -> otherwise persist REVIEW_REQUIRED
  -> never mutate device/order/inspection/schedule lifecycle

Admin review
  -> page and inspect registration
  -> accept or reject with permission
  -> accept repeats all validation before Delivery binding
```

## Domain Decisions

### Registration Aggregate

- `rental_return_registration` owns share-link lifecycle, customer logistics,
  submission state, review state and optional Delivery linkage.
- `rental_return_registration_device` preserves submitted and normalized serial
  values plus match result and optional matched device.
- `rental_return_registration_attachment` references an existing
  `infra_file.id`; it does not duplicate object storage metadata.
- One row remains the audit record through issue, submission and review.
- Public token hash is globally unique because tenant identity is not known
  before token resolution.

### Token and Tenant Boundary

- Generate 32 random bytes with `SecureRandom`, encode URL-safe without padding
  and store only SHA-256 hex.
- Public lookup runs outside ordinary tenant interception only long enough to
  resolve the registration and tenant ID from the hash.
- All subsequent order, device, attachment and Delivery operations execute with
  that tenant context and explicit relationship validation.
- Invalid, expired and revoked tokens return safe status codes without revealing
  order identity.

### Device Matching

- Normalize Unicode dash variants to ASCII `-`, remove whitespace and uppercase
  with `Locale.ROOT`.
- Validate 2-5 groups of 1-8 alphanumeric characters.
- Candidate devices come only from active assignments for the bound order.
- Unknown, duplicate, cross-order or cross-tenant serials persist with a clear
  match result and force `REVIEW_REQUIRED`.
- Matching never changes device or assignment state.

### Tenant Device Catalog

- `rental_device_category` and `rental_device_model` are tenant-aware rental
  domain tables; ERP, Mall and IoT category tables are not reused as business
  authority.
- The current seven categories and 24 models are migration seed data, not a
  frontend or Java hard-coded runtime catalog.
- The existing device page owns two small quick-create dialogs exposed through
  the category and model select footers. No route or menu is added.
- Manual device creation validates the selected enabled category/model pair,
  normalizes the administrator-entered `1-999` number to the canonical
  `01-999` form, combines it
  with the configured prefix and relies on the tenant device-number unique
  constraint for concurrent duplicate protection. ERP bulk inbound retains the
  row-locked sequence allocator.
- Existing order, assignment, schedule, dispatch, return and inspection
  services remain unchanged.

### Attachment Boundary

- Reuse the current default S3 file client through an infra module API instead
  of importing infra Mapper or S3 implementation into rental.
- An upload authorization contains a random key under
  `tenant-{id}/registration-{id}/{category}/{uuid}` and expires quickly.
- Confirmation performs object metadata checks, creates `infra_file` and stores
  a registration ownership marker before the file can be submitted.
- Final submission accepts only confirmed attachment rows for the same token.
- Required categories are `DEVICE_EXTERIOR` and `SERIAL_LABEL`;
  `PACKAGING` and `DAMAGE_DETAIL` are optional.

### Return Delivery

- The existing `RentalDeliveryService` remains the package authority.
- Registration acceptance creates or reuses one Delivery with direction
  `RETURN`, source `CUSTOMER_RETURN_FORM`, normalized carrier and waybill.
- The stable key is tenant, rental order, direction, normalized carrier and
  normalized waybill.
- Every matched device relation must belong to the same order and tenant.
- Delivery creation is skipped when matching is unsafe; the registration remains
  available for operator review.

## Database

Migration `20260801_036_customer_return_registration.sql` creates three tables.

`rental_return_registration` includes form number, order/channel references,
external order confirmation, token hash, state, carrier, waybill, shipped date,
issue description, Delivery linkage, expiry/open/submission/review timestamps
and reviewer.

`rental_return_registration_device` includes registration, optional device,
submitted and normalized serial, match status/message and ordering.

`rental_return_registration_attachment` includes registration, `infra_file`
reference, category, object ownership key/hash, content metadata and ordering.

Indexes support global token lookup, tenant/order/status page queries, expiry,
waybill, device serial and attachment category. The migration is additive and
does not backfill historical orders.

## APIs

### Public

- `GET /app-api/rental/return-registration/{token}`
- `POST /app-api/rental/return-registration/{token}/upload-authorizations`
- `POST /app-api/rental/return-registration/{token}/attachments/confirm`
- `DELETE /app-api/rental/return-registration/{token}/attachments/{fileId}`
- `POST /app-api/rental/return-registration/{token}/submit`

### Admin

- `POST /admin-api/rental/return-registration/create`
- `GET /admin-api/rental/return-registration/page`
- `GET /admin-api/rental/return-registration/get?id={id}`
- `POST /admin-api/rental/return-registration/{id}/revoke`
- `POST /admin-api/rental/return-registration/{id}/review`

## Frontend

### Nuxt

- Route `/return/[token]` is SSR-safe and fetches safe context with `useFetch`.
- The page shell owns step navigation and draft persistence for one browser
  session; domain components own one visible responsibility.
- Photo uploads use a reusable hook with per-file progress, retry and removal.
- Local validation improves feedback, but final validation comes from the API.
- Theme and locale are shared Nuxt site preferences, not form-specific stores.

### Admin

- A return registration page reuses the current filter/table/pagination pattern.
- A detail drawer shows order linkage, logistics, serial matches, photos and
  audit history.
- Create, revoke and review actions use explicit permission checks and server
  responses.

## RustFS and Deployment

- RustFS runs in a dedicated container with persistent data, health check,
  restart policy and loopback/private console binding.
- The S3 API is available through a TLS file endpoint; the bucket is private and
  the application credential is least privilege.
- Object-store credentials are provisioned outside Git and entered through the
  existing infrastructure file configuration.
- The deployment script executes unapplied numbered migrations before switching
  the active release, records applied files, and stops on migration failure.
- GitHub `main` deployment builds changed components, restarts backend and web,
  then verifies backend health, public route and release metadata.

## Failure Handling

- Upload authorization failure retains form data and permits retry.
- Orphaned unconfirmed objects are cleaned by a bounded retention job.
- Submission validation errors retain confirmed attachments and field values.
- Concurrent duplicate submissions return the first receipt.
- Delivery creation failure rolls back the final submission transaction.
- RustFS or database migration failure blocks release activation.

## File Scope

- camera-rental-server/yudao-module-rental/yudao-module-rental-biz
- camera-rental-server/yudao-module-infra/yudao-module-infra-api
- camera-rental-server/yudao-module-infra/src/main/java
- camera-rental-server/sql/mysql/migrations
- camera-rental-web
- camera-rental-admin/src/api/rental
- camera-rental-admin/src/views/rental/return-registration
- camera-rental-admin/src/locales
- ops/github-deploy
- ops/rustfs
- openspec/changes/add-customer-return-registration

## Rollout

1. Apply additive schema and menu/permission migration.
2. Deploy RustFS privately and configure the existing S3 file provider.
3. Deploy backend, Nuxt Web and admin UI with the feature disabled from ordinary
   navigation until storage health is green.
4. Create one test registration for a non-sensitive test order.
5. Verify upload, `infra_file`, registration rows, Return Delivery and admin
   review.
6. Enable operator access and monitor upload failures, invalid tokens and review
   volume.
