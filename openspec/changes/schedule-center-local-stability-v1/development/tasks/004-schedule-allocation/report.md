# Task Report: 004-schedule-allocation

## Status

DONE

## Files Changed

- `camera-rental-schedule-center/package.json`
- `camera-rental-schedule-center/src/components/GanttScheduleView.tsx`
- `camera-rental-schedule-center/src/components/OrderAllocationModal.tsx`
- `camera-rental-schedule-center/src/features/schedule/**`
- `camera-rental-schedule-center/src/shared/ui/DateRangeDisplay.tsx`
- `camera-rental-schedule-center/src/shared/ui/BillableOccupiedRangeLegend.tsx`
- `camera-rental-schedule-center/src/shared/ui/ConfirmDialogShell.tsx`
- `camera-rental-schedule-center/src/features/preferences/messages.ts`
- `camera-rental-schedule-center/src/lib/scheduleEngine.ts`

## What Changed

- Replaced the legacy Gantt implementation with a focused schedule page,
  filters, timeline, responsive device list, explicit internal horizontal
  scroller, and localized state legend.
- Kept billable dates separate from the inclusive display of the backend
  end-exclusive occupied range.
- Changed provisional device recommendation to inspect the occupied interval,
  while retaining the backend assignment transaction as the only authority.
- Split physical-device allocation into a focus-managed dialog, requirement
  cards, candidate picker, progress state, permission/details/period guards,
  and server-authority explanation.
- Prevented repair and locked devices from displaying as schedulable when no
  schedule block is present.
- Added textual and accessible labels for rental, reservation, repair, lock,
  and free timeline states so color is not the only signal.
- Repair and locked devices no longer show an immediate-availability fallback
  in list mode, incomplete occupied ranges disable both automatic and manual
  candidate selection, and compact timeline/dialog controls meet 44px targets.
- Kept the dialog open after submission because the existing command seam does
  not expose a reliable success/failure result to this slice.

## TDD Evidence

- Added pure model tests for local date windows, billable/occupied ranges,
  device filters, allocation progress, submit gates, and occupied-range
  recommendation.
- The schedule-center suite now contains 36 passing tests.

## Verification Commands

- `pnpm test`: 36 tests passed, 0 failed.
- `pnpm lint`: `tsc --noEmit` exited 0.
- `pnpm build`: Vite production build exited 0.
- `git diff --check`: exited 0.
- Production line-ceiling scan: no TypeScript, TSX, or CSS source exceeded 600
  physical lines.
- Browser route and empty-state check passed against the current local
  management snapshot.

## Concerns

- The current local management snapshot contains no registered device models,
  so populated schedule lanes and the allocation dialog could not be exercised
  in the browser without writing fixture data.
- Responsive shell behavior at 1440, 768, 390, and 360 CSS pixels was already
  system-verified in task 003. This slice still needs populated-data browser
  evidence before release-level sensory verification.

## Scope Deviations

- The implementation remained within the approved frontend slice and did not
  change APIs, scheduling semantics, commands, or persistence.

## Follow-up Needed

- Run the populated-data timeline and allocation interaction matrix in the
  final change-level browser verification task.

## Adjudication

No unresolved implementation blocker. Independent spec and quality review are
required before marking task 004 complete.
