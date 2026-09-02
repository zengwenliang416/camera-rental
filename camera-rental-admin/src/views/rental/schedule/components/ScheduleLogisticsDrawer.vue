<template>
  <el-drawer
    :model-value="visible"
    :title="t('rental.schedule.logisticsDrawerTitle')"
    size="min(720px, 96vw)"
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
    <el-empty v-else-if="!detail" :description="t('rental.schedule.detailEmpty')" />
    <div v-else class="schedule-drawer-body">
      <div class="schedule-drawer-summary">
        <div>
          <span>{{ t('rental.schedule.deliveryDirection') }}</span>
          <strong>{{ directionLabel(detail.direction) }}</strong>
          <small class="schedule-data">{{ detail.maskedWaybillNo || '-' }}</small>
        </div>
        <el-tag :type="detail.stale ? 'warning' : 'success'" effect="light">
          {{ detail.stale ? t('rental.schedule.trackingStale') : statusLabel(detail.trackingStatus) }}
        </el-tag>
      </div>

      <el-descriptions :column="2" border>
        <el-descriptions-item :label="t('rental.schedule.carrier')">
          {{ detail.carrierName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('rental.schedule.latestLocation')">
          {{ detail.latestLocation || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('rental.schedule.latestTrace')" :span="2">
          {{ detail.latestTraceText || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('rental.schedule.latestEventTime')">
          {{ formatDateTime(detail.latestEventTime) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('rental.schedule.expectedDelivery')">
          {{ formatDateTime(detail.estimatedDeliveryAt) }}
        </el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-for="risk in detail.risks"
        :key="risk.code"
        class="schedule-risk-alert"
        type="warning"
        :closable="false"
        :title="risk.safeMessage"
        :description="risk.nextAction"
      />

      <section class="schedule-drawer-section">
        <div class="schedule-section-heading">
          <h4>{{ t('rental.schedule.trackingTrace') }}</h4>
          <el-button
            v-hasPermi="['rental:logistics:refresh', 'rental:delivery:tracking']"
            type="primary"
            plain
            :loading="refreshing"
            @click="emit('refresh')"
          >
            <Icon icon="ep:refresh" class="mr-5px" />{{ t('rental.schedule.refreshTracking') }}
          </el-button>
        </div>
        <el-empty v-if="detail.traces.length === 0" :description="t('rental.schedule.noTrackingTrace')" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="trace in detail.traces"
            :key="trace.eventSeq"
            :timestamp="formatDateTime(trace.businessTime)"
          >
            <strong>{{ statusLabel(trace.trackingStatus) }}</strong>
            <p>{{ trace.traceText || '-' }}</p>
            <small>{{ trace.location || '' }}</small>
          </el-timeline-item>
        </el-timeline>
      </section>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { useI18n } from '@/hooks/web/useI18n'
import type { RentalDeliveryTrackingDetailVO } from '@/api/rental/logistics'
import { formatDateTime } from '../scheduleModel'

defineProps<{
  visible: boolean
  loading: boolean
  error: boolean
  refreshing: boolean
  detail?: RentalDeliveryTrackingDetailVO
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  retry: []
  refresh: []
}>()

const { t } = useI18n()

const directionLabel = (value: string) => {
  const key = `rental.schedule.deliveryDirections.${value}`
  const translated = t(key)
  return translated === key ? value : translated
}

const statusLabel = (value?: string) => {
  if (!value) return '-'
  const key = `rental.schedule.trackingStatuses.${value}`
  const translated = t(key)
  return translated === key ? value : translated
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

.schedule-section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.schedule-drawer-section h4 {
  margin: 0;
  font-size: 14px;
}

.schedule-drawer-section p {
  margin: 4px 0 0;
}

.schedule-data {
  font-family: var(--el-font-family);
  font-variant-numeric: tabular-nums;
}

.schedule-risk-alert + .schedule-risk-alert {
  margin-top: 8px;
}
</style>
