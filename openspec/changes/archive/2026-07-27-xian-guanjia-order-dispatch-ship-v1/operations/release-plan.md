# Release Plan: xian-guanjia-order-dispatch-ship-v1

## Release Target

`project-deploy`

## Required Artifacts

- `operations/readiness.md`
- `operations/readiness.json`
- `operations/release-checklist.json`
- `operations/changelog.md`
- `operations/release-notes.md`
- `operations/deploy-plan.md`
- `operations/rollback-plan.md`
- `operations/monitor-plan.md`
- `operations/migration-deployment.json`
- `operations/update-spec.json`

## Release Decision

Archive is allowed after the operations gate passes. Deployment should apply the
SQL migration first, then deploy backend, admin, and staff builds together so the
shipment table, permissions, API surface, and UI are consistent.

Do not enable `XGJ_WRITE_ENABLED=true` in production until a controlled shop
write test is approved and scheduled.
