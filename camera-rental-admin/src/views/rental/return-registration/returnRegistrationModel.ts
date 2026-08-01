import type { ReturnRegistrationPageParams } from '@/api/rental/returnRegistration'

export const RETURN_REGISTRATION_STATUSES = [
  'DRAFT',
  'REVIEW_REQUIRED',
  'ACCEPTED',
  'REJECTED',
  'EXPIRED',
  'REVOKED'
] as const

const STATUS_LABELS: Record<string, string> = {
  DRAFT: '待客户填写',
  REVIEW_REQUIRED: '待人工复核',
  ACCEPTED: '已登记',
  REJECTED: '已驳回',
  EXPIRED: '已过期',
  REVOKED: '已撤销'
}

export const returnRegistrationStatusLabel = (status: string) =>
  STATUS_LABELS[status] || status

export const canReissueReturnRegistration = (status: string) => status === 'DRAFT'

export const canRevokeReturnRegistration = (status: string) => status === 'DRAFT'

export const canReviewReturnRegistration = (status: string) =>
  status === 'REVIEW_REQUIRED'

export function buildReturnRegistrationPageParams(
  query: ReturnRegistrationPageParams,
  submittedRange?: [string, string]
) {
  return {
    ...query,
    submittedStart: submittedRange?.[0],
    submittedEnd: submittedRange?.[1]
  }
}
