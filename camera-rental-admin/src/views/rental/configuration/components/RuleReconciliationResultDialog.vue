<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('rental.configuration.reconciliationTitle')"
    width="min(720px, calc(100vw - 24px))"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-alert
      :type="alertType"
      :closable="false"
      show-icon
      :title="statusTitle"
      :description="statusDescription"
    />
    <el-alert
      v-if="loadError"
      class="mt-12px"
      type="warning"
      :closable="false"
      show-icon
      :title="t('rental.configuration.reconciliationLoadError')"
    />
    <div class="result-grid">
      <article v-for="item in counts" :key="item.key">
        <span>{{ t(item.labelKey) }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </div>
    <template #footer>
      <el-button :loading="refreshing" @click="emit('refresh')">
        {{ t('common.refresh') }}
      </el-button>
      <el-button type="primary" @click="emit('update:modelValue', false)">
        {{ t('common.close') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { RentalChannelReconciliationRunVO } from '@/api/rental/configuration'
import { useI18n } from '@/hooks/web/useI18n'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    run?: RentalChannelReconciliationRunVO
    refreshing?: boolean
    loadError?: boolean
  }>(),
  {
    refreshing: false,
    loadError: false
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  refresh: []
}>()

const { t } = useI18n()

const alertType = computed(() => {
  if (props.run?.status === 'FAILED') return 'error'
  if (props.run?.status === 'COMPLETED_WITH_ERRORS') return 'warning'
  if (props.run?.status === 'SUCCEEDED') return 'success'
  return 'info'
})

const statusTitle = computed(() =>
  t(`rental.configuration.reconciliationStatus.${props.run?.status ?? 'PENDING'}`)
)

const statusDescription = computed(() =>
  props.run?.lastErrorCode
    ? t('rental.configuration.reconciliationFailedWithCode', {
        code: props.run.lastErrorCode
      })
    : t('rental.configuration.reconciliationHint')
)

const counts = computed(() => [
  {
    key: 'scanned',
    labelKey: 'rental.configuration.reconciliationScanned',
    value: props.run?.scannedCount ?? 0
  },
  {
    key: 'skipped',
    labelKey: 'rental.configuration.reconciliationSkipped',
    value: props.run?.skippedCount ?? 0
  },
  {
    key: 'created',
    labelKey: 'rental.configuration.reconciliationCreated',
    value: props.run?.createdCount ?? 0
  },
  {
    key: 'updated',
    labelKey: 'rental.configuration.reconciliationUpdated',
    value: props.run?.updatedCount ?? 0
  },
  {
    key: 'conflict',
    labelKey: 'rental.configuration.reconciliationConflict',
    value: props.run?.conflictCount ?? 0
  },
  {
    key: 'failed',
    labelKey: 'rental.configuration.reconciliationFailed',
    value: props.run?.failedCount ?? 0
  },
  {
    key: 'review',
    labelKey: 'rental.configuration.reconciliationReview',
    value: props.run?.reviewRequiredCount ?? 0
  }
])
</script>

<style scoped>
.result-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 18px;
}

.result-grid article {
  padding: 14px 10px;
  text-align: center;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.result-grid span {
  display: block;
  min-height: 34px;
  font-size: 12px;
  line-height: 17px;
  color: var(--el-text-color-secondary);
}

.result-grid strong {
  display: block;
  margin-top: 6px;
  font-size: 21px;
  color: var(--el-text-color-primary);
}

@media (width <= 560px) {
  .result-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
