# 系统架构

## 目标

相机及摄影器材租赁平台由一套后端业务服务统一管理以下客户端和业务域：

- PC 客户租赁官网
- Web 管理后台
- uni-app 客户端
- 员工移动端
- 闲鱼 / 闲管家订单渠道
- 商品、设备实例、仓库、排期、交付、检测和维修

所有客户端共享后端的库存、排期、金额和订单结果。客户端可以展示预检查结果，但不能把自己的计算结果作为权威数据。

## 仓库边界

| 仓库 | 职责 | 当前状态 |
|---|---|---|
| `camera-rental-server` | Spring Boot 后端、权限、领域规则、数据库、支付和第三方集成 | 已存在，`master-jdk17` |
| `camera-rental-admin` | Vue 3 + Element Plus Web 管理后台 | 已存在，`master` |
| `camera-rental-uniapp` | 客户商城、微信小程序、H5 和 App | 已存在，`master` |
| `camera-rental-staff` | 员工扫码、出库、回仓、检测和维修 | 已存在，`master` |
| `camera-rental-web` | Nuxt PC 客户官网和 SEO 页面 | 已创建，`main`，尚未形成稳定提交基线 |

当前后端已启用 `yudao-module-system`、`yudao-module-infra`、`yudao-module-rental`、`yudao-module-erp`。
ERP 负责采购/数量库存；出库回仓与设备实例状态以租赁模块为准（见 `docs/decisions/0002-rental-erp-inventory-ops.md`）。
会员、支付、商城、WMS 等仍按需启用。

## 逻辑架构

```text
PC 客户官网 ───────┐
uni-app 客户端 ────┤
员工移动端 ────────┤──> camera-rental-server
Web 管理后台 ──────┘       │
                           ├── 租赁领域模块
                           ├── 商品 / 会员 / 支付
                           ├── ERP / WMS / 仓储能力
                           ├── MySQL / Redis
                           └── 闲管家集成
```

## 后端模块边界

租赁业务计划放入新的 `yudao-module-rental`，而不是直接修改商城订单核心表。该模块应按芋道现有模块模式拆分 API 与 Biz：

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

供管理后台和员工端使用，必须执行后台权限和数据范围校验。

```text
/app-api/rental/**
```

供 PC 客户官网和 uni-app 客户端使用，不能暴露管理能力、第三方密钥或内部设备敏感信息。

## 数据流原则

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
