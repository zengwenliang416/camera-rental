---
name: rental-device-label-qr
description: "Generate and verify camera-rental device QR label packages from CSV or TSV manifests containing server-signed CRD1 payloads. Use for P4P, P3, or other rental-device labels in the Changsha Jiezu Da format: shop name, phone, device number plus serial suffix, right-side QR, 40x20 mm PNGs, A4 PDF, preview, manifest, verification report, and ZIP. Excludes QR signing, secret handling, database/server mutation, shipping actions, and generic QR artwork."
---

# Rental Device Label QR

Generate print-ready equipment labels from a trusted export without signing or
server access.

## Required Inputs

- A UTF-8 CSV or TSV file containing `device_no`, `serial_number`, and
  `payload`. `serial_number` may be empty when the trusted export has no serial
  number; in that case the visible label falls back to `device_no`.
- A business contact phone number supplied with `--phone`.
- Every `payload` must already be signed by the trusted backend and match:
  `CRD1|{device_no}|{equipment_model_code}|{sig16}`.

Read [input-contract.md](references/input-contract.md) before accepting an
export and [layout-contract.md](references/layout-contract.md) before changing
dimensions or typography.

## Workflow

1. Confirm the input is a trusted, current export. Do not infer, repair, or sign
   a missing payload.
2. Create an isolated environment and install `requirements.txt`.
3. Run:

```bash
python scripts/generate_labels.py \
  --input /absolute/path/devices.tsv \
  --output /absolute/path/device-labels \
  --phone 19918960111
```

4. If replacing a prior package created by this skill, add `--overwrite`. The
   script refuses to replace unowned directories.
5. Inspect the preview sheet and `verification.json`.
6. Print the PDF at `100%` or actual size. Disable fit-to-page.
7. Physically scan representative printed labels before operational use.

## Output Contract

The output directory contains:

- `PNG-40x20mm-600DPI/`: verified device PNGs.
- `设备二维码-A4打印版-40x20mm.pdf`: 4 by 13 A4 grid.
- `设备二维码-预览总表.png`: preview.
- `设备二维码清单.csv`: source, labels, hashes, and decode status.
- `verification.json`: dimensions, counts, hashes, pages, and scan status.
- `使用说明.txt` and a sibling ZIP archive.

## Safety Boundaries

- Never accept a signing key, AppSecret, database credential, or server session.
- Never generate or guess a signature. Invalid or unsigned rows fail closed.
- Never query or mutate the database, device ledger, order, shipment, or server.
- Never print payloads or serial numbers to the console.
- Do not copy production exports into this skill, tests, reports, or Git.
- Do not claim physical scan acceptance while
  `physical_scan_tested` is `false`.
- After renderer changes, rerun unit tests and a trusted-export regression.
- After trigger changes, rerun `evals/trigger_cases.json` with
  `evals/semantic_config.json`.
