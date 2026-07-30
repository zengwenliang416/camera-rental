# Task Report: 007-state-decomposition-final

## Status

DONE

## Files Changed

- `camera-rental-schedule-center/package.json`
- `camera-rental-schedule-center/pnpm-lock.yaml`
- `camera-rental-schedule-center/src/App.tsx`
- `camera-rental-schedule-center/src/context/AppContext.tsx`
- `camera-rental-schedule-center/src/api/auth.ts`
- `camera-rental-schedule-center/src/api/auth.test.ts`
- `camera-rental-schedule-center/src/api/client.ts`
- `camera-rental-schedule-center/src/api/client.test.ts`
- `camera-rental-schedule-center/src/api/rental.ts`
- `camera-rental-schedule-center/src/api/snapshotLoader.ts`
- `camera-rental-schedule-center/src/api/snapshot.test.ts`
- `camera-rental-schedule-center/src/app/accessModel.ts`
- `camera-rental-schedule-center/src/app/accessModel.test.ts`
- `camera-rental-schedule-center/src/features/session/**`
- `camera-rental-schedule-center/src/features/permissions/**`
- `camera-rental-schedule-center/src/features/data/**`
- `camera-rental-schedule-center/src/features/commands/**`
- `camera-rental-schedule-center/src/features/workspace/**`
- `camera-rental-schedule-center/src/features/providers/**`
- `camera-rental-schedule-center/src/features/dashboard/DashboardPage.tsx`
- `camera-rental-schedule-center/src/features/dashboard/dashboardModel.ts`
- `camera-rental-schedule-center/src/features/dashboard/dashboardModel.test.ts`
- `camera-rental-schedule-center/src/features/preferences/messages.ts`
- `camera-rental-schedule-center/src/shared/hooks/useLatestRequest.ts`
- `camera-rental-schedule-center/src/shared/lib/safeError.ts`
- `camera-rental-schedule-center/src/index.css`

## What Changed

- Replaced the monolithic 598-line mutable `AppContext` provider with focused
  session, permission, server-data, command, and workspace providers.
- Preserved `useApp()` as a 95-line compatibility facade that composes focused
  hooks and owns no Context, state, reducer, cache, or server collection.
- Session state now owns only login, logout, current identity, authentication
  reset, pending state, and a revision used to invalidate downstream state.
  Successful account switching clears the prior permission/identity cache
  before the new permission bootstrap, and failed login leaves no stale token
  or identity that can reappear after reload.
- Permission state refreshes the existing permission-info contract, preserves
  same-session cached fallback, distinguishes access denial from
  synchronization failure, centralizes wildcard/any-of permission checks, and
  rejects superseded or pre-reset responses with a monotonic
  request-generation guard. Committed values are revision-bound, so a new
  account cannot see the previous identity or permissions while bootstrapping.
- Data state owns permission-scoped snapshot reads, partial collection failure,
  mapped orders/devices/schedules/reviews, integration readiness, and refresh.
  Authentication failures reset the session; collection failures do not erase
  successful sibling collections. Snapshot responses are scoped to the active
  session and permission revisions so logout, relogin, permission changes, and
  overlapping refreshes cannot restore stale private data.
- Command state owns assignment, shipment, dispatch, return, manual-review,
  duplicate-submit protection, pending state, command errors, and refresh after
  accepted backend responses. Terminal authentication failures reset Session,
  and no optimistic server-state mutation was added. Each command is scoped to
  the session/permission revision and owns a unique pending token; stale
  success, generic failure, or authentication failure cannot refresh, report
  an error, clear a newer duplicate key, or reset a newer Session.
  Distinct command keys remain independently active within the same revision,
  and each completion updates the shared pending count without superseding
  another accepted command.
- Workspace state owns the active route, selected model, allocation/device
  overlays, shipping preselection, and login overlay. Unauthorized routes fall
  back to dashboard and transient overlays reset with the session.
- XianGuanJia configuration is queried only with
  `rental:xianyu:query`; ship-only access no longer issues a known-forbidden
  readiness request and the shipment endpoint remains the authoritative gate.
- Synchronization state stores only timestamp and count facts. Dashboard copy
  is formatted through the active locale instead of retaining Chinese display
  strings in the data provider.
- Authentication cache reads, writes, and removals now fail safely when browser
  storage is unavailable while preserving all existing keys and serialization.
- A second `401` after token refresh is normalized to `AUTH_REQUIRED`, clears
  cached authentication state, and reaches the same Session reset seam used by
  provider and command failures.
- Removed the route-permission repair effect from `App.tsx`; workspace state is
  now the single owner of route validity.

## TDD Evidence

- Added session bootstrap/reset model tests.
- Added permission wildcard, any-of, and schedule-center access tests.
- Added partial query-health modeling while retaining the existing per-source
  snapshot failure tests.
- Added persisted XianGuanJia shipment guard and duplicate command-key tests.
- Added route fallback, selected-model repair, and state-free facade tests.
- Added authentication cache behavior coverage for unavailable browser storage.
- Added mounted shared-overlay lifecycle and route-preserving quick-binding
  reachability coverage.
- Added mounted Provider orchestration coverage for superseded permission and
  snapshot responses, logout invalidation, ship-only configuration access,
  command authentication reset, and refresh after an accepted command.
- Added account-switch coverage proving prior identity, permissions, data, and
  synchronization facts disappear immediately, and failed new-session
  permission loading does not reuse the old cache.
- Added stale-command coverage for success, generic failure, and
  `AUTH_REQUIRED` completing after a newer session is established.
- Added concurrent distinct-command coverage proving both accepted commands
  refresh independently and pending state returns to idle only after both
  complete.
- Added API-client coverage for a repeated `401` after token refresh and
  dashboard coverage for locale-correct synchronization summaries.
- The complete schedule-center suite contains 74 passing tests.

## Verification Commands

- `pnpm test`: 74 tests passed, 0 failed.
- `pnpm lint`: `tsc --noEmit` exited 0.
- `pnpm build`: Vite production build exited 0; schedule, order, device,
  exception, allocation, QR/detail, and shipping features remain lazy chunks.
- `pnpm install --frozen-lockfile --offline`: exited 0, proving the checked-in
  lockfile and cached dependency graph reproduce without registry access.
- `git diff --check`: exited 0.
- Production line-ceiling scan: no TypeScript, TSX, or CSS file exceeds 600
  physical lines; `messages.ts` is 599 lines and the largest new provider is
  below the ceiling.
- Source-boundary scans found no AppleDouble files, mutable state in
  `AppContext.tsx`, provider URL/storage persistence, or raw error rendering.
- Browser responsive matrix at 1440, 1087, 768, 390, and 360 CSS pixels found
  no page-level horizontal overflow.
- All six routes were reachable at 1440 and 390, rendered localized headings,
  and exposed no raw translation keys.
- Representative dark/en validation rendered complete English exception copy
  without Chinese feature fragments; light/`zh-CN` was restored afterwards.
- Login overlay validation confirmed initial username focus, Escape dismissal,
  and focus return to the account trigger.
- A fresh clean browser tab loaded the current provider tree with zero console
  errors or warnings.

## Concerns

- The current local management snapshot contains zero registered devices and
  zero manual reviews. Populated device drawer, signed QR success, allocation
  dialog, and review-resolution browser scenarios remain unverified without
  controlled backend responses; no database fixture or invented business data
  was introduced.
- The static locale dictionary is 599 lines. It remains one bounded lookup
  responsibility and below the hard ceiling, but future copy additions should
  use an explicitly approved locale-file scope expansion.
- Provider orchestration tests use injected loaders and command services to
  control request completion deterministically. They validate ownership and
  reset boundaries without calling production services or inventing business
  records.

## Scope Deviations

- Added `happy-dom` as a development-only interaction-test dependency and
  generated `pnpm-lock.yaml` for reproducible `pnpm` installs. No production
  dependency, API, permission, storage key, business command, backend,
  database, deployment, or production-data write changed.
- After independent review, the 007 task packet was explicitly refined to list
  the task-owned shared request-generation hook, Provider orchestration tests,
  API authentication regressions, Dashboard synchronization localization, and
  lockfile. These files implement existing approved lifecycle requirements and
  introduce no new business scope.

## Follow-up Needed

- Run populated overlay and command scenarios only when controlled tenant data
  or a dedicated mocked browser environment is available.

## Adjudication

All independent review findings have been implemented and revalidated. The
final spec and quality reviews are approved with A1-A5 verified, so task 007 is
ready for the development handoff contract.
