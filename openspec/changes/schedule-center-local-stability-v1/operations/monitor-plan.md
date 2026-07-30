# Monitor Plan

## Signals

- `camera-rental-server.service` state and startup exceptions.
- HTTP status and release SHA for the admin and schedule-center endpoints.
- XianGuanJia synchronization failures, authorization expiry, and write errors.
- Duplicate shipment/idempotency conflicts and device assignment transitions.
- Database orphan counts and unexpected decreases across rental/XianGuanJia tables.

## Observation Window

- Immediate smoke window after deployment.
- Elevated review for the first 24 hours.
- Explicit review around the first operator-authorized real shipment.

## Normal Values

- Public admin routes return HTTP 200 and report release `b5968cf2`.
- No encryption, bad-padding, unknown-column, or application-startup errors.
- No table row count decreases attributable to migrations.
- No third-party write occurs without an authorized operator action.

## Owner

- Operations owner: 老大.
- Technical triage: 小G.

## Escalation

- Stop new shipment actions and disable the persisted write switch in the
  management UI.
- Preserve logs and database evidence.
- Use `operations/rollback-plan.md` when application or data integrity is at risk.
