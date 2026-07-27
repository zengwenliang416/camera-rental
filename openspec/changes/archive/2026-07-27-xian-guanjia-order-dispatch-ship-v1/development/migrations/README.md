# Migration Evidence

## Execution Order

- Apply `camera-rental-server/sql/mysql/migrations/20260726_023_xianyu_ship_workflow.sql` once per target database.
- The SpecNav evidence copy is `openspec/changes/xian-guanjia-order-dispatch-ship-v1/development/migrations/20260726_023_xianyu_ship_workflow.sql`.
- SHA-256 verified on 2026-07-27: `c7bf4b14a36f2f089adf498ef1277bbb8bb8933f8bb62c3ccd4a171c64b43f3a`.

## Validation

- `git diff --check` passed.
- Focused backend tests passed with `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuOrderShipServiceTest,ShipmentOcrServiceTest,OpenAiCompatibleShipmentOcrClientTest -Dsurefire.failIfNoSpecifiedTests=false test`.

## Rollback

Rollback must be manual and data-aware:

```sql
DELETE FROM system_role_menu WHERE menu_id IN (7070, 7071);
DELETE FROM system_menu WHERE id IN (7070, 7071);
DROP TABLE IF EXISTS rental_device_shipment;
```

Do not drop `rental_device_shipment` in an environment that already uses the workflow unless shipment audit evidence has been exported or intentionally discarded.
