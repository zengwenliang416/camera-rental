# Task Brief: 007-state-decomposition-final

## Goal

An operator can move across every schedule-center route with independent
session, permission, server-query, business-command, navigation, overlay,
theme, and locale state while preserving all existing backend-authoritative
behavior.

## Parent Artifacts

- `openspec/changes/schedule-center-local-stability-v1/requirements.md`
- `openspec/changes/schedule-center-local-stability-v1/acceptance.md`
- `openspec/changes/schedule-center-local-stability-v1/prototype/handoff.md`

## Vertical Slice

Bootstrap the existing management session and permissions, load independently
tolerant server collections, navigate and open overlays through focused
workspace state, execute existing guarded commands through a separate command
provider, and expose a compatibility `useApp()` facade that composes focused
hooks without owning mutable state.

## In Scope

- Split session bootstrap/login/logout from permissions and server collections.
- Split permission checks from navigation and feature read state.
- Split server query refresh, partial failures, integration state, and mapped
  collections from business commands.
- Split assignment, shipment, dispatch, return, review, and refresh command
  pending/error behavior into a focused command provider.
- Split active route, selected model, dialogs, drawers, and shipping preselection
  into a focused workspace provider.
- Preserve `useApp()` as a compatibility facade until consumers migrate; it
  owns no state and only composes focused hooks.
- Guard authentication cache access against unavailable browser storage while
  preserving existing keys and serialization.
- Add provider/model tests for session reset, permission refresh, partial query
  state, command guards, invalid route fallback, overlay reset, and facade
  composition.
- Run the final source boundary, translation, privacy, line-ceiling,
  responsive, theme, locale, keyboard, and cross-route browser matrix.

## Out Of Scope

- New global state dependencies, APIs, permissions, storage keys, business
  commands, optimistic mutations, scheduling logic, or deployment.
- Rewriting already approved feature presentation unless final verification
  exposes a contained regression within this change.

## Files Allowed

- `camera-rental-schedule-center/package.json`
- `camera-rental-schedule-center/pnpm-lock.yaml`
- `camera-rental-schedule-center/src/App.tsx`
- `camera-rental-schedule-center/src/context/AppContext.tsx`
- `camera-rental-schedule-center/src/features/session/**`
- `camera-rental-schedule-center/src/features/permissions/**`
- `camera-rental-schedule-center/src/features/data/**`
- `camera-rental-schedule-center/src/features/commands/**`
- `camera-rental-schedule-center/src/features/workspace/**`
- `camera-rental-schedule-center/src/api/auth.ts`
- `camera-rental-schedule-center/src/api/auth.test.ts`
- `camera-rental-schedule-center/src/api/client.ts`
- `camera-rental-schedule-center/src/api/client.test.ts`
- `camera-rental-schedule-center/src/api/rental.ts`
- `camera-rental-schedule-center/src/api/snapshotLoader.ts`
- `camera-rental-schedule-center/src/api/snapshot.test.ts`
- `camera-rental-schedule-center/src/app/accessModel.ts`
- `camera-rental-schedule-center/src/app/accessModel.test.ts`
- `camera-rental-schedule-center/src/features/dashboard/DashboardPage.tsx`
- `camera-rental-schedule-center/src/features/dashboard/dashboardModel.ts`
- `camera-rental-schedule-center/src/features/dashboard/dashboardModel.test.ts`
- `camera-rental-schedule-center/src/features/preferences/messages.ts`
- `camera-rental-schedule-center/src/features/providers/**`
- `camera-rental-schedule-center/src/shared/hooks/**`
- `camera-rental-schedule-center/src/shared/lib/safeError.ts`
- `camera-rental-schedule-center/src/index.css`

## Interfaces / Seams

- `SessionContext` owns authentication state and login/logout only.
- `PermissionContext` owns stable permission information and permission checks.
- `ScheduleCenterDataContext` owns server queries, mapped collections, safe
  partial failures, integration state, and refresh.
- `ScheduleCenterCommandsContext` owns business commands and their pending/error
  lifecycle, then invalidates data through the query seam.
- `WorkspaceContext` owns navigation, selected model, overlays, and temporary
  cross-route UI selection.
- `PreferenceContext` remains separate and unchanged in ownership.
- `useApp()` composes the focused hooks and preserves the current consumer
  contract without a provider or mutable state of its own.

## Components To Create

- `SessionProvider`
- `PermissionProvider`
- `ScheduleCenterDataProvider`
- `ScheduleCenterCommandsProvider`
- `WorkspaceProvider`
- `ScheduleCenterProviders`

## Components To Reuse

- Existing authentication cache/client, typed rental APIs, snapshot loader,
  mappers, access model, safe error classification, preferences, and feature
  components.

## Components To Extract

- Session bootstrap/reset.
- Permission refresh and checks.
- Query loading/partial-error/auth-error state.
- Command submit guards, pending/error state, and refresh-after-success.
- Navigation, selected model, overlay, and temporary cross-route state.

## API / Data Flow Contracts

- Preserve every existing route, request, response, token/tenant storage key,
  idempotency behavior, permission code, and mapped read model.
- Authentication failures reset session state; expected feature permission or
  query failures remain scoped to that collection.
- No mutation is accepted before the backend response; successful commands
  trigger server refresh through the data provider.
- Private search or customer values remain feature-local and do not enter any
  provider, URL, storage, analytics, or logs.

## State / Error / Empty / Loading Behavior

- Loading: session, permissions, queries, and commands expose independent
  pending state rather than one global spinner.
- Empty: mapped collections retain their existing explicit empty semantics.
- Error: authentication, permission, partial query, integration, and command
  errors remain distinct and safely localized.
- Disabled: command providers explain missing permission or server readiness
  without changing read state.
- Permission: invalid active routes return to dashboard and unavailable
  queries are not issued.

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `pnpm test`
- `pnpm lint`
- `pnpm build`
- `git diff --check`
- Production source 600-line ceiling, forbidden-import, translation-key,
  private-data, raw-error, invented-data, and AppleDouble scans.
- Browser route and overlay matrix at 1440, 1087, 768, 390, and 360 CSS pixels
  in light/dark and representative `zh-CN`/`en`.

## Stop Conditions

- Scope lock mismatch.
- Decomposition requires a new API, permission, business transition, storage
  key, or global state dependency.
- The compatibility facade would retain mutable state or create another source
  of server truth.
- Final verification exposes a structural requirement outside the approved
  change.

## Unsafe Assumptions

- One loading boolean can represent session, queries, and commands correctly.
- Cached permissions are always current.
- A client command result can be accepted before refresh.
- A compatibility facade may continue owning global mutable state.
- Browser storage is always available.
