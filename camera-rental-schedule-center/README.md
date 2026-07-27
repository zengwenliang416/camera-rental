# 设备排期中心

独立 React/Vite 应用，复用相机租赁管理后台登录态、租户缓存、权限信息和 `/admin-api/rental/*` 接口。

生产部署路径建议为 `/admin/schedule-center/`，这样它与管理后台同源，可以直接复用 `ACCESS_TOKEN`、`REFRESH_TOKEN` 和租户缓存。

## Run Locally

**Prerequisites:** Node.js 或 Bun

1. 安装依赖：`bun install`
2. 按需复制 `.env.example` 为 `.env.local`
3. 启动：`bun run dev`

未登录时会展示独立登录页，但账号密码会调用管理端 `/admin-api/system/auth/login`，并写入与管理后台一致的 `ACCESS_TOKEN`、`REFRESH_TOKEN`、`tenantId` 和 `user` 缓存。也可以先在 `camera-rental-admin` 登录，再直接打开本应用免登进入。

## SSO / 权限

- 认证：同源 localStorage 互通，独立应用不维护另一套账号体系。
- 权限：启动时调用 `/admin-api/system/auth/get-permission-info`，前端按权限隐藏或禁用功能。
- 后端：所有写操作仍必须由管理端接口的后端权限注解和业务校验兜底。
- 入口权限：建议给角色分配 `rental:schedule-center:access`，再按需分配 `rental:device:*`、`rental:schedule:*`、`rental:xianyu:*`、`rental:review:*`。
