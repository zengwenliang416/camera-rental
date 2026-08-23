# Prototype Question: add-xianyu-dispatch-backfill

## Question

在现有 Camera Rental Web 管理后台壳层中，“已发货订单补录出库设备”是否能让无扫码硬件的店铺管理员清楚理解这是本地历史补录，并在桌面/窄屏、light/dark、`zh-CN`/`en` 下安全完成输入、校验、冲突处理和成功确认？

## Branch

`ui-html`

## Review Target

- Entry: `artifact/index.html`
- Required reviewer decision: 批准或拒绝 `admin-dialog-v1` 作为开发与 Verification 2.0 的视觉/交互基线。
- Review states: default, validation, loading, conflict, success, permission.
- Review matrix: desktop/narrow × light/dark × `zh-CN`/`en`.

## Out of Scope

- Production implementation.
- Database writes.
- Deployment behavior.
- Real Xianyu, device, shipment, or logistics API calls.
- Physical scanner, camera, OCR, staff app, and customer surfaces.
