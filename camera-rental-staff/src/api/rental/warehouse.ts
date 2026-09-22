import { http } from '@/http/http'
import type { PageResult } from '@/http/types'
import type { RentalDeviceOpsResult } from './device'

export type TaskQueue = 'SHIP' | 'RETURN' | 'OVERDUE' | 'INSPECT' | 'REPAIR'
export interface StaffTask { sourceType?: string, orderId?: number, deviceId?: number, assignmentId?: number, orderNo?: string, deviceNo?: string, equipmentModelCode?: string, dueDate?: string | number[] }
export interface StaffIssue { id: number, rentalOrderId?: number, deviceId?: number, title: string, note: string, status: string, ownerId?: number, revision: number }
export interface StocktakeLine { deviceId: number, deviceNo: string, originalWarehouse?: string, expected: boolean, scanned: boolean, adjusted: boolean }
export interface Stocktake { id: number, warehouseCode: string, status: string, lines?: StocktakeLine[] }
export interface InspectionCheck { label: string, result: 'PASS' | 'FAIL', expected?: number, actual?: number }
export interface InspectionRecord { id: number, assignmentId: number, deviceId: number, passed: boolean, note?: string, checklistJson: string, photoIdsJson: string, createTime: string }
export const getTasks = (queue: TaskQueue, pageNo = 1) => http.get<PageResult<StaffTask>>('/rental/staff/tasks', { queue, pageNo, pageSize: 20 })
export const getIssues = (status: string, pageNo = 1) => http.get<PageResult<StaffIssue>>('/rental/staff/issues', { status, pageNo, pageSize: 20 })
export const createIssue = (data: { requestKey?: string, title: string, note?: string, deviceId?: number, rentalOrderId?: number }) => http.post<number>('/rental/staff/issues', data)
export const actOnIssue = (data: { id: number, revision: number, action: 'CLAIM' | 'NOTE' | 'RESOLVE' | 'REOPEN', note?: string }) => http.post<boolean>('/rental/staff/issues/action', data)
export const getStocktakes = (pageNo = 1) => http.get<PageResult<Stocktake>>('/rental/staff/stocktakes', { pageNo, pageSize: 20 })
export const getStocktake = (id: number) => http.get<Stocktake>(`/rental/staff/stocktakes/${id}`)
export const createStocktake = (warehouseCode: string, idempotencyKey: string) => http.post<number>('/rental/staff/stocktakes', { warehouseCode, idempotencyKey })
export const scanStocktake = (id: number, deviceId: number) => http.post<boolean>('/rental/staff/stocktakes/scan', { id, deviceId })
export const closeStocktake = (id: number) => http.post<boolean>(`/rental/staff/stocktakes/${id}/close`)
export const moveStocktakeDevice = (id: number, deviceId: number) => http.post<boolean>('/rental/staff/stocktakes/move', { id, deviceId })
export const getInspectionTemplate = (modelCode: string) => http.get<InspectionCheck[]>('/rental/staff/inspection/template', { modelCode })
export const saveInspectionTemplate = (modelCode: string, checks: InspectionCheck[]) => http.put<boolean>('/rental/staff/inspection/template', { modelCode, checks })
export const getInspectionHistory = (deviceId: number) => http.get<InspectionRecord[]>('/rental/staff/inspection/history', { deviceId })
export const submitInspection = (data: { deviceId: number, assignmentId: number, idempotencyKey: string, checks: InspectionCheck[], photoIds: number[], note?: string }) => http.post<RentalDeviceOpsResult>('/rental/staff/inspection', data)
export const authorizePhoto = (deviceId: number, assignmentId: number, contentType: string) => http.post<{ id: number, uploadUrl: string }>('/rental/staff/photos', { deviceId, assignmentId, contentType })
export const confirmPhoto = (id: number) => http.post<{ id: number, url: string }>(`/rental/staff/photos/${id}/confirm`)
export const getAvailableDevices = (modelCode: string, from: string, endExclusive: string) => http.get<import('./order').DeviceCandidateResult>('/rental/staff/available-devices', { modelCode, from, endExclusive })

export const getPhoto = (id: number) => http.get<{ id: number, url: string }>(`/rental/staff/photos/${id}`)
