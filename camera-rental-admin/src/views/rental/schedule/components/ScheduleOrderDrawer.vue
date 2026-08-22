<template>
  <el-drawer
    :model-value="visible"
    :title="t('rental.schedule.orderDrawerTitle')"
    size="min(760px, 96vw)"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <el-skeleton v-if="loading" :rows="8" animated />
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
    <el-empty v-else-if="!order" :description="t('rental.schedule.detailEmpty')" />
    <div v-else class="schedule-drawer-body">
      <div class="schedule-drawer-summary">
        <div>
          <span>{{ t('rental.schedule.orderNo') }}</span>
          <strong class="schedule-data">{{ getScheduleOrderDisplayNo(order) }}</strong>
        </div>
        <div class="schedule-summary-actions">
          <el-tag type="warning" effect="light">{{ statusLabel(order.status) }}</el-tag>
          <el-button
            v-if="order.status !== 'CANCELED'"
            v-hasPermi="['rental:device:assign']"
            size="small"
            type="danger"
            plain
            @click="emit('cancel-order')"
          >
            {{ t('rental.schedule.cancelOrder') }}
          </el-button>
        </div>
      </div>

      <el-descriptions :column="2" border>
        <el-descriptions-item :label="t('rental.schedule.internalOrderId')">
          <span class="schedule-data">#{{ order.id }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('rental.schedule.externalOrderNo')">
          <span class="schedule-data">{{ order.externalOrderNo || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('rental.schedule.channel')">
          {{ order.sourceType || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('rental.schedule.reviewState')">
          {{ order.riskCodes.length ? order.riskCodes.join(' / ') : t('rental.schedule.noReview') }}
        </el-descriptions-item>
      </el-descriptions>

      <section class="schedule-drawer-section">
        <h4>{{ t('rental.schedule.orderItems') }}</h4>
        <el-table :data="order.items" size="small">
          <el-table-column :label="t('rental.schedule.equipmentModelCode')" min-width="130">
            <template #default="{ row }">{{ row.equipmentModelCode }}</template>
          </el-table-column>
          <el-table-column :label="t('rental.schedule.billableWindow')" min-width="150">
            <template #default="{ row }">
              {{ dateRange(row.billableStartDate, row.billableEndDate) }}
            </template>
          </el-table-column>
          <el-table-column :label="t('rental.schedule.occupyWindow')" min-width="170">
            <template #default="{ row }">
              {{ formatOccupyRange(row.occupyStartDate, row.occupyEndDateExclusive) }}
            </template>
          </el-table-column>
          <el-table-column :label="t('rental.schedule.quantity')" width="145">
            <template #default="{ row }">
              {{ row.assignedQuantity }} / {{ row.requiredQuantity }}
              <span class="schedule-muted">
                ({{ t('rental.schedule.remainingCount', { count: row.remainingQuantity }) }})
              </span>
            </template>
          </el-table-column>
          <el-table-column :label="t('table.action')" width="110" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.remainingQuantity > 0"
                link
                type="primary"
                @click="emit('allocate', row)"
              >
                {{ t('rental.schedule.allocateDevice') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-if="order.items.some((item) => item.assignments?.length)" class="schedule-drawer-section">
        <h4>{{ t('rental.schedule.assignedDevices') }}</h4>
        <el-table :data="assignedDevices" size="small">
          <el-table-column :label="t('rental.schedule.deviceNo')" min-width="120">
            <template #default="{ row }">
              <span class="schedule-data">{{ row.deviceNo || `#${row.deviceId}` }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('rental.schedule.assignmentStatus')" width="100">
            <template #default="{ row }">
              <el-tag :type="assignmentTagType(row.status)" effect="light">
                {{ assignmentStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('rental.schedule.occupyWindow')" min-width="170">
            <template #default="{ row }">
              {{ formatOccupyRange(row.occupyStartDate, row.occupyEndDateExclusive) }}
            </template>
          </el-table-column>
          <el-table-column :label="t('table.action')" width="100" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'ASSIGNED'"
                v-hasPermi="['rental:device:assign']"
                link
                type="danger"
                @click="emit('unassign', row)"
              >
                {{ t('rental.schedule.unassign') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-if="order.deliveries?.length" class="schedule-drawer-section">
        <h4>{{ t('rental.schedule.logisticsRisk') }}</h4>
        <el-alert
          v-for="delivery in order.deliveries"
          :key="delivery.id"
          class="schedule-risk-alert"
          :type="delivery.stale ? 'warning' : 'info'"
          :closable="false"
          :title="`${delivery.sourceCarrierName || '-'} · ${delivery.trackingStatus || '-'}`"
          :description="delivery.stale ? t('rental.schedule.trackingStale') : undefined"
        />
      </section>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import type {
  RentalScheduleAssignmentVO,
  RentalScheduleOrderDetailVO,
  RentalScheduleOrderItemVO
} from '@/api/rental/schedule'
import {
  formatClosedRange,
  formatOccupyRange,
  getScheduleOrderDisplayNo
} from '../scheduleModel'

const props = defineProps<{
  visible: boolean
  loading: boolean
  error: boolean
  order?: RentalScheduleOrderDetailVO
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  retry: []
  allocate: [item: RentalScheduleOrderItemVO]
  unassign: [assignment: RentalScheduleAssignmentVO]
  'cancel-order': []
}>()

const { t } = useI18n()
const order = computed(() => props.order)

const assignedDevices = computed<RentalScheduleAssignmentVO[]>(() =>
  props.order?.items.flatMap((item) => item.assignments || []) || []
)

const dateRange = (start?: string, end?: string) => {
  if (!start || !end) return '-'
  return formatClosedRange(start, end)
}

const statusLabel = (value: string) => {
  const key = `rental.schedule.orderStatuses.${value}`
  const translated = t(key)
  return translated === key ? value : translated
}

const assignmentStatusLabel = (value: string) => {
  const key = `rental.schedule.assignmentStatuses.${value}`
  const translated = t(key)
  return translated === key ? value : translated
}

const assignmentTagType = (value: string) =>
  value === 'ASSIGNED'
    ? 'success'
    : value === 'DISPATCHED'
      ? 'warning'
      : value === 'CANCELED'
        ? 'info'
        : 'primary'
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
.schedule-muted {
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

.schedule-summary-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.schedule-data {
  font-family: var(--el-font-family);
  font-variant-numeric: tabular-nums;
}

.schedule-risk-alert + .schedule-risk-alert {
  margin-top: 8px;
}
</style>
