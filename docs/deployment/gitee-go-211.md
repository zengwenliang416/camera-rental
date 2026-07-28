# Gitee Go 部署到 211.101.246.160

本部署方案以 Gitee `main` 为发布源，通过 Gitee Go 主机组在生产服务器上拉取
指定提交、构建并原子切换发布目录。GitHub Actions 仅保留手动应急发布，不再
响应 `main` 分支推送。

## 仓库和密钥

- Gitee 仓库：`git@gitee.com:wenliang_zeng/camera-rental.git`
- 本机推送密钥：`~/.ssh/camera_rental_gitee`
- 生产服务器只读拉取密钥：`~/.ssh/camera_rental_gitee_pull`
- 生产部署目录：`/opt/camera-rental`

私钥、数据库配置、闲管家凭据和其他生产密钥不得进入 Git。

## 主机组

在 Gitee `设置 -> 主机管理` 创建生产主机组，并按页面命令在
`211.101.246.160` 安装主机代理。流水线使用 Gitee 生成的真实
`hostGroupID` 和 `hostID`，不要手工猜测。

主机代理账户必须有以下权限：

- 读取 Gitee 私有仓库；
- 写入 `/opt/camera-rental`；
- 构建 Java、Node、pnpm 和 Bun 项目；
- 重启 `camera-rental-server.service` 和 `camera-rental-web.service`；
- 执行 `nginx -t` 并重新加载 Nginx。

## 流水线

流水线配置保存在 `.workflow/deploy-production.yml`，首轮仅允许手动触发。
主机步骤使用 `shell@agent`，目标主机组标识为
`camera-rental-production-211`。

主机步骤应执行：

```bash
export REPO_URL=git@gitee.com:wenliang_zeng/camera-rental.git
export RELEASE_SHA="${GITEE_COMMIT:?missing GITEE_COMMIT}"
bash ops/github-deploy/server-build-deploy.sh
```

`GITEE_COMMIT` 是 Gitee Go 当前提供的流水线提交 ID 内置变量。

## 发布前检查

1. 确认本地测试和构建通过。
2. 确认新增 SQL 已备份并按顺序执行。
3. 确认生产服务器拉取公钥已添加到 Gitee 仓库部署公钥。
4. 首次发布先手动运行流水线，不直接依赖自动触发。
5. 发布后检查后端服务、管理端、排期中心和 Nginx。

数据库迁移仍不在流水线中自动执行，避免未经备份直接修改生产数据。
