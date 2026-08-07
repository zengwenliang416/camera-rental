<template>
  <el-card shadow="never" class="mb-16px logistics-card">
    <template #header>
      <div class="section-header">
        <div>
          <div class="section-title">{{ t('rental.logistics.backfillTitle') }}</div>
          <div class="section-description">
            {{ t('rental.logistics.backfillDescription') }}
          </div>
        </div>
        <el-tag type="warning" effect="plain">
          {{ t('rental.logistics.backfillAdminOnly') }}
        </el-tag>
      </div>
    </template>

    <el-alert
      class="mb-16px"
      type="warning"
      :closable="false"
      show-icon
      :title="t('rental.logistics.backfillSafetyHint')"
    />

    <el-form :model="form" label-width="140px" class="max-w-1000px">
      <div class="grid grid-cols-1 gap-x-24px lg:grid-cols-2">
        <el-form-item :label="t('rental.logistics.backfillDateRange')">
          <div class="field-control">
            <el-date-picker
              v-model="form.consignDateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              :range-separator="t('rental.logistics.backfillDateSeparator')"
              :start-placeholder="t('rental.logistics.backfillStartDate')"
              :end-placeholder="t('rental.logistics.backfillEndDate')"
              class="!w-full"
            />
            <div class="form-tip">{{ t('rental.logistics.backfillDateHint') }}</div>
          </div>
        </el-form-item>
        <el-form-item :label="t('rental.logistics.backfillLimit')">
          <div class="field-control">
            <el-input-number
              v-model="form.limit"
              :min="1"
              :max="100"
              controls-position="right"
              class="!w-full"
            />
            <div class="form-tip">{{ t('rental.logistics.backfillLimitHint') }}</div>
          </div>
        </el-form-item>
      </div>

      <el-form-item>
        <el-button
          type="primary"
          plain
          :loading="previewing"
          v-hasRole="['super_admin']"
          @click="preview"
        >
          <Icon icon="ep:view" class="mr-5px" />
          {{ t('rental.logistics.backfillPreview') }}
        </el-button>
        <el-button
          type="danger"
          :loading="applying"
          :disabled="!canApply"
          v-hasRole="['super_admin']"
          @click="apply"
        >
          <Icon icon="ep:connection" class="mr-5px" />
          {{ t('rental.logistics.backfillApply') }}
        </el-button>
      </el-form-item>
    </el-form>

    <div v-if="result" class="backfill-result">
      <div class="backfill-metrics">
        <div v-for="metric in metrics" :key="metric.label" class="backfill-metric">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </div>
      </div>

      <el-alert
        class="mt-12px"
        :type="result.dryRun ? 'info' : 'success'"
        :closable="false"
        show-icon
        :title="resultSummary"
      />

      <el-table
        v-if="result.items.length"
        :data="result.items.slice(0, 20)"
        class="mt-12px"
        size="small"
      >
        <el-table-column
          prop="maskedWaybillNo"
          :label="t('rental.logistics.backfillWaybill')"
          min-width="160"
        >
          <template #default="{ row }">
            {{ row.maskedWaybillNo || t('rental.logistics.backfillWaybillUnavailable') }}
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.logistics.backfillItemStatus')" width="130">
          <template #default="{ row }">
            <el-tag :type="backfillStatusTagType(row.status)" size="small">
              {{ translatedStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.logistics.backfillReason')" min-width="220">
          <template #default="{ row }">
            {{ translatedReason(row.reason) }}
          </template>
        </el-table-column>
      </el-table>
      <div v-if="result.items.length > 20" class="form-tip">
        {{ t('rental.logistics.backfillItemsTruncated', { count: result.items.length }) }}
      </div>
    </div>
  </el-card>
</template>

<script lang="ts" setup>
import dayjs from 'dayjs'
import { computed, reactive, ref } from 'vue'
import { backfillRentalLogistics } from '@/api/rental/logistics'
import type {
  RentalLogisticsBackfillReqVO,
  RentalLogisticsBackfillResultVO
} from '@/api/rental/logistics'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'
import { backfillStatusKey, backfillStatusTagType } from '../logisticsConfigModel'

const { t } = useI18n()
const message = useMessage()
const previewing = ref(false)
const applying = ref(false)
const result = ref<RentalLogisticsBackfillResultVO>()
const previewedScope = ref('')
const form = reactive({
  consignDateRange: [
    dayjs().subtract(2, 'day').format('YYYY-MM-DD'),
    dayjs().subtract(1, 'day').format('YYYY-MM-DD')
  ] as [string, string],
  limit: 100
})

const currentScope = computed(() => `${form.consignDateRange?.join(':') || ''}:${form.limit}`)
const eligibleCount = computed(
  () => result.value?.items.filter((item) => item.status === 'ELIGIBLE').length || 0
)
const distinctWaybillCount = computed(() => result.value?.distinctWaybillCount || 0)
const canApply = computed(
  () =>
    Boolean(result.value?.dryRun) &&
    previewedScope.value === currentScope.value &&
    eligibleCount.value > 0
)
const metrics = computed(() => [
  { label: t('rental.logistics.backfillCandidates'), value: result.value?.candidateCount || 0 },
  { label: t('rental.logistics.backfillEligible'), value: eligibleCount.value },
  { label: t('rental.logistics.backfillDistinctWaybills'), value: distinctWaybillCount.value },
  { label: t('rental.logistics.backfillSkipped'), value: result.value?.skippedCount || 0 }
])
const resultSummary = computed(() =>
  result.value?.dryRun
    ? t('rental.logistics.backfillPreviewSummary', {
        count: eligibleCount.value,
        waybills: distinctWaybillCount.value
      })
    : t('rental.logistics.backfillAppliedSummary', {
        count: result.value?.createdOrReusedCount || 0
      })
)

const buildPayload = (
  dryRun: boolean,
  enqueueProviderTasks: boolean
): RentalLogisticsBackfillReqVO | undefined => {
  const range = form.consignDateRange
  if (!range?.[0] || !range?.[1]) {
    message.warning(t('rental.logistics.backfillDateRequired'))
    return
  }
  return {
    dryRun,
    limit: form.limit,
    enqueueProviderTasks,
    consignDateStart: range[0],
    consignDateEnd: range[1]
  }
}

const preview = async () => {
  const payload = buildPayload(true, false)
  if (!payload) return
  previewing.value = true
  try {
    result.value = await backfillRentalLogistics(payload)
    previewedScope.value = currentScope.value
    message.success(t('rental.logistics.backfillPreviewCompleted'))
  } finally {
    previewing.value = false
  }
}

const apply = async () => {
  if (!canApply.value) {
    message.warning(t('rental.logistics.backfillPreviewRequired'))
    return
  }
  await message.confirm(
    t('rental.logistics.backfillApplyConfirm', {
      count: eligibleCount.value,
      waybills: distinctWaybillCount.value
    })
  )
  const payload = buildPayload(false, true)
  if (!payload) return
  applying.value = true
  try {
    result.value = await backfillRentalLogistics(payload)
    previewedScope.value = ''
    message.success(
      t('rental.logistics.backfillApplied', {
        count: result.value.createdOrReusedCount
      })
    )
  } finally {
    applying.value = false
  }
}

const translatedStatus = (status: string) => {
  const key = backfillStatusKey(status)
  return key === status ? status : t(key)
}

const translatedReason = (reason: string) => {
  const key = `rental.logistics.backfillReasons.${reason}`
  const translated = t(key)
  return translated === key ? reason : translated
}
</script>

<style scoped>
.logistics-card {
  border-color: rgb(15 118 110 / 16%);
}

.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.section-title {
  font-weight: 700;
}

.section-description,
.form-tip {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
}

.field-control {
  width: 100%;
}

.backfill-result {
  padding-top: 4px;
}

.backfill-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.backfill-metric {
  padding: 14px 16px;
  background: linear-gradient(135deg, var(--el-fill-color-light), var(--el-fill-color-lighter));
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.backfill-metric span {
  display: block;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.backfill-metric strong {
  display: block;
  margin-top: 6px;
  font-size: 24px;
  line-height: 1;
}

@media (width <= 720px) {
  .section-header {
    flex-direction: column;
  }

  .backfill-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
