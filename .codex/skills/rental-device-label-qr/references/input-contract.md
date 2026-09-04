# Input Contract

## File

- Encoding: UTF-8 or UTF-8 with BOM.
- Format: comma-delimited CSV or tab-delimited TSV.
- Header names are case-sensitive.
- Empty files and files with no data rows are rejected.
- Production exports must remain outside the skill directory and Git.

## Required Fields

| Field | Rule |
|---|---|
| `device_no` | Non-empty; letters, digits, period, underscore, and hyphen only; unique ignoring case. |
| `serial_number` | May be empty when the trusted export has no serial number; otherwise it must contain at least four alphanumeric characters. |
| `payload` | Exactly four pipe-separated CRD1 segments. |

The accepted payload shape is:

```text
CRD1|{device_no}|{equipment_model_code}|{sig16}
```

- The payload device number must exactly equal `device_no`.
- `equipment_model_code` must use letters, digits, period, underscore, or
  hyphen.
- `sig16` must be exactly 16 hexadecimal characters.
- The generator validates shape and consistency only. It cannot authenticate
  the signature because it must not receive a signing secret.

## Label Name

The visible label and PNG filename are:

```text
{device_no}-{last four alphanumeric serial characters}
```

The serial suffix is normalized to uppercase. Duplicate generated label names
are rejected. When `serial_number` is empty, the label name falls back to
`{device_no}` because no serial suffix may be invented.

## Privacy And Trust

- Obtain the input from an authorized, trusted server-side export.
- Do not place production snapshots in `tests/`, `reports/`, or version control.
- Do not pass secrets, database credentials, cookies, or signing keys.
- The CLI intentionally does not print payloads or serial numbers.
