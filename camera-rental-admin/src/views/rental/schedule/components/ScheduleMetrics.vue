<template>
  <section class="schedule-metrics" :aria-label="t('rental.schedule.overview')">
    <article
      v-for="metric in metricItems"
      :key="metric.key"
      class="schedule-metric"
      :class="`schedule-metric--${metric.tone}`"
    >
      <span class="schedule-metric__icon"><Icon :icon="metric.icon" /></span>
      <div class="schedule-metric__copy">
        <span>{{ t(metric.label) }}</span>
        <strong>{{ metric.value }}</strong>
        <small v-if="metric.hint">{{ t(metric.hint) }}</small>
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import type { RentalScheduleMetricsVO } from '@/api/rental/schedule'

const props = defineProps<{ metrics: RentalScheduleMetricsVO }>()
const { t } = useI18n()

const utilization = computed(
  () => `${(Number(props.metrics.utilizationRate || 0) * 100).toFixed(1)}%`
)

const metricItems = computed(() => [
  {
    key: 'total',
    icon: 'ep:box',
    label: 'rental.schedule.metrics.totalDevices',
    value: props.metrics.totalDevices,
    hint: 'rental.schedule.metrics.totalDevicesHint',
    tone: 'primary'
  },
  {
    key: 'available',
    icon: 'ep:circle-check',
    label: 'rental.schedule.metrics.availableDevices',
    value: props.metrics.availableDevices,
    hint: 'rental.schedule.metrics.availableDevicesHint',
    tone: 'success'
  },
  {
    key: 'occupied',
    icon: 'ep:timer',
    label: 'rental.schedule.metrics.occupiedDevices',
    value: props.metrics.occupiedDevices,
    hint: 'rental.schedule.metrics.occupiedDevicesHint',
    tone: 'info'
  },
  {
    key: 'transit',
    icon: 'ep:van',
    label: 'rental.schedule.metrics.inTransitDevices',
    value: props.metrics.inTransitDevices,
    hint: 'rental.schedule.metrics.inTransitDevicesHint',
    tone: 'warning'
  },
  {
    key: 'pending',
    icon: 'ep:document',
    label: 'rental.schedule.metrics.pendingAllocationOrders',
    value: props.metrics.pendingAllocationOrders,
    hint: 'rental.schedule.metrics.pendingAllocationOrdersHint',
    tone: 'warning'
  },
  {
    key: 'utilization',
    icon: 'ep:data-analysis',
    label: 'rental.schedule.metrics.utilization',
    value: utilization.value,
    hint: 'rental.schedule.metrics.utilizationHint',
    tone: 'neutral'
  }
])
</script>

<style scoped>
.schedule-metrics {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.schedule-metric {
  display: flex;
  min-width: 0;
  min-height: 104px;
  padding: 16px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  align-items: flex-start;
  gap: 12px;
}

.schedule-metric__icon {
  display: grid;
  flex: 0 0 36px;
  width: 36px;
  height: 36px;
  color: var(--metric-color);
  background: var(--metric-bg);
  border-radius: 8px;
  place-items: center;
}

.schedule-metric__copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.schedule-metric__copy > span,
.schedule-metric__copy small {
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-metric__copy strong {
  color: var(--el-text-color-primary);
  font-size: 24px;
  font-variant-numeric: tabular-nums;
  line-height: 30px;
}

.schedule-metric--primary {
  --metric-color: var(--el-color-primary);
  --metric-bg: var(--el-color-primary-light-9);
}

.schedule-metric--success {
  --metric-color: var(--el-color-success);
  --metric-bg: var(--el-color-success-light-9);
}

.schedule-metric--info,
.schedule-metric--neutral {
  --metric-color: var(--el-color-info);
  --metric-bg: var(--el-color-info-light-9);
}

.schedule-metric--warning {
  --metric-color: var(--el-color-warning);
  --metric-bg: var(--el-color-warning-light-9);
}

.schedule-metric--danger {
  --metric-color: var(--el-color-danger);
  --metric-bg: var(--el-color-danger-light-9);
}

@media (width <= 1280px) {
  .schedule-metrics {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (width <= 720px) {
  .schedule-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
