# Deploy Plan

## Environment

- Production host: `211.101.246.160`
- Public origin: `https://rental.motion-cover.com`
- Deploy root: `/opt/camera-rental`
- Release scope: backend, admin frontend, and schedule-center frontend
- Existing staff and PC web artifacts are preserved by `ops/github-deploy/server-deploy.sh`

## Command

Build locally, create a release archive containing `server/yudao-server.jar`,
`admin/`, and `schedule-center/`, copy it over SSH, then run:

```bash
RELEASE_SHA=manual-20260728-device-p3 \
  /opt/camera-rental/server-deploy.sh /tmp/camera-rental-release.tgz
```

The deployment is direct to the server and does not use GitHub.

## Config / Secrets

- Runtime config: `/opt/camera-rental/shared/application-prod.yaml`
- Runtime environment: `/opt/camera-rental/shared/backend.env`
- MySQL password file: `/opt/camera-rental/shared/.mysql-password`
- Device QR signing secret: `/opt/camera-rental/shared/.device-qr-secret`
- Secret values are not copied into source, release archives, or deployment logs.

## Migrations

- Manifest: `development/migrations/manifest.json`
- Deployment evidence: `operations/migration-deployment.json`
- Back up the production database before applying additive migrations.
- Apply only migrations whose target columns are absent.
- Current deployment adds receiver snapshot fields and logistics date fields.

## Smoke Checks

- `camera-rental-server.service` and `nginx` are active.
- Nginx configuration passes `nginx -t`.
- Backend health endpoint returns HTTP 200.
- Admin and schedule-center public routes return HTTP 200.
- Authenticated device page includes `P3-05-5WTCN7F002B088`.
- Production-signed QR raster decodes to the expected `CRD1` payload.

## Owner

- Deployment operator: Codex, acting on the user's explicit production deployment request.
- Business owner: workspace user.

## Deploy Window

- Immediate deployment on July 28, 2026 after local tests and production backup.
