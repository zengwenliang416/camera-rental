# Rental Device Label QR

项目级生产技能，用于将服务器可信运行时导出的已签名设备二维码载荷，生成与门店
现有标签一致的 `40x20mm` 设备贴纸包。

## 能力

- 自动识别 UTF-8 CSV/TSV。
- 校验 `device_no`、`serial_number`（可为空）、`payload` 和重复项。
- 只接受 `CRD1|设备编号|设备型号|16位十六进制签名`。
- 生成 600 DPI 独立 PNG、300 DPI A4 PDF、预览总表、清单、验证报告和 ZIP。
- 使用 `zxing-cpp` 逐张回读二维码，回读值必须与输入载荷完全一致。
- 使用 staging 目录和原子替换发布，覆盖时只允许替换本技能创建的目录。

## 快速使用

```bash
cd /Volumes/zwl/camera-rental-github/.codex/skills/rental-device-label-qr
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/python scripts/generate_labels.py \
  --input /absolute/path/devices.tsv \
  --output /absolute/path/device-labels \
  --phone 19918960111
```

重复生成同一路径时使用 `--overwrite`。若目录不是该技能创建，命令会拒绝覆盖。

## 输入

必需列：

- `device_no`
- `serial_number`（可信导出没有序列号时可为空；不为空时至少 4 个字母或数字）
- `payload`

详细约束见 `references/input-contract.md`。测试 Fixture 只能使用虚构设备和虚构签名，
不得提交生产快照。

## 边界

本技能不生成签名、不读取密钥、不连接数据库、不修改服务器、不执行发货，也不替代
实体打印扫码验收。软件逐张回读通过只能证明数字文件可解码。

## 目录

- `SKILL.md`：触发、工作流和安全边界。
- `scripts/generate_labels.py`：确定性生成器。
- `references/`：输入与版式契约。
- `tests/`：虚构数据测试。
- `evals/`：触发语义评测。
- `reports/`：Yao 生成的技能工程报告。
