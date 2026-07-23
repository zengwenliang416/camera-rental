# Camera Rental Server Instructions

本仓库是相机租赁平台后端，当前基于 ruoyi-vue-pro `master-jdk17`。

开始任务前：

1. 阅读父目录 `../AGENTS.md` 和 `../docs/` 中与任务相关的文档。
2. 阅读本仓库 README、根 `pom.xml`、目标模块的 `pom.xml` 和现有代码模式。
3. 租赁业务计划放入 `yudao-module-rental`，当前该模块尚未创建。
4. 遵循现有 Controller、Service、DAL、VO、Convert、权限和事务模式。
5. 不要把租赁排期直接写入商城订单核心表。
6. 涉及闲管家时，先读取 `../docs/integrations/xianyu/source.md` 和在线官方接口文档。
7. 修改完成后运行真实存在的模块测试或编译命令。

不得提交真实密钥、生产凭据、客户隐私或未经确认的第三方接口参数。
