# Camera Rental 文档索引

本目录保存跨仓库架构、领域规则、接口约定和决策记录。

## 文档导航

- [系统架构](architecture.md)
- [租赁订单](domain/rental-order.md)
- [设备排期](domain/device-scheduling.md)
- [闲鱼 / 闲管家集成](domain/xianyu-integration.md)
- [闲管家官方接口来源](integrations/xianyu/source.md)
- [闲管家集成总览](integrations/xianyu/overview.md)
- [闲管家认证与签名](integrations/xianyu/authentication.md)
- [闲管家店铺授权](integrations/xianyu/shop-authorization.md)
- [闲管家商品同步](integrations/xianyu/product-sync.md)
- [闲管家订单同步](integrations/xianyu/order-sync.md)
- [闲管家售后同步](integrations/xianyu/after-sale-sync.md)
- [闲管家推送回调](integrations/xianyu/webhook.md)
- [闲管家字段映射](integrations/xianyu/field-mapping.md)
- [API 约定](api/README.md)
- [架构决策记录](decisions/0001-initial-platform-boundary.md)

## 文档规则

- 文档描述的是跨仓库事实和稳定业务规则，不替代代码、数据库迁移或在线第三方文档。
- 行为、接口或业务口径发生变化时，先更新对应文档，再修改受影响代码。
- 不能确认的字段、状态或外部接口行为必须标记为待核对，不得用猜测补齐。
- 真实凭据、客户隐私和生产数据不得写入文档。
