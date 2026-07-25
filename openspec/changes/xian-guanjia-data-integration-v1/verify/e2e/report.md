# Domain Report: e2e

## Domain

e2e

## Verdict

partial

## Inputs Reviewed

- requirements.md
- acceptance.md
- development/handoff-to-verify.md
- verify/command-results.md
- verify/runtime-evidence.json

## Evidence

- Backend unit and admin static checks passed.
- Authenticated API-level runtime smoke was executed against current jar
  `48082`, connected to the real local XianGuanJia data store with
  scheduler/infra jobs disabled.
- Runtime smoke returned config `READY`, shops total `7`, orders total `730`,
  and sync-run total `1841` on first check / `1861` on repeat check. The
  sync-run table is live data.
- A 2026-07-25 API-level smoke against the rebuilt current jar on `48082`
  returned config `READY`, shops total `7` (`4` valid, `3` invalid), orders
  total `732`, manual-review page total `1` without HTTP 500, `94`
  express companies, and one after-sale sync page with `20` received and `20`
  persisted records.
- A later 2026-07-25 API-level smoke after applying migration 020 to the active
  local MySQL database returned code `0` for config, shop page, order page,
  after-sale page, express-company list, alert page, raw-payload page,
  manual-review page, sync-run page, and report overview with required dates.
  It also verified invalid raw and push replay requests return safe business
  codes instead of HTTP 500.
- Runtime comparison showed `48080` is an older running process with real data
  but no `/admin-api/rental/xianyu/sync-run/page` route; `48081` is current
  isolated verification code/data.
- Admin `5174` was restarted with `VITE_BASE_URL=http://127.0.0.1:48082`.
- Authenticated browser E2E was executed with a temporary headless Chrome
  profile against admin `5174` and backend `48082`.
- Browser E2E covered `/rental/xianyu`, `/rental/review`,
  `/rental/sync-run`, and `/rental/report`. All observed
  `/admin-api/rental/**` responses were HTTP 200 and the API host was
  `127.0.0.1:48082`.
- Initial browser E2E exposed missing dynamic menus for sync-run and report,
  which produced 404 pages. Migration 018 and a runtime menu/role repair fixed
  this; the repeat browser run showed no 404 for those routes.
- Screenshot artifacts were recorded under `verify/e2e/artifacts/`; the Xianyu
  screenshot was re-generated with table bodies blurred to avoid retaining
  external order or after-sale identifiers.
- Runtime permission-denied behavior was verified at API level against backend
  `48082` by creating a temporary role and user with no rental menu grants. The
  temporary user could login and read permission info, but
  `/admin-api/rental/report/overview` returned business code `403`. The
  temporary user and role were deleted after the probe.
- Permission-denied browser click-through was verified against admin `5174`
  and backend `48082` with a temporary no-rental-grant user. The user had no
  rental menu/permission, direct navigation to `/rental/report` rendered a
  denied/not-found state without rental content, the sidebar had no rental
  entry, and the temporary user/role were deleted.
- Default front-end masking was added for channel identifiers in ordinary
  management lists after the browser run: rental order external order id,
  Xianyu external shop id, authorization id, after-sale id, and after-sale
  external order id. This specific masking change has `pnpm ts:check` evidence,
  but not a repeated browser run yet.
- Seller-remark masking was added after the browser run. The backend now redacts
  ordinary order-page `sellerRemark` responses, and the front-end table applies
  a defensive text mask before rendering. This change has focused backend unit
  evidence and `pnpm ts:check`, but not a repeated browser run yet.
- After the browser run, backend-default masking was added for ordinary order,
  shop authorization, and after-sale external identifiers. A rebuilt current jar
  on `48083` verified masked real-data API responses for these three pagination
  endpoints; this still lacks a repeated browser screenshot pass.
- After the browser run, local startup config was fixed so
  `XGJ_JOB_REGISTER_INFRA_JOBS=false` disables infra-job registration. This was
  verified by a backend startup-log probe on temporary port `48084`; it is not
  browser-observable behavior.
- Browser masking verification was repeated against the current rebuilt backend
  jar on temporary `48085` and admin `5175` through an isolated headless Chrome
  CDP session on `9226`.
- `/rental/order` stayed on the target route, observed 4 rental API HTTP 200
  responses, had masked markers in visible table text, and had no mainland
  mobile-number or 10+ continuous-digit pattern in visible table text.
- `/rental/xianyu` stayed on the target route, observed 6 rental API HTTP 200
  responses, had masked markers in visible table text, and had no mainland
  mobile-number or 10+ continuous-digit pattern in visible table text.
- Blurred screenshots were retained under `verify/e2e/artifacts/`:
  `2026-07-24T17-49-42-075Z-rental-order-masked.png` and
  `2026-07-24T17-49-42-075Z-rental-xianyu-masked.png`.
- A current browser transition/sensory pass on 2026-07-25 used isolated admin
  `5176` and rebuilt backend `48086`. It covered `/rental/xianyu`,
  `/rental/order`, `/rental/schedule`, `/rental/review`, `/rental/sync-run`,
  and `/rental/report` across `zh-CN` / `en` and light / dark states.
  All 24 route/language/theme states settled on target routes, showed no
  loading, denied, or error state, and all observed `/admin-api/rental/**`
  responses were HTTP 2xx.
- That pass also verified visible privacy patterns after fixing report product
  external-ID display: no visible mainland mobile-number pattern and no
  10+ continuous digit pattern were detected. Representative blurred
  screenshots and the structured JSON receipt are under
  `verify/e2e/artifacts/2026-07-25T00-44-37-775Z-*`.

## Commands Run

- `node <<'NODE' ...` authenticated API smoke for `48080`, `48081`, and `48082`
  without printing tokens, secrets, order numbers, or PII.
- `node --input-type=module <<'NODE' ...` authenticated API smoke for config,
  shop, order, manual-review, express-company, after-sale page, and after-sale
  one-page sync against `48082` without printing tokens, secrets, order numbers,
  after-sale identifiers, or PII.
- `node <<'NODE' ...` authenticated API smoke after runtime migration 020 for
  config, shop, order, after-sale, express-company, alert, raw-payload,
  manual-review, sync-run, report overview with required dates, no-token raw
  access, invalid raw replay, and invalid push replay against `48082`, without
  printing tokens, secrets, external identifiers, seller remarks, or PII.
- `VITE_PORT=5174 VITE_BASE_URL=http://127.0.0.1:48082 bash camera-rental-admin/scripts/start-local.sh`
- `node --input-type=module <<'NODE' ...` browser E2E through Chrome DevTools
  Protocol for login, Xianyu integration, manual review, sync-run, and report
  pages.
- Runtime system-menu repair through official `/system/menu/**` and
  `/system/permission/assign-role-menu` admin APIs to align the current local
  database with migration 018 before repeating browser E2E.
- `python3 <<'PY' ...` temporary-role permission-denied API probe against
  backend `48082`, without printing tokens, secrets, order identifiers, or PII.
- `node --input-type=module <<'NODE' ...` permission-denied browser
  click-through through Chrome DevTools Protocol against admin `5174` and
  backend `48082`, without printing tokens, secrets, order identifiers, or PII.
- `node --input-type=module <<'NODE' ...` browser masking verification through
  Chrome DevTools Protocol for `/rental/order` and `/rental/xianyu` against
  admin `5175` and backend `48085`, without printing tokens, external
  identifiers, seller remarks, or PII.
- `VITE_PORT=5176 VITE_OPEN=false VITE_BASE_URL=http://127.0.0.1:48086 bash scripts/start-local.sh`
- `node --input-type=module <<'NODE' ...` browser transition/sensory flow for
  six rental pages across zh-CN/en and light/dark states against admin `5176`
  and backend `48086`, without printing tokens, secrets, raw payloads,
  external identifiers, seller remarks, or PII.

## Findings

- API-level runtime routing is now proven for current code plus real data,
  including after-sale and express-company read-only surfaces.
- The manual-review null `resolved_by` regression is fixed at runtime.
- Browser routing and basic page rendering are now proven for the core rental
  XianGuanJia, review, sync-run, and report pages.
- Menu-role drift was a real runtime issue; migration 018 now repairs existing
  databases by granting schedule, sync-run, and standalone report menu
  visibility to roles that already have the rental root/report permissions.
- Permission denial is proven at API level for a temporary user with no rental
  menu grants.
- Permission denial is now also proven at browser route level for a temporary
  user with no rental menu grants.
- Ordinary management list code now masks channel identifiers by default before
  rendering them in table cells.
- Ordinary admin order pagination now redacts `sellerRemark` at the response
  boundary before browser code receives it.
- Ordinary order, shop authorization, and after-sale pagination APIs now return
  masked external channel identifiers by default.
- Latest browser masking evidence now covers ordinary order and Xianyu
  shop/after-sale visible table text against the current rebuilt backend jar.
- Runtime migration 020 fixed the active database drift behind express-company
  raw evidence persistence and push replay token persistence; focused API smoke
  no longer reproduces the previous HTTP 500 failures.
- Current browser transition, localization, theme, visible privacy, and desktop
  sensory behavior are now proven for the six V1 rental admin pages.

## Required Fixes

- Broader red-team scenarios remain: cross-tenant probes and more replay abuse
  cases.

## Residual Risk

- Mobile/responsive browser behavior remains a follow-up because this pass used
  a desktop `1440x1000` viewport.

## Follow-up Domain Routing

- E2E is green for the current desktop admin V1 browser smoke. Keep broader
  red-team routing separate from browser transition evidence.
