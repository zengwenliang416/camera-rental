# GitHub Actions 部署到 211.101.246.160

本部署方案把 GitHub `main` 作为发布源。Actions 仅通过 SSH 下发小型部署脚本，
由 `211.101.246.160` 拉取指定 Git SHA，并复用服务器上的 Maven、pnpm、Bun
依赖和上一版构建产物完成增量发布。公网域名使用 `rental.motion-cover.com`，
生产密钥和数据库配置不进入 Git。

## 发布范围

- `camera-rental-server`：后端 JAR。
- `camera-rental-admin`：管理后台。
- `camera-rental-schedule-center`：设备排期中心。
- `camera-rental-staff`：员工端 H5。
- `camera-rental-web`：Nuxt PC Web。

`camera-rental-uniapp` 当前没有可靠的 H5/小程序构建脚本纳入流水线；微信小程序
和 App 发布还需要单独接入平台上传、审核和证书流程。

服务器会对比当前生产 SHA 与目标 SHA，只重建源代码发生变化的组件。依赖清单
未变化时复用现有 `node_modules`，Maven 复用 `/root/.m2/repository`。未变化
组件直接复制当前生产产物，不重新构建。

## 国内 GitHub 加速

211 服务器位于国内，直连 GitHub 可能超时。流水线按顺序探测：

1. `gh-proxy.com` Git 镜像。
2. `ghfast.top` Git 镜像。
3. Gitee 仓库镜像。

候选源的 `main` 必须精确等于本次 `${{ github.sha }}` 才会使用，避免镜像延迟
导致发布旧代码。所有候选源均没有目标 SHA 时，发布直接失败。

## GitHub Secrets

在 GitHub 仓库 `Settings -> Secrets and variables -> Actions` 添加：

| Secret | 必填 | 说明 |
| --- | --- | --- |
| `DEPLOY_SSH_PRIVATE_KEY` | 是 | 可登录服务器的私钥 |
| `DEPLOY_HOST` | 否 | 默认 `211.101.246.160` |
| `DEPLOY_PORT` | 否 | 默认 `22` |
| `DEPLOY_USER` | 否 | 默认 `root` |
| `DEPLOY_ROOT` | 否 | 默认 `/opt/camera-rental` |
| `DEPLOY_SSH_KNOWN_HOSTS` | 建议 | 服务器 known_hosts，未配置时 Actions 会 ssh-keyscan |

当前已创建专用部署 key，本机私钥位置：

```text
~/.ssh/camera_rental_actions_211
```

对应公钥需要加入服务器 `root` 用户：

```text
ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAILVxM555NweJzQ6vuAvj37q2BeKDnGP3F7QIqQl5YmUH camera-rental-actions@github
```

服务器执行示例：

```bash
mkdir -p /root/.ssh
chmod 700 /root/.ssh
printf '%s\n' 'ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAILVxM555NweJzQ6vuAvj37q2BeKDnGP3F7QIqQl5YmUH camera-rental-actions@github' >> /root/.ssh/authorized_keys
chmod 600 /root/.ssh/authorized_keys
```

## 服务器一次性准备

以下命令只做示例，实际生产前先确认域名、数据库、Redis 和 TLS：

```bash
mkdir -p /opt/camera-rental/shared /opt/camera-rental/releases
cp ops/github-deploy/backend.env.example /opt/camera-rental/shared/backend.env
cp ops/github-deploy/web.env.example /opt/camera-rental/shared/web.env
```

将真实后端配置写入：

```text
/opt/camera-rental/shared/application-prod.yaml
```

该文件应包含生产 MySQL、Redis、租户、文件存储、闲管家、OCR/NewAPI 等配置。
不要提交到 Git。

安装 systemd 服务：

```bash
cp ops/github-deploy/camera-rental-server.service.example /etc/systemd/system/camera-rental-server.service
cp ops/github-deploy/camera-rental-web.service.example /etc/systemd/system/camera-rental-web.service
systemctl daemon-reload
systemctl enable camera-rental-server.service camera-rental-web.service
```

安装 Nginx 示例：

```bash
cp ops/github-deploy/nginx.camera-rental.conf.example /etc/nginx/conf.d/camera-rental.conf
nginx -t && systemctl reload nginx
```

RustFS 也必须在服务器上单独预安装，流水线只检查其健康状态，不负责安装 Docker、
拉取镜像或生成凭据：

```bash
cd /opt/camera-rental/source
bash ops/rustfs/provision.sh /opt/camera-rental/rustfs /opt/camera-rental
```

## 发布方式

- 手动：GitHub Actions 页面运行 `Deploy camera rental to 211`。
- 自动：向 GitHub `main` 推送后端、管理后台、排期中心、员工端、PC Web 或
  部署脚本变更时，自动运行 `Deploy camera rental to 211`。
- `ops/rustfs/**` 变更不会触发应用发布；RustFS 由服务器运维独立安装和升级。
- Actions 会根据服务器现有源码 HEAD 生成并上传经过 Git 校验的增量 bundle；
  生产机构建不依赖访问 GitHub、GitHub 代理或 Gitee。
- 纯文档、SpecNav 状态等非发布文件变化不会重启生产服务。
- 启用 GitHub 自动发布后，应在 Gitee 页面关闭自动触发，避免两套流水线并发
  操作同一生产环境。

旧版 Actions 在 GitHub Runner 构建后上传完整发布包。历史运行中，约 166 MB
产物从 GitHub 美国 Runner 上传到 211 曾耗时约 10 分钟，最慢超过 1 小时 46
分钟，而服务器解包、切换和重启约 3 秒。当前方案不再上传完整构建产物。

## 生产注意事项

- `ops/github-deploy/migrations.txt` 中登记的数据库迁移会在版本激活前自动执行；
  迁移失败或历史迁移校验和变化时发布会停止，线上仍保留上一版本。
- 闲管家写操作、AppSecret、NewAPI Key、数据库密码必须只放在 GitHub Secrets 或服务器配置。
- 首次部署前先在服务器上确认 Java 21、Node、Nginx、MySQL、Redis 和端口策略。
- 如果需要 HTTPS，建议先配置域名和证书，再把 Nginx 示例改成 443。
