import request from '@/config/axios'

export type ApiLocalDate = string | [number, number, number]

export interface ReturnRegistrationRow {
  id: number
  formNo: string
  rentalOrderId: number
  orderNo: string
  status: string
  returnMethod?: string
  carrierName?: string
  waybillNo?: string
  expiresAt: string
  submittedAt?: string
  createTime: string
}

export interface ReturnRegistrationDevice {
  submittedSerial: string
  normalizedSerial: string
  matchStatus: string
  matchMessage?: string
  deviceId?: number
}

export interface ReturnRegistrationAttachment {
  attachmentId: number
  fileId: number
  category: string
  name: string
  size: number
  previewUrl: string
}

export interface ReturnRegistrationDetail extends ReturnRegistrationRow {
  carrierCode?: string
  senderMobile?: string
  shippedDate?: ApiLocalDate
  issueDescription?: string
  deliveryId?: number
  reviewedAt?: string
  reviewerId?: number
  reviewNote?: string
  customer?: {
    name?: string
    mobile?: string
    address?: string
  }
  devices: ReturnRegistrationDevice[]
  attachments: ReturnRegistrationAttachment[]
}

export interface ReturnRegistrationPageParams {
  pageNo: number
  pageSize: number
  keyword?: string
  status?: string
  rentalOrderId?: number
  serial?: string
  submittedStart?: string
  submittedEnd?: string
}

const path = '/rental/return-registration'

export const getReturnRegistrationPage = (params: ReturnRegistrationPageParams) =>
  request.get<{ list: ReturnRegistrationRow[]; total: number }>({ url: `${path}/page`, params })

export const getReturnRegistration = (id: number) =>
  request.get<ReturnRegistrationDetail>({ url: `${path}/get`, params: { id } })

export const revokeReturnRegistration = (id: number) =>
  request.post<boolean>({ url: `${path}/${id}/revoke` })

export const reviewReturnRegistration = (id: number, data: { accept: boolean; note?: string }) =>
  request.post<boolean>({ url: `${path}/${id}/review`, data })
