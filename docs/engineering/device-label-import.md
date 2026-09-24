# 设备 TXT 批量导入与二维码打印

入口：Vue 管理后台「租赁设备 → 批量导入与打印」；也可勾选当前页设备后直接补打。沿用现有 Element Plus、请求封装、租户和权限。

## 操作与文件

每批最多20个TXT文件、每文件256KiB、合计200条设备记录。每文件选择一个现有启用的大类和型号；文件名可提供唯一匹配的预选，提交仍校验目录。未知型号先在租赁配置维护。

UTF-8优先，兼容GB18030、带BOM的UTF-16。两列为设备编号、序列号，支持逗号、Tab或空格；也支持中文表头或 `device_no` / `serial_number`。无表头单列的含义由用户选择，默认序列号，不按内容猜测；所有值保留文本及前导零。坏行和无法确定的列不能静默跳过。

- 补打：只匹配已有启用设备，不改变台账。
- 新设备入库：预览显示已匹配、待新增、重复、冲突、未找到。只有全批无冲突时才能确认；重复行不重复入库。
- 同编号不同序列号、同序列号不同编号或型号、逻辑删除/停用设备均阻断。不覆盖现有设备数据。
- 新设备自动编号复用目录编号分配器。跳过历史已占用/逻辑删除的编号；无可用编号时整个事务回滚。
- 新设备提交的批次行锁、设备唯一键、事务回执防止重复提交。网络不确定时可重试同一批次。
- 入库与打印独立；打印失败或再次下载不创建新设备。

## 新增接口

统一前缀 `/admin-api/rental/device`，现有接口保持不变。

| POST路径 | 权限 | 请求 / 返回 |
|---|---|---|
| `/import/preview` | `rental:device:query` | `{mode: REPRINT或CREATE, rows:[{fileName,lineNumber,categoryCode,equipmentModelCode,deviceNo,serialNumber}]}`；返回 `batchId, rows, canSubmit` |
| `/import/commit?batchId=…` | query + create | 返回去重后的设备ID；只允许原租户、原用户，预览24小时有效；成功回执可重试读取 |
| `/labels/preview` | query | `{deviceIds,shopName,phone,locale}`；返回第一台的PNG data URL和设备编号 |
| `/labels/download` | query | 同上；返回 `application/zip`，只导出当前租户可见的启用设备 |

预览是非权威快照，提交时重新校验；台账变化返回明确错误，用户重新预览。输入/响应日志关闭，签名不进入普通日志或CSV，密钥只在现有后端签名器中使用。清单包含设备编号和序列号，仅供有管理查询权限的用户下载。

新增 `rental_device_import_batch` 存储原用户的导入请求、预览和成功设备ID，沿用租户、审计、逻辑删除。没有修改已执行迁移。上线前应用 `20260923_061_rental_device_import_batch.sql`；本地开发不代表已执行线上迁移。

## 图片合同

权威参考：`.codex/skills/rental-device-label-qr/references/layout-contract.md` 和 `scripts/generate_labels.py`。

- 单张PNG：945×472像素、600DPI（pHYs=23622像素/米），白底，约40×20mm。
- 左侧520像素，店名、业务电话、设备编号及序列号末四位分三行居中；文字适配字号，不截断。无序列号仅显示设备编号；非空序列号必须至少含4个字母或数字。
- 右侧410×410像素二维码，位置(527,31)，M级纠错，四模块静区，使用现有CRD1后端签名，不接收上传文件中的二维码载荷。
- A4：2480×3508像素对应300DPI，4列×13行；标签472×236像素，水平/垂直间隙118/31像素，与原脚本一致。PDF按原尺寸打印。
- PNG预览总表：每行最多4张，472×236缩略图，36像素外边距、24像素间距、150DPI。
- ZIP按大类和型号分组：`PNG-40x20mm-600DPI/`、A4 PDF、PNG预览；根目录含全部标签PDF、设备清单、验证报告和打印说明。英文界面对应英文说明/文件名。
- Java服务端实现使用ZXing和PDFBox，不依赖用户电脑运行Python Skill；几何和打印参数对齐该Skill，字体与缩放实现不保证逐像素相同。
- 服务端逐张回读固定二维码区域（避免文字被Java解码器误当作定位图案）；自动化测试检查编码后的PNG元数据和PDF中二维码区域扫码。软件验证不等于实体扫码，报告始终保留 `physical_scan_tested=false`。

## 运行配置

复用 `rental.device.qr-secret`；未配置签名时禁止导出。新增可选 `rental.device.label-font-family`（默认SansSerif）。服务端必须安装能显示店名文字的中文字体，例如Noto Sans CJK，必要时指定该字体族；缺字或文字无法容纳时明确失败，不生成方框字或截断标签。无生产密钥或真实联系电话默认值。

新增PDFBox 3.0.7依赖，复用现有ZXing；前端无新增依赖。导出上限200台，前端下载超时120秒。

## 本地验证

- 前端：`node --test --experimental-strip-types tests/deviceImportModel.test.ts`、`pnpm ts:check`、目标文件ESLint、`pnpm build:local`。
- 后端：`mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=RentalDeviceImportServiceTest,RentalDeviceImportTransactionTest,RentalDeviceLabelServiceTest,RentalDeviceAdminServiceTest,RentalDeviceCatalogServiceTest,RentalDeviceQrCodecTest -Dsurefire.failIfNoSpecifiedTests=false test`。
- `RentalDeviceImportTransactionTest` 使用H2真实事务代理和租户拦截器检查批次并发重试、回滚、租户/用户隔离、跳过历史编号；不等同于MySQL生产并发验收。
- 标签测试仅使用合成设备和测试签名，不调用生产服务。

## 2026-09-24 本地验证记录

- 前端解析及既有设备模型回归：14/14通过；类型检查、目标文件ESLint、生产构建通过。构建仍提示既有IM模块混合静态/动态导入及包体积告警。
- 后端相关测试：44/44通过，包含4个H2事务/并发测试；标签用例覆盖53张跨两页A4及中文字体可用/不可用分支。
- 浏览器采用实际Vue弹窗组件、隔离的合成API响应；检查中文/英文、浅色/暗色、桌面及390px窄屏。上传工具的文件路径权限配置拒绝本地文件路径，因此使用验证页内置的虚构File对象触发文件输入事件；不能据此宣称原生文件选择器或生产联调已验收。
- 样张及机器可读验证记录位于 `outputs/device-import-20260924/`，均为测试签名，不能粘贴到真实设备。
- 尚未执行生产迁移、部署、MySQL专项并发验收或实体打印扫码。
