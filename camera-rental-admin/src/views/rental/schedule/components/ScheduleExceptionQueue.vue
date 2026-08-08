<template>
  <section class="schedule-panel">
    <div class="schedule-panel-heading">
      <div>
        <h3>{{ t('rental.schedule.exceptionTitle') }}</h3>
        <p>{{ t('rental.schedule.exceptionHint') }}</p>
      </div>
      <el-tag type="danger" effect="plain">
        {{ t('rental.schedule.exceptionCount', { count: exceptions.length }) }}
      </el-tag>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />
    <el-empty
      v-else-if="exceptions.length === 0"
      :description="t('rental.schedule.exceptionEmpty')"
      :image-size="64"
    />
    <div v-else class="schedule-exception-list">
      <button
        v-for="exception in exceptions"
        :key="`${exception.code}-${exception.deviceId || ''}-${exception.sourceId || ''}`"
        class="schedule-exception"
        type="button"
        @click="emit('select', exception)"
      >
        <span class="schedule-exception-icon" :class="`is-${exception.severity.toLowerCase()}`">
          <Icon :icon="exceptionIcon(exception.severity)" />
        </span>
        <span class="schedule-exception-copy">
          <span class="schedule-exception-title">
            <strong>{{ exception.code }}</strong>
            <el-tag :type="tagType(exception.severity)" size="small" effect="light">
              {{ severityLabel(exception.severity) }}
            </el-tag>
          </span>
          <small>{{ exception.message }}</small>
          <span class="schedule-exception-meta">
            <span v-if="exception.expectedReleaseDate">
              {{ t('rental.schedule.expectedRelease') }}:
              {{ formatDateTime(exception.expectedReleaseDate) }}
            </span>
            <span v-if="exception.nextAction">{{ exception.nextAction }}</span>
          </span>
        </span>
        <Icon icon="ep:arrow-right" class="schedule-exception-arrow" />
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useI18n } from '@/hooks/web/useI18n'
import type { RentalScheduleExceptionVO } from '@/api/rental/schedule'
import { formatDateTime } from '../scheduleModel'

defineProps<{
  exceptions: RentalScheduleExceptionVO[]
  loading: boolean
}>()

const emit = defineEmits<{
  select: [exception: RentalScheduleExceptionVO]
}>()

const { t } = useI18n()

const severityLabel = (value: string) => {
  const key = `rental.schedule.severity.${value}`
  const translated = t(key)
  return translated === key ? value : translated
}

const tagType = (value: string) => {
  if (value === 'CRITICAL' || value === 'HIGH') return 'danger'
  if (value === 'MEDIUM' || value === 'WARNING') return 'warning'
  return 'info'
}

const exceptionIcon = (value: string) => {
  if (value === 'CRITICAL' || value === 'HIGH') return 'ep:warning-filled'
  if (value === 'MEDIUM' || value === 'WARNING') return 'ep:warning'
  return 'ep:info-filled'
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

.schedule-exception-list {
  display: grid;
  gap: 1px;
}

.schedule-exception {
  display: flex;
  min-width: 0;
  padding: 10px 4px;
  background: transparent;
  border: 0;
  border-bottom: 1px solid var(--el-border-color-extra-light);
  color: inherit;
  cursor: pointer;
  align-items: flex-start;
  gap: 10px;
  text-align: left;
}

.schedule-exception:last-child {
  border-bottom: 0;
}

.schedule-exception:hover {
  background: var(--el-fill-color-light);
}

.schedule-exception-icon {
  display: grid;
  flex: 0 0 28px;
  width: 28px;
  height: 28px;
  border-radius: 7px;
  place-items: center;
}

.schedule-exception-icon.is-critical,
.schedule-exception-icon.is-high {
  color: var(--el-color-danger);
  background: var(--el-color-danger-light-9);
}

.schedule-exception-icon.is-medium,
.schedule-exception-icon.is-warning {
  color: var(--el-color-warning);
  background: var(--el-color-warning-light-9);
}

.schedule-exception-icon.is-low,
.schedule-exception-icon.is-info {
  color: var(--el-color-info);
  background: var(--el-color-info-light-9);
}

.schedule-exception-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 3px;
}

.schedule-exception-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.schedule-exception-title strong,
.schedule-exception-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-exception-title strong {
  color: var(--el-text-color-primary);
  font-size: 13px;
}

.schedule-exception-copy small,
.schedule-exception-meta {
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.schedule-exception-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.schedule-exception-arrow {
  margin-top: 7px;
  color: var(--el-text-color-placeholder);
}
</style>
