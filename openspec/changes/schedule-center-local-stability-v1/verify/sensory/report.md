# Sensory Report

## Domain

sensory

## Verdict

green

## Inputs Reviewed

- Approved cases, sensory rubric, responsive JSON results, desktop/mobile screenshots, DOM snapshots, preference/focus evidence, console evidence, source architecture, and resolved root causes.

## Evidence

- `reviewer-independence.md` records the read-only, evidence-backed review boundary.
- `review.md` covers UX, accessibility, responsive behavior, privacy, maintainability, and performance feel.
- `findings.jsonl` contains evidence-linked passing findings.
- `runs/schedule-center-verification-20260730/orders-pagination-verification.json` records the order-page interaction review without retaining customer values.

## Commands Run

- Visual inspection of latest dashboard, shipping, and admin screenshots.
- DOM and responsive matrix inspection.
- Preference, focus, privacy-state, source-size, and browser-console evidence review.

## Findings

- The interface is coherent on desktop and mobile and preserves operational priorities.
- Accessibility naming, preference persistence, focus restoration, touch layout, and reduced-motion handling are present.
- Authorized order cards keep complete delivery details readable while limiting each page to 10 records.
- Shipping makes sensitive-data use and disabled write state explicit without retaining search values in final evidence.
- The frontend decomposition is understandable and respects the 600-line limit.

## Required Fixes

- None.

## Residual Risk

- Contrast was reviewed from captured states rather than a dedicated automated contrast analyzer.
- Performance was assessed through build output and interaction feel, not a production network trace.

## Follow-up Domain Routing

- Dedicated contrast automation and production performance telemetry may be added during operations hardening.

## Incremental Rerun

- At `2026-07-30T09:38:30Z`, the live 1280px schedule page was reviewed visually and through computed layout metrics.
- The device identity, “空闲在库” badge, and SN now form a stable hierarchy; the badge no longer breaks into an isolated second line.
- The schedule model/status selects and the order, device, exception, and page-size selects use one consistent arrow treatment with visible right breathing room.
- No new visual clutter, unnamed control, page-level overflow, or console error was found at the available desktop viewport.
- A fresh mobile screenshot was not produced by the current browser surface; existing 360px/390px artifacts and unchanged responsive containers remain the supporting mobile evidence.
- At `2026-07-30T10:44:21Z`, the final committed source and successful production builds were reconciled with the independent visual/DOM evidence; no post-review UI source drift was found.
