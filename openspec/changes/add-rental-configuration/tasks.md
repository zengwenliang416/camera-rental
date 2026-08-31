## 1. Schema and identifier foundation

User outcome: 渠道订单、商品和 SKU 使用语义明确且不可互相回退的标识，管理员看到的
每个商品/SKU ID 都能追溯到权威同步来源。

- [ ] 1.1 Add an incremental migration for explicit XianGuanJia/Xianyu product and SKU fields, product rules, SKU mappings, remark history, preparation status, indexes, menu, and permissions.
- [ ] 1.2 Update order, product, and SKU DOs/Mappers and parsing snapshots to persist explicit identifiers without fallback.
- [ ] 1.3 Update product detail and SKU persistence to save shop item IDs and Xianyu SKU IDs with exact ownership checks.
- [ ] 1.4 Add migration and unit fixtures proving safe backfill of explicit identifiers and rejection of ambiguous legacy mappings.

## 2. Rental configuration backend

User outcome: 具备权限的管理员可以安全维护设备目录和店铺商品规则，并在提交危险变更
前看到影响范围，过滤规则不会跨店铺误命中。

- [ ] 2.1 Extend device catalog services with configuration-scoped create, update, enable, and disable operations.
- [ ] 2.2 Implement product-rule and child-SKU-mapping persistence, validation, impact preview, optimistic versioning, and audit behavior.
- [ ] 2.3 Implement configuration admin APIs with `rental:configuration:query` and `rental:configuration:update`.
- [ ] 2.4 Seed the 29 shop-specific skipped items only after unique shop resolution and test cross-shop isolation.

## 3. Immediate order creation and reconciliation

User outcome: 普通已付款渠道订单在详情落库后立即且只创建一次内部订单，即使备注、
型号或日期尚未补齐，也能持续自动重评直到具备排期条件。

- [ ] 3.1 Refactor order-detail persistence to evaluate exact skip rules before remark parsing and to create normal internal orders immediately.
- [ ] 3.2 Implement centralized idempotent reconciliation for identifiers, internal order creation, exact model mapping, valid remark plans, and preparation status.
- [ ] 3.3 Remove runtime use of ambiguous external product/SKU fields and remove shipment-time mapping creation as a competing source of truth.
- [ ] 3.4 Gate assignment and schedule creation through the authoritative preparation policy.

## 4. Remark history and fulfillment-safe updates

User outcome: 新备注和配置只更新允许变化的计划信息，续租、早退、改期和换机不会覆盖
已分配设备、实际出库、回仓、检测、退款或结算事实。

- [ ] 4.1 Persist every remark parse snapshot and retain the previous effective plan after invalid updates.
- [ ] 4.2 Implement change classification for extension, early return, reschedule, replacement, damage, loss, overdue, and logistics delay.
- [ ] 4.3 Implement transactional guards for unassigned, assigned, dispatched, returned, inspected, and settled orders.
- [ ] 4.4 Add focused unit and redteam tests for conflicts, early release prevention, replacement history, model mismatch, and immutable facts.

## 5. Historical reconciliation

User outcome: 历史未转换订单可以按可恢复批次安全补建，运营人员能够预览、暂停、恢复
并核对每批创建、跳过、冲突、失败和待复核数量。

- [ ] 5.1 Implement a bounded resumable backfill job with stable order-ID paging, per-batch transactions, counters, and safe failure records.
- [ ] 5.2 Backfill all eligible normal orders, mark only eligible configured skipped orders, and report fulfilled matches without reversal.
- [ ] 5.3 Add dry-run/impact output and operational documentation for production execution, monitoring, pause, resume, and rollback.

## 6. Rental configuration admin UI

User outcome: 管理员在一个中英文、明暗主题和窄屏可用的页面内完成设备目录、单型号/
多 SKU 精确映射及闲鱼备注规范配置。

- [ ] 6.1 Add typed configuration API clients and a route/page under Rental Operations.
- [ ] 6.2 Build the device catalog panel with create, edit, enable/disable, permission, loading, empty, and error states.
- [ ] 6.3 Build product-rule list/form flows for shop, item, handling policy, single model, synchronized multi-SKU mapping, impact preview, and reconciliation result.
- [ ] 6.4 Build the remark convention panel with approved templates, special-case help, and clipboard feedback.
- [ ] 6.5 Remove category/model quick-create controls from Rental Device while preserving catalog filtering and device creation.
- [ ] 6.6 Add complete `zh-CN` and `en` copy and verify light/dark desktop/narrow layouts.

## 7. Documentation and verification

User outcome: 上线人员获得可执行、可验证、可回滚的迁移与发布证据，在未获生产授权前
不会执行真实店铺写入、历史补建或部署。

- [ ] 7.1 Update Xianyu source, field-mapping, order-sync, domain, and database behavior documentation.
- [ ] 7.2 Run focused Maven tests and the rental module test suite with the project Maven/repository configuration.
- [ ] 7.3 Run admin model tests, `pnpm ts:check`, lint checks for touched files, and a production build.
- [ ] 7.4 Run SpecNav facticity, static, unit, redteam, E2E, and sensory verification with immutable evidence.
- [ ] 7.5 Prepare deployment, migration ordering, historical dry-run, monitoring, and rollback evidence without changing production until explicitly authorized.
