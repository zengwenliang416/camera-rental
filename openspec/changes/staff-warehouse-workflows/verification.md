# 本地验证记录 — 2026-09-22

基线 94f8f4a4；实现分支 zwl/staff-warehouse-workflows。代码已分批提交并快进合入 main，生产部署与手机发布另行核验。
保留独立的管理端订单创建页改动和既有 outputs 文件。

## 后端

- 全 Maven reactor：BUILD SUCCESS。1578 项，112 跳过，1466 实际执行，0 失败/错误。
  日志 `/tmp/jzd-workflows-full-maven.log`。后续小改动由下述定向测试覆盖。
- 最终后端定向测试 82 项全部通过，含 4 项真实 MySQL 事务/服务测试：
  `/tmp/jzd-workflows-backend-ready.log`。
- MySQL 使用独立本地 jzd-workflow-mysql 容器及 jzd_workflow_test 数据库；
  覆盖收货与检测隔离、并发重复收货、发货保护记录跨回滚保留、盘点与跨租户拒绝。
  未连接生产数据库，也未执行真实渠道发货。
- 迁移执行器回归通过：`/tmp/jzd-workflows-migration-test.log`。
  新迁移为 20260922_060_staff_warehouse_workflows.sql，已登记迁移清单。

## 手机代码

7 组脚本通过：test-staff-workflow、test-mobile-workbench、test-shipment-result、
test-staff-theme、test-scanner、test-device-quantity、test-staff-update。
日志 `/tmp/jzd-workflows-regressions-final.log`。这些是本地回归，不替代光学扫码。

最终窄屏修订后，目标 ESLint、vue-tsc、App/H5/小程序构建均通过。
最终日志：`/tmp/jzd-ui-final-{lint,type,h5,app,mp}.log`。
其余改动页 ESLint：`/tmp/jzd-layout-final-lint.log`。
`git diff --check` 通过。
构建已有 carbon-warehouse 图标和 Browserslist 数据过期警告；未擅自更新依赖。

## 可见页面与 Computer Use

使用原生 Chrome Computer Use，页面来自实际 H5 构建，API 全部由本地模拟服务响应。
不使用客户手机号、地址或真实店铺订单作测试数据。截图保存到
`outputs/jiezuda-release-1.1.0/ui/`，属于模拟页面证据。

已观察今日作业切换、直接携带设备轮次打开检测、异常认领变为处理中、盘点创建与应盘列表。
检测页“登记维修异常”最初点击未跳转；移除 uni 别名后，已观察正确跳转到关联设备的异常表单。

Jev 决策与直接操作分别记录：地址设置和待检测切换有 verified:true；
今日作业导航达到页面但即时判据未通过，按 max_steps 后人工核验记录。
本地测试登录曾触发 Jev confirm，基于已有测试授权由 Codex 接管；未降低策略门槛。
后续页面按钮及截图由 Codex 直接操作，不冒充 Jev 成功率。

最终可见检查：320px 下首页三列、作业五个标签、检测两列按钮均完整显示，
无按钮文字相互覆盖。390px 首页同样检查通过；见 home-320.png、home-390.png、
tasks-320.png、inspection-320.png。检测页关联异常跳转已修复并可见核验。
独立 MySQL 测试容器已停止，保留数据以便复核；未停止其他用户容器。

## 交付边界

版本为 1.1.0 / 110。后端新增 API 和 7 张表必须先上线，再发布手机更新。
当前 adb 无连接设备；尚未安装新版或验证真机光学扫描、真实图片上传、弱网恢复。
发货结果 UNKNOWN 的多设备补录仍需运营核对，不把异常结单视为履约已修复。
候选预览每型号最多 100 台；单次盘点应盘最多 500 台。

## 安装包

最终签名 APK 已生成：`outputs/jiezuda-release-1.1.0/jiezuda-1.1.0.apk`。
解码 Android 清单核对包名 com.motioncover.rental.staff，版本 1.1.0 / 110。
apksigner 校验通过，证书 SHA-256 与原发布证书一致；APK 内 app-service.js
与最终本地 App 构建逐字节一致。APK SHA-256：
`21602a580f2c8e1972cda5556821df2a558858cac796e25c45afa0029700425b`。
此为待后端配套上线的本地产物，未上传下载页、未安装真机。

## 代码交付

用户于 2026-09-22 授权合入主分支并推送。
- `1dcb4663`：仓务履约、检测维修、盘点后端及迁移。
- `0e3340ba`：手机工作流、窄屏操作布局及版本 1.1.0。

main 快进合并，无冲突；管理端订单创建页的既有未提交改动保持原样。
`.woodpecker/deploy.yml` 配置 main 推送触发自动部署，CI 完成及线上版本需要独立核验。
安装包与截图保留在本地 outputs，不将 APK 或临时测试截图混入源码提交。
