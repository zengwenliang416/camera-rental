# 系统架构

## 目标

相机及摄影器材租赁平台由一套后端业务服务统一管理以下客户端和业务域：

- PC 客户租赁官网
- Web 管理后台
- 独立 React 排期与履约工作台
- uni-app 客户端
- 员工移动端
- 闲鱼 / 闲管家订单渠道
- 商品、设备实例、仓库、排期、交付、检测和维修

所有客户端共享后端的库存、排期、金额和订单结果。客户端可以展示预检查结果，但不能把自己的计算结果作为权威数据。

## 应用边界

当前为一个 Git 仓库下的六个应用目录，不使用 Git submodule。
各应用保留独立构建与依赖边界；不使用上游分支名称描述当前 checkout。

| 应用 | 职责 | 实现入口 |
|---|---|---|
| `camera-rental-server` | Spring Boot 后端、权限、领域规则、数据库、支付集成边界和第三方集成 | [后端规则](../camera-rental-server/AGENTS.md) |
| `camera-rental-admin` | Vue 3 + Element Plus Web 管理后台 | [管理端规则](../camera-rental-admin/AGENTS.md) |
| `camera-rental-schedule-center` | React 排期与履约工作台，复用管理端认证、租户、权限和后端 API | [排期中心规则](../camera-rental-schedule-center/AGENTS.md) |
| `camera-rental-uniapp` | 客户商城、微信小程序、H5 和 App | [客户端规则](../camera-rental-uniapp/AGENTS.md) |
| `camera-rental-staff` | 员工扫码、出库、回仓、检测和维修 | [员工端规则](../camera-rental-staff/AGENTS.md) |
| `camera-rental-web` | Nuxt PC 客户官网、客户归还入口和 SEO 页面 | [官网规则](../camera-rental-web/AGENTS.md) |

2026-09-05 核对根 POM 与 `yudao-server/pom.xml`：装配了
system、infra、rental-biz、ERP 与 AI 模块。该记录是源码装配快照，不是运行/部署证明；
后续任务仍须读取当前 POM。
ERP 负责采购/数量库存；出库回仓与设备实例状态以租赁模块为准（见[ERP 库存边界决策](decisions/0002-rental-erp-inventory-ops.md)）。
会员、支付、商城、WMS 等仍按需启用。

## 逻辑架构

```text
PC 客户官网 ───────┐
uni-app 客户端 ────┤
员工移动端 ────────┤──> camera-rental-server
Web 管理后台 ──────┤        │
React 排期中心 ────┘        │
                           ├── 租赁领域模块
                           ├── 商品 / 会员 / 支付
                           ├── ERP / WMS / 仓储能力
                           ├── MySQL / Redis
                           └── 闲管家集成
```

## 后端模块边界

租赁业务已放入 `yudao-module-rental`，按芋道模块模式拆分 API 与 Biz。
继续演进现有服务，不直接改造商城订单核心表为租赁排期表：

```text
yudao-module-rental/
├── yudao-module-rental-api/
└── yudao-module-rental-biz/
```

建议的业务边界：

- 商品 SKU 与租赁配置
- 设备实例与设备状态
- 套装及套装明细
- 租赁订单、订单明细和渠道映射
- 设备占用、分配和排期冲突
- 发货、签收、发回、回仓
- 归还检测、维修和损坏
- 押金、退款和赔偿
- 闲管家订单同步、推送和售后同步

## API 分层

```text
/admin-api/rental/**
```

供管理后台、排期中心和员工端使用，必须执行后台权限、租户和数据范围校验。

```text
/app-api/rental/**
```

供 PC 客户官网和 uni-app 客户端使用，不能暴露管理能力、第三方密钥或内部设备敏感信息。

认证、权限、文件等公共能力仍调用相应 system/infra 等已有模块 API；
以上路径是租赁领域边界，不限制公共能力复用。

## 依赖与复用边界

- 后端用例由 Service 统一编排，DAL 维护持久化，外部协议封装在 Integration；
  API/VO 与 DO 分离，跨模块通过公开契约协作，不形成循环依赖。
- 前端保持页面、功能过程、API/模型和公共 UI 的职责分离；公共层不反向依赖业务页面。
- Vue、React 和 uni-app 共享后端契约与领域语义，组件按各端技术栈实现；
  不通过导入相邻应用内部源码共享认证或 UI，也不默认引入跨应用共享包。
- 批处理、推送、人工重试和配置重评应调用同一业务入口；
  计划重评不得覆盖已发生的履约事实，写操作与读模型同步维护。

详细选择规则见[工程规范](engineering/standards.md)。本节定义目标依赖约束，
不宣称所有历史代码已完成分层整改；不因此扩大当前任务的重构范围。

## 数据流原则（约束与目标，不代表全流程均已验收）

1. 客户端提交租期和商品意向。
2. 后端返回可用性、租金和订单校验结果。
3. 后端在事务内重新检查 SKU 容量并创建有期限的数量预留。
4. 支付成功后确认预留，员工拣货前再绑定具体设备并复查实例级排期冲突。
5. 支付、发货、归还、检测和退款通过状态变化驱动后续流程。
6. 闲管家原始数据和标准化数据同时保存，支持幂等同步和重新解析。

## 当前不做的事情

- 不在多个前端复制一套订单或排期计算。
- 不用 SKU 数量替代具体设备实例。
- 不在本阶段引入微服务拆分。
- 不在没有确认在线接口文档的情况下实现闲管家写操作。
