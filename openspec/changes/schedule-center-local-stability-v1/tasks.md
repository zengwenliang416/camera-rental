# Tasks: schedule-center-local-stability-v1

- [x] `001-shipping-workbench`: authorized operators receive complete management-order customer fields, can review a waybill draft, search and select an available device, search pending-shipment orders by receiver name/full phone/order number, inspect all returned shipment fields, and submit only after every server-authoritative gate is satisfied.
- [x] `002-admin-xianyu-config`: authorized operators manage all tenant
  XianGuanJia business configuration from `camera-rental-admin`; credentials and
  operational settings persist on `xianyu_application`, AppSecret is encrypted
  and never returned, integration clients resolve persisted configuration
  dynamically, and real shipment remains denied before any side effect unless
  the persisted write switch is enabled.
- [x] `003-app-shell-dashboard`: an operator can use a responsive application shell
  with persisted theme and locale controls, safe synchronization feedback, and
  a task-oriented dashboard derived only from the current server snapshot.
- [x] `004-schedule-allocation`: an operator can inspect billable and occupied
  ranges, filter SN-level schedule lanes, and confirm a device assignment only
  after the server-authoritative allocation gates are satisfied.
- [x] `005-orders-devices`: an operator can filter rental orders and registered
  devices, inspect consistent identity, status, range, and lifecycle detail,
  and enter only the permitted existing assignment, shipping, or return flow.
- [x] `006-auth-exceptions-overlays`: a user can sign in with the existing
  management account, review and resolve permitted exceptions, and operate
  accessible dialogs, drawers, and QR surfaces without losing focus or context.
- [x] `007-state-decomposition-final`: an operator can move across every
  schedule-center route with independent session, permission, query, command,
  navigation, overlay, theme, and locale state while preserving existing
  backend-authoritative behavior.
