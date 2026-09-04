<template>
  <section class="schedule-timeline-card">
    <div class="schedule-timeline-heading">
      <div>
        <span class="schedule-kicker">{{ t('rental.schedule.timelineKicker') }}</span>
        <h3>{{ t('rental.schedule.timelineTitleV2') }}</h3>
        <p>{{ t('rental.schedule.timelineHint') }}</p>
      </div>
      <div class="schedule-window-actions">
        <el-button-group>
          <el-button @click="emit('previous')">{{ t('rental.schedule.previousWindow') }}</el-button>
          <el-button @click="emit('today')">{{ t('rental.schedule.todayWindow') }}</el-button>
          <el-button @click="emit('next')">{{ t('rental.schedule.nextWindow') }}</el-button>
        </el-button-group>
        <el-radio-group
          :model-value="windowDays"
          size="small"
          @update:model-value="onWindowDaysChange"
        >
          <el-radio-button v-for="days in windowOptions" :key="days" :label="days">
            {{ t('rental.schedule.daysCount', { count: days }) }}
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="schedule-device-toolbar">
      <div class="schedule-device-summary">
        <strong>{{ t('rental.schedule.deviceList') }}</strong>
        <el-tag type="info" effect="plain">
          {{ t('rental.schedule.devicePageRange', pageRange) }}
        </el-tag>
        <span>{{ t('rental.schedule.totalDevices', { count: total }) }}</span>
      </div>
      <span class="schedule-range-label">
        {{ formatOccupyRange(bounds.start, bounds.endExclusive) }}
      </span>
    </div>

    <el-skeleton v-if="loading" :rows="7" animated />
    <el-empty
      v-else-if="rows.length === 0"
      :description="t('rental.schedule.noDevicesInWindow')"
      :image-size="80"
    />
    <div v-else class="schedule-timeline-scroll">
      <div
        class="schedule-timeline-board"
        :style="{
          '--timeline-width': `${timelineWidth}px`,
          '--timeline-days': `${bounds.days}`
        }"
      >
        <div class="schedule-timeline-months">
          <div class="schedule-timeline-corner">{{ t('rental.schedule.deviceTimeAxis') }}</div>
          <div class="schedule-timeline-track schedule-timeline-track--months">
            <span
              v-for="group in monthGroups"
              :key="group.key"
              class="schedule-month-cell"
              :style="{ gridColumn: `${group.startIndex + 1} / span ${group.span}` }"
            >
              {{ group.label }}
            </span>
          </div>
        </div>
        <div class="schedule-timeline-days">
          <div class="schedule-timeline-corner">
            {{ t('rental.schedule.timelineScale', { count: bounds.days }) }}
          </div>
          <div class="schedule-timeline-track schedule-timeline-track--days">
            <span
              v-for="column in columns"
              :key="column.date"
              class="schedule-day-cell"
              :class="{ 'is-today': column.isToday }"
            >
              <strong>{{ column.day }}</strong>
              <small>{{ column.weekday }}</small>
            </span>
          </div>
        </div>

        <div v-for="row in rows" :key="row.deviceId" class="schedule-timeline-row">
          <button class="schedule-device-cell" type="button" @click="emit('select-device', row)">
            <span class="schedule-device-icon"><Icon icon="ep:camera" /></span>
            <span class="schedule-device-copy">
              <strong>{{ row.deviceNo }}</strong>
              <small>{{ row.equipmentModelCode }}</small>
              <span class="schedule-device-meta">
                {{ statusLabel(row.deviceStatus) }}
                <template v-if="row.logisticsStatus">
                  · {{ logisticsLabel(row.logisticsStatus) }}
                </template>
              </span>
            </span>
            <el-tag v-if="row.deviceStatus === 'MAINTENANCE'" type="warning" size="small" effect="plain">
              {{ t('rental.schedule.locked') }}
            </el-tag>
          </button>
          <div class="schedule-timeline-track schedule-timeline-track--row">
            <button
              v-for="segment in row.segments"
              :key="segment.scheduleId || `${row.deviceId}-${segment.occupyStartDate}`"
              class="schedule-segment"
              :class="segmentClass(segment)"
              type="button"
              :style="segmentStyle(segment)"
              :title="segmentTitle(segment)"
              @click="emit('select-segment', row, segment)"
            >
              <span v-if="getDisplaySegment(segment).continuesLeft" class="schedule-continuation">
                ‹
              </span>
              <span class="schedule-segment-label">{{ segment.label || segmentKindLabel(segment) }}</span>
              <span v-if="getDisplaySegment(segment).continuesRight" class="schedule-continuation">
                ›
              </span>
              <i
                v-if="getBillableDisplaySegment(segment)"
                class="schedule-billable-marker"
                :style="billableStyle(segment)"
                aria-hidden="true"
              />
            </button>
            <span v-if="row.segments.length === 0" class="schedule-available">
              <Icon icon="ep:circle-check" />
              {{ t('rental.schedule.noEffectiveOccupancy') }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="schedule-timeline-legend">
      <span><i class="legend-dot legend-dot--occupied" />{{ t('rental.schedule.legendOccupied') }}</span>
      <span><i class="legend-dot legend-dot--transit" />{{ t('rental.schedule.legendTransit') }}</span>
      <span><i class="legend-dot legend-dot--inspection" />{{ t('rental.schedule.legendInspection') }}</span>
      <span><i class="legend-dot legend-dot--locked" />{{ t('rental.schedule.legendLocked') }}</span>
      <span><i class="legend-line" />{{ t('rental.schedule.legendBillable') }}</span>
      <span>{{ t('rental.schedule.continuationLegend') }}</span>
    </div>

    <el-pagination
      v-if="total > 0"
      v-model:current-page="currentPage"
      v-model:page-size="currentPageSize"
      class="schedule-pagination"
      background
      layout="total, sizes, prev, pager, next"
      :page-sizes="[25, 50, 100]"
      :total="total"
      @size-change="emit('page-change')"
      @current-change="emit('page-change')"
    />
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import { getRentalLabelKey } from '@/utils/rentalLabels'
import type {
  RentalScheduleDeviceLaneVO,
  RentalScheduleSegmentVO,
  ScheduleWindowDays
} from '@/api/rental/schedule'
import {
  buildDevicePageRange,
  buildTimelineColumns,
  buildTimelineMonthGroups,
  formatOccupyRange,
  getBillableSegment,
  getTimelineSegment,
  type TimelineBounds
} from '../scheduleModel'

const props = defineProps<{
  bounds: TimelineBounds
  rows: RentalScheduleDeviceLaneVO[]
  total: number
  loading: boolean
  pageNo: number
  pageSize: number
  windowDays: ScheduleWindowDays
}>()

const emit = defineEmits<{
  previous: []
  next: []
  today: []
  'update:windowDays': [value: ScheduleWindowDays]
  'select-device': [row: RentalScheduleDeviceLaneVO]
  'select-segment': [row: RentalScheduleDeviceLaneVO, segment: RentalScheduleSegmentVO]
  'page-change': []
  'update:pageNo': [value: number]
  'update:pageSize': [value: number]
}>()

const { t } = useI18n()
const windowOptions: ScheduleWindowDays[] = [14, 30, 90]
const currentPage = computed({
  get: () => props.pageNo,
  set: (value: number) => emit('update:pageNo', value)
})
const currentPageSize = computed({
  get: () => props.pageSize,
  set: (value: number) => emit('update:pageSize', value)
})
const columns = computed(() => buildTimelineColumns(props.bounds))
const monthGroups = computed(() => buildTimelineMonthGroups(columns.value))
const timelineWidth = computed(() => Math.max(props.bounds.days * 48, 900))
const pageRange = computed(() =>
  buildDevicePageRange(props.total, props.pageNo, props.pageSize)
)

const onWindowDaysChange = (value: string | number | boolean | undefined) => {
  const nextValue = Number(value)
  if (windowOptions.includes(nextValue as ScheduleWindowDays)) {
    emit('update:windowDays', nextValue as ScheduleWindowDays)
  }
}

const statusLabel = (value: string) => {
  const key = getRentalLabelKey('device', value)
  const translated = t(key, { code: value })
  return translated === key ? value : translated
}

const logisticsLabel = (value: string) => {
  const key = `rental.schedule.logistics.${value}`
  const translated = t(key)
  return translated === key ? value : translated
}

const segmentKindLabel = (segment: RentalScheduleSegmentVO) => {
  const key = `rental.schedule.segmentKinds.${segment.segmentType || 'OCCUPIED'}`
  const translated = t(key)
  return translated === key ? t('rental.schedule.occupied') : translated
}

const getDisplaySegment = (segment: RentalScheduleSegmentVO) =>
  getTimelineSegment(segment.occupyStartDate, segment.occupyEndDateExclusive, props.bounds)

const getBillableDisplaySegment = (segment: RentalScheduleSegmentVO) =>
  getBillableSegment(segment, props.bounds)

const segmentStyle = (segment: RentalScheduleSegmentVO) => {
  const display = getDisplaySegment(segment)
  return {
    left: `${display.left}%`,
    width: `${display.width}%`
  }
}

const billableStyle = (segment: RentalScheduleSegmentVO) => {
  const display = getBillableDisplaySegment(segment)
  if (!display) return {}
  return {
    left: `${display.left}%`,
    width: `${display.width}%`
  }
}

const segmentClass = (segment: RentalScheduleSegmentVO) => {
  const kind = segment.segmentType || 'OCCUPIED'
  return {
    'is-transit': ['OUTBOUND_TRANSIT', 'RETURN_TRANSIT'].includes(kind),
    'is-inspection': kind === 'RETURN_INSPECTION',
    'is-locked': kind === 'LOCKED',
    'is-customer': kind === 'CUSTOMER_POSSESSION',
    'is-pending-plan': kind === 'PENDING_PLAN',
    'is-continued-left': getDisplaySegment(segment).continuesLeft,
    'is-continued-right': getDisplaySegment(segment).continuesRight
  }
}

const segmentTitle = (segment: RentalScheduleSegmentVO) => {
  const range = formatOccupyRange(segment.occupyStartDate, segment.occupyEndDateExclusive)
  return [segment.label || segmentKindLabel(segment), range, segment.logisticsStatus]
    .filter(Boolean)
    .join(' · ')
}
</script>

<style scoped>
.schedule-timeline-card {
  min-width: 0;
  padding: 20px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
}

.schedule-timeline-heading,
.schedule-device-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.schedule-timeline-heading {
  margin-bottom: 18px;
}

.schedule-kicker {
  color: var(--el-color-primary);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.schedule-timeline-heading h3 {
  margin: 3px 0 2px;
  color: var(--el-text-color-primary);
  font-size: 18px;
}

.schedule-timeline-heading p {
  max-width: 760px;
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.schedule-window-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.schedule-device-toolbar {
  min-height: 38px;
  padding: 10px 12px;
  margin-bottom: 10px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  align-items: center;
}

.schedule-device-summary {
  display: flex;
  align-items: center;
  gap: 10px;
}

.schedule-device-summary span,
.schedule-range-label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.schedule-range-label {
  font-variant-numeric: tabular-nums;
}

.schedule-timeline-scroll {
  max-width: 100%;
  overflow-x: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.schedule-timeline-board {
  min-width: calc(260px + var(--timeline-width));
  background: var(--el-bg-color);
}

.schedule-timeline-months,
.schedule-timeline-days,
.schedule-timeline-row {
  display: grid;
  grid-template-columns: 260px var(--timeline-width);
}

.schedule-timeline-months,
.schedule-timeline-days {
  position: sticky;
  z-index: 1;
  background: var(--el-fill-color-light);
}

.schedule-timeline-months {
  top: 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.schedule-timeline-days {
  top: 35px;
  border-bottom: 1px solid var(--el-border-color);
}

.schedule-timeline-corner {
  display: flex;
  min-height: 35px;
  padding: 0 12px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  align-items: center;
}

.schedule-timeline-track {
  display: grid;
  min-width: 0;
  width: var(--timeline-width);
}

.schedule-timeline-track--months {
  grid-template-columns: repeat(var(--timeline-days), minmax(0, 1fr));
}

.schedule-timeline-track--days {
  grid-template-columns: repeat(var(--timeline-days), minmax(0, 1fr));
}

.schedule-month-cell {
  min-height: 35px;
  padding: 8px 6px;
  overflow: hidden;
  border-left: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-primary);
  font-size: 12px;
  font-weight: 600;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-day-cell {
  display: flex;
  min-height: 42px;
  border-left: 1px solid var(--el-border-color-lighter);
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
}

.schedule-day-cell strong {
  color: var(--el-text-color-primary);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.schedule-day-cell small {
  color: var(--el-text-color-secondary);
  font-size: 10px;
}

.schedule-day-cell.is-today {
  background: var(--el-color-primary-light-9);
}

.schedule-timeline-row {
  min-height: 72px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.schedule-timeline-row:last-child {
  border-bottom: 0;
}

.schedule-device-cell {
  display: flex;
  min-width: 0;
  padding: 10px 12px;
  background: transparent;
  border: 0;
  border-right: 1px solid var(--el-border-color-lighter);
  color: inherit;
  cursor: pointer;
  align-items: center;
  gap: 9px;
  text-align: left;
}

.schedule-device-cell:hover {
  background: var(--el-fill-color-light);
}

.schedule-device-icon {
  display: grid;
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-radius: 7px;
  place-items: center;
}

.schedule-device-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 1px;
}

.schedule-device-copy strong,
.schedule-device-copy small,
.schedule-device-meta {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-device-copy strong {
  color: var(--el-text-color-primary);
  font-family: var(--el-font-family);
  font-size: 13px;
}

.schedule-device-copy small,
.schedule-device-meta {
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.schedule-device-meta {
  font-size: 10px;
}

.schedule-timeline-track--row {
  position: relative;
  display: block;
  min-height: 72px;
  background-image: linear-gradient(
    to right,
    transparent calc(100% - 1px),
    var(--el-border-color-extra-light) calc(100% - 1px)
  );
  background-size: calc(100% / v-bind('bounds.days')) 100%;
}

.schedule-segment {
  position: absolute;
  top: 21px;
  display: flex;
  min-width: 0;
  height: 30px;
  padding: 0 8px;
  overflow: hidden;
  background: var(--el-color-primary-light-8);
  border: 1px solid var(--el-color-primary-light-5);
  border-radius: 6px;
  color: var(--el-color-primary-dark-2);
  cursor: pointer;
  align-items: center;
  gap: 2px;
  text-align: left;
  white-space: nowrap;
}

.schedule-segment:hover {
  z-index: 2;
  box-shadow: 0 0 0 2px var(--el-color-primary-light-7);
}

.schedule-segment.is-transit {
  background: var(--el-color-warning-light-8);
  border-color: var(--el-color-warning-light-5);
  color: var(--el-color-warning-dark-2);
}

.schedule-segment.is-inspection {
  background: var(--el-color-info-light-8);
  border-color: var(--el-color-info-light-5);
  color: var(--el-color-info-dark-2);
}

.schedule-segment.is-locked {
  background: var(--el-fill-color);
  border-color: var(--el-border-color);
  color: var(--el-text-color-secondary);
}

.schedule-segment.is-customer {
  background: var(--el-color-success-light-8);
  border-color: var(--el-color-success-light-5);
  color: var(--el-color-success-dark-2);
}

.schedule-segment.is-pending-plan {
  background: var(--el-color-danger-light-9);
  border-color: var(--el-color-danger-light-5);
  color: var(--el-color-danger-dark-2);
  border-style: dashed;
}

.schedule-segment.is-continued-left {
  border-left-style: dashed;
}

.schedule-segment.is-continued-right {
  border-right-style: dashed;
}

.schedule-segment-label {
  overflow: hidden;
  font-size: 11px;
  font-weight: 600;
  text-overflow: ellipsis;
}

.schedule-continuation {
  flex: 0 0 auto;
  font-size: 17px;
  line-height: 1;
}

.schedule-billable-marker {
  position: absolute;
  bottom: 3px;
  height: 3px;
  background: var(--el-color-success);
  border-radius: 2px;
}

.schedule-available {
  display: flex;
  height: 72px;
  padding-left: 14px;
  color: var(--el-color-success);
  font-size: 11px;
  align-items: center;
  gap: 5px;
}

.schedule-timeline-legend {
  display: flex;
  padding-top: 12px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
  gap: 14px;
  flex-wrap: wrap;
}

.schedule-timeline-legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.legend-dot {
  width: 9px;
  height: 9px;
  border-radius: 3px;
}

.legend-dot--occupied {
  background: var(--el-color-primary-light-8);
  border: 1px solid var(--el-color-primary-light-5);
}

.legend-dot--transit {
  background: var(--el-color-warning-light-8);
  border: 1px solid var(--el-color-warning-light-5);
}

.legend-dot--inspection {
  background: var(--el-color-info-light-8);
  border: 1px solid var(--el-color-info-light-5);
}

.legend-dot--locked {
  background: var(--el-fill-color);
  border: 1px solid var(--el-border-color);
}

.legend-line {
  width: 14px;
  height: 3px;
  background: var(--el-color-success);
  border-radius: 2px;
}

.schedule-pagination {
  justify-content: flex-end;
}

@media (width <= 900px) {
  .schedule-timeline-heading,
  .schedule-device-toolbar {
    flex-direction: column;
  }

  .schedule-window-actions {
    justify-content: flex-start;
  }
}
</style>
