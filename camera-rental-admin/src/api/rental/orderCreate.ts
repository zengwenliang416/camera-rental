import request from '@/config/axios'

export type RentalDeliveryMethod = 'EXPRESS' | 'ERRAND' | 'SELF_DELIVERY'

export interface RentalManualOrderCustomerReqVO {
  name: string
  mobile: string
  wechatId?: string
}

export interface RentalManualOrderItemReqVO {
  modelCode: string
  quantity: number
  /** 租金，单位：分 */
  rentAmount: number
}

export interface RentalManualOrderDeliveryReqVO {
  method: RentalDeliveryMethod
  receiverName?: string
  receiverMobile?: string
  receiverAddress?: string
  remark?: string
}

export interface RentalManualOrderCreateReqVO {
  customer: RentalManualOrderCustomerReqVO
  items: RentalManualOrderItemReqVO[]
  /** 计租开始日期（闭区间），YYYY-MM-DD */
  billableStartDate: string
  /** 计租结束日期（闭区间），YYYY-MM-DD */
  billableEndDate: string
  /** 押金，单位：分 */
  depositAmount?: number
  delivery: RentalManualOrderDeliveryReqVO
}

export interface RentalManualOrderCreateRespVO {
  id: number
  orderNo: string
}

export interface RentalCustomerSuggestVO {
  id: number
  name: string
  mobile: string
  wechatId?: string
}

export const createRentalManualOrder = (data: RentalManualOrderCreateReqVO) => {
  return request.post<RentalManualOrderCreateRespVO>({
    url: '/rental/order/create-manual',
    data
  })
}

export const confirmRentalOrderOutbound = (orderId: number) => {
  return request.post<boolean>({ url: '/rental/order/confirm-outbound', data: { orderId } })
}

export const suggestRentalCustomer = (mobile: string) => {
  return request.get<RentalCustomerSuggestVO | null>({
    url: '/rental/customer/suggest',
    params: { mobile }
  })
}
