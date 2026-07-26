# GitHub Actions 部署到 211.101.246.160

本部署方案把 GitHub `main` 作为发布源，Actions 构建产物后通过 SSH 上传到
`211.101.246.160`，公网域名使用 `rental.motion-cover.com`。生产密钥和数据库配置不进入 Git。

## 自动部署范围

- `camera-rental-server`：构建 `yudao-server.jar`。
- `camera-rental-admin`：构建管理后台静态文件。
- `camera-rental-staff`：构建员工端 H5 静态文件。
- `camera-rental-web`：构建 Nuxt PC Web `.output`。

`camera-rental-uniapp` 当前没有可靠的 H5/小程序构建脚本纳入流水线；微信小程序
和 App 发布还需要单独接入平台上传、审核和证书流程。

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

## 发布方式

- 手动：GitHub Actions 页面运行 `Deploy camera rental to 211`。
- 自动：推送 `main` 且改动命中后端、admin、staff、web 或部署脚本时触发。

## 生产注意事项

- 数据库迁移不在流水线中自动执行；每次有 `sql/mysql/migrations` 变更时，先备份再人工执行。
- 闲管家写操作、AppSecret、NewAPI Key、数据库密码必须只放在 GitHub Secrets 或服务器配置。
- 首次部署前先在服务器上确认 Java 21、Node、Nginx、MySQL、Redis 和端口策略。
- 如果需要 HTTPS，建议先配置域名和证书，再把 Nginx 示例改成 443。
