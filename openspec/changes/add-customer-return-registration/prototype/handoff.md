# Prototype Handoff: add-customer-return-registration

## Approved Branch Variant

- Approved branch: `ui-html`.
- Approved variant: `warm-mobile-first-five-step-return-flow`.

## Screens Or Flows

- Order-bound public return registration with order confirmation, logistics,
  device serial numbers, categorized photos, review and receipt.
- Production adds expired, revoked, already submitted, upload failure and
  review-required screens without changing the approved primary flow.

## Components To Create

- Create `ReturnFlowShell`, step progress, logistics step, device step, photo
  step, review step and shared status panel in the Nuxt customer site.
- Create an admin registration page and detail drawer for link and review work.

## Components To Reuse

- Reuse the existing Nuxt SSR runtime, admin request/table/pagination/drawer
  patterns, Element Plus image preview, rental order/device services, Delivery
  service and infrastructure file service.

## Extraction Targets

- Extract token handling, serial normalization, attachment policy, order-device
  matching, photo upload state and localized registration status presentation.

## API Contracts

- Public API contracts are context read, upload authorization, attachment
  confirmation/removal and idempotent submission under
  `/app-api/rental/return-registration/**`.
- Admin API contracts are create, page, detail, revoke and review under
  `/admin-api/rental/return-registration/**`.

## Data Flows

- `FLOW-RETURN-LINK-ISSUE` creates an order-scoped token hash.
- `FLOW-PUBLIC-RETURN-UPLOAD` authorizes private RustFS upload and confirms an
  `infra_file` owned by the registration.
- `FLOW-PUBLIC-RETURN-SUBMIT` validates and creates or reuses a Return Delivery.
- `FLOW-RETURN-REGISTRATION-REVIEW` revalidates before operator acceptance.

## State Behavior

- Loading: skeleton and disabled primary action while safe context loads.
- Empty: no generic empty state; invalid tokens use the safe not-available page.
- Error: preserve drafts and provide bounded upload or submission retry.
- Disabled: prevent duplicate submit and per-file duplicate upload actions.
- Permission: admin actions follow backend permissions; public access is token
  scoped and never exposes administrative data.

## Theme And Locale Policy

- Theme support: production `light-dark`.
- Theme modes shown in prototype: approved visual direction is `light`.
- Theme toggle: intentionally omitted from the isolated prototype; production
  uses the Nuxt site preference.
- Internationalization: enabled in production.
- Locales shown in prototype: default `zh-CN`.
- Locale switcher: intentionally omitted from the isolated prototype; production
  provides `zh-CN` and `en`.

## Out Of Scope Items

- No automatic device availability, order completion, inspection completion or
  schedule release.
- No public RustFS console, long-lived credentials, anonymous Bucket or generic
  unrestricted file upload.
- No OCR automation in the first production version.

## Required Tests

- Required tests cover token security, attachment ownership, serial matching,
  idempotency, Return Delivery reuse, no lifecycle side effects, Nuxt SSR,
  responsive states, admin permissions and production RustFS upload.

## Open Risks

- Production RustFS needs persistent storage, private networking or TLS endpoint,
  least-privilege credentials and restore verification.
- Existing production migrations are not automatically executed by the current
  deployment script and must be added before release.
- The worktree contains unrelated logistics changes, so commits must stage only
  feature-owned paths.
