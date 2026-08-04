<template>
  <ContentWrap class="schedule-content-wrap">
    <div class="schedule-shell">
      <div class="schedule-page-heading">
        <div>
          <h2>{{ t('rental.schedule.pageTitle') }}</h2>
          <p>{{ t('rental.schedule.rangeHint') }}</p>
        </div>
        <div class="schedule-page-meta">
          <span>{{ t('rental.schedule.visibleRange') }}</span>
          <strong>{{ visibleRangeLabel }}</strong>
        </div>
      </div>

      <section class="schedule-metrics" :aria-label="t('rental.schedule.overview')">
        <article class="schedule-metric schedule-metric--primary">
          <span class="schedule-metric__icon"><Icon icon="ep:data-line" /></span>
          <div>
            <small>{{ t('rental.schedule.matchingSchedules') }}</small>
            <strong>{{ total }}</strong>
          </div>
        </article>
        <article class="schedule-metric">
          <span class="schedule-metric__icon"><Icon icon="ep:circle-check" /></span>
          <div>
            <small>{{ t('rental.schedule.currentPageEffective') }}</small>
            <strong>{{ currentPageEffective }}</strong>
          </div>
        </article>
        <article class="schedule-metric">
          <span class="schedule-metric__icon"><Icon icon="ep:camera" /></span>
          <div>
            <small>{{ t('rental.schedule.currentPageDevices') }}</small>
            <strong>{{ currentPageDeviceCount }}</strong>
          </div>
        </article>
        <article class="schedule-metric">
          <span class="schedule-metric__icon"><Icon icon="ep:timer" /></span>
          <div>
            <small>{{ t('rental.schedule.visibleDays') }}</small>
            <strong>{{ visibleDays }}</strong>
          </div>
        </article>
      </section>

      <el-alert
        v-if="loadError"
        class="schedule-alert"
        type="error"
        :closable="false"
        :title="t('rental.common.loadError')"
      >
        <el-button link type="primary" @click="getList">
          {{ t('rental.common.retry') }}
        </el-button>
      </el-alert>

      <section class="schedule-filter-panel">
        <div class="schedule-section-heading">
          <div>
            <h3>{{ t('rental.schedule.filterTitle') }}</h3>
          </div>
          <p>{{ t('rental.schedule.filterHint') }}</p>
        </div>

        <el-form class="schedule-filter-form" :inline="true" :model="queryParams" @submit.prevent>
          <el-form-item :label="t('rental.schedule.deviceId')">
            <el-input-number
              v-model="queryParams.deviceId"
              class="!w-150px"
              :min="1"
              controls-position="right"
              :placeholder="t('rental.schedule.deviceId')"
            />
          </el-form-item>
          <el-form-item :label="t('rental.schedule.status')">
            <el-select
              v-model="queryParams.status"
              class="!w-160px"
              clearable
              :placeholder="t('common.selectText')"
            >
              <el-option
                v-for="option in statusOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('rental.schedule.occupyRange')">
            <el-date-picker
              v-model="occupyRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              class="!w-300px"
              :range-separator="t('rental.schedule.rangeSeparator')"
              :start-placeholder="t('rental.schedule.rangeStart')"
              :end-placeholder="t('rental.schedule.rangeEnd')"
            />
          </el-form-item>
          <el-form-item class="schedule-filter-actions">
            <el-button type="primary" @click="handleQuery">
              <Icon icon="ep:search" class="mr-5px" />{{ t('common.query') }}
            </el-button>
            <el-button @click="resetQuery">{{ t('common.reset') }}</el-button>
            <el-button class="schedule-advanced-button" text @click="showAdvanced = !showAdvanced">
              {{
                showAdvanced ? t('rental.schedule.hideAdvanced') : t('rental.schedule.showAdvanced')
              }}
              <Icon :icon="showAdvanced ? 'ep:arrow-up' : 'ep:arrow-down'" class="ml-4px" />
            </el-button>
          </el-form-item>
        </el-form>

        <el-collapse-transition>
          <el-form
            v-show="showAdvanced"
            class="schedule-advanced-form"
            :inline="true"
            :model="queryParams"
            @submit.prevent
          >
            <el-form-item :label="t('rental.schedule.rentalOrderId')">
              <el-input-number
                v-model="queryParams.rentalOrderId"
                class="!w-180px"
                :min="1"
                controls-position="right"
              />
            </el-form-item>
          </el-form>
        </el-collapse-transition>
      </section>

      <section class="schedule-list-panel">
        <div class="schedule-list-heading">
          <div>
            <h3>{{ t('rental.schedule.timelineTitle') }}</h3>
          </div>
          <div class="schedule-legend">
            <span
              ><i class="schedule-legend__dot schedule-legend__dot--occupy"></i
              >{{ t('rental.schedule.occupyWindow') }}</span
            >
            <span
              ><i class="schedule-legend__dot schedule-legend__dot--billable"></i
              >{{ t('rental.schedule.billableWindow') }}</span
            >
          </div>
        </div>

        <div class="schedule-timeline-scale">
          <span>{{ formatDisplayDate(timelineBounds.start) }}</span>
          <span>{{
            formatDisplayDate(inclusiveEndFromExclusive(timelineBounds.endExclusive))
          }}</span>
        </div>

        <el-table
          v-loading="loading"
          class="schedule-table"
          :data="list"
          :row-class-name="getRowClassName"
        >
          <el-table-column :label="t('rental.schedule.device')" min-width="210" fixed>
            <template #default="{ row }">
              <div class="schedule-device">
                <span class="schedule-device__glyph"><Icon icon="ep:camera" /></span>
                <div>
                  <strong>{{ row.deviceNo || `#${row.deviceId}` }}</strong>
                  <small>{{ row.equipmentModelCode || t('rental.schedule.modelUnknown') }}</small>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="t('rental.schedule.status')" width="110">
            <template #default="{ row }">
              <el-tag effect="light" round :type="getRentalTagType('schedule', row.status)">
                {{ t(getRentalLabelKey('schedule', row.status)) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('rental.schedule.occupyWindow')" min-width="330">
            <template #default="{ row }">
              <div class="schedule-window">
                <div class="schedule-window__heading">
                  <strong>{{
                    formatOccupyRange(row.occupyStartDate, row.occupyEndDateExclusive)
                  }}</strong>
                  <span
                    >{{ occupyDays(row.occupyStartDate, row.occupyEndDateExclusive) }}
                    {{ t('rental.schedule.days') }}</span
                  >
                </div>
                <div class="schedule-track" aria-hidden="true">
                  <i
                    class="schedule-track__fill schedule-track__fill--occupy"
                    :style="getSegmentStyle(row.occupyStartDate, row.occupyEndDateExclusive)"
                  ></i>
                </div>
                <small>{{ t('rental.schedule.occupyRule') }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="t('rental.schedule.billableWindow')" min-width="300">
            <template #default="{ row }">
              <div v-if="row.billableStartDate && row.billableEndDate" class="schedule-window">
                <div class="schedule-window__heading">
                  <strong>{{
                    formatClosedRange(row.billableStartDate, row.billableEndDate)
                  }}</strong>
                  <span
                    >{{ closedRangeDays(row.billableStartDate, row.billableEndDate) }}
                    {{ t('rental.schedule.days') }}</span
                  >
                </div>
                <div class="schedule-track" aria-hidden="true">
                  <i
                    class="schedule-track__fill schedule-track__fill--billable"
                    :style="getBillableSegmentStyle(row.billableStartDate, row.billableEndDate)"
                  ></i>
                </div>
                <small>{{ t('rental.schedule.billableRule') }}</small>
              </div>
              <span v-else class="schedule-empty-value">{{ t('rental.schedule.notSet') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('rental.schedule.orderInfo')" min-width="170">
            <template #default="{ row }">
              <div class="schedule-order">
                <strong>
                  {{ row.rentalOrderId ? `#${row.rentalOrderId}` : t('rental.schedule.notSet') }}
                </strong>
                <small v-if="row.rentalOrderItemId">
                  {{ t('rental.schedule.orderItemShort') }} #{{ row.rentalOrderItemId }}
                </small>
              </div>
            </template>
          </el-table-column>
          <template #empty>
            <div class="schedule-empty">
              <span><Icon icon="ep:calendar" /></span>
              <strong>{{ t('rental.schedule.emptyTitle') }}</strong>
              <p>{{ t('rental.schedule.emptyHint') }}</p>
            </div>
          </template>
        </el-table>

        <Pagination
          :total="total"
          v-model:page="queryParams.pageNo"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </section>
    </div>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import {
  getRentalSchedulePage,
  type RentalSchedulePageReqVO,
  type RentalScheduleVO
} from '@/api/rental/schedule'
import { getRentalLabelKey, getRentalStatusValues, getRentalTagType } from '@/utils/rentalLabels'
import {
  buildTimelineBounds,
  closedRangeDays,
  formatClosedRange,
  formatDisplayDate,
  formatOccupyRange,
  getTimelineSegment,
  inclusiveEndFromExclusive,
  occupyDays,
  toExclusiveEnd,
  toPickerOccupyRange,
  toQueryOccupyRange
} from './scheduleModel'

defineOptions({ name: 'RentalSchedule' })
const { t } = useI18n()
const route = useRoute()

const loading = ref(false)
const loadError = ref(false)
const showAdvanced = ref(false)
const list = ref<RentalScheduleVO[]>([])
const total = ref(0)
const queryParams = reactive<RentalSchedulePageReqVO>({ pageNo: 1, pageSize: 10 })
const occupyRange = ref<[string, string]>()

const statusOptions = computed(() =>
  getRentalStatusValues('schedule').map((value) => ({
    value,
    label: t(getRentalLabelKey('schedule', value))
  }))
)

const currentPageEffective = computed(
  () => list.value.filter((row) => row.status === 'EFFECTIVE').length
)
const currentPageDeviceCount = computed(() => new Set(list.value.map((row) => row.deviceId)).size)
const timelineBounds = computed(() => buildTimelineBounds(list.value, occupyRange.value))
const visibleDays = computed(() =>
  occupyDays(timelineBounds.value.start, timelineBounds.value.endExclusive)
)
const visibleRangeLabel = computed(() =>
  formatOccupyRange(timelineBounds.value.start, timelineBounds.value.endExclusive)
)

watch(
  occupyRange,
  (range) => {
    const queryRange = toQueryOccupyRange(range)
    queryParams.occupyStartDate = queryRange?.[0]
    queryParams.occupyEndDateExclusive = queryRange?.[1]
  },
  { deep: true }
)

const getList = async () => {
  loading.value = true
  loadError.value = false
  try {
    const data = await getRentalSchedulePage(queryParams)
    list.value = data.list
    total.value = data.total
  } catch {
    list.value = []
    total.value = 0
    loadError.value = true
  } finally {
    loading.value = false
  }
}

const handleQuery = async () => {
  queryParams.pageNo = 1
  await getList()
}

const resetQuery = async () => {
  queryParams.deviceId = undefined
  queryParams.rentalOrderId = undefined
  queryParams.status = undefined
  queryParams.occupyStartDate = undefined
  queryParams.occupyEndDateExclusive = undefined
  occupyRange.value = undefined
  queryParams.pageNo = 1
  showAdvanced.value = false
  await getList()
}

const getSegmentStyle = (start: string, endExclusive: string) => {
  const segment = getTimelineSegment(
    start,
    endExclusive,
    timelineBounds.value.start,
    timelineBounds.value.endExclusive
  )
  return {
    '--segment-left': `${segment.left}%`,
    '--segment-width': `${segment.width}%`
  }
}

const getBillableSegmentStyle = (start: string, endInclusive: string) =>
  getSegmentStyle(start, toExclusiveEnd(endInclusive))

const getRowClassName = ({ row }: { row: RentalScheduleVO }) =>
  row.status === 'CANCELLED' ? 'is-cancelled' : ''

onMounted(() => {
  const deviceId = Number(route.query.deviceId)
  queryParams.deviceId = Number.isInteger(deviceId) && deviceId > 0 ? deviceId : undefined
  const start =
    typeof route.query.occupyStartDate === 'string' ? route.query.occupyStartDate : undefined
  const endExclusive =
    typeof route.query.occupyEndDateExclusive === 'string'
      ? route.query.occupyEndDateExclusive
      : undefined
  queryParams.occupyStartDate = start
  queryParams.occupyEndDateExclusive = endExclusive
  occupyRange.value = toPickerOccupyRange(start, endExclusive)

  const orderId = Number(route.query.rentalOrderId)
  if (Number.isInteger(orderId) && orderId > 0) {
    queryParams.rentalOrderId = orderId
    showAdvanced.value = true
  }
  return getList()
})
</script>

<style scoped>
.schedule-content-wrap {
  --schedule-ink: var(--el-text-color-primary);
  --schedule-muted: var(--el-text-color-secondary);
  --schedule-blue: var(--el-color-primary);
  --schedule-mint: var(--el-color-success);
  --schedule-surface: var(--el-fill-color-blank);
}

.schedule-shell {
  display: flex;
  font-family: inherit;
  color: var(--schedule-ink);
  flex-direction: column;
  gap: 18px;
}

.schedule-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.schedule-metric {
  display: flex;
  min-height: 88px;
  padding: 18px;
  background: var(--schedule-surface);
  border: 1px solid rgb(216 227 236 / 88%);
  border-radius: 19px;
  box-shadow: 0 14px 34px rgb(31 58 82 / 7%);
  align-items: center;
  gap: 14px;
  animation: schedule-rise 360ms ease-out both;
}

.schedule-metric:nth-child(2) {
  animation-delay: 40ms;
}

.schedule-metric:nth-child(3) {
  animation-delay: 80ms;
}

.schedule-metric:nth-child(4) {
  animation-delay: 120ms;
}

.schedule-metric__icon {
  display: grid;
  width: 42px;
  height: 42px;
  color: #1b6fa9;
  background: #eaf5fc;
  border-radius: 14px;
  place-items: center;
}

.schedule-metric--primary .schedule-metric__icon {
  color: #fff;
  background: #409eff;
  box-shadow: none;
}

.schedule-metric__icon > .iconify {
  width: 20px;
  height: 20px;
}

.schedule-metric div {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.schedule-metric small {
  overflow: hidden;
  font-size: 12px;
  color: var(--schedule-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-metric strong {
  font-family: inherit;
  font-size: 25px;
  font-variant-numeric: tabular-nums;
  line-height: 1.05;
}

.schedule-alert {
  border-radius: 16px;
}

.schedule-filter-panel,
.schedule-list-panel {
  padding: 22px;
  background: var(--schedule-surface);
  border: 1px solid rgb(216 227 236 / 90%);
  border-radius: 22px;
  box-shadow: 0 18px 44px rgb(31 58 82 / 8%);
}

.schedule-section-heading,
.schedule-list-heading {
  display: flex;
  margin-bottom: 18px;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}

.schedule-section-heading span {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.13em;
  color: #2878b5;
  text-transform: uppercase;
}

.schedule-section-heading h3,
.schedule-list-heading h3 {
  margin: 4px 0 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.schedule-section-heading p {
  max-width: 520px;
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--schedule-muted);
  text-align: right;
}

.schedule-filter-form {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.schedule-filter-form :deep(.el-form-item) {
  margin: 0;
}

.schedule-filter-form :deep(.el-form-item__label),
.schedule-advanced-form :deep(.el-form-item__label) {
  padding-bottom: 4px;
  font-size: 12px;
  font-weight: 650;
  line-height: 1.3;
  color: #42586d;
}

.schedule-filter-form :deep(.el-input__wrapper),
.schedule-filter-form :deep(.el-select__wrapper),
.schedule-filter-form :deep(.el-range-editor.el-input__wrapper),
.schedule-advanced-form :deep(.el-input__wrapper) {
  min-height: 40px;
  background: rgb(246 249 252 / 92%);
  border: 1px solid transparent;
  border-radius: 12px;
  box-shadow: none;
  transition:
    background 160ms ease,
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.schedule-filter-form :deep(.el-input__wrapper:hover),
.schedule-filter-form :deep(.el-select__wrapper:hover),
.schedule-filter-form :deep(.el-range-editor.el-input__wrapper:hover),
.schedule-advanced-form :deep(.el-input__wrapper:hover) {
  background: #fff;
  border-color: #bcd9eb;
}

.schedule-filter-form :deep(.is-focus),
.schedule-advanced-form :deep(.is-focus) {
  background: #fff;
  border-color: #69b7ea;
  box-shadow: 0 0 0 4px rgb(10 132 255 / 10%);
}

.schedule-filter-actions {
  margin-left: auto !important;
}

.schedule-filter-actions :deep(.el-button) {
  min-height: 40px;
  border-radius: 12px;
}

.schedule-filter-actions :deep(.el-button:active) {
  transform: scale(0.97);
}

.schedule-filter-actions :deep(.el-button--primary) {
  padding-inline: 20px;
  background: #409eff;
  border: 1px solid #409eff;
  box-shadow: none;
}

.schedule-advanced-button {
  color: #276f9f;
}

.schedule-advanced-form {
  padding: 16px 16px 4px;
  margin-top: 16px;
  background: rgb(233 243 249 / 70%);
  border: 1px solid rgb(190 216 231 / 68%);
  border-radius: 16px;
}

.schedule-list-heading {
  align-items: center;
}

.schedule-legend {
  display: flex;
  padding: 8px 12px;
  font-size: 12px;
  color: var(--schedule-muted);
  background: #f3f7fa;
  border-radius: 999px;
  gap: 14px;
}

.schedule-legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.schedule-legend__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.schedule-legend__dot--occupy {
  background: var(--schedule-blue);
  box-shadow: 0 0 0 4px rgb(10 132 255 / 11%);
}

.schedule-legend__dot--billable {
  background: var(--schedule-mint);
  box-shadow: 0 0 0 4px rgb(36 163 122 / 11%);
}

.schedule-timeline-scale {
  display: flex;
  padding: 0 12px 8px 337px;
  font-size: 10px;
  font-variant-numeric: tabular-nums;
  color: #8a99a8;
  justify-content: space-between;
}

.schedule-table {
  --el-table-border-color: transparent;
  --el-table-header-bg-color: #f4f8fb;
  --el-table-row-hover-bg-color: #f2f8fc;

  overflow: hidden;
  border: 1px solid #e3edf3;
  border-radius: 17px;
}

.schedule-table :deep(th.el-table__cell) {
  height: 48px;
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.05em;
  color: #5c7184;
  text-transform: uppercase;
}

.schedule-table :deep(td.el-table__cell) {
  height: 92px;
  border-bottom: 1px solid #edf2f6;
}

.schedule-table :deep(.el-table__row) {
  transition:
    background 160ms ease,
    opacity 160ms ease;
}

.schedule-table :deep(.el-table__row.is-cancelled) {
  opacity: 0.58;
}

.schedule-device {
  display: flex;
  align-items: center;
  gap: 12px;
}

.schedule-device__glyph {
  display: grid;
  width: 42px;
  height: 42px;
  color: #176fa9;
  background: #ecf5ff;
  border: 1px solid #d5e8f4;
  border-radius: 14px;
  place-items: center;
}

.schedule-device__glyph > .iconify {
  width: 19px;
  height: 19px;
}

.schedule-device div,
.schedule-order {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.schedule-device strong,
.schedule-order strong {
  overflow: hidden;
  font-size: 14px;
  font-weight: 700;
  color: #17344c;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-device small,
.schedule-order small {
  overflow: hidden;
  font-size: 11px;
  color: var(--schedule-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-window {
  display: flex;
  min-width: 240px;
  flex-direction: column;
  gap: 8px;
}

.schedule-window__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.schedule-window__heading strong {
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  color: #28465e;
  white-space: nowrap;
}

.schedule-window__heading span {
  padding: 3px 7px;
  font-size: 10px;
  font-weight: 650;
  color: #5c7184;
  white-space: nowrap;
  background: #edf3f7;
  border-radius: 999px;
}

.schedule-window > small {
  font-size: 10px;
  color: #8a99a8;
}

.schedule-track {
  position: relative;
  height: 8px;
  overflow: hidden;
  background: #f5f7fa;
  border-radius: 999px;
}

.schedule-track__fill {
  position: absolute;
  top: 0;
  bottom: 0;
  left: var(--segment-left);
  width: var(--segment-width);
  min-width: 5px;
  border-radius: 999px;
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 45%);
}

.schedule-track__fill--occupy {
  background: #409eff;
}

.schedule-track__fill--billable {
  background: #67c23a;
}

.schedule-empty-value {
  color: #9aa8b5;
}

.schedule-empty {
  display: flex;
  min-height: 230px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.schedule-empty > span {
  display: grid;
  width: 56px;
  height: 56px;
  margin-bottom: 14px;
  color: #377fae;
  background: #eaf4fa;
  border-radius: 19px;
  place-items: center;
}

.schedule-empty > span > .iconify {
  width: 24px;
  height: 24px;
}

.schedule-empty strong {
  font-size: 15px;
  color: #28465e;
}

.schedule-empty p {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--schedule-muted);
}

@keyframes schedule-rise {
  from {
    opacity: 0;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (width <= 1180px) {
  .schedule-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .schedule-filter-actions {
    width: 100%;
    margin-left: 0 !important;
  }

  .schedule-timeline-scale {
    display: none;
  }
}

@media (width <= 760px) {
  .schedule-metrics {
    grid-template-columns: 1fr;
  }

  .schedule-filter-panel,
  .schedule-list-panel {
    padding: 16px;
    border-radius: 18px;
  }

  .schedule-section-heading,
  .schedule-list-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .schedule-section-heading p {
    text-align: left;
  }

  .schedule-filter-form {
    align-items: stretch;
    flex-direction: column;
  }

  .schedule-filter-form :deep(.el-form-item),
  .schedule-filter-form :deep(.el-form-item__content),
  .schedule-filter-form :deep(.el-input-number),
  .schedule-filter-form :deep(.el-select),
  .schedule-filter-form :deep(.el-date-editor) {
    width: 100% !important;
  }

  .schedule-filter-actions :deep(.el-form-item__content) {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .schedule-advanced-button {
    grid-column: 1 / -1;
  }

  .schedule-legend {
    width: 100%;
    justify-content: space-between;
  }
}

@media (prefers-reduced-motion: reduce) {
  .schedule-metric {
    animation: none;
  }

  .schedule-filter-actions :deep(.el-button:active) {
    transform: none;
  }
}

/* Keep the page aligned with the flat Element Plus admin surfaces. */
.schedule-shell {
  gap: 16px;
  font-family: inherit;
  color: var(--el-text-color-primary);
}

.schedule-page-heading {
  display: flex;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.schedule-page-heading h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.5;
}

.schedule-page-heading p {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
}

.schedule-page-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-align: right;
}

.schedule-page-meta strong {
  font-weight: 500;
  color: var(--el-text-color-regular);
  white-space: nowrap;
}

.schedule-metrics {
  gap: 12px;
}

.schedule-metric {
  min-height: 72px;
  padding: 12px 14px;
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--el-border-radius-base);
  box-shadow: none;
  animation: none;
}

.schedule-metric__icon,
.schedule-metric--primary .schedule-metric__icon {
  width: 32px;
  height: 32px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-radius: var(--el-border-radius-base);
  box-shadow: none;
}

.schedule-metric strong {
  font-family: inherit;
  font-size: 22px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.schedule-alert {
  border-radius: var(--el-border-radius-base);
}

.schedule-filter-panel,
.schedule-list-panel {
  padding: 0;
  background: transparent;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.schedule-filter-panel {
  padding-bottom: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.schedule-list-panel {
  padding-top: 16px;
}

.schedule-section-heading,
.schedule-list-heading {
  margin-bottom: 12px;
  align-items: center;
}

.schedule-section-heading h3,
.schedule-list-heading h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.schedule-section-heading p {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.schedule-filter-form {
  gap: 12px;
}

.schedule-filter-form :deep(.el-form-item),
.schedule-advanced-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.schedule-filter-form :deep(.el-form-item__label),
.schedule-advanced-form :deep(.el-form-item__label) {
  padding-bottom: 0;
  font-size: var(--el-form-label-font-size);
  font-weight: 400;
  color: var(--el-text-color-regular);
}

.schedule-filter-form :deep(.el-input__wrapper),
.schedule-filter-form :deep(.el-select__wrapper),
.schedule-filter-form :deep(.el-range-editor.el-input__wrapper),
.schedule-advanced-form :deep(.el-input__wrapper) {
  min-height: var(--el-component-size);
  background: var(--el-fill-color-blank);
  border: 0;
  border-radius: var(--el-border-radius-base);
  box-shadow: 0 0 0 1px var(--el-border-color) inset;
}

.schedule-filter-form :deep(.el-input__wrapper:hover),
.schedule-filter-form :deep(.el-select__wrapper:hover),
.schedule-filter-form :deep(.el-range-editor.el-input__wrapper:hover),
.schedule-advanced-form :deep(.el-input__wrapper:hover) {
  border-color: transparent;
  box-shadow: 0 0 0 1px var(--el-border-color-hover) inset;
}

.schedule-filter-form :deep(.is-focus),
.schedule-advanced-form :deep(.is-focus) {
  border-color: transparent;
  box-shadow: 0 0 0 1px var(--el-color-primary) inset;
}

.schedule-filter-actions :deep(.el-button) {
  min-height: var(--el-component-size);
  border-radius: var(--el-border-radius-base);
}

.schedule-filter-actions :deep(.el-button--primary) {
  padding-inline: 16px;
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
  box-shadow: none;
}

.schedule-filter-actions :deep(.el-button:active) {
  transform: none;
}

.schedule-advanced-form {
  padding: 12px;
  margin-top: 12px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--el-border-radius-base);
}

.schedule-legend {
  padding: 0;
  background: transparent;
  border-radius: 0;
}

.schedule-legend__dot--occupy,
.schedule-legend__dot--billable {
  box-shadow: none;
}

.schedule-table {
  --el-table-header-bg-color: var(--el-fill-color-light);
  --el-table-row-hover-bg-color: var(--el-fill-color-light);

  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--el-border-radius-base);
}

.schedule-table :deep(th.el-table__cell) {
  height: 44px;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: normal;
  color: var(--el-text-color-regular);
  text-transform: none;
}

.schedule-table :deep(td.el-table__cell) {
  height: auto;
}

.schedule-device__glyph {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--el-border-radius-base);
}

.schedule-track {
  background: var(--el-fill-color-light);
}

.schedule-track__fill--occupy {
  background: var(--el-color-primary);
  box-shadow: none;
}

.schedule-track__fill--billable {
  background: var(--el-color-success);
  box-shadow: none;
}

.schedule-empty > span {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-radius: var(--el-border-radius-base);
}

@media (width <= 760px) {
  .schedule-page-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .schedule-page-meta {
    align-items: flex-start;
    text-align: left;
  }
}
</style>
