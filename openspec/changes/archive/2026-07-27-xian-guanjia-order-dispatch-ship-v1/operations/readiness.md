# Operations Readiness: xian-guanjia-order-dispatch-ship-v1

## Operations Scope

- Release target: `project-deploy`

## Readiness Decision

Ready for archive and project-deploy handoff.

This readiness decision does not authorize real XianGuanJia shipment writes.
Production write enablement remains gated by `XGJ_WRITE_ENABLED=true`, controlled
shop credentials, and explicit operator approval.

## Evidence

- `verify/aggregate-report.json` verdict is `green` across facticity, static,
  unit, redteam, e2e, and sensory domains.
- `verify/receipt.json` confidence is `B`, with no uncovered scope.
- `verify/e2e/artifacts/mock-ship-success-db-proof-20260727.json` proves local
  shipment persistence, device state changes, assignment state changes, and mock
  XianGuanJia ship request payloads.
- `git diff --check` exited 0 after verification.
- Runtime was returned to safe local mode: backend listening on 48080,
  `XGJ_WRITE_ENABLED=false`, mock server on 18089 stopped, and Quartz startup
  disabled for this process.
- Migration SQL hash matches manifest:
  `c7bf4b14a36f2f089adf498ef1277bbb8bb8933f8bb62c3ccd4a171c64b43f3a`.
