# Changelog: xian-guanjia-order-dispatch-ship-v1

## Changed

- Added backend shipment workflow for pending XianGuanJia channel orders with
  idempotency, tenant/shop/device validation, local shipment audit rows, and
  state updates only after channel success.
- Added admin shipment workbench support for waybill/device capture, candidate
  order search, manual confirmation, and write-disabled feedback.
- Added staff H5/mobile shipment screen and shared API types for scanning,
  searching, and submitting shipment.
- Added migration `20260726_023_xianyu_ship_workflow` for shipment audit storage
  and menu permissions.
