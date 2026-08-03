<template>
  <ContentWrap class="schedule-content-wrap">
    <div class="schedule-shell">
      <section class="schedule-hero">
        <div class="schedule-hero__copy">
          <span class="schedule-kicker">{{ t('rental.schedule.kicker') }}</span>
          <h2>{{ t('rental.schedule.pageTitle') }}</h2>
          <p>{{ t('rental.schedule.rangeHint') }}</p>
        </div>
        <div class="schedule-hero__summary">
          <span>{{ t('rental.schedule.matchingSchedules') }}</span>
          <strong>{{ total }}</strong>
          <div class="schedule-hero__range">
            <Icon icon="ep:calendar" />
            <div>
              <small>{{ t('rental.schedule.visibleRange') }}</small>
              <b>{{ visibleRangeLabel }}</b>
            </div>
          </div>
        </div>
      </section>

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
            <span>{{ t('rental.schedule.filterKicker') }}</span>
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
            <span class="schedule-kicker schedule-kicker--dark">
              {{ t('rental.schedule.timelineKicker') }}
            </span>
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
  --schedule-ink: #102338;
  --schedule-muted: #66788a;
  --schedule-blue: #0a84ff;
  --schedule-mint: #24a37a;
  --schedule-surface: rgb(255 255 255 / 86%);
}

.schedule-shell {
  display: flex;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'PingFang SC', sans-serif;
  color: var(--schedule-ink);
  flex-direction: column;
  gap: 18px;
}

.schedule-hero {
  position: relative;
  display: flex;
  min-height: 220px;
  padding: 30px;
  overflow: hidden;
  color: #fff;
  background:
    radial-gradient(circle at 86% 14%, rgb(10 132 255 / 46%), transparent 31%),
    radial-gradient(circle at 64% 84%, rgb(48 209 88 / 20%), transparent 29%),
    linear-gradient(135deg, #0b1725 0%, #12314b 54%, #0b4f70 100%);
  border: 1px solid rgb(255 255 255 / 54%);
  border-radius: 26px;
  box-shadow: 0 26px 66px rgb(15 35 56 / 18%);
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  isolation: isolate;
}

.schedule-hero::before {
  position: absolute;
  z-index: -1;
  pointer-events: none;
  background-image:
    linear-gradient(rgb(255 255 255 / 7%) 1px, transparent 1px),
    linear-gradient(90deg, rgb(255 255 255 / 7%) 1px, transparent 1px);
  background-size: 32px 32px;
  border: 1px solid rgb(255 255 255 / 10%);
  border-radius: 20px;
  content: '';
  inset: 18px;
  mask-image: linear-gradient(90deg, #000 0%, transparent 78%);
}

.schedule-hero__copy {
  max-width: 650px;
}

.schedule-kicker {
  display: inline-block;
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.17em;
  color: #8ed7ff;
  text-transform: uppercase;
}

.schedule-kicker--dark {
  color: #2878b5;
}

.schedule-hero h2 {
  margin: 12px 0 14px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'PingFang SC', sans-serif;
  font-size: clamp(30px, 4vw, 44px);
  font-weight: 720;
  line-height: 1.04;
  letter-spacing: -0.035em;
}

.schedule-hero p {
  max-width: 610px;
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: rgb(232 243 250 / 76%);
}

.schedule-hero__summary {
  display: flex;
  width: min(340px, 36%);
  min-width: 290px;
  padding: 22px;
  flex-direction: column;
  background: rgb(255 255 255 / 12%);
  border: 1px solid rgb(255 255 255 / 20%);
  border-radius: 22px;
  backdrop-filter: blur(22px) saturate(150%);
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 18%);
}

.schedule-hero__summary > span {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: rgb(232 243 250 / 68%);
}

.schedule-hero__summary > strong {
  margin: 4px 0 16px;
  font-family: 'DIN Alternate', 'SF Pro Display', sans-serif;
  font-size: 46px;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.schedule-hero__range {
  display: flex;
  padding-top: 16px;
  border-top: 1px solid rgb(255 255 255 / 15%);
  align-items: center;
  gap: 12px;
}

.schedule-hero__range > .iconify {
  width: 19px;
  height: 19px;
  color: #8ed7ff;
}

.schedule-hero__range div {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.schedule-hero__range small {
  color: rgb(232 243 250 / 62%);
}

.schedule-hero__range b {
  overflow: hidden;
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  backdrop-filter: blur(18px) saturate(140%);
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
  background: linear-gradient(145deg, #1d9bf0, #0a67b2);
  box-shadow: 0 10px 22px rgb(10 103 178 / 24%);
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
  font-family: 'DIN Alternate', 'SF Pro Display', sans-serif;
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
  background:
    linear-gradient(180deg, rgb(255 255 255 / 94%), rgb(249 252 254 / 90%)), var(--schedule-surface);
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
  background: linear-gradient(145deg, #168ce0, #0868b5);
  border: 0;
  box-shadow: 0 11px 24px rgb(10 103 178 / 22%);
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
  background: linear-gradient(145deg, #eef8fe, #dceef8);
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
  background: repeating-linear-gradient(
    90deg,
    #e7eef3 0,
    #e7eef3 calc(12.5% - 1px),
    #d9e4eb calc(12.5% - 1px),
    #d9e4eb 12.5%
  );
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
  background: linear-gradient(90deg, #0a84ff, #46b5ff);
}

.schedule-track__fill--billable {
  background: linear-gradient(90deg, #1f9d74, #53c99b);
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
  .schedule-hero {
    padding: 24px;
    flex-direction: column;
    align-items: stretch;
  }

  .schedule-hero__summary {
    width: auto;
    min-width: 0;
  }

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

@media (prefers-reduced-transparency: reduce) {
  .schedule-hero__summary,
  .schedule-metric {
    backdrop-filter: none;
  }

  .schedule-hero__summary {
    background: #173b56;
  }

  .schedule-metric {
    background: #fff;
  }
}
</style>
