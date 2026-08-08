<template>
  <section class="schedule-filter-card">
    <div class="schedule-filter-heading">
      <div>
        <span class="schedule-kicker">{{ t('rental.schedule.filterKicker') }}</span>
        <h3>{{ t('rental.schedule.filterTitle') }}</h3>
        <p>{{ t('rental.schedule.filterHintV2') }}</p>
      </div>
      <el-tag type="info" effect="plain">{{ t('rental.schedule.singleWarehouse') }}</el-tag>
    </div>

    <el-form class="schedule-filter-form" :inline="true" @submit.prevent="emit('submit')">
      <el-form-item :label="t('rental.schedule.deviceKeyword')">
        <el-input
          :model-value="modelValue.keyword"
          class="!w-260px"
          clearable
          :placeholder="t('rental.schedule.deviceKeywordPlaceholder')"
          @update:model-value="update('keyword', $event)"
          @keyup.enter="emit('submit')"
        >
          <template #prefix><Icon icon="ep:search" /></template>
        </el-input>
      </el-form-item>
      <el-form-item :label="t('rental.schedule.equipmentModelCode')">
        <el-input
          :model-value="modelValue.equipmentModelCode"
          class="!w-190px"
          clearable
          :placeholder="t('rental.schedule.equipmentModelPlaceholder')"
          @update:model-value="update('equipmentModelCode', $event)"
          @keyup.enter="emit('submit')"
        />
      </el-form-item>
      <el-form-item :label="t('rental.schedule.deviceStatus')">
        <el-select
          :model-value="modelValue.deviceStatus"
          class="!w-170px"
          clearable
          :placeholder="t('common.selectText')"
          @update:model-value="update('deviceStatus', $event)"
        >
          <el-option
            v-for="option in deviceStatusOptions"
            :key="option"
            :label="statusLabel('device', option)"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('rental.schedule.logisticsStatus')">
        <el-select
          :model-value="modelValue.logisticsStatus"
          class="!w-190px"
          clearable
          :placeholder="t('common.selectText')"
          @update:model-value="update('logisticsStatus', $event)"
        >
          <el-option
            v-for="option in logisticsStatusOptions"
            :key="option"
            :label="logisticsLabel(option)"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item class="schedule-filter-actions">
        <el-button type="primary" native-type="submit">
          <Icon icon="ep:search" class="mr-5px" />{{ t('common.query') }}
        </el-button>
        <el-button @click="emit('reset')">{{ t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>
  </section>
</template>

<script setup lang="ts">
import { useI18n } from '@/hooks/web/useI18n'
import { getRentalLabelKey } from '@/utils/rentalLabels'
import type { ScheduleFilterDraft } from '../scheduleModel'

const props = defineProps<{
  modelValue: ScheduleFilterDraft
  deviceStatusOptions: string[]
  logisticsStatusOptions: string[]
}>()

const emit = defineEmits<{
  submit: []
  reset: []
  'update:modelValue': [value: ScheduleFilterDraft]
}>()

const { t } = useI18n()

const update = <K extends keyof ScheduleFilterDraft>(key: K, value: ScheduleFilterDraft[K]) => {
  emit('update:modelValue', { ...props.modelValue, [key]: value })
}

const statusLabel = (group: 'device', value: string) => t(getRentalLabelKey(group, value))
const logisticsLabel = (value: string) => {
  const key = `rental.schedule.logistics.${value}`
  const translated = t(key)
  return translated === key ? value : translated
}
</script>

<style scoped>
.schedule-filter-card {
  padding: 18px 20px 12px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
}

.schedule-filter-heading {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.schedule-kicker {
  color: var(--el-color-primary);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.schedule-filter-heading h3 {
  margin: 3px 0 2px;
  color: var(--el-text-color-primary);
  font-size: 18px;
}

.schedule-filter-heading p {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.schedule-filter-form {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.schedule-filter-form :deep(.el-form-item) {
  margin: 0 0 10px;
}

.schedule-filter-actions {
  margin-left: auto !important;
}

@media (width <= 920px) {
  .schedule-filter-actions {
    margin-left: 0 !important;
  }
}

@media (width <= 640px) {
  .schedule-filter-form :deep(.el-form-item),
  .schedule-filter-form :deep(.el-form-item__content),
  .schedule-filter-form :deep(.el-input),
  .schedule-filter-form :deep(.el-select) {
    width: 100% !important;
  }

  .schedule-filter-heading {
    flex-direction: column;
  }
}
</style>
