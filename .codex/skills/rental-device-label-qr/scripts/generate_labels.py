#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
import os
import re
import shutil
import sys
import tempfile
import uuid
import zipfile
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable


SKILL_NAME = "rental-device-label-qr"
SKILL_VERSION = "1.0.0"
OWNER_MARKER = ".rental-device-label-qr-owner.json"

LABEL_WIDTH = 945
LABEL_HEIGHT = 472
LABEL_DPI = 600
LEFT_WIDTH = 520
QR_PANEL_WIDTH = LABEL_WIDTH - LEFT_WIDTH
QR_TARGET_SIZE = 410

A4_WIDTH = 2480
A4_HEIGHT = 3508
A4_DPI = 300
PRINT_LABEL_WIDTH = 472
PRINT_LABEL_HEIGHT = 236
PRINT_COLUMNS = 4
PRINT_ROWS = 13

PNG_DIR_NAME = "PNG-40x20mm-600DPI"
PDF_NAME = "设备二维码-A4打印版-40x20mm.pdf"
PREVIEW_NAME = "设备二维码-预览总表.png"
MANIFEST_NAME = "设备二维码清单.csv"
VERIFICATION_NAME = "verification.json"
README_NAME = "使用说明.txt"

REQUIRED_FIELDS = {"device_no", "serial_number", "payload"}
DEVICE_PATTERN = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")
MODEL_PATTERN = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")
PAYLOAD_PATTERN = re.compile(
    r"^CRD1\|([A-Za-z0-9][A-Za-z0-9._-]{0,63})"
    r"\|([A-Za-z0-9][A-Za-z0-9._-]{0,63})\|([0-9A-Fa-f]{16})$"
)
PHONE_PATTERN = re.compile(r"^[0-9+() -]{5,32}$")


class LabelError(RuntimeError):
    pass


def load_runtime_dependencies() -> None:
    global qrcode, zxingcpp, Image, ImageDraw, ImageFont, ERROR_CORRECT_M
    try:
        import qrcode as qrcode_module
        import zxingcpp as zxingcpp_module
        from PIL import Image as image_module
        from PIL import ImageDraw as image_draw_module
        from PIL import ImageFont as image_font_module
        from qrcode.constants import ERROR_CORRECT_M as error_correct_m
    except ModuleNotFoundError as error:
        raise LabelError(
            "runtime dependencies are missing; install requirements.txt"
        ) from error

    qrcode = qrcode_module
    zxingcpp = zxingcpp_module
    Image = image_module
    ImageDraw = image_draw_module
    ImageFont = image_font_module
    ERROR_CORRECT_M = error_correct_m


@dataclass(frozen=True)
class DeviceRow:
    row_number: int
    device_no: str
    serial_number: str
    payload: str
    equipment_model_code: str
    label_name: str


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def validate_text_option(value: str, label: str, maximum: int) -> str:
    normalized = value.strip()
    if not normalized:
        raise LabelError(f"{label} must not be empty")
    if len(normalized) > maximum or "\n" in normalized or "\r" in normalized:
        raise LabelError(f"{label} is invalid")
    return normalized


def serial_suffix(serial_number: str, row_number: int) -> str:
    if not serial_number:
        return ""
    normalized = "".join(char for char in serial_number if char.isalnum())
    if len(normalized) < 4:
        raise LabelError(
            f"row {row_number}: serial_number needs at least four alphanumeric characters"
        )
    return normalized[-4:].upper()


def detect_delimiter(header: str) -> str:
    tab_count = header.count("\t")
    comma_count = header.count(",")
    if tab_count == 0 and comma_count == 0:
        raise LabelError("input must be a comma-delimited CSV or tab-delimited TSV")
    return "\t" if tab_count >= comma_count else ","


def read_devices(source_path: Path) -> list[DeviceRow]:
    if not source_path.is_file():
        raise LabelError("input file does not exist")

    try:
        with source_path.open("r", encoding="utf-8-sig", newline="") as source:
            first_line = source.readline()
            if not first_line:
                raise LabelError("input file is empty")
            delimiter = detect_delimiter(first_line)
            source.seek(0)
            reader = csv.DictReader(source, delimiter=delimiter)
            fields = set(reader.fieldnames or [])
            missing = sorted(REQUIRED_FIELDS - fields)
            if missing:
                raise LabelError(f"input is missing required fields: {', '.join(missing)}")

            devices: list[DeviceRow] = []
            device_numbers: set[str] = set()
            label_names: set[str] = set()
            for row_number, raw in enumerate(reader, start=2):
                device_no = (raw.get("device_no") or "").strip()
                serial_number = (raw.get("serial_number") or "").strip()
                payload = (raw.get("payload") or "").strip()

                if not DEVICE_PATTERN.fullmatch(device_no):
                    raise LabelError(f"row {row_number}: device_no is invalid")
                if "\n" in serial_number or "\r" in serial_number:
                    raise LabelError(f"row {row_number}: serial_number is invalid")
                if serial_number and len(
                    "".join(char for char in serial_number if char.isalnum())
                ) < 4:
                    raise LabelError(
                        f"row {row_number}: serial_number needs at least four alphanumeric characters"
                    )
                payload_match = PAYLOAD_PATTERN.fullmatch(payload)
                if not payload_match:
                    raise LabelError(
                        f"row {row_number}: payload must match the signed CRD1 contract"
                    )

                payload_device_no, equipment_model_code, _signature = payload_match.groups()
                if payload_device_no != device_no:
                    raise LabelError(
                        f"row {row_number}: payload device number does not match device_no"
                    )
                if not MODEL_PATTERN.fullmatch(equipment_model_code):
                    raise LabelError(
                        f"row {row_number}: payload equipment model code is invalid"
                    )

                device_key = device_no.casefold()
                if device_key in device_numbers:
                    raise LabelError(f"row {row_number}: duplicate device_no")
                device_numbers.add(device_key)

                suffix = serial_suffix(serial_number, row_number)
                label_name = f"{device_no}-{suffix}" if suffix else device_no
                label_key = label_name.casefold()
                if label_key in label_names:
                    raise LabelError(f"row {row_number}: duplicate generated label name")
                label_names.add(label_key)

                devices.append(
                    DeviceRow(
                        row_number=row_number,
                        device_no=device_no,
                        serial_number=serial_number,
                        payload=payload,
                        equipment_model_code=equipment_model_code,
                        label_name=label_name,
                    )
                )
    except UnicodeDecodeError as error:
        raise LabelError("input must be UTF-8 encoded") from error

    if not devices:
        raise LabelError("input contains no device rows")
    return devices


def font_candidates() -> list[Path]:
    return [
        Path("/System/Library/Fonts/PingFang.ttc"),
        Path("/System/Library/Fonts/STHeiti Medium.ttc"),
        Path("/System/Library/Fonts/Hiragino Sans GB.ttc"),
        Path("/Library/Fonts/Arial Unicode.ttf"),
        Path("C:/Windows/Fonts/msyh.ttc"),
        Path("C:/Windows/Fonts/simhei.ttf"),
        Path("/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc"),
        Path("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"),
        Path("/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc"),
    ]


def resolve_font(explicit_font: Path | None) -> Path:
    candidates = [explicit_font] if explicit_font else font_candidates()
    for candidate in candidates:
        if candidate and candidate.is_file():
            try:
                ImageFont.truetype(str(candidate), 32)
                return candidate
            except OSError:
                continue
    if explicit_font:
        raise LabelError("the supplied font cannot be loaded")
    raise LabelError(
        "no Chinese-capable font was found; pass an installed font with --font"
    )


def fit_font(
    draw: ImageDraw.ImageDraw,
    text: str,
    font_path: Path,
    preferred_size: int,
    minimum_size: int,
    max_width: int,
    max_height: int,
) -> ImageFont.FreeTypeFont:
    for size in range(preferred_size, minimum_size - 1, -2):
        font = ImageFont.truetype(str(font_path), size)
        left, top, right, bottom = draw.textbbox((0, 0), text, font=font)
        if right - left <= max_width and bottom - top <= max_height:
            return font
    raise LabelError("label text cannot fit the fixed layout without truncation")


def draw_centered_line(
    draw: ImageDraw.ImageDraw,
    text: str,
    font_path: Path,
    preferred_size: int,
    minimum_size: int,
    top: int,
    height: int,
) -> None:
    font = fit_font(
        draw,
        text,
        font_path,
        preferred_size,
        minimum_size,
        LEFT_WIDTH - 50,
        height - 8,
    )
    left, bbox_top, right, bottom = draw.textbbox((0, 0), text, font=font)
    text_width = right - left
    text_height = bottom - bbox_top
    x = (LEFT_WIDTH - text_width) / 2 - left
    y = top + (height - text_height) / 2 - bbox_top
    draw.text(
        (x, y),
        text,
        font=font,
        fill="black",
        stroke_width=1,
        stroke_fill="black",
    )


def render_qr(payload: str) -> Image.Image:
    qr = qrcode.QRCode(
        version=None,
        error_correction=ERROR_CORRECT_M,
        box_size=1,
        border=4,
    )
    qr.add_data(payload)
    qr.make(fit=True)
    matrix = qr.get_matrix()
    matrix_size = len(matrix)
    module_size = QR_TARGET_SIZE // matrix_size
    if module_size < 2:
        raise LabelError("payload is too large for the fixed QR area")

    rendered_size = matrix_size * module_size
    image = Image.new("RGB", (QR_TARGET_SIZE, QR_TARGET_SIZE), "white")
    draw = ImageDraw.Draw(image)
    offset = (QR_TARGET_SIZE - rendered_size) // 2
    for y, row in enumerate(matrix):
        for x, enabled in enumerate(row):
            if enabled:
                x0 = offset + x * module_size
                y0 = offset + y * module_size
                draw.rectangle(
                    (x0, y0, x0 + module_size - 1, y0 + module_size - 1),
                    fill="black",
                )
    return image


def render_label(
    device: DeviceRow,
    shop_name: str,
    phone: str,
    font_path: Path,
) -> Image.Image:
    image = Image.new("RGB", (LABEL_WIDTH, LABEL_HEIGHT), "white")
    draw = ImageDraw.Draw(image)

    draw_centered_line(draw, shop_name, font_path, 72, 42, 28, 112)
    draw_centered_line(draw, phone, font_path, 68, 40, 166, 104)
    draw_centered_line(draw, device.label_name, font_path, 70, 38, 304, 112)

    qr_image = render_qr(device.payload)
    qr_x = LEFT_WIDTH + (QR_PANEL_WIDTH - QR_TARGET_SIZE) // 2
    qr_y = (LABEL_HEIGHT - QR_TARGET_SIZE) // 2
    image.paste(qr_image, (qr_x, qr_y))
    return image


def build_a4_pdf(labels: list[Image.Image], destination: Path) -> int:
    per_page = PRINT_COLUMNS * PRINT_ROWS
    page_count = math.ceil(len(labels) / per_page)
    pages: list[Image.Image] = []

    horizontal_gap = (
        A4_WIDTH - PRINT_COLUMNS * PRINT_LABEL_WIDTH
    ) // (PRINT_COLUMNS + 1)
    vertical_gap = (
        A4_HEIGHT - PRINT_ROWS * PRINT_LABEL_HEIGHT
    ) // (PRINT_ROWS + 1)

    for page_index in range(page_count):
        page = Image.new("RGB", (A4_WIDTH, A4_HEIGHT), "white")
        page_labels = labels[page_index * per_page : (page_index + 1) * per_page]
        for index, label in enumerate(page_labels):
            row = index // PRINT_COLUMNS
            column = index % PRINT_COLUMNS
            x = horizontal_gap + column * (PRINT_LABEL_WIDTH + horizontal_gap)
            y = vertical_gap + row * (PRINT_LABEL_HEIGHT + vertical_gap)
            rendered = label.resize(
                (PRINT_LABEL_WIDTH, PRINT_LABEL_HEIGHT),
                Image.Resampling.LANCZOS,
            )
            page.paste(rendered, (x, y))
        pages.append(page)

    pages[0].save(
        destination,
        "PDF",
        resolution=float(A4_DPI),
        save_all=True,
        append_images=pages[1:],
    )
    with destination.open("rb") as pdf:
        if pdf.read(5) != b"%PDF-" or destination.stat().st_size < 1024:
            raise LabelError("generated PDF is invalid")
    return page_count


def build_preview(labels: list[Image.Image], destination: Path) -> None:
    columns = min(4, len(labels))
    rows = math.ceil(len(labels) / columns)
    thumb_width = PRINT_LABEL_WIDTH
    thumb_height = PRINT_LABEL_HEIGHT
    margin = 36
    gap = 24
    width = margin * 2 + columns * thumb_width + (columns - 1) * gap
    height = margin * 2 + rows * thumb_height + (rows - 1) * gap
    preview = Image.new("RGB", (width, height), "#E7E4DD")
    for index, label in enumerate(labels):
        thumb = label.resize((thumb_width, thumb_height), Image.Resampling.LANCZOS)
        x = margin + (index % columns) * (thumb_width + gap)
        y = margin + (index // columns) * (thumb_height + gap)
        preview.paste(thumb, (x, y))
    preview.save(destination, dpi=(150, 150), optimize=True)


def write_manifest(
    devices: list[DeviceRow],
    png_hashes: dict[str, str],
    destination: Path,
) -> None:
    fields = [
        "序号",
        "设备编号",
        "设备序列号",
        "设备型号",
        "标签文字",
        "二维码载荷",
        "PNG文件名",
        "PNG_SHA256",
        "二维码回读",
    ]
    with destination.open("w", encoding="utf-8-sig", newline="") as target:
        writer = csv.DictWriter(target, fieldnames=fields)
        writer.writeheader()
        for index, device in enumerate(devices, start=1):
            filename = f"{device.label_name}.png"
            writer.writerow(
                {
                    "序号": index,
                    "设备编号": device.device_no,
                    "设备序列号": device.serial_number,
                    "设备型号": device.equipment_model_code,
                    "标签文字": device.label_name,
                    "二维码载荷": device.payload,
                    "PNG文件名": filename,
                    "PNG_SHA256": png_hashes[filename],
                    "二维码回读": "一致",
                }
            )


def write_readme(
    destination: Path,
    device_count: int,
    page_count: int,
    shop_name: str,
) -> None:
    destination.write_text(
        "\n".join(
            [
                "租赁设备二维码标签包",
                "",
                f"店铺：{shop_name}",
                f"标签数量：{device_count}",
                f"A4 页数：{page_count}",
                "",
                "文件说明：",
                f"1. {PNG_DIR_NAME}：每台设备一张 40x20mm、600 DPI PNG。",
                f"2. {PDF_NAME}：A4 300 DPI 打印版，每行 4 张、每页 13 行。",
                f"3. {PREVIEW_NAME}：标签预览总表。",
                f"4. {MANIFEST_NAME}：设备、载荷、文件和哈希清单。",
                f"5. {VERIFICATION_NAME}：软件回读与尺寸验证结果。",
                "",
                "打印要求：",
                "- 使用 100% 或实际尺寸打印。",
                "- 禁用适应页面、缩放页面和打印机驱动自动缩放。",
                "- 数字文件已逐张软件回读，但尚未完成实体打印扫码验收。",
                "- 投入发货或仓库操作前，请打印样张并使用实际扫码设备测试。",
                "",
            ]
        ),
        encoding="utf-8",
    )


def verify_png(
    path: Path,
    expected_payload: str,
    row_number: int,
) -> str:
    with Image.open(path) as image:
        if image.size != (LABEL_WIDTH, LABEL_HEIGHT):
            raise LabelError(f"row {row_number}: generated PNG dimensions are invalid")
        dpi = image.info.get("dpi")
        if not dpi or any(abs(float(value) - LABEL_DPI) > 1 for value in dpi[:2]):
            raise LabelError(f"row {row_number}: generated PNG DPI metadata is invalid")
        result = zxingcpp.read_barcode(image.convert("RGB"))
    if result is None:
        raise LabelError(f"row {row_number}: generated QR cannot be decoded")
    if result.text != expected_payload:
        raise LabelError(f"row {row_number}: decoded QR does not match the input payload")
    return sha256_file(path)


def write_verification(
    output_dir: Path,
    source_path: Path,
    font_path: Path,
    labels: list[dict[str, object]],
    page_count: int,
) -> None:
    artifact_names = [PDF_NAME, PREVIEW_NAME, MANIFEST_NAME, README_NAME, OWNER_MARKER]
    artifacts = {
        name: {"sha256": sha256_file(output_dir / name)}
        for name in artifact_names
    }
    report = {
        "schema_version": 1,
        "skill": SKILL_NAME,
        "skill_version": SKILL_VERSION,
        "generated_at": utc_now(),
        "source": {
            "filename": source_path.name,
            "sha256": sha256_file(source_path),
        },
        "font": font_path.name,
        "device_count": len(labels),
        "layout": {
            "label_pixels": [LABEL_WIDTH, LABEL_HEIGHT],
            "label_dpi": LABEL_DPI,
            "physical_mm": [40, 20],
            "a4_pixels": [A4_WIDTH, A4_HEIGHT],
            "a4_dpi": A4_DPI,
            "a4_columns": PRINT_COLUMNS,
            "a4_rows": PRINT_ROWS,
            "pdf_pages": page_count,
        },
        "qr_verification": {
            "decoder": "zxing-cpp",
            "decoded_count": len(labels),
            "exact_match_count": len(labels),
            "all_exact": True,
        },
        "physical_scan_tested": False,
        "physical_scan_note": (
            "Software decoding does not prove printed-label scan performance. "
            "Print at 100% and complete representative physical scans."
        ),
        "labels": labels,
        "artifacts": artifacts,
    }
    (output_dir / VERIFICATION_NAME).write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def write_owner_marker(output_dir: Path) -> None:
    marker = {
        "schema_version": 1,
        "owner": SKILL_NAME,
        "skill_version": SKILL_VERSION,
        "created_at": utc_now(),
    }
    (output_dir / OWNER_MARKER).write_text(
        json.dumps(marker, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def is_owned_output(output_dir: Path) -> bool:
    marker_path = output_dir / OWNER_MARKER
    if not marker_path.is_file():
        return False
    try:
        marker = json.loads(marker_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return False
    return marker.get("owner") == SKILL_NAME


def validate_destination(output_dir: Path, overwrite: bool) -> Path:
    output_dir = output_dir.expanduser().resolve()
    if output_dir in {Path("/").resolve(), Path.home().resolve()}:
        raise LabelError("output path is too broad")
    if output_dir.suffix.casefold() == ".zip":
        raise LabelError("output must be a directory path, not a ZIP path")
    if output_dir.is_symlink():
        raise LabelError("output path must not be a symbolic link")

    archive_path = output_dir.with_suffix(".zip")
    if archive_path.is_symlink():
        raise LabelError("output ZIP path must not be a symbolic link")

    if output_dir.exists():
        if not output_dir.is_dir():
            raise LabelError("output path exists and is not a directory")
        if not overwrite:
            raise LabelError("output directory exists; use --overwrite to replace it")
        if not is_owned_output(output_dir):
            raise LabelError("refusing to overwrite a directory not owned by this skill")
    elif archive_path.exists():
        raise LabelError("output ZIP already exists without an owned output directory")
    return output_dir


def build_zip(source_dir: Path, destination: Path, archive_root_name: str) -> None:
    with zipfile.ZipFile(
        destination,
        "w",
        compression=zipfile.ZIP_DEFLATED,
        compresslevel=9,
    ) as archive:
        for path in sorted(source_dir.rglob("*")):
            if path.is_file():
                relative = path.relative_to(source_dir)
                archive.write(path, Path(archive_root_name) / relative)


def remove_if_exists(path: Path) -> None:
    if path.is_dir() and not path.is_symlink():
        shutil.rmtree(path)
    elif path.exists() or path.is_symlink():
        path.unlink()


def publish_atomically(
    staging_dir: Path,
    archive_temp: Path,
    output_dir: Path,
) -> Path:
    archive_path = output_dir.with_suffix(".zip")
    token = uuid.uuid4().hex
    output_backup = output_dir.parent / f".{output_dir.name}.backup-{token}"
    archive_backup = output_dir.parent / f".{archive_path.name}.backup-{token}"
    installed_output = False
    installed_archive = False

    try:
        if output_dir.exists():
            os.replace(output_dir, output_backup)
        if archive_path.exists():
            os.replace(archive_path, archive_backup)

        os.replace(staging_dir, output_dir)
        installed_output = True
        os.replace(archive_temp, archive_path)
        installed_archive = True

        remove_if_exists(output_backup)
        remove_if_exists(archive_backup)
        return archive_path
    except Exception:
        if installed_archive:
            remove_if_exists(archive_path)
        if installed_output:
            remove_if_exists(output_dir)
        if output_backup.exists():
            os.replace(output_backup, output_dir)
        if archive_backup.exists():
            os.replace(archive_backup, archive_path)
        raise


def generate_package(
    source_path: Path,
    output_dir: Path,
    phone: str,
    shop_name: str,
    font_path: Path,
) -> tuple[int, int, Path]:
    devices = read_devices(source_path)
    parent = output_dir.parent
    parent.mkdir(parents=True, exist_ok=True)
    staging_dir = Path(
        tempfile.mkdtemp(prefix=f".{output_dir.name}.staging-", dir=parent)
    )
    archive_handle = tempfile.NamedTemporaryFile(
        prefix=f".{output_dir.name}.archive-",
        suffix=".zip",
        dir=parent,
        delete=False,
    )
    archive_temp = Path(archive_handle.name)
    archive_handle.close()

    try:
        png_dir = staging_dir / PNG_DIR_NAME
        png_dir.mkdir()
        write_owner_marker(staging_dir)

        labels: list[Image.Image] = []
        png_hashes: dict[str, str] = {}
        verification_labels: list[dict[str, object]] = []
        for device in devices:
            label = render_label(device, shop_name, phone, font_path)
            filename = f"{device.label_name}.png"
            destination = png_dir / filename
            label.save(destination, "PNG", dpi=(LABEL_DPI, LABEL_DPI), optimize=True)
            png_hash = verify_png(destination, device.payload, device.row_number)
            labels.append(label)
            png_hashes[filename] = png_hash
            verification_labels.append(
                {
                    "filename": filename,
                    "sha256": png_hash,
                    "decoded": True,
                    "exact_match": True,
                }
            )

        page_count = build_a4_pdf(labels, staging_dir / PDF_NAME)
        build_preview(labels, staging_dir / PREVIEW_NAME)
        write_manifest(devices, png_hashes, staging_dir / MANIFEST_NAME)
        write_readme(
            staging_dir / README_NAME,
            len(devices),
            page_count,
            shop_name,
        )
        write_verification(
            staging_dir,
            source_path,
            font_path,
            verification_labels,
            page_count,
        )
        build_zip(staging_dir, archive_temp, output_dir.name)
        archive_path = publish_atomically(staging_dir, archive_temp, output_dir)
        return len(devices), page_count, archive_path
    except Exception:
        remove_if_exists(staging_dir)
        remove_if_exists(archive_temp)
        raise


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "Generate verified 40x20 mm rental-device labels from server-signed "
            "CRD1 CSV/TSV input."
        )
    )
    parser.add_argument("--input", required=True, type=Path, help="UTF-8 CSV or TSV")
    parser.add_argument("--output", required=True, type=Path, help="Output directory")
    parser.add_argument("--phone", required=True, help="Business contact phone")
    parser.add_argument("--shop-name", default="长沙捷租达", help="Visible shop name")
    parser.add_argument("--font", type=Path, help="Chinese-capable TTF, TTC, or OTF")
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="Replace an existing output owned by this skill",
    )
    return parser


def main(argv: Iterable[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        load_runtime_dependencies()
        phone = validate_text_option(args.phone, "phone", 32)
        if not PHONE_PATTERN.fullmatch(phone):
            raise LabelError("phone contains unsupported characters")
        shop_name = validate_text_option(args.shop_name, "shop name", 32)
        source_path = args.input.expanduser().resolve()
        output_dir = validate_destination(args.output, args.overwrite)
        font_path = resolve_font(
            args.font.expanduser().resolve() if args.font else None
        )
        device_count, page_count, archive_path = generate_package(
            source_path,
            output_dir,
            phone,
            shop_name,
            font_path,
        )
        print(
            json.dumps(
                {
                    "ok": True,
                    "device_count": device_count,
                    "decoded_count": device_count,
                    "pdf_pages": page_count,
                    "output": str(output_dir),
                    "zip": str(archive_path),
                    "physical_scan_tested": False,
                },
                ensure_ascii=False,
            )
        )
        return 0
    except LabelError as error:
        print(f"error: {error}", file=sys.stderr)
        return 2
    except Exception as error:
        print(
            f"error: unexpected {type(error).__name__}; no package was published",
            file=sys.stderr,
        )
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
