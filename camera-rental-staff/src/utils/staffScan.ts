export function normalizeCode(value?: string) {
  return String(value || '')
    .trim()
    .replace(/\s+/g, '')
    .toUpperCase()
}

function extractQueryValue(raw: string, keys: string[]) {
  for (const key of keys) {
    const matched = raw.match(new RegExp(`[?&]${key}=([^&#]+)`, 'i'))
    if (matched?.[1])
      return decodeURIComponent(matched[1])
  }
  return ''
}

function extractJsonValue(raw: string, keys: string[]) {
  try {
    const json = JSON.parse(raw) as Record<string, unknown>
    for (const key of keys) {
      const value = json[key]
      if (value !== undefined && value !== null)
        return String(value)
    }
  }
  catch {
    return ''
  }
  return ''
}

export function extractDeviceNo(raw: string) {
  const keys = ['deviceNo', 'device_no', 'serialNo', 'serial_no', 'sn']
  return normalizeCode(extractQueryValue(raw, keys) || extractJsonValue(raw, keys) || raw)
}

export function extractWaybillNo(raw: string) {
  const text = normalizeCode(raw)
  const sfMatched = text.match(/SF[A-Z0-9]{10,}/)
  if (sfMatched?.[0])
    return sfMatched[0]
  const candidates = text.match(/[A-Z0-9]{10,}/g) || []
  return candidates.find(item => /\d{8,}/.test(item)) || text
}

export function maskText(value?: string) {
  const text = String(value || '').trim()
  if (!text)
    return '-'
  if (text.length <= 2)
    return `${text[0]}*`
  return `${text.slice(0, 1)}***${text.slice(-1)}`
}

export function maskOrderId(value?: string) {
  const text = String(value || '').trim()
  if (!text)
    return ''
  return text.length <= 10 ? '***' : `${text.slice(0, 6)}***${text.slice(-4)}`
}

export function maskWaybill(value?: string) {
  const text = normalizeCode(value)
  if (!text)
    return '-'
  if (text.length <= 8)
    return `${text.slice(0, 2)}***`
  return `${text.slice(0, 6)}····${text.slice(-4)}`
}
