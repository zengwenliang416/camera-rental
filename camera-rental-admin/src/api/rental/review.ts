import request from '@/config/axios'

export interface RentalManualReviewVO {
  id: number
  reviewType: string
  sourceType: string
  sourceIdentifier: string
  status: string
  reasonCode?: string
  reasonMessage?: string
  resolutionNote?: string
  resolvedBy?: number
  resolvedByName?: string
  resolvedAt?: string
}

export interface RentalManualReviewHandleReqVO {
  id: number
  resolutionNote: string
}

export const getManualReviewPage = (params: PageParam & { status?: string }) => {
  return request.get<PageResult<RentalManualReviewVO[]>>({
    url: '/rental/manual-review/page',
    params
  })
}

export const resolveManualReview = (data: RentalManualReviewHandleReqVO) => {
  return request.put({ url: '/rental/manual-review/resolve', data })
}

export const closeManualReview = (data: RentalManualReviewHandleReqVO) => {
  return request.put({ url: '/rental/manual-review/close', data })
}
