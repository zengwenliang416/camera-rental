import { computed, reactive, ref } from 'vue'
import {
  getRentalPendingAllocationOrders,
  getRentalScheduleWorkbench,
  type RentalSchedulePendingAllocationPageReqVO,
  type RentalPendingAllocationOrderVO,
  type RentalScheduleWorkbenchVO,
  type ScheduleWindowDays
} from '@/api/rental/schedule'
import {
  buildTimelineBounds,
  extractCandidates,
  shiftTimelineWindow,
  todayDate,
  toWorkbenchQuery,
  type ScheduleFilterDraft
} from './scheduleModel'

export interface ScheduleWorkbenchOptions {
  startDate?: string
  windowDays?: ScheduleWindowDays
}

export const isScheduleForbiddenError = (error: unknown) => {
  const candidate = error as {
    response?: { status?: number }
    status?: number
    code?: number | string
  }
  return (
    candidate?.response?.status === 403 ||
    candidate?.status === 403 ||
    candidate?.code === 403 ||
    candidate?.code === '403'
  )
}

export function useScheduleWorkbench(options: ScheduleWorkbenchOptions = {}) {
  const startDate = ref(options.startDate || todayDate())
  const windowDays = ref<ScheduleWindowDays>(options.windowDays || 30)
  const filters = ref<ScheduleFilterDraft>({
    keyword: '',
    equipmentModelCode: undefined,
    deviceStatus: undefined,
    logisticsStatus: undefined
  })
  const devicePage = reactive({ pageNo: 1, pageSize: 50 })
  const pendingPage = reactive({ pageNo: 1, pageSize: 10 })
  const workbench = ref<RentalScheduleWorkbenchVO>()
  const pendingItems = ref<RentalPendingAllocationOrderVO[]>([])
  const pendingTotal = ref(0)
  const loading = ref(false)
  const pendingLoading = ref(false)
  const loadError = ref(false)
  const pendingError = ref(false)
  const permissionDenied = ref(false)
  const pendingPermissionDenied = ref(false)
  let workbenchRequestId = 0
  let pendingRequestId = 0

  const bounds = computed(() => buildTimelineBounds(startDate.value, windowDays.value))
  const hasData = computed(() => Boolean(workbench.value))

  const loadWorkbench = async () => {
    const requestId = ++workbenchRequestId
    loading.value = true
    loadError.value = false
    permissionDenied.value = false
    try {
      const data = await getRentalScheduleWorkbench(
        toWorkbenchQuery(
          startDate.value,
          windowDays.value,
          filters.value,
          devicePage.pageNo,
          devicePage.pageSize
        )
      )
      if (requestId !== workbenchRequestId) return
      workbench.value = data
    } catch (error) {
      if (requestId !== workbenchRequestId) return
      loadError.value = true
      permissionDenied.value = isScheduleForbiddenError(error)
    } finally {
      if (requestId === workbenchRequestId) loading.value = false
    }
  }

  const loadPending = async () => {
    const requestId = ++pendingRequestId
    pendingLoading.value = true
    pendingError.value = false
    pendingPermissionDenied.value = false
    const params: RentalSchedulePendingAllocationPageReqVO = {
      pageNo: pendingPage.pageNo,
      pageSize: pendingPage.pageSize
    }
    try {
      const data = await getRentalPendingAllocationOrders(params)
      if (requestId !== pendingRequestId) return
      pendingItems.value = data.list
      pendingTotal.value = data.total
    } catch (error) {
      if (requestId !== pendingRequestId) return
      pendingError.value = true
      pendingPermissionDenied.value = isScheduleForbiddenError(error)
    } finally {
      if (requestId === pendingRequestId) pendingLoading.value = false
    }
  }

  const load = async () => {
    await Promise.all([loadWorkbench(), loadPending()])
  }

  const query = async () => {
    devicePage.pageNo = 1
    await load()
  }

  const resetFilters = async () => {
    filters.value = {
      keyword: '',
      equipmentModelCode: undefined,
      deviceStatus: undefined,
      logisticsStatus: undefined
    }
    await query()
  }

  const setWindowDays = async (value: ScheduleWindowDays) => {
    windowDays.value = value
    devicePage.pageNo = 1
    await loadWorkbench()
  }

  const shiftWindow = async (direction: -1 | 1) => {
    startDate.value = shiftTimelineWindow(startDate.value, windowDays.value, direction)
    await loadWorkbench()
  }

  const goToday = async () => {
    startDate.value = todayDate()
    await loadWorkbench()
  }

  const loadDevicePage = async () => {
    await loadWorkbench()
  }

  const loadPendingPage = async () => {
    await loadPending()
  }

  return {
    bounds,
    startDate,
    windowDays,
    filters,
    devicePage,
    pendingPage,
    workbench,
    pendingItems,
    pendingTotal,
    loading,
    pendingLoading,
    loadError,
    pendingError,
    permissionDenied,
    pendingPermissionDenied,
    hasData,
    load,
    loadWorkbench,
    loadPending,
    query,
    resetFilters,
    setWindowDays,
    shiftWindow,
    goToday,
    loadDevicePage,
    loadPendingPage,
    extractCandidates
  }
}
