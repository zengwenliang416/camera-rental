<template>
  <section class="schedule-panel">
    <div class="schedule-panel-heading">
      <div>
        <h3>{{ t('rental.schedule.pendingTitle') }}</h3>
        <p>{{ t('rental.schedule.pendingHint') }}</p>
      </div>
      <el-tag type="warning" effect="plain">
        {{ t('rental.schedule.pendingCount', { count: total }) }}
      </el-tag>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />
    <el-empty
      v-else-if="items.length === 0"
      :description="t('rental.schedule.pendingEmpty')"
      :image-size="64"
    />
    <el-table
      v-else
      :data="items"
      row-key="id"
    >
      <el-table-column :label="t('rental.schedule.orderNo')" min-width="150">
        <template #default="{ row }">
          <button class="schedule-link schedule-data" type="button" @click.stop="emit('open-order', row)">
            {{ getScheduleOrderDisplayNo(row) }}
          </button>
        </template>
      </el-table-column>
      <el-table-column :label="t('rental.schedule.equipmentModelCode')" min-width="125">
        <template #default="{ row }">
          {{ row.items.map((item) => item.equipmentModelCode).join(' / ') || '-' }}
        </template>
      </el-table-column>
      <el-table-column :label="t('rental.schedule.billableWindow')" min-width="145">
        <template #default="{ row }">
          {{ dateRange(row.billableStartDate, row.billableEndDate) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('rental.schedule.occupyWindow')" min-width="165">
        <template #default="{ row }">
          {{ formatOccupyRange(row.occupyStartDate, row.occupyEndDateExclusive) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('rental.schedule.quantity')" width="116">
        <template #default="{ row }">
          <strong class="schedule-quantity">{{ row.remainingQuantity }}</strong>
          <span class="schedule-quantity-meta">
            / {{ row.requiredQuantity }} {{ t('rental.schedule.remaining') }}
          </span>
        </template>
      </el-table-column>
      <el-table-column :label="t('rental.schedule.status')" width="110">
        <template #default="{ row }">
          <el-tag type="warning" effect="light">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('table.action')" width="110" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="emit('open-order', row)">
            {{ t('rental.schedule.viewOrder') }}
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty :description="t('rental.schedule.pendingEmpty')" />
      </template>
    </el-table>

    <el-pagination
      v-if="total > 0"
      v-model:current-page="currentPage"
      v-model:page-size="currentPageSize"
      class="schedule-pagination"
      background
      layout="total, sizes, prev, pager, next"
      :page-sizes="[5, 10, 20]"
      :total="total"
      @size-change="emit('page-change')"
      @current-change="emit('page-change')"
    />
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import type { RentalPendingAllocationOrderVO } from '@/api/rental/schedule'
import {
  formatClosedRange,
  formatOccupyRange,
  getScheduleOrderDisplayNo
} from '../scheduleModel'

const props = defineProps<{
  items: RentalPendingAllocationOrderVO[]
  total: number
  loading: boolean
  pageNo: number
  pageSize: number
  selectedItemId?: number
}>()

const emit = defineEmits<{
  'open-order': [item: RentalPendingAllocationOrderVO]
  'page-change': []
  'update:pageNo': [value: number]
  'update:pageSize': [value: number]
}>()

const { t } = useI18n()

const currentPage = computed({
  get: () => props.pageNo,
  set: (value: number) => emit('update:pageNo', value)
})
const currentPageSize = computed({
  get: () => props.pageSize,
  set: (value: number) => emit('update:pageSize', value)
})

const dateRange = (start?: string, end?: string) => {
  if (!start || !end) return t('rental.schedule.notSet')
  return formatClosedRange(start, end)
}

const statusLabel = (value: string) => {
  const key = `rental.schedule.pendingStatuses.${value}`
  const translated = t(key)
  return translated === key ? value : translated
}
</script>

<style scoped>
.schedule-panel {
  min-width: 0;
  padding: 18px 20px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
}

.schedule-panel-heading {
  display: flex;
  margin-bottom: 12px;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.schedule-panel-heading h3 {
  margin: 0 0 3px;
  font-size: 16px;
}

.schedule-panel-heading p {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.schedule-link {
  padding: 0;
  background: transparent;
  border: 0;
  color: var(--el-color-primary);
  cursor: pointer;
}

.schedule-data {
  font-family: var(--el-font-family);
  font-variant-numeric: tabular-nums;
}

.schedule-quantity {
  color: var(--el-color-warning-dark-2);
  font-variant-numeric: tabular-nums;
}

.schedule-quantity-meta {
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.schedule-pagination {
  justify-content: flex-end;
}
</style>
