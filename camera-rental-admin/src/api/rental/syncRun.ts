import request from '@/config/axios'

export interface RentalSyncRunVO {
  id: number
  shopId?: number | string
  resourceType: string
  triggerType: string
  status: string
  windowStart?: string
  windowEnd?: string
  receivedCount: number
  deduplicatedCount: number
  succeededCount: number
  reviewRequiredCount: number
  failedCount: number
  lastErrorCode?: string
  lastErrorMessage?: string
  startedAt?: string
  finishedAt?: string
}

export interface RentalSyncRunPageReqVO extends PageParam {
  shopId?: string
  resourceType?: string
  status?: string
  triggerType?: string
}

export const getRentalSyncRunPage = (params: RentalSyncRunPageReqVO) => {
  return request.get<PageResult<RentalSyncRunVO[]>>({
    url: '/rental/xianyu/sync-run/page',
    params
  })
}
