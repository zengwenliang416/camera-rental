# Rollback Plan

## Triggers

- Backend service does not become healthy after restart.
- Admin or schedule-center routes fail their smoke checks.
- New rental APIs return server errors.
- Database migration verification does not match the expected additive columns.

## Rollback Command

Point `/opt/camera-rental/current` back to the prior release directory, restart
`camera-rental-server.service`, validate Nginx, and reload it. Restore the
database from the pre-deploy dump only if an additive migration itself caused
data corruption; do not drop rental, channel, schedule, or audit history as a
routine application rollback.

## Verification

- Confirm the previous release is the target of `/opt/camera-rental/current`.
- Confirm backend health and both public frontend routes return HTTP 200.
- Confirm `camera-rental-server.service` and `nginx` are active.
- Confirm migration rollback, if required, from an isolated restore before
  changing production data.
