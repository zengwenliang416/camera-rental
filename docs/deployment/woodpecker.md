# Woodpecker CI 部署到生产服务器

GitHub 继续作为代码源。Woodpecker 通过 GitHub OAuth 和 Webhook 接收事件，
由生产服务器上的单并发 Docker Agent 执行流水线，再通过受限 SSH 凭据调用
现有增量构建和原子发布脚本。

## 组件

- Woodpecker Server：`https://ci.motion-cover.com`。
- Woodpecker Agent：仅接受 `zengwenliang416/camera-rental`。
- 流水线：`.woodpecker/deploy.yml`。
- 生产部署根目录：通过仓库 Secret `deploy_root` 提供。
- 发布实现：继续复用 `ops/github-deploy/server-build-deploy.sh`。

## GitHub OAuth

在 GitHub `Settings -> Developer settings -> OAuth Apps` 创建 OAuth App：

- Application name：`MotionCover Woodpecker CI`
- Homepage URL：`https://ci.motion-cover.com`
- Authorization callback URL：`https://ci.motion-cover.com/authorize`

Client ID 和 Client Secret 只写入服务器
`/opt/woodpecker/.env`，不得提交到 Git。

## 服务器安装

```bash
install -d -m 0700 /opt/woodpecker
cp ops/woodpecker/docker-compose.yml /opt/woodpecker/docker-compose.yml
cp ops/woodpecker/.env.example /opt/woodpecker/.env
chmod 600 /opt/woodpecker/.env
mkdir -p /opt/woodpecker/data /opt/woodpecker/agent
chown -R 1000:1000 /opt/woodpecker/data
```

分别生成 Agent 认证密钥和 gRPC JWT 签名密钥：

```bash
openssl rand -hex 32
openssl rand -hex 32
```

完成 `.env` 后启动：

```bash
cd /opt/woodpecker
docker compose config
docker compose up -d
docker compose ps
```

Nginx 使用 `ops/woodpecker/nginx.conf.example`。证书和 DNS 配置完成后，
`https://ci.motion-cover.com/healthz` 必须可访问。

## 仓库 Secrets

在 Woodpecker 仓库设置中添加：

| Secret | 用途 |
| --- | --- |
| `deploy_host` | 生产服务器地址 |
| `deploy_port` | SSH 端口 |
| `deploy_user` | 受限部署用户 |
| `deploy_root` | 生产发布目录 |
| `deploy_ssh_private_key` | 专用部署私钥 |
| `deploy_ssh_known_hosts` | 固定 known_hosts |

Secrets 只允许 `push` 和 `manual` 事件，不允许 `pull_request` 使用。

## 首次启用

1. 使用 GitHub 登录 Woodpecker。
2. 激活 `zengwenliang416/camera-rental` 仓库。
3. 添加仓库 Secrets。
4. 首次仅手动运行并检查部署日志、生产 SHA、服务状态及前端资源。
5. 手动部署验证后，保留 `main` push 自动触发。

Agent 挂载 Docker Socket，等价于宿主机高权限。必须保持关闭注册、
固定管理员、固定仓库所有者、单仓库 Agent 标签和单并发，不得开放给
其他不受信任仓库。
