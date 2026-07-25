export type RentalLabelGroup =
  | 'auth'
  | 'integration'
  | 'conversion'
  | 'channelOrder'
  | 'review'
  | 'device'
  | 'schedule'

export type RentalTagType = 'success' | 'warning' | 'danger' | 'info' | 'primary'

const RENTAL_STATUS_VALUES: Record<RentalLabelGroup, readonly string[]> = {
  auth: ['VALID', 'INVALID', 'UNKNOWN'],
  integration: ['READY', 'DISABLED', 'MISSING_CREDENTIALS'],
  conversion: ['PENDING', 'CONVERTED', 'REVIEW_REQUIRED', 'FAILED', 'CLOSED'],
  channelOrder: ['11', '12', '21', '22', '23', '24', 'UNKNOWN'],
  review: ['OPEN', 'RESOLVED', 'CLOSED'],
  device: ['AVAILABLE', 'RENTED', 'MAINTENANCE', 'RETIRED', 'DISABLED'],
  schedule: ['EFFECTIVE', 'CANCELLED']
}

const RENTAL_TAG_TYPES: Partial<Record<RentalLabelGroup, Record<string, RentalTagType>>> = {
  auth: { VALID: 'success', INVALID: 'danger', UNKNOWN: 'info' },
  integration: { READY: 'success', DISABLED: 'info', MISSING_CREDENTIALS: 'warning' },
  conversion: {
    PENDING: 'info',
    CONVERTED: 'success',
    REVIEW_REQUIRED: 'warning',
    FAILED: 'danger',
    CLOSED: 'info'
  },
  channelOrder: {
    '11': 'info',
    '12': 'warning',
    '21': 'warning',
    '22': 'success',
    '23': 'info',
    '24': 'info',
    UNKNOWN: 'info'
  },
  review: { OPEN: 'warning', RESOLVED: 'success', CLOSED: 'info' },
  device: {
    AVAILABLE: 'success',
    RENTED: 'warning',
    MAINTENANCE: 'danger',
    RETIRED: 'info',
    DISABLED: 'info'
  },
  schedule: { EFFECTIVE: 'success', CANCELLED: 'info' }
}

export const getRentalStatusValues = (group: RentalLabelGroup) => RENTAL_STATUS_VALUES[group]

export const getRentalLabelKey = (group: RentalLabelGroup, value?: string | number | null) => {
  const normalized = String(value ?? 'UNKNOWN')
  if (RENTAL_STATUS_VALUES[group].includes(normalized)) {
    return `rental.labels.${group}.${normalized}`
  }
  return group === 'channelOrder'
    ? 'rental.labels.channelOrder.other'
    : `rental.labels.${group}.UNKNOWN`
}

export const getRentalTagType = (
  group: RentalLabelGroup,
  value?: string | number | null
): RentalTagType => {
  const normalized = String(value ?? 'UNKNOWN')
  return RENTAL_TAG_TYPES[group]?.[normalized] ?? 'info'
}
