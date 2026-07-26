import type { IResponse, PageParam, PageResult } from '@/http/types'
import { http } from '@/http/http'

export interface XianyuExpressCompany {
  code: string
  expressName: string
  expressAlias?: string
  hot?: boolean
}

export interface XianyuPendingShipOrder {
  id: number
  shopId: number
  externalOrderId: string
  orderStatus: string
  goodsTitle?: string
  goodsQuantity?: number
  payAmount?: number
  buyerNick?: string
  rentalOrderId?: number
  conversionStatus: string
  orderTime?: string
  sourceUpdatedAt?: string
}

export interface XianyuShipmentOcrResult {
  waybillNo?: string
  expressCode?: string
  expressName?: string
  confidence?: number
  extractionSource?: string
}

export interface XianyuOrderShipReq {
  channelOrderId: number
  deviceId?: number
  deviceNo?: string
  idempotencyKey: string
  expressCode: string
  expressName: string
  waybillNo: string
  source: 'ADMIN' | 'STAFF'
  ocrConfirmed?: boolean
}

export interface XianyuOrderShipResult {
  shipmentId: number
  channelOrderId: number
  assignmentId?: number
  deviceId: number
  deviceNo: string
  maskedWaybillNo: string
  expressCode: string
  expressName: string
  remoteCode?: number
  remoteMsg?: string
  assignmentStatus?: string
  source: string
}

export function getXianyuPendingShipOrderPage(
  params: PageParam & { shopId?: number, keyword?: string },
) {
  return http.get<PageResult<XianyuPendingShipOrder>>(
    '/rental/xianyu/order/pending-ship/page',
    params,
  )
}

export function getXianyuExpressCompanyList() {
  return http.get<XianyuExpressCompany[]>(
    '/rental/xianyu/express-company/list',
  )
}

export function shipXianyuOrder(data: XianyuOrderShipReq) {
  return http.post<XianyuOrderShipResult>('/rental/xianyu/order/ship', data)
}

export function recognizeXianyuShipmentImage(filePath: string) {
  return new Promise<XianyuShipmentOcrResult>((resolve, reject) => {
    uni.uploadFile({
      url: '/rental/xianyu/order/ship/ocr',
      filePath,
      name: 'file',
      success: (res) => {
        try {
          if (res.statusCode < 200 || res.statusCode >= 300) {
            reject(new Error('发货图片识别请求失败'))
            return
          }
          const body = JSON.parse(
            res.data,
          ) as IResponse<XianyuShipmentOcrResult>
          if (body.code !== 0 && body.code !== 200) {
            reject(
              new Error(
                ('msg' in body ? body.msg : body.message) || '发货图片识别失败',
              ),
            )
            return
          }
          resolve(body.data)
        } catch {
          reject(new Error('发货图片识别响应解析失败'))
        }
      },
      fail: reject,
    })
  })
}
