# Changelog: schedule-center-local-stability-v1

## Changed

- Rebuilt the schedule-center frontend architecture and responsive operational UI.
- Added authorized complete receiver details, order pagination, and device/order search.
- Moved all tenant XianGuanJia business configuration into the management database.
- Removed legacy XGJ environment and credential-reference compatibility.
- Applied migrations 029 through 031 and deployed release `b5968cf2`.
- Standardized schedule-center deployment on pnpm with incremental dependency reuse.
