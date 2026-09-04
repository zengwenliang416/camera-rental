# Layout Contract

## Individual Label

- Physical size: `40x20 mm`, landscape.
- Raster size: `945x472 px`.
- PNG metadata: `600 DPI`.
- Background: white.
- Left block:
  - shop name, default `长沙捷租达`;
  - operator-supplied phone number;
  - `{device_no}-{serial suffix}`.
- Right block: black QR on white with a four-module quiet zone.
- Text is automatically reduced when necessary but is never truncated.

The dimensions round to approximately `40.0x20.0 mm` at 600 DPI. Do not resize
the independent PNG before label-printer output.

## A4 PDF

- Canvas: A4 portrait at `300 DPI` (`2480x3508 px`).
- Label render size: `472x236 px`.
- Grid: 4 columns by 13 rows, centered with even gaps.
- Additional devices continue on following pages.

Print with actual size or `100%`. Disable fit-to-page, shrink, and printer-driver
scaling.

## Verification

- Each independent PNG is decoded using `zxing-cpp`.
- Decoded text must exactly equal the corresponding input payload.
- `verification.json` records dimensions, DPI, counts, hashes, PDF page count,
  and `physical_scan_tested`.
- `physical_scan_tested` remains `false`; only an operator can complete
  physical print-and-scan acceptance.
