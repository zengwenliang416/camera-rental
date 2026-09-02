<template>
  <el-drawer
    :model-value="visible"
    :title="t('rental.schedule.deviceDrawerTitle')"
    size="min(820px, 96vw)"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <el-skeleton v-if="loading" :rows="9" animated />
    <el-alert
      v-else-if="error"
      type="error"
      :closable="false"
      :title="t('rental.schedule.detailLoadError')"
    >
      <el-button link type="primary" @click="emit('retry')">
        {{ t('rental.common.retry') }}
      </el-button>
    </el-alert>
    <el-empty v-else-if="!device" :description="t('rental.schedule.detailEmpty')" />
    <div v-else class="schedule-drawer-body">
      <div class="schedule-drawer-summary">
        <div>
          <span>{{ t('rental.schedule.deviceNo') }}</span>
          <strong class="schedule-data">{{ device.deviceNo }}</strong>
          <small>{{ device.equipmentModelCode }}</small>
        </div>
        <el-tag :type="deviceTagType(device.status)" effect="light">
          {{ statusLabel(device.status) }}
        </el-tag>
      </div>

      <el-descriptions :column="2" border>
        <el-descriptions-item :label="t('rental.schedule.serialNumber')">
          <span class="schedule-data">{{ device.serialNumber || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('rental.schedule.logisticsStatus')">
          {{ logisticsLabel(device.deliveries?.[0]?.trackingStatus) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('rental.schedule.availabilityState')">
          {{ device.reasonCodes?.length ? reasonCodesLabel(device.reasonCodes) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('rental.schedule.expectedRelease')">
          {{ device.expectedReleaseDate || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <section v-if="device.currentAssignment" class="schedule-drawer-section">
        <h4>{{ t('rental.schedule.currentAssignment') }}</h4>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item :label="t('rental.schedule.assignmentId')">
            <span class="schedule-data">#{{ device.currentAssignment.id }}</span>
          </el-descriptions-item>
          <el-descriptions-item :label="t('rental.schedule.internalOrderId')">
            <span class="schedule-data">#{{ device.currentAssignment.rentalOrderId }}</span>
          </el-descriptions-item>
          <el-descriptions-item :label="t('rental.schedule.occupyWindow')" :span="2">
            {{ formatOccupyRange(device.currentAssignment.occupyStartDate, device.currentAssignment.occupyEndDateExclusive) }}
          </el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="schedule-drawer-section">
        <h4>{{ t('rental.schedule.effectiveSchedules') }}</h4>
        <el-table :data="device.schedules" size="small">
          <el-table-column :label="t('rental.schedule.occupyWindow')" min-width="180">
            <template #default="{ row }">
              {{ formatOccupyRange(row.occupyStartDate, row.occupyEndDateExclusive) }}
            </template>
          </el-table-column>
          <el-table-column :label="t('rental.schedule.status')" min-width="100">
            <template #default="{ row }">{{ scheduleStatusLabel(row.status) }}</template>
          </el-table-column>
        </el-table>
      </section>

      <section v-if="device.deliveries?.length" class="schedule-drawer-section">
        <h4>{{ t('rental.schedule.deliverySummary') }}</h4>
        <el-table :data="device.deliveries" size="small">
          <el-table-column :label="t('rental.schedule.deliveryDirection')" width="90">
            <template #default="{ row }">{{ directionLabel(row.direction) }}</template>
          </el-table-column>
          <el-table-column :label="t('rental.schedule.carrier')" min-width="115">
            <template #default="{ row }">{{ row.sourceCarrierName || '-' }}</template>
          </el-table-column>
          <el-table-column :label="t('rental.schedule.latestTrace')" min-width="160">
            <template #default="{ row }">{{ trackingLabel(row.trackingStatus) }}</template>
          </el-table-column>
          <el-table-column width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="emit('open-logistics', row.id)">
                {{ t('rental.schedule.viewLogistics') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-if="device.activeLocks?.length" class="schedule-drawer-section">
        <h4>{{ t('rental.schedule.activeLocks') }}</h4>
        <el-alert
          v-for="lock in device.activeLocks"
          :key="lock.id"
          type="warning"
          :closable="false"
          :title="lockTypeLabel(lock.lockType)"
          :description="`${lock.reason} · ${formatDateTime(lock.plannedEndTime)}`"
        />
      </section>

      <section v-if="device.inspectionState || device.maintenanceState" class="schedule-drawer-section">
        <h4>{{ t('rental.schedule.lifecycleState') }}</h4>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item v-if="device.inspectionState" :label="t('rental.schedule.inspectionState')">
            {{ device.inspectionState }}
          </el-descriptions-item>
          <el-descriptions-item v-if="device.maintenanceState" :label="t('rental.schedule.maintenanceState')">
            {{ device.maintenanceState }}
          </el-descriptions-item>
        </el-descriptions>
      </section>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { useI18n } from '@/hooks/web/useI18n'
import { getRentalLabelKey } from '@/utils/rentalLabels'
import type { RentalDeviceScheduleDetailVO } from '@/api/rental/device'
import { formatDateTime, formatOccupyRange, translateReasonCodes } from '../scheduleModel'

defineProps<{
  visible: boolean
  loading: boolean
  error: boolean
  device?: RentalDeviceScheduleDetailVO
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  retry: []
  'open-logistics': [deliveryId: number]
}>()

const { t } = useI18n()

const statusLabel = (value: string) => {
  const key = `rental.schedule.deviceStates.${value}`
  const translated = t(key)
  return translated === key ? value : translated
}

const logisticsLabel = (value?: string) => {
  if (!value) return '-'
  const key = `rental.schedule.logistics.${value}`
  const translated = t(key)
  return translated === key ? value : translated
}

const trackingLabel = (value?: string) => {
  if (!value) return '-'
  const key = `rental.schedule.trackingStatuses.${value}`
  const translated = t(key)
  return translated === key ? value : translated
}

const scheduleStatusLabel = (value?: string) =>
  value ? t(getRentalLabelKey('schedule', value), { code: value }) : '-'

const reasonCodesLabel = (codes: string[]) => translateReasonCodes(t, codes)

const directionLabel = (value: string) => {
  const key = `rental.schedule.deliveryDirections.${value}`
  const translated = t(key)
  return translated === key ? value : translated
}

const lockTypeLabel = (value: string) => {
  const key = `rental.schedule.lockTypes.${value}`
  const translated = t(key)
  return translated === key ? value : translated
}

const deviceTagType = (value: string) => {
  if (value === 'AVAILABLE') return 'success'
  if (value === 'MAINTENANCE' || value === 'DISABLED') return 'danger'
  return 'warning'
}
</script>

<style scoped>
.schedule-drawer-body {
  display: grid;
  gap: 18px;
}

.schedule-drawer-summary {
  display: flex;
  padding: 12px 14px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.schedule-drawer-summary div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.schedule-drawer-summary span,
.schedule-drawer-summary small {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.schedule-drawer-summary strong {
  color: var(--el-text-color-primary);
  font-size: 16px;
}

.schedule-drawer-section {
  display: grid;
  gap: 10px;
}

.schedule-drawer-section h4 {
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 14px;
}

.schedule-data {
  font-family: var(--el-font-family);
  font-variant-numeric: tabular-nums;
}
</style>
