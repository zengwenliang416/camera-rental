<template>
  <el-drawer
    :model-value="visible"
    :title="t('rental.schedule.candidateDrawerTitle')"
    size="min(780px, 96vw)"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <el-skeleton v-if="loading" :rows="8" animated />
    <el-alert
      v-else-if="error"
      type="error"
      :closable="false"
      :title="t('rental.schedule.candidateLoadError')"
    >
      <el-button link type="primary" @click="emit('retry')">
        {{ t('rental.common.retry') }}
      </el-button>
    </el-alert>
    <el-empty v-else-if="!result" :description="t('rental.schedule.detailEmpty')" />
    <div v-else class="candidate-body">
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="t('rental.schedule.orderNo')">
          {{ getScheduleOrderDisplayNo(result) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('rental.schedule.equipmentModelCode')">
          {{ result.equipmentModelCode }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('rental.schedule.occupyWindow')" :span="2">
          {{ formatOccupyRange(result.occupyStartDate, result.occupyEndDateExclusive) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('rental.schedule.quantity')">
          {{ result.assignedQuantity }} / {{ result.requiredQuantity }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('rental.schedule.remaining')">
          {{ result.remainingQuantity }}
        </el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-if="result.reasonCodes?.length"
        type="warning"
        :closable="false"
        :title="result.reasonCodes.join(' / ')"
      />

      <el-table :data="result.candidates" row-key="id">
        <el-table-column :label="t('rental.schedule.deviceNo')" min-width="130">
          <template #default="{ row }">
            <strong class="candidate-device">{{ row.deviceNo }}</strong>
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.schedule.status')" width="100">
          <template #default="{ row }">{{ row.status }}</template>
        </el-table-column>
        <el-table-column :label="t('rental.schedule.candidateDecision')" min-width="190">
          <template #default="{ row }">
            <el-tag :type="row.eligible ? 'success' : 'warning'" effect="light">
              {{ row.eligible ? t('rental.schedule.candidateEligible') : t('rental.schedule.candidateBlocked') }}
            </el-tag>
            <small v-if="row.reasonCodes.length" class="candidate-reasons">
              {{ row.reasonCodes.join(' / ') }}
            </small>
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.schedule.nextAvailable')" width="130">
          <template #default="{ row }">{{ row.nextAvailableDate || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('table.action')" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              v-hasPermi="['rental:device:assign']"
              link
              type="primary"
              :disabled="!row.eligible || result.remainingQuantity <= 0"
              :loading="assigningDeviceId === row.id"
              @click="emit('assign', row)"
            >
              {{ t('rental.schedule.confirmAssignment') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { useI18n } from '@/hooks/web/useI18n'
import type {
  RentalScheduleCandidateResponseVO,
  RentalScheduleCandidateVO
} from '@/api/rental/schedule'
import { formatOccupyRange, getScheduleOrderDisplayNo } from '../scheduleModel'

defineProps<{
  visible: boolean
  loading: boolean
  error: boolean
  assigningDeviceId?: number
  result?: RentalScheduleCandidateResponseVO
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  retry: []
  assign: [candidate: RentalScheduleCandidateVO]
}>()

const { t } = useI18n()
</script>

<style scoped>
.candidate-body {
  display: grid;
  gap: 16px;
}

.candidate-device {
  font-variant-numeric: tabular-nums;
}

.candidate-reasons {
  display: block;
  margin-top: 5px;
  color: var(--el-text-color-secondary);
  line-height: 1.45;
}
</style>
