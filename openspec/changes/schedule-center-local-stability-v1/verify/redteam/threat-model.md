# Red-Team Threat Model

## Assets

- Tenant-scoped XianGuanJia AppKey and AppSecret.
- Customer receiver name, phone, address, and full channel order number.
- Device availability, schedule, assignment, waybill, and shipment state.
- Synchronization cursors, raw payload references, and historical order data.

## Trust Boundaries

- Browser to admin and schedule-center APIs.
- Tenant context and permission enforcement in backend controllers and services.
- Persisted application configuration to third-party client construction.
- Read synchronization versus explicitly enabled real write operations.
- OCR/manual draft state versus confirmed server mutation.

## Threats

- Cross-tenant application, shop, order, or payload access.
- Secret echo in APIs, logs, DOM, screenshots, or committed configuration.
- Enabling writes or jobs with incomplete credentials.
- Bypassing permissions or shipment prerequisites from frontend state.
- Duplicate shipment submission or stale asynchronous completion.
- Malformed URL, waybill, stored preference, or remote payload input.
- Destructive synchronization behavior after partial third-party failure.

## Safety Boundary

- Probes were non-destructive and used tests, static inspection, browser review, and read-only database queries.
- No real shipment, refund, inventory mutation, deployment, or production switch change was performed.
