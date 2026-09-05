# Camera Rental Platform

相机及摄影器材租赁平台的独立多项目仓库。

本仓库是从本地工作区导出的独立 Git 快照，不使用 Git submodule，也不依赖
原始上游仓库的 Git 历史。各目录保留自己的应用边界和原有包管理器。

## 项目结构

```text
camera-rental/
├── camera-rental-server/     # Java 后端
├── camera-rental-admin/      # Web 管理后台
├── camera-rental-schedule-center/ # React 排期与履约工作台
├── camera-rental-uniapp/     # 客户端
├── camera-rental-staff/      # 员工移动端
├── camera-rental-web/        # PC 客户官网
├── docs/                     # 跨项目架构与业务文档
└── AGENTS.md                 # Codex 协作规则
```

## 独立仓库规则

- 六个应用目录是普通源码目录，不是 Git 子模块；当前分支和远程以 Git 实时状态为准。
- 修改前阅读根目录和目标目录中的 `AGENTS.md`。
- 上游仓库只作为代码来源，不作为本仓库的远程依赖。
- 凭据、环境文件、依赖目录和构建产物不进入版本库。
- 服务端业务规则是库存、排期和金额的权威来源。

## 开发入口

请先阅读：

- [工作区规则](AGENTS.md)
- [跨项目文档](docs/README.md)
- [闲管家集成总览](docs/integrations/xianyu/overview.md)

各应用的具体命令以对应目录中的 `README.md`、锁文件和 `package.json` /
`pom.xml` 为准。
