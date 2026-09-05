# Server — 后端规则

本目录是统一仓库中的后端应用，基于 ruoyi-vue-pro、Java 17、Spring Boot 3 和 Maven。
租赁模块 `yudao-module-rental` 已存在并拆为 `api` / `biz`，不得按旧说明重新创建。

## 按需读取

- 先应用[根规则](../AGENTS.md)，读取 README 相关章节、根/目标模块 POM、
  `yudao-server/pom.xml` 及附近源码/测试。模块是否启用以装配 POM 为准。
- 业务修改读[工程规范的后端章节](../docs/engineering/standards.md#2-后端职责与依赖)；
  跨模块/API 修改再读[系统架构](../docs/architecture.md)、[API 约定](../docs/api/README.md)。
- 涉及闲管家时读[集成规则](../docs/engineering/xianyu.md)及其官方文档入口；
  修改排期/金额时按根规则加载对应领域文档，不预读全部业务材料。

## 实现与复用

- 租赁实现位于 `yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/`。
  沿用 `controller`、`service`、`dal`、`convert`、`enums`、`integration/xianyu`、`job`。
- Controller 负责协议入口，Service 负责用例和事务，Mapper 负责数据；
  VO/DTO/DO 显式转换，复用统一响应、分页、错误码、权限和租户机制。
- 先查同业务服务及现有 policy/guard，跨入口调用同一规则；集成客户端统一承担外部协议。
  同步、重评、补建复用 reconciliation；设备锁、分配和履约保护不得另写旁路。
- 优先复用 system/infra/ERP 等已装配模块公开能力；新增依赖先核对 POM。
  不跨模块直接改表，不让租赁状态混入商城核心订单表，不引入依赖环。
- 金额/排期/权限/租户条件必须在 Service 与数据库层保持一致。
  写路径明确事务、锁顺序、幂等和失败补偿；SQL 参数绑定，不拼接不可信排序/条件。
- 状态变化同步核对相关分配、排期、缓存、读模型与审计；避免循环单条查询造成 N+1。

## 验证

在本目录执行 `mvn -pl yudao-module-rental/yudao-module-rental-biz -am test`，
如存在 Maven Wrapper 优先使用。小改动可先筛选相关测试，确认 Biz 的实际测试数；
业务修改至少执行相关测试或编译，并按[验证规则](../docs/engineering/validation.md)
补充接口、事务、并发等行为证据。编译和 Mock 单测不代替数据库并发验证。
