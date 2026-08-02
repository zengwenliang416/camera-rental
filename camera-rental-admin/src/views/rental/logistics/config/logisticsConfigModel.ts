import type { TagProps } from 'element-plus'

const STATUS_KEYS: Record<string, string> = {
  INCOMPLETE: 'rental.logistics.statusIncomplete',
  READY_UNVERIFIED: 'rental.logistics.statusReadyUnverified',
  LOCALLY_VERIFIED: 'rental.logistics.statusLocallyVerified'
}

export function logisticsStatusKey(status: string) {
  return STATUS_KEYS[status] || status
}

export function logisticsStatusTagType(status?: string | null): TagProps['type'] {
  if (status === 'LOCALLY_VERIFIED') return 'success'
  if (status === 'READY_UNVERIFIED') return 'warning'
  if (status === 'INCOMPLETE') return 'danger'
  return 'info'
}

export function backfillStatusKey(status: string) {
  const keys: Record<string, string> = {
    ELIGIBLE: 'rental.logistics.backfillStatusEligible',
    CREATED: 'rental.logistics.backfillStatusCreated',
    REUSED: 'rental.logistics.backfillStatusReused',
    SKIPPED: 'rental.logistics.backfillStatusSkipped'
  }
  return keys[status] || status
}

export function backfillStatusTagType(status: string): TagProps['type'] {
  if (status === 'CREATED' || status === 'REUSED') return 'success'
  if (status === 'ELIGIBLE') return 'primary'
  if (status === 'SKIPPED') return 'warning'
  return 'info'
}
