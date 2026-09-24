# API 约定

## 路径分层

```text
/admin-api/rental/**
```

管理后台、独立排期中心和员工端接口。

```text
/app-api/rental/**
```

PC 客户官网和 uni-app 客户端接口。

## 实现约束

- Controller、Service、DAL 分层，Controller 不直接访问 Mapper。
- 使用 Request VO、Response VO 和转换层，不能直接暴露数据库实体。
- 使用项目统一返回结构、分页对象、参数校验和权限注解。
- 管理接口权限不能只依赖前端菜单。
- 业务日期和金额按照 `docs/domain/` 规则处理。
- 破坏性变更必须先记录迁移范围、兼容窗口和回滚限制。
- 新增或修改 API 时同步检查管理后台、排期中心、客户 Web、uni-app 和员工端的受影响调用方。
- 对照方法/路径、权限/租户、字段类型、金额单位、日期区间、状态/错误码、分页和幂等语义；前端 DTO 不得只按字段名猜测。
- 认证、权限、文件等公共能力继续使用各自已有模块 API，不复制到租赁 Controller。

## 统一错误场景

涉及租赁业务的接口至少要能区分：

```text
参数校验失败
未登录 / 无权限
库存或设备不可用
排期冲突
订单状态不允许当前操作
支付状态异常
第三方授权失效
第三方限流或暂时不可用
需要人工复核
```

具体错误码必须复用项目现有错误码管理方式，不在前端硬编码后端内部异常信息。


## 员工订单日期与每日作业查询

- `GET /admin-api/rental/order/staff-page` 和 `staff-channel-page` 可选参数
  `orderDateStart`、`orderDateEnd`，格式 `YYYY-MM-DD`；同时不传表示全部日期。
  传入时必须成对、起止有序且在 2000–2100 年内。用户日期闭区间在查询中转换为
  上海业务时间的 `[开始日00:00, 结束日次日00:00)`。渠道 `order_time` / `source_created_at`
  优先；缺失时使用现有 UTC 审计 `create_time` 转上海时间，内部补建时间不覆盖已知渠道下单时间。
- `GET /admin-api/rental/staff/tasks` 的 `queue` 支持 SHIP_TODAY（今日）、SHIP_OVERDUE（逾期未发）、
  SHIP_ALL（全部到期未发）、RETURN（今日回仓计划）、OVERDUE（超期未回）、INSPECT、REPAIR。
  旧版 SHIP 保留全部到期的内部订单，不返回缺少 orderId 的渠道记录，兼容尚未升级的 APK。
  日期由服务端按 `Asia/Shanghai` 生成，分页与计数共享条件。
  发货队列纳入有发货日期的未转换渠道记录，返回 `channelOrderId`，此时 `orderId` 和
  `requiredQuantity` 可为空，客户端不能把渠道 ID 传入内部订单接口或将计价数量作为设备台数。
- 新增的型号、商品、准备状态/原因与设备台数均来自服务端；查询不会补建订单、修改日期、分配设备或发货。
