# Quality Review: 004-schedule-allocation

## Verdict

approved

## Separation Of Concerns

- The former 523-line Gantt component and 343-line allocation modal are thin
  compatibility entries. Page coordination, pure schedule/allocation models,
  filters, timeline, responsive list/table rendering, candidate selection,
  range presentation, and dialog focus management are separated into bounded
  modules.
- Presentational schedule and shared components do not import transport
  clients. The page and dialog containers use the approved `useApp()` seam and
  pass read models and intent callbacks downward.
- Candidate availability remains explicitly provisional; the existing backend
  assignment command is the authoritative mutation boundary.

## Component Cohesion / Coupling

- `ConfirmDialogShell`, `DateRangeDisplay`, and
  `BillableOccupiedRangeLegend` are cohesive reusable components with
  behavior-facing props.
- Occupied-range validity is owned by the allocation container and passed to
  `AllocationItemCard`; invalid ranges disable the manual entry and prevent
  `AllocationCandidatePicker` from mounting or evaluating empty dates.
- Repair/locked status presentation is consistent between responsive list rows
  and empty Gantt cells. All compact schedule actions now use a guaranteed
  44px target without introducing a second interaction abstraction.

## Test Quality

- Independently rerun `pnpm test`: 47 tests passed, 0 failed in the current
  collaborative worktree. Focused schedule coverage verifies local date
  windows, billable/occupied separation, filtering, allocation progress and
  submit gates, and occupied-range recommendation.
- Static review additionally verified repair/lock presentation, incomplete
  occupied-range candidate suppression, focus lifecycle implementation,
  localized dismissal, non-color status text, and the corrected touch-target
  classes.
- Populated browser evidence is intentionally deferred to task 007/change-level
  verification because the current snapshot has no registered models. This is
  a documented residual verification boundary, not a code-quality defect.

## Error Handling

- Permission, rental/occupied period, internal-detail, incomplete-selection,
  and submitting states are explicit and localized. Duplicate submission is
  prevented while the command promise is pending.
- Automatic and manual candidate paths reject incomplete occupied ranges.
  Repair and locked states remain hard blockers even without schedule blocks.
- The dialog does not close or render success before server acceptance, so
  assignment state remains server-owned.

## Reuse / Duplication

- Date ranges, billable/occupied legend, schedule-status legend, dialog shell,
  status badge, empty state, filters, and pure schedule functions are reused
  rather than embedded in the page.
- No new production dependency was added. Existing mapped models, permissions,
  commands, tokens, and schedule helpers remain in use.

## Complexity Delta

- Independently rerun `pnpm lint` and `pnpm build`; TypeScript no-emit and the
  Vite production build both exited 0. Schedule and allocation remain separate
  lazy chunks.
- Repository `git diff --check` passed. No production TypeScript, TSX, or CSS
  file exceeds 600 physical lines; the largest files are `AppContext.tsx` at
  598 lines and `messages.ts` at 597 lines, while the largest new 004 module is
  `AllocationDialog.tsx` at 241 lines.
- The refactor removes large mixed-responsibility implementations, preserves
  the existing command and data seams, and does not add duplicated server
  state or a new framework.

## Required Fixes

- No blocking quality fixes remain for task 004 before development handoff.
