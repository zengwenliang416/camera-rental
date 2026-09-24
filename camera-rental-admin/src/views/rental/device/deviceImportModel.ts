export type SingleColumn = 'serial' | 'device'
export interface ParsedDeviceRow {
  lineNumber: number
  deviceNo: string
  serialNumber: string
}
export interface ImportParseIssue {
  lineNumber: number
  code: string
}
export const MAX_IMPORT_ROWS = 200
export const MAX_FILE_BYTES = 256 * 1024
export const MAX_IMPORT_FILES = 20

export function decodeDeviceFile(bytes: ArrayBuffer): string {
  const buffer = new Uint8Array(bytes)
  let text: string
  if (buffer[0] === 0xff && buffer[1] === 0xfe)
    text = new TextDecoder('utf-16le', { fatal: true }).decode(buffer)
  else if (buffer[0] === 0xfe && buffer[1] === 0xff)
    text = new TextDecoder('utf-16be', { fatal: true }).decode(buffer)
  else {
    try {
      text = new TextDecoder('utf-8', { fatal: true }).decode(buffer)
    } catch {
      text = new TextDecoder('gb18030', { fatal: true }).decode(buffer)
    }
  }
  if (/[\u0000-\u0008\u000b\u000c\u000e-\u001f\ufffd]/.test(text)) throw new Error('ENCODING')
  return text.replace(/^\uFEFF/, '')
}

// Split one record only. Embedded newlines / broken quotes fail with a source line, never guessed.
function columns(line: string): string[] | undefined {
  const delimiter = line.includes('\t')
    ? '\t'
    : line.includes(',')
      ? ','
      : line.includes('，')
        ? '，'
        : undefined
  if (!delimiter) return line.trim().split(/\s+/)
  const values: string[] = []
  let value = '',
    quoted = false,
    closed = false
  for (let i = 0; i < line.length; i++) {
    const char = line[i]
    if (quoted) {
      if (char === '"' && line[i + 1] === '"') {
        value += '"'
        i++
      } else if (char === '"') {
        quoted = false
        closed = true
      } else value += char
    } else if (char === delimiter) {
      values.push(value.trim())
      value = ''
      closed = false
    } else if (char === '"' && !value.trim() && !closed) quoted = true
    else if (char === '"' || (closed && char.trim())) return undefined
    else value += char
  }
  if (quoted) return undefined
  values.push(value.trim())
  return values
}

export function parseDeviceText(text: string, singleColumn: SingleColumn = 'serial') {
  const rows: ParsedDeviceRow[] = [],
    issues: ImportParseIssue[] = []
  let mapping: ('device' | 'serial')[] | undefined
  let first = true
  const header = (value: string): 'device' | 'serial' | undefined => {
    if (['设备编号', '设备号', 'deviceno', 'device_no'].includes(value.toLowerCase()))
      return 'device'
    if (
      ['序列号', '机身序列号', 'sn', 'serialnumber', 'serial_number'].includes(value.toLowerCase())
    )
      return 'serial'
  }
  for (const [index, raw] of text
    .replace(/^\uFEFF/, '')
    .split(/\r?\n|\r/)
    .entries()) {
    if (!raw.trim()) continue
    const parts = columns(raw)
    if (!parts || parts.length > 2) {
      issues.push({ lineNumber: index + 1, code: 'COLUMNS' })
      first = false
      continue
    }
    if (first) {
      first = false
      const fields = parts.map(header)
      if (fields.some(Boolean)) {
        if (fields.some((field) => !field) || new Set(fields).size !== fields.length)
          issues.push({ lineNumber: index + 1, code: 'HEADER' })
        else mapping = fields as ('device' | 'serial')[]
        continue
      }
    }
    const fields = mapping || (parts.length === 1 ? [singleColumn] : ['device', 'serial'])
    if (fields.length !== parts.length) {
      issues.push({ lineNumber: index + 1, code: 'COLUMNS' })
      continue
    }
    const row: ParsedDeviceRow = { lineNumber: index + 1, deviceNo: '', serialNumber: '' }
    fields.forEach((field, i) => {
      row[field === 'device' ? 'deviceNo' : 'serialNumber'] = parts[i]
    })
    if (
      (!row.deviceNo && !row.serialNumber) ||
      row.deviceNo.length > 64 ||
      row.serialNumber.length > 128
    ) {
      issues.push({ lineNumber: index + 1, code: 'IDENTIFIER' })
      continue
    }
    rows.push(row)
  }
  if (!rows.length && !issues.length) issues.push({ lineNumber: 1, code: 'EMPTY' })
  if (rows.length > MAX_IMPORT_ROWS) issues.push({ lineNumber: 1, code: 'LIMIT' })
  return { rows, issues }
}
