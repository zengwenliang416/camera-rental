/**
 * Convert Element Plus datetime values into epoch millis for Yudao LocalDateTime JSON.
 * Prefer value-format="x" on date pickers so the value is already millis (string or number).
 */
export function toEpochMillis(value: string | number | Date | null | undefined): number | null {
  if (value === null || value === undefined || value === '') {
    return null
  }
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }
  if (typeof value === 'string') {
    const trimmed = value.trim()
    if (/^\d+$/.test(trimmed)) {
      return Number(trimmed)
    }
    const parsed = Date.parse(trimmed.replace(' ', 'T'))
    return Number.isNaN(parsed) ? null : parsed
  }
  if (value instanceof Date) {
    return value.getTime()
  }
  return null
}
