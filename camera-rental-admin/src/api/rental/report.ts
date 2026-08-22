import request from '@/config/axios'

export interface RentalRevenueSummaryVO {
  rentAmountFen: number
  refundAmountFen: number
  orderCount: number
}

export interface RentalReportQuery {
  startDate: string
  endDate: string
  pageNo: number
  pageSize: number
}

export interface RentalReportSourceVO {
  sourceType: string
  orderCount: number
  rentAmountFen: number
  refundAmountFen: number
}

export interface RentalReportOverviewVO extends RentalRevenueSummaryVO {
  startDate: string
  endDate: string
  deviceCount: number
  totalDeviceDays: number
  occupiedDeviceDays: number
  idleDeviceDays: number
  utilizationBasisPoints: number
  assignedIncomeFen: number
  currency: string
  sources: RentalReportSourceVO[]
}

export interface RentalProductSkuReportVO {
  shopId: number
  externalProductId?: string
  externalSkuId?: string
  goodsTitle?: string
  orderCount: number
  goodsQuantity: number
  rentAmountFen: number
  refundAmountFen: number
}

export interface RentalDevicePerformanceReportVO {
  deviceId: number
  deviceNo: string
  equipmentModelCode: string
  status: string
  totalDays: number
  occupiedDays: number
  idleDays: number
  utilizationBasisPoints: number
  scheduleCount: number
  assignmentCount: number
  assignedIncomeFen: number
  latestRentalOrderId?: number
}

export interface RentalShipDateSummaryVO {
  date: string
  shipOrderCount: number
  shipAmountFen: number
  refundAmountFen: number
  currency: string
}

export const getRevenueSummary = (params?: { shopId?: number }) => {
  return request.get<RentalRevenueSummaryVO>({
    url: '/rental/report/revenue-summary',
    params
  })
}

export const getRentalReportOverview = (params: RentalReportQuery) => {
  return request.get<RentalReportOverviewVO>({
    url: '/rental/report/overview',
    params
  })
}

export const getRentalProductSkuReportPage = (params: RentalReportQuery) => {
  return request.get<PageResult<RentalProductSkuReportVO[]>>({
    url: '/rental/report/product-sku-page',
    params
  })
}

export const getRentalDevicePerformanceReportPage = (params: RentalReportQuery) => {
  return request.get<PageResult<RentalDevicePerformanceReportVO[]>>({
    url: '/rental/report/device-performance-page',
    params
  })
}

export const getRentalReportShipDateSummary = (params: { date: string }) => {
  return request.get<RentalShipDateSummaryVO>({
    url: '/rental/report/ship-date-summary',
    params
  })
}
