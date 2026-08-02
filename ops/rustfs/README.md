# RustFS Return Photo Storage

RustFS is deployed under `/opt/camera-rental/rustfs` with loopback-only S3 and
console ports. Provision it once on the 211 server before running the application
deployment:

```bash
cd /opt/camera-rental/source
bash ops/rustfs/provision.sh /opt/camera-rental/rustfs /opt/camera-rental
```

The provisioning script creates persistent random root and application
credentials, starts the container, creates the private `camera-rental-return`
bucket, and attaches a bucket-scoped policy to a dedicated service account.
GitHub Actions only runs `verify.sh`; it never installs Docker, pulls the RustFS
image, rotates credentials, or changes the RustFS container.

## Public S3 Endpoint

Copy `nginx-storage.conf.example` into the server Nginx configuration after:

1. Creating `storage.motion-cover.com` DNS for `154.9.235.80`.
2. Issuing a TLS certificate for that hostname.
3. Testing and reloading Nginx.

Do not publish port `9001`; it is the private administration console. Do not
proxy the S3 API under a URL path prefix because that changes the signed request
path. Preserve the original `Host` header.

## Management File Configuration

Read `/opt/camera-rental/shared/rustfs-app.env` on the server and create a
private S3 file configuration in the management application:

- endpoint: `https://storage.motion-cover.com`
- domain: `https://storage.motion-cover.com/camera-rental-return`
- bucket: `camera-rental-return`
- region: `us-east-1`
- path-style access: enabled
- public access: disabled
- access key and secret: `RUSTFS_APP_ACCESS_KEY` and
  `RUSTFS_APP_SECRET_KEY`

Never enter `RUSTFS_ACCESS_KEY` or `RUSTFS_SECRET_KEY` into the application.

## Backup

Run `backup.sh` from a root-owned timer. It uses the application account and
retains local snapshots for 14 days. Production recovery still requires an
off-host copy; a backup on the same server does not protect against host loss.

RustFS is still an early-stage storage system. Keep the bucket private, monitor
health and backups, and do not treat it as the only copy of irreplaceable
customer evidence.
