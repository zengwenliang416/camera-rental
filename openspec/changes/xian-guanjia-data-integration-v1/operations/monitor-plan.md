# Monitor Plan

## Signals

- Systemd status and recent logs for `camera-rental-server.service`.
- Nginx configuration and HTTP status for admin and schedule-center routes.
- Backend actuator health.
- Authenticated device list, signed QR generation, and QR resolution.
- MySQL column presence, target-device uniqueness, and Xianyu order counts.

## Observation Window

- Observe continuously during deployment and for at least ten minutes after
  service restart.
- Recheck public routes and backend health after the first post-deploy sync
  activity.

## Escalation

- Roll back the application symlink immediately for startup, routing, or API
  regressions.
- Stop further data writes and retain the pre-deploy database dump for any
  migration-related anomaly.
- Keep XianGuanJia write operations disabled unless separately authorized.
