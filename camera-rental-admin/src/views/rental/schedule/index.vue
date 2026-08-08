<template>
  <Error v-if="!canQuery || permissionDenied" type="403" />
  <ContentWrap v-else class="schedule-content-wrap">
    <div class="schedule-shell">
      <header class="schedule-page-heading">
        <div>
          <span class="schedule-kicker">{{ t('rental.schedule.kicker') }}</span>
          <h2>{{ t('rental.schedule.pageTitleV2') }}</h2>
          <p>{{ t('rental.schedule.rangeHintV2') }}</p>
        </div>
        <div class="schedule-page-range">
          <span>{{ t('rental.schedule.visibleRange') }}</span>
          <strong>{{ formatOccupyRange(bounds.start, bounds.endExclusive) }}</strong>
        </div>
      </header>

      <el-alert
        v-if="loadError"
        class="schedule-alert"
        type="error"
        :closable="false"
        :title="t('rental.common.loadError')"
      >
        <el-button link type="primary" @click="load">
          {{ t('rental.common.retry') }}
        </el-button>
      </el-alert>
      <el-alert
        v-if="pendingError && !pendingPermissionDenied"
        class="schedule-alert"
        type="warning"
        :closable="false"
        :title="t('rental.schedule.pendingLoadError')"
      >
        <el-button link type="primary" @click="loadPendingPage">
          {{ t('rental.common.retry') }}
        </el-button>
      </el-alert>

      <ScheduleFilters
        v-model="filters"
        :model-options="modelOptions"
        :device-status-options="deviceStatusOptions"
        :logistics-status-options="logisticsStatusOptions"
        @submit="query"
        @reset="resetFilters"
      />

      <el-skeleton v-if="loading && !workbench" class="schedule-metrics-skeleton" :rows="2" animated />
      <ScheduleMetrics v-else-if="workbench" :metrics="workbench.metrics" />

      <ScheduleTimeline
        :bounds="bounds"
        :rows="workbench?.devicePage.list || []"
        :total="workbench?.devicePage.total || 0"
        :loading="loading"
        :page-no="devicePage.pageNo"
        :page-size="devicePage.pageSize"
        :window-days="windowDays"
        @previous="() => shiftWindow(-1)"
        @next="() => shiftWindow(1)"
        @today="goToday"
        @update:window-days="setWindowDays"
        @page-change="loadDevicePage"
        @update:page-no="devicePage.pageNo = $event"
        @update:page-size="devicePage.pageSize = $event"
        @select-device="openDevice"
        @select-segment="openSegment"
      />

      <div class="schedule-lower-grid">
        <PendingAllocationPanel
          :items="pendingItems"
          :total="pendingTotal"
          :loading="pendingLoading"
          :page-no="pendingPage.pageNo"
          :page-size="pendingPage.pageSize"
          @update:page-no="pendingPage.pageNo = $event"
          @update:page-size="pendingPage.pageSize = $event"
          @page-change="loadPendingPage"
          @select="selectPending"
          @open-order="openPendingOrder"
        />
        <ScheduleExceptionQueue
          :exceptions="workbench?.exceptions || []"
          :loading="loading && !workbench"
          @select="openException"
        />
      </div>
    </div>

    <ScheduleOrderDrawer
      :visible="orderDrawer.visible"
      :loading="orderDrawer.loading"
      :error="orderDrawer.error"
      :order="orderDrawer.detail"
      @update:visible="orderDrawer.visible = $event"
      @retry="loadOrderDetail"
      @allocate="openCandidates"
    />
    <ScheduleDeviceDrawer
      :visible="deviceDrawer.visible"
      :loading="deviceDrawer.loading"
      :error="deviceDrawer.error"
      :device="deviceDrawer.detail"
      @update:visible="deviceDrawer.visible = $event"
      @retry="loadDeviceDetail"
      @open-logistics="openLogistics"
    />
    <ScheduleLogisticsDrawer
      :visible="logisticsDrawer.visible"
      :loading="logisticsDrawer.loading"
      :error="logisticsDrawer.error"
      :refreshing="logisticsDrawer.refreshing"
      :detail="logisticsDrawer.detail"
      @update:visible="logisticsDrawer.visible = $event"
      @retry="loadLogisticsDetail"
      @refresh="refreshLogistics"
    />
    <ScheduleCandidateDrawer
      :visible="candidateDrawer.visible"
      :loading="candidateDrawer.loading"
      :error="candidateDrawer.error"
      :assigning-device-id="candidateDrawer.assigningDeviceId"
      :result="candidateDrawer.result"
      @update:visible="candidateDrawer.visible = $event"
      @retry="loadCandidates"
      @assign="confirmAssignment"
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from '@/hooks/web/useI18n'
import { hasPermission } from '@/directives/permission/hasPermi'
import {
  getRentalDeliveryTracking,
  refreshRentalDeliveryTracking,
  type RentalDeliveryTrackingDetailVO
} from '@/api/rental/logistics'
import {
  assignRentalDevice,
  getRentalDeviceDetail,
  type RentalDeviceScheduleDetailVO
} from '@/api/rental/device'
import {
  getRentalDeviceCandidates,
  getRentalOrderDetail,
  type RentalScheduleCandidateResponseVO,
  type RentalScheduleCandidateVO,
  type RentalScheduleDeviceLaneVO,
  type RentalScheduleExceptionVO,
  type RentalScheduleOrderDetailVO,
  type RentalScheduleOrderItemVO,
  type RentalPendingAllocationOrderVO,
  type RentalScheduleSegmentVO
} from '@/api/rental/schedule'
import { getRentalStatusValues } from '@/utils/rentalLabels'
import { formatOccupyRange } from './scheduleModel'
import { useScheduleWorkbench } from './useScheduleWorkbench'
import ScheduleMetrics from './components/ScheduleMetrics.vue'
import ScheduleFilters from './components/ScheduleFilters.vue'
import ScheduleTimeline from './components/ScheduleTimeline.vue'
import PendingAllocationPanel from './components/PendingAllocationPanel.vue'
import ScheduleExceptionQueue from './components/ScheduleExceptionQueue.vue'
import ScheduleOrderDrawer from './components/ScheduleOrderDrawer.vue'
import ScheduleDeviceDrawer from './components/ScheduleDeviceDrawer.vue'
import ScheduleLogisticsDrawer from './components/ScheduleLogisticsDrawer.vue'
import ScheduleCandidateDrawer from './components/ScheduleCandidateDrawer.vue'

defineOptions({ name: 'RentalSchedule' })

const { t } = useI18n()
const route = useRoute()
const canQuery = computed(() => hasPermission(['rental:schedule:query']))
const {
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
  load,
  loadPendingPage,
  query,
  resetFilters,
  setWindowDays,
  shiftWindow,
  goToday,
  loadDevicePage
} = useScheduleWorkbench()

const orderDrawer = reactive<{
  visible: boolean
  loading: boolean
  error: boolean
  orderId?: number
  detail?: RentalScheduleOrderDetailVO
}>({
  visible: false,
  loading: false,
  error: false
})

const deviceDrawer = reactive<{
  visible: boolean
  loading: boolean
  error: boolean
  deviceId?: number
  detail?: RentalDeviceScheduleDetailVO
}>({
  visible: false,
  loading: false,
  error: false
})

const logisticsDrawer = reactive<{
  visible: boolean
  loading: boolean
  refreshing: boolean
  error: boolean
  deliveryId?: number
  detail?: RentalDeliveryTrackingDetailVO
}>({
  visible: false,
  loading: false,
  refreshing: false,
  error: false
})

const candidateDrawer = reactive<{
  visible: boolean
  loading: boolean
  error: boolean
  rentalOrderItemId?: number
  assigningDeviceId?: number
  result?: RentalScheduleCandidateResponseVO
}>({
  visible: false,
  loading: false,
  error: false
})

const selectedPendingItem = ref<RentalPendingAllocationOrderVO>()

const modelOptions = computed(() => {
  return Array.from(
    new Set((workbench.value?.devicePage.list || []).map((row) => row.equipmentModelCode))
  )
})

const deviceStatusOptions = computed(() => [...getRentalStatusValues('device')])

const logisticsStatusOptions = computed(
  () => ['NONE', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'RETURNING',
    'RETURNED_PENDING_INSPECTION', 'EXCEPTION']
)

const loadOrderDetail = async () => {
  if (!orderDrawer.orderId) return
  orderDrawer.loading = true
  orderDrawer.error = false
  try {
    orderDrawer.detail = await getRentalOrderDetail(orderDrawer.orderId)
  } catch {
    orderDrawer.error = true
  } finally {
    orderDrawer.loading = false
  }
}

const loadDeviceDetail = async () => {
  if (!deviceDrawer.deviceId) return
  deviceDrawer.loading = true
  deviceDrawer.error = false
  try {
    deviceDrawer.detail = await getRentalDeviceDetail(deviceDrawer.deviceId)
  } catch {
    deviceDrawer.error = true
  } finally {
    deviceDrawer.loading = false
  }
}

const loadLogisticsDetail = async () => {
  if (!logisticsDrawer.deliveryId) return
  logisticsDrawer.loading = true
  logisticsDrawer.error = false
  try {
    logisticsDrawer.detail = await getRentalDeliveryTracking(logisticsDrawer.deliveryId)
  } catch {
    logisticsDrawer.error = true
  } finally {
    logisticsDrawer.loading = false
  }
}

const loadCandidates = async () => {
  if (!candidateDrawer.rentalOrderItemId) return
  candidateDrawer.loading = true
  candidateDrawer.error = false
  try {
    candidateDrawer.result = await getRentalDeviceCandidates(candidateDrawer.rentalOrderItemId)
  } catch {
    candidateDrawer.error = true
  } finally {
    candidateDrawer.loading = false
  }
}

const openOrder = async (orderId: number) => {
  orderDrawer.orderId = orderId
  orderDrawer.detail = undefined
  orderDrawer.error = false
  orderDrawer.visible = true
  await loadOrderDetail()
}

const openPendingOrder = async (item: RentalPendingAllocationOrderVO) => {
  selectedPendingItem.value = item
  await openOrder(item.id)
}

const selectPending = (item: RentalPendingAllocationOrderVO) => {
  selectedPendingItem.value = item
}

const openDevice = async (row: RentalScheduleDeviceLaneVO) => {
  deviceDrawer.deviceId = row.deviceId
  deviceDrawer.detail = undefined
  deviceDrawer.error = false
  deviceDrawer.visible = true
  await loadDeviceDetail()
}

const openSegment = async (row: RentalScheduleDeviceLaneVO, segment: RentalScheduleSegmentVO) => {
  if (segment.rentalOrderId) {
    await openOrder(segment.rentalOrderId)
    return
  }
  await openDevice(row)
}

const openException = async (exception: RentalScheduleExceptionVO) => {
  if (exception.rentalOrderId) {
    await openOrder(exception.rentalOrderId)
    return
  }
  if (exception.deviceId) {
    await openDevice({
      deviceId: exception.deviceId,
      deviceNo: `#${exception.deviceId}`,
      equipmentModelCode: '-',
      deviceStatus: 'UNKNOWN',
      enabled: true,
      segments: []
    })
  }
}

const openLogistics = async (deliveryId: number) => {
  logisticsDrawer.deliveryId = deliveryId
  logisticsDrawer.detail = undefined
  logisticsDrawer.error = false
  logisticsDrawer.visible = true
  await loadLogisticsDetail()
}

const openCandidates = async (item: RentalScheduleOrderItemVO) => {
  candidateDrawer.rentalOrderItemId = item.id
  candidateDrawer.result = undefined
  candidateDrawer.error = false
  candidateDrawer.visible = true
  await loadCandidates()
}

const confirmAssignment = async (candidate: RentalScheduleCandidateVO) => {
  const result = candidateDrawer.result
  if (!result || !candidate.eligible) return
  candidateDrawer.assigningDeviceId = candidate.id
  try {
    await assignRentalDevice({
      rentalOrderItemId: result.rentalOrderItemId,
      deviceId: candidate.id,
      occupyStartDate: result.occupyStartDate,
      occupyEndDateExclusive: result.occupyEndDateExclusive,
      idempotencyKey: `schedule-v2-${result.rentalOrderItemId}-${candidate.id}-${Date.now()}`
    })
    ElMessage.success(t('rental.schedule.assignmentSuccess'))
    await Promise.all([loadCandidates(), loadOrderDetail(), load()])
  } catch {
    ElMessage.error(t('rental.schedule.assignmentFailed'))
    await loadCandidates()
  } finally {
    candidateDrawer.assigningDeviceId = undefined
  }
}

const refreshLogistics = async () => {
  if (!logisticsDrawer.deliveryId) return
  logisticsDrawer.refreshing = true
  try {
    const result = await refreshRentalDeliveryTracking(logisticsDrawer.deliveryId)
    if (result.accepted) {
      ElMessage.success(t('rental.schedule.trackingRefreshAccepted'))
      await loadLogisticsDetail()
      return
    }
    ElMessage.warning(result.reason || t('rental.schedule.trackingRefreshRejected'))
  } catch {
    ElMessage.error(t('rental.schedule.trackingRefreshFailed'))
  } finally {
    logisticsDrawer.refreshing = false
  }
}

onMounted(async () => {
  const routeStart = typeof route.query.occupyStartDate === 'string' ? route.query.occupyStartDate : ''
  if (routeStart) startDate.value = routeStart
  await load()

  const routeDeviceId = Number(route.query.deviceId)
  if (Number.isInteger(routeDeviceId) && routeDeviceId > 0) {
    await openDevice({
      deviceId: routeDeviceId,
      deviceNo: `#${routeDeviceId}`,
      equipmentModelCode: '-',
      deviceStatus: 'UNKNOWN',
      enabled: true,
      segments: []
    })
  }
})
</script>

<style scoped>
.schedule-content-wrap {
  --schedule-ink: var(--el-text-color-primary);
  --schedule-muted: var(--el-text-color-secondary);
}

.schedule-shell {
  display: flex;
  min-width: 0;
  color: var(--schedule-ink);
  flex-direction: column;
  gap: 16px;
}

.schedule-page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.schedule-kicker {
  color: var(--el-color-primary);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.schedule-page-heading h2 {
  margin: 3px 0 4px;
  color: var(--el-text-color-primary);
  font-size: 24px;
}

.schedule-page-heading p {
  max-width: 840px;
  margin: 0;
  color: var(--schedule-muted);
  font-size: 12px;
  line-height: 1.7;
}

.schedule-page-range {
  display: flex;
  min-width: 185px;
  padding: 10px 12px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  flex-direction: column;
  gap: 2px;
}

.schedule-page-range span {
  color: var(--schedule-muted);
  font-size: 11px;
}

.schedule-page-range strong {
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.schedule-alert {
  border-radius: 8px;
}

.schedule-metrics-skeleton {
  padding: 16px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
}

.schedule-lower-grid {
  display: grid;
  min-width: 0;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.8fr);
  gap: 16px;
}

@media (width <= 1080px) {
  .schedule-lower-grid {
    grid-template-columns: 1fr;
  }
}

@media (width <= 720px) {
  .schedule-page-heading {
    flex-direction: column;
  }

  .schedule-page-range {
    width: 100%;
  }
}
</style>
