# 捷租达 Android 品牌与下载入口

页面品牌源图来自 `camera-rental-web/public/images/jiezuda-logo.png`，员工端本地副本为
`src/static/brand/jiezuda-logo.png`。手机桌面图标基于此标志设计，使用 `src/static/app/icons/`
中的多尺寸 PNG；默认头像为 `src/static/images/default-avatar.png`。`src/static/logo.svg`
内嵌 PNG，以保持图像一致性，并非可编辑的路径矢量。

应用名为“捷租达”，DCloud AppID 为 `__UNI__BB8C9BD`，Android 包名为
`com.motioncover.rental.staff`。AppID 与标题通过忽略提交的 `env/.env.production.local`
配置；`manifest.config.ts` 维护版本号。签名私钥、密码、打包配置不得提交。

使用自有证书和安心打包生成独立 APK。更新必须沿用同一包名与签名，提高 versionCode，
通过 `adb install -r` 覆盖安装；不得通过卸载或清除应用数据来更新。

扫码安装入口为 `https://rental.motion-cover.com/downloads/jiezuda/`。下载目录
`/opt/camera-rental/downloads/jiezuda/` 位于原子 Web 发布目录之外；Nginx 路由见
`ops/github-deploy/nginx.camera-rental.conf.example`。目录只放已核验的 APK、品牌图、
二维码、下载页和公开版本摘要，不放源码、签名文件、账号或构建配置。

更新流程：先验证 APK 包名、版本和签名，将新 APK 以版本化文件名上传；核对本地、服务器、
HTTPS 下载文件的 SHA-256 一致后更新下载页。旧版安装包保留以便回退，二维码地址保持不变。
发布静态下载文件不等于部署后端或完成真实扫码业务验收。
