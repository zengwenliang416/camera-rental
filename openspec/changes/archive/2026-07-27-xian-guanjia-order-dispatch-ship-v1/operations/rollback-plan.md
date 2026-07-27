# Rollback Plan

## Triggers

- Shipment submit returns incorrect state transitions.
- Cross-tenant or unauthorized shop checks fail.
- Production operator accidentally enables writes without controlled shop
  approval.
- Migration causes startup or permission-menu failures.

## Rollback Command

```sql
DELETE FROM system_role_menu WHERE menu_id IN (7070, 7071);
DELETE FROM system_menu WHERE id IN (7070, 7071);
DROP TABLE IF EXISTS rental_device_shipment;
```

## Data Recovery

- Migration rollback is destructive for `rental_device_shipment`; export any
  shipment audit rows first if the environment has processed real shipments.
- If frontend deployment must roll back without dropping data, revert admin and
  staff artifacts first and keep the shipment table until audit retention is
  decided.

## Verification

- Confirm backend starts after rollback.
- Confirm admin and staff clients no longer route to shipment submit features.
- Confirm no pending real XianGuanJia shipment write is in progress before
  rollback.
