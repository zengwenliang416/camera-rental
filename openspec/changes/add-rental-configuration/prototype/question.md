# Prototype Question: add-rental-configuration

## Question

在现有 Camera Rental Admin 壳层内，三页签“租赁配置”是否能让管理员清楚完成：

- 将设备大类和型号维护从“租赁设备”迁移到“设备目录”；
- 按“店铺 + 闲鱼商品 ID”配置单型号商品；
- 按“店铺 + 闲鱼商品 ID + 已同步闲管家 SKU”配置多型号商品；
- 明确看到四种渠道标识，且多型号缺少 SKU 映射时不回退商品级默认型号；
- 在危险变更前预览受影响订单；
- 直接复制简洁、统一的闲鱼卖家备注格式。

## Branch

`ui-html`

## Review Target

- Entry: `artifact/index.html`
- Variant: `admin-three-tab-precise-mapping-v1`
- Required reviewer decision: 页面结构、精确映射交互、影响预览和备注规范是否可进入
  Development。

## Out of Scope

- Production implementation.
- Database writes.
- Deployment behavior.
- 真实店铺、商品、SKU、订单或客户数据。
- 真实 API 调用和历史订单重评执行。
