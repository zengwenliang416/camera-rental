from __future__ import annotations

import csv
import json
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

import zxingcpp
from PIL import Image


SKILL_DIR = Path(__file__).resolve().parents[1]
SCRIPT = SKILL_DIR / "scripts/generate_labels.py"
FIXTURE = SKILL_DIR / "tests/fixtures/devices.tsv"


class GenerateLabelsTest(unittest.TestCase):
    def run_generator(
        self,
        source: Path,
        output: Path,
        *extra: str,
    ) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [
                sys.executable,
                str(SCRIPT),
                "--input",
                str(source),
                "--output",
                str(output),
                "--phone",
                "19900001111",
                *extra,
            ],
            check=False,
            capture_output=True,
            text=True,
        )

    def test_generates_and_verifies_complete_package(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            output = Path(temporary) / "labels"
            result = self.run_generator(FIXTURE, output)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertNotIn("CRD1|", result.stdout)
            self.assertNotIn("FICTIONAL-AB12", result.stdout)

            report = json.loads(
                (output / "verification.json").read_text(encoding="utf-8")
            )
            self.assertEqual(report["device_count"], 2)
            self.assertEqual(report["qr_verification"]["decoded_count"], 2)
            self.assertTrue(report["qr_verification"]["all_exact"])
            self.assertFalse(report["physical_scan_tested"])
            self.assertEqual(report["layout"]["label_pixels"], [945, 472])
            self.assertEqual(report["layout"]["label_dpi"], 600)
            self.assertEqual(report["layout"]["pdf_pages"], 1)

            png_dir = output / "PNG-40x20mm-600DPI"
            png_files = sorted(png_dir.glob("*.png"))
            self.assertEqual(len(png_files), 2)
            for png_file in png_files:
                with Image.open(png_file) as image:
                    self.assertEqual(image.size, (945, 472))
                    self.assertAlmostEqual(image.info["dpi"][0], 600, delta=1)
                    self.assertIsNotNone(
                        zxingcpp.read_barcode(image.convert("RGB"))
                    )

            archive_path = output.with_suffix(".zip")
            self.assertTrue(archive_path.is_file())
            with zipfile.ZipFile(archive_path) as archive:
                names = set(archive.namelist())
            self.assertIn("labels/verification.json", names)
            self.assertIn("labels/设备二维码-A4打印版-40x20mm.pdf", names)

            without_overwrite = self.run_generator(FIXTURE, output)
            self.assertEqual(without_overwrite.returncode, 2)
            with_overwrite = self.run_generator(FIXTURE, output, "--overwrite")
            self.assertEqual(with_overwrite.returncode, 0, with_overwrite.stderr)

    def test_rejects_payload_device_mismatch_without_publishing(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source = root / "invalid.csv"
            source.write_text(
                "device_no,serial_number,payload\n"
                "P4P-T01,FICTIONAL-AB12,"
                "CRD1|P4P-T99|P4P|0123456789abcdef\n",
                encoding="utf-8",
            )
            output = root / "labels"
            result = self.run_generator(source, output)
            self.assertEqual(result.returncode, 2)
            self.assertIn("does not match device_no", result.stderr)
            self.assertNotIn("CRD1|", result.stderr)
            self.assertFalse(output.exists())
            self.assertFalse(output.with_suffix(".zip").exists())

    def test_refuses_to_overwrite_unowned_directory(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            output = Path(temporary) / "labels"
            output.mkdir()
            (output / "keep.txt").write_text("user-owned", encoding="utf-8")
            result = self.run_generator(FIXTURE, output, "--overwrite")
            self.assertEqual(result.returncode, 2)
            self.assertIn("not owned by this skill", result.stderr)
            self.assertEqual(
                (output / "keep.txt").read_text(encoding="utf-8"),
                "user-owned",
            )

    def test_manifest_payloads_equal_fixture_payloads(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            output = Path(temporary) / "labels"
            result = self.run_generator(FIXTURE, output)
            self.assertEqual(result.returncode, 0, result.stderr)

            with FIXTURE.open("r", encoding="utf-8", newline="") as source:
                expected = [
                    row["payload"] for row in csv.DictReader(source, delimiter="\t")
                ]
            with (output / "设备二维码清单.csv").open(
                "r", encoding="utf-8-sig", newline=""
            ) as manifest:
                actual = [
                    row["二维码载荷"] for row in csv.DictReader(manifest)
                ]
            self.assertEqual(actual, expected)

    def test_empty_serial_falls_back_to_device_number(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source = root / "no-serial.tsv"
            source.write_text(
                "device_no\tserial_number\tpayload\n"
                "XT50-01\t\tCRD1|XT50-01|XT50|0123456789abcdef\n",
                encoding="utf-8",
            )
            output = root / "labels"
            result = self.run_generator(source, output)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertTrue((output / "PNG-40x20mm-600DPI" / "XT50-01.png").is_file())
            with (output / "设备二维码清单.csv").open(
                "r", encoding="utf-8-sig", newline=""
            ) as manifest:
                row = next(csv.DictReader(manifest))
            self.assertEqual(row["标签文字"], "XT50-01")
            self.assertEqual(row["设备序列号"], "")


if __name__ == "__main__":
    unittest.main()
