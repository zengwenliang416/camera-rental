<template>
  <ContentWrap class="rental-report-page">
    <section class="report-shell">
      <div class="report-hero">
        <div class="hero-copy">
          <div class="report-kicker">{{ t('rental.report.kicker') }}</div>
          <h2>{{ t('rental.report.pageTitle') }}</h2>
          <p>{{ t('rental.report.scopeHint') }}</p>
        </div>

        <div class="hero-panel">
          <div class="hero-panel__label">{{ t('rental.report.dateRange') }}</div>
          <strong>{{ reportWindowLabel }}</strong>
          <el-form :inline="true" :model="queryParams" class="report-filter">
            <el-form-item>
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                value-format="YYYY-MM-DD"
                :range-separator="t('rental.report.dateSeparator')"
                :start-placeholder="t('rental.report.startDate')"
                :end-placeholder="t('rental.report.endDate')"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="querying" @click="handleQuery">
                <Icon icon="ep:data-analysis" class="mr-5px" />
                {{ t('rental.report.refresh') }}
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <el-alert
        v-if="loadError"
        class="mb-16px"
        type="error"
        :closable="false"
        :title="t('rental.report.loadError')"
      >
        <el-button link type="primary" @click="loadAll">
          {{ t('rental.common.retry') }}
        </el-button>
      </el-alert>

      <div v-loading="overviewLoading" class="metric-grid">
        <article class="metric-card metric-card--income">
          <div class="metric-card__top">
            <span>{{ t('rental.report.rentAmountFen') }}</span>
            <i class="metric-card__icon">¥</i>
          </div>
          <strong>{{ formatYuan(overview?.rentAmountFen) }}</strong>
          <small>{{ t('rental.report.payAmountHint') }}</small>
        </article>
        <article class="metric-card metric-card--net">
          <div class="metric-card__top">
            <span>{{ t('rental.report.netIncome') }}</span>
            <i class="metric-card__icon">=</i>
          </div>
          <strong>{{ formatYuan(netIncomeFen) }}</strong>
          <small>{{ t('rental.report.netIncomeHint') }}</small>
        </article>
        <article class="metric-card">
          <div class="metric-card__top">
            <span>{{ t('rental.report.orderCount') }}</span>
            <i class="metric-card__icon">#</i>
          </div>
          <strong>{{ overview?.orderCount ?? 0 }}</strong>
          <small>{{ t('rental.report.sourceOrderHint') }}</small>
        </article>
        <article class="metric-card metric-card--utilization">
          <div class="metric-card__top">
            <span>{{ t('rental.report.utilization') }}</span>
            <i class="metric-card__icon">%</i>
          </div>
          <strong>{{ formatPercent(overview?.utilizationBasisPoints) }}</strong>
          <el-progress
            :percentage="utilizationPercent"
            :stroke-width="9"
            :show-text="false"
            color="#0f766e"
          />
          <small>{{ t('rental.report.utilizationHint') }}</small>
        </article>
        <article class="metric-card">
          <div class="metric-card__top">
            <span>{{ t('rental.report.deviceDays') }}</span>
            <i class="metric-card__icon">D</i>
          </div>
          <strong>
            {{ overview?.occupiedDeviceDays ?? 0 }} / {{ overview?.totalDeviceDays ?? 0 }}
          </strong>
          <div class="device-day-bar">
            <span :style="{ width: `${utilizationPercent}%` }"></span>
          </div>
          <small>
            {{ t('rental.report.idleDaysValue', { days: overview?.idleDeviceDays ?? 0 }) }}
          </small>
        </article>
        <article class="metric-card">
          <div class="metric-card__top">
            <span>{{ t('rental.report.assignedIncome') }}</span>
            <i class="metric-card__icon">↗</i>
          </div>
          <strong>{{ formatYuan(overview?.assignedIncomeFen) }}</strong>
          <small>{{ t('rental.report.assignedIncomeHint') }}</small>
        </article>
      </div>

      <section class="report-section report-section--source">
        <div class="section-heading">
          <div>
            <span class="section-eyebrow">{{ sourceCountLabel }}</span>
            <h3>{{ t('rental.report.sourceBreakdown') }}</h3>
          </div>
          <small>{{ t('rental.report.sourceBreakdownHint') }}</small>
        </div>
        <el-table :data="overview?.sources || []" class="report-table">
          <el-table-column :label="t('rental.report.sourceType')" min-width="160">
            <template #default="{ row }">
              <div class="source-name">
                <span></span>
                {{ sourceLabel(row.sourceType) }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="orderCount" :label="t('rental.report.orderCount')" width="130" />
          <el-table-column :label="t('rental.report.rentAmountFen')" min-width="160">
            <template #default="{ row }">{{ formatYuan(row.rentAmountFen) }}</template>
          </el-table-column>
          <el-table-column :label="t('rental.report.refundAmountFen')" min-width="160">
            <template #default="{ row }">{{ formatYuan(row.refundAmountFen) }}</template>
          </el-table-column>
          <template #empty>
            <div class="empty-state">{{ t('rental.report.sourceEmpty') }}</div>
          </template>
        </el-table>
      </section>

      <section class="report-section report-section--tabs">
        <el-tabs v-model="activeTab" class="report-tabs">
          <el-tab-pane :label="t('rental.report.productTab')" name="product">
            <el-table v-loading="productLoading" :data="productList" class="report-table">
              <el-table-column prop="shopId" :label="t('rental.report.shopId')" width="100" />
              <el-table-column
                prop="goodsTitle"
                :label="t('rental.report.goodsTitle')"
                min-width="220"
                show-overflow-tooltip
              />
              <el-table-column :label="t('rental.report.externalProductId')" min-width="160">
                <template #default="{ row }">{{
                  maskChannelIdentifier(row.externalProductId)
                }}</template>
              </el-table-column>
              <el-table-column :label="t('rental.report.externalSkuId')" min-width="150">
                <template #default="{ row }">{{
                  maskChannelIdentifier(row.externalSkuId)
                }}</template>
              </el-table-column>
              <el-table-column
                prop="orderCount"
                :label="t('rental.report.orderCount')"
                width="110"
              />
              <el-table-column
                prop="goodsQuantity"
                :label="t('rental.report.goodsQuantity')"
                width="110"
              />
              <el-table-column :label="t('rental.report.rentAmountFen')" width="150">
                <template #default="{ row }">{{ formatYuan(row.rentAmountFen) }}</template>
              </el-table-column>
              <el-table-column :label="t('rental.report.refundAmountFen')" width="140">
                <template #default="{ row }">{{ formatYuan(row.refundAmountFen) }}</template>
              </el-table-column>
              <el-table-column :label="t('table.action')" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openSourceOrders(row)">
                    {{ t('rental.report.viewSource') }}
                  </el-button>
                </template>
              </el-table-column>
              <template #empty>
                <div class="empty-state">{{ t('rental.report.productEmpty') }}</div>
              </template>
            </el-table>
            <Pagination
              :total="productTotal"
              v-model:page="productPage.pageNo"
              v-model:limit="productPage.pageSize"
              @pagination="loadProducts"
            />
          </el-tab-pane>

          <el-tab-pane :label="t('rental.report.deviceTab')" name="device">
            <el-table v-loading="deviceLoading" :data="deviceList" class="report-table">
              <el-table-column
                prop="deviceNo"
                :label="t('rental.device.deviceNo')"
                min-width="150"
              />
              <el-table-column
                prop="equipmentModelCode"
                :label="t('rental.device.modelCode')"
                min-width="170"
              />
              <el-table-column :label="t('rental.device.status')" width="120">
                <template #default="{ row }">
                  <el-tag :type="getRentalTagType('device', row.status)">
                    {{ t(getRentalLabelKey('device', row.status)) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column :label="t('rental.report.utilization')" width="120">
                <template #default="{ row }">
                  <el-progress
                    :percentage="row.utilizationBasisPoints / 100"
                    :stroke-width="8"
                    :show-text="false"
                  />
                  <span class="progress-label">{{
                    formatPercent(row.utilizationBasisPoints)
                  }}</span>
                </template>
              </el-table-column>
              <el-table-column
                prop="occupiedDays"
                :label="t('rental.report.occupiedDays')"
                width="110"
              />
              <el-table-column prop="idleDays" :label="t('rental.report.idleDays')" width="100" />
              <el-table-column
                prop="assignmentCount"
                :label="t('rental.report.assignmentCount')"
                width="110"
              />
              <el-table-column :label="t('rental.report.assignedIncome')" width="160">
                <template #default="{ row }">{{ formatYuan(row.assignedIncomeFen) }}</template>
              </el-table-column>
              <el-table-column :label="t('table.action')" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openDeviceSchedules(row)">
                    {{ t('rental.report.viewSchedules') }}
                  </el-button>
                </template>
              </el-table-column>
              <template #empty>
                <div class="empty-state">{{ t('rental.report.deviceEmpty') }}</div>
              </template>
            </el-table>
            <Pagination
              :total="deviceTotal"
              v-model:page="devicePage.pageNo"
              v-model:limit="devicePage.pageSize"
              @pagination="loadDevices"
            />
          </el-tab-pane>
        </el-tabs>
      </section>
    </section>
  </ContentWrap>
</template>

<script lang="ts" setup>
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'
import {
  getRentalDevicePerformanceReportPage,
  getRentalProductSkuReportPage,
  getRentalReportOverview,
  type RentalDevicePerformanceReportVO,
  type RentalProductSkuReportVO,
  type RentalReportOverviewVO,
  type RentalReportQuery
} from '@/api/rental/report'
import { fenToYuan } from '@/utils'
import { getRentalLabelKey, getRentalTagType } from '@/utils/rentalLabels'
import { maskChannelIdentifier } from '@/utils/rentalPrivacy'

defineOptions({ name: 'RentalBusinessReport' })

const { t } = useI18n()
const message = useMessage()
const router = useRouter()
const activeTab = ref('product')
const overviewLoading = ref(false)
const productLoading = ref(false)
const deviceLoading = ref(false)
const loadError = ref(false)
const overview = ref<RentalReportOverviewVO>()
const productList = ref<RentalProductSkuReportVO[]>([])
const deviceList = ref<RentalDevicePerformanceReportVO[]>([])
const productTotal = ref(0)
const deviceTotal = ref(0)
const productPage = reactive({ pageNo: 1, pageSize: 10 })
const devicePage = reactive({ pageNo: 1, pageSize: 10 })
const dateRange = ref<[string, string]>([
  dayjs().subtract(29, 'day').format('YYYY-MM-DD'),
  dayjs().format('YYYY-MM-DD')
])
const queryParams = computed(() => ({
  startDate: dateRange.value?.[0],
  endDate: dateRange.value?.[1]
}))
const querying = computed(
  () => overviewLoading.value || productLoading.value || deviceLoading.value
)
const netIncomeFen = computed(
  () => (overview.value?.rentAmountFen || 0) - (overview.value?.refundAmountFen || 0)
)
const utilizationPercent = computed(() =>
  Math.min(100, Math.max(0, (overview.value?.utilizationBasisPoints || 0) / 100))
)
const reportWindowLabel = computed(() => {
  const startDate = queryParams.value.startDate || '-'
  const endDate = queryParams.value.endDate || '-'
  return `${startDate} ${t('rental.report.dateSeparator')} ${endDate}`
})
const sourceCountLabel = computed(() =>
  t('rental.report.sourceCount', { count: overview.value?.sources?.length || 0 })
)

const buildQuery = (page: { pageNo: number; pageSize: number }): RentalReportQuery => ({
  startDate: queryParams.value.startDate,
  endDate: queryParams.value.endDate,
  pageNo: page.pageNo,
  pageSize: page.pageSize
})

const formatYuan = (amount?: number) => {
  return amount == null ? '-' : t('rental.common.yuanAmount', { amount: fenToYuan(amount) })
}

const formatPercent = (basisPoints?: number) => {
  return `${((basisPoints || 0) / 100).toFixed(2)}%`
}

const sourceLabel = (sourceType?: string) => {
  return sourceType === 'XIANYU' ? t('rental.report.source.XIANYU') : sourceType || '-'
}

const loadOverview = async () => {
  overviewLoading.value = true
  try {
    overview.value = await getRentalReportOverview(buildQuery({ pageNo: 1, pageSize: 1 }))
  } finally {
    overviewLoading.value = false
  }
}

const loadProducts = async () => {
  productLoading.value = true
  try {
    const data = await getRentalProductSkuReportPage(buildQuery(productPage))
    productList.value = data.list || []
    productTotal.value = data.total || 0
  } finally {
    productLoading.value = false
  }
}

const loadDevices = async () => {
  deviceLoading.value = true
  try {
    const data = await getRentalDevicePerformanceReportPage(buildQuery(devicePage))
    deviceList.value = data.list || []
    deviceTotal.value = data.total || 0
  } finally {
    deviceLoading.value = false
  }
}

const loadAll = async () => {
  loadError.value = false
  try {
    await Promise.all([loadOverview(), loadProducts(), loadDevices()])
  } catch {
    loadError.value = true
  }
}

const handleQuery = async () => {
  if (!dateRange.value?.[0] || !dateRange.value?.[1]) {
    message.warning(t('rental.report.dateRequired'))
    return
  }
  const days = dayjs(dateRange.value[1]).diff(dayjs(dateRange.value[0]), 'day') + 1
  if (days < 1 || days > 366) {
    message.warning(t('rental.report.dateInvalid'))
    return
  }
  productPage.pageNo = 1
  devicePage.pageNo = 1
  await loadAll()
}

const openSourceOrders = (row: RentalProductSkuReportVO) => {
  router.push({
    name: 'RentalChannelOrder',
    query: {
      shopId: String(row.shopId),
      externalProductId: row.externalProductId || undefined,
      externalSkuId: row.externalSkuId || undefined,
      startDate: queryParams.value.startDate,
      endDate: queryParams.value.endDate
    }
  })
}

const openDeviceSchedules = (row: RentalDevicePerformanceReportVO) => {
  router.push({
    name: 'RentalSchedule',
    query: {
      deviceId: String(row.deviceId),
      occupyStartDate: queryParams.value.startDate,
      occupyEndDateExclusive: dayjs(queryParams.value.endDate).add(1, 'day').format('YYYY-MM-DD')
    }
  })
}

onMounted(loadAll)
</script>

<style scoped>
.rental-report-page {
  --report-ink: #172033;
  --report-muted: #64748b;
  --report-line: rgb(15 23 42 / 8%);
  --report-teal: #0f766e;
  --report-gold: #c47f24;

  background:
    radial-gradient(circle at 8% 8%, rgb(20 184 166 / 10%), transparent 28%),
    linear-gradient(180deg, #f8fafc 0%, #eef3f8 100%);
}

.report-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.report-hero {
  position: relative;
  display: flex;
  min-height: 218px;
  padding: 28px;
  overflow: hidden;
  color: #fff;
  background:
    radial-gradient(circle at 86% 14%, rgb(245 158 11 / 34%), transparent 30%),
    radial-gradient(circle at 62% 76%, rgb(20 184 166 / 30%), transparent 28%),
    linear-gradient(135deg, #111827 0%, #17313c 48%, #0f766e 100%);
  border: 1px solid rgb(255 255 255 / 56%);
  border-radius: 24px;
  box-shadow: 0 24px 60px rgb(15 23 42 / 14%);
  align-items: stretch;
  justify-content: space-between;
  gap: 24px;
  isolation: isolate;
}

.report-hero::before {
  position: absolute;
  z-index: -1;
  pointer-events: none;
  background-image:
    linear-gradient(rgb(255 255 255 / 8%) 1px, transparent 1px),
    linear-gradient(90deg, rgb(255 255 255 / 8%) 1px, transparent 1px);
  background-size: 34px 34px;
  border: 1px solid rgb(255 255 255 / 12%);
  border-radius: 18px;
  content: '';
  inset: 18px;
  mask-image: linear-gradient(90deg, #000 0%, transparent 72%);
}

.hero-copy {
  max-width: 520px;
  align-self: center;
}

.report-hero h2 {
  margin: 10px 0 12px;
  font-family: 'DIN Alternate', 'Avenir Next', 'PingFang SC', sans-serif;
  font-size: 38px;
  line-height: 1.08;
  letter-spacing: -0.02em;
}

.report-hero p {
  margin: 0;
  font-size: 15px;
  line-height: 1.8;
  color: rgb(226 232 240 / 78%);
}

.report-kicker {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: #a7f3d0;
  text-transform: uppercase;
}

.hero-panel {
  display: flex;
  width: min(520px, 48%);
  padding: 20px;
  align-self: center;
  flex-direction: column;
  justify-content: center;
  background: rgb(255 255 255 / 12%);
  border: 1px solid rgb(255 255 255 / 20%);
  border-radius: 20px;
  backdrop-filter: blur(18px);
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 18%);
}

.hero-panel__label {
  font-size: 12px;
  letter-spacing: 0.12em;
  color: rgb(226 232 240 / 70%);
}

.hero-panel strong {
  margin: 6px 0 18px;
  font-family: 'DIN Alternate', 'Avenir Next', 'PingFang SC', sans-serif;
  font-size: 24px;
  font-variant-numeric: tabular-nums;
}

.report-filter {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.report-filter :deep(.el-form-item) {
  margin: 0;
}

.report-filter :deep(.el-range-editor.el-input__wrapper) {
  width: 290px;
  background: rgb(255 255 255 / 88%);
  border: 0;
  box-shadow: none;
}

.report-filter :deep(.el-button--primary) {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  border: 0;
  box-shadow: 0 12px 26px rgb(146 64 14 / 26%);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  min-height: 172px;
}

.metric-card {
  position: relative;
  display: flex;
  min-height: 142px;
  padding: 20px;
  overflow: hidden;
  flex-direction: column;
  justify-content: space-between;
  background: rgb(255 255 255 / 88%);
  border: 1px solid rgb(255 255 255 / 72%);
  border-radius: 20px;
  box-shadow: 0 18px 42px rgb(15 23 42 / 8%);
}

.metric-card::after {
  position: absolute;
  right: -38px;
  bottom: -46px;
  width: 116px;
  height: 116px;
  background: rgb(15 118 110 / 8%);
  border-radius: 999px;
  content: '';
}

.metric-card--income {
  background:
    linear-gradient(145deg, rgb(255 251 235 / 94%), rgb(255 255 255 / 92%)),
    linear-gradient(135deg, #f59e0b, #fef3c7);
  border-color: rgb(245 158 11 / 24%);
}

.metric-card--income::after,
.metric-card--net::after {
  background: rgb(245 158 11 / 14%);
}

.metric-card--net {
  background: linear-gradient(145deg, rgb(240 253 250 / 94%), rgb(255 255 255 / 92%));
  border-color: rgb(20 184 166 / 20%);
}

.metric-card--utilization {
  background:
    radial-gradient(circle at 92% 20%, rgb(20 184 166 / 20%), transparent 28%),
    rgb(255 255 255 / 92%);
}

.metric-card__top {
  display: flex;
  font-size: 14px;
  font-weight: 600;
  color: var(--report-muted);
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.metric-card__icon {
  display: inline-flex;
  width: 32px;
  height: 32px;
  font-family: 'DIN Alternate', 'Avenir Next', sans-serif;
  font-style: normal;
  font-weight: 700;
  color: var(--report-teal);
  background: rgb(15 118 110 / 10%);
  border-radius: 12px;
  align-items: center;
  justify-content: center;
}

.metric-card small {
  position: relative;
  z-index: 1;
  line-height: 1.55;
  color: var(--report-muted);
}

.metric-card strong {
  position: relative;
  z-index: 1;
  margin: 10px 0 8px;
  font-family: 'DIN Alternate', 'Avenir Next', 'PingFang SC', sans-serif;
  font-size: 30px;
  line-height: 1.1;
  color: var(--report-ink);
  font-variant-numeric: tabular-nums;
}

.metric-card :deep(.el-progress) {
  margin: 4px 0 9px;
}

.device-day-bar {
  height: 9px;
  margin: 2px 0 9px;
  overflow: hidden;
  background: #e2e8f0;
  border-radius: 999px;
}

.device-day-bar span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #0f766e, #14b8a6);
  border-radius: inherit;
}

.report-section {
  padding: 20px 22px 22px;
  background: rgb(255 255 255 / 92%);
  border: 1px solid rgb(255 255 255 / 76%);
  border-radius: 22px;
  box-shadow: 0 18px 46px rgb(15 23 42 / 7%);
}

.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 16px;
}

.section-heading h3 {
  margin: 4px 0 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--report-ink);
}

.section-heading small {
  color: var(--report-muted);
}

.section-eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--report-gold);
  text-transform: uppercase;
}

.source-name {
  display: inline-flex;
  font-weight: 600;
  color: var(--report-ink);
  align-items: center;
  gap: 9px;
}

.source-name span {
  width: 10px;
  height: 10px;
  background: linear-gradient(135deg, #f59e0b, #0f766e);
  border-radius: 999px;
}

.report-tabs {
  --el-tabs-header-height: 46px;
}

.report-tabs :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.report-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: var(--report-line);
}

.report-tabs :deep(.el-tabs__item) {
  font-weight: 700;
  color: var(--report-muted);
}

.report-tabs :deep(.el-tabs__item.is-active) {
  color: var(--report-teal);
}

.report-table {
  --el-table-header-bg-color: #f8fafc;
  --el-table-header-text-color: #475569;
  --el-table-row-hover-bg-color: #f0fdfa;

  overflow: hidden;
  border: 1px solid var(--report-line);
  border-radius: 16px;
}

.report-table :deep(th.el-table__cell) {
  font-weight: 700;
}

.report-table :deep(.el-table__cell) {
  border-bottom-color: rgb(15 23 42 / 6%);
}

.empty-state {
  padding: 34px 0;
  color: var(--report-muted);
}

.progress-label {
  display: inline-block;
  margin-top: 4px;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

@media (width <= 1080px) {
  .report-hero {
    align-items: stretch;
    flex-direction: column;
  }

  .hero-panel {
    width: auto;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (width <= 600px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .report-hero {
    padding: 22px;
  }

  .report-hero h2 {
    font-size: 30px;
  }

  .report-filter :deep(.el-range-editor.el-input__wrapper) {
    width: 100%;
  }

  .section-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
