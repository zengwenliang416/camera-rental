import request from '@/config/axios'

export interface RentalScheduleVO {
  id: number
  deviceId: number
  deviceNo?: string
  equipmentModelCode?: string
  rentalOrderId?: number
  rentalOrderItemId?: number
  scheduleType: string
  status: string
  billableStartDate?: string
  billableEndDate?: string
  occupyStartDate: string
  occupyEndDateExclusive: string
}

export interface RentalSchedulePageReqVO extends PageParam {
  deviceId?: number
  rentalOrderId?: number
  status?: string
  occupyStartDate?: string
  occupyEndDateExclusive?: string
}

export const getRentalSchedulePage = (params: RentalSchedulePageReqVO) => {
  return request.get<PageResult<RentalScheduleVO[]>>({
    url: '/rental/schedule/page',
    params
  })
}
