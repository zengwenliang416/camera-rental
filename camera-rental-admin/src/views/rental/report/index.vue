<template>
  <ContentWrap>
    <div class="report-hero">
      <div>
        <div class="report-kicker">{{ t('rental.report.kicker') }}</div>
        <h2>{{ t('rental.report.pageTitle') }}</h2>
        <p>{{ t('rental.report.scopeHint') }}</p>
      </div>
      <el-form :inline="true" :model="queryParams" class="report-filter">
        <el-form-item :label="t('rental.report.dateRange')">
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
        <span>{{ t('rental.report.rentAmountFen') }}</span>
        <strong>{{ formatYuan(overview?.rentAmountFen) }}</strong>
        <small>{{ t('rental.report.payAmountHint') }}</small>
      </article>
      <article class="metric-card">
        <span>{{ t('rental.report.refundAmountFen') }}</span>
        <strong>{{ formatYuan(overview?.refundAmountFen) }}</strong>
        <small>{{ t('rental.report.refundHint') }}</small>
      </article>
      <article class="metric-card">
        <span>{{ t('rental.report.orderCount') }}</span>
        <strong>{{ overview?.orderCount ?? 0 }}</strong>
        <small>{{ t('rental.report.sourceOrderHint') }}</small>
      </article>
      <article class="metric-card metric-card--utilization">
        <span>{{ t('rental.report.utilization') }}</span>
        <strong>{{ formatPercent(overview?.utilizationBasisPoints) }}</strong>
        <small>{{ t('rental.report.utilizationHint') }}</small>
      </article>
      <article class="metric-card">
        <span>{{ t('rental.report.deviceDays') }}</span>
        <strong>
          {{ overview?.occupiedDeviceDays ?? 0 }} / {{ overview?.totalDeviceDays ?? 0 }}
        </strong>
        <small>
          {{ t('rental.report.idleDaysValue', { days: overview?.idleDeviceDays ?? 0 }) }}
        </small>
      </article>
      <article class="metric-card">
        <span>{{ t('rental.report.assignedIncome') }}</span>
        <strong>{{ formatYuan(overview?.assignedIncomeFen) }}</strong>
        <small>{{ t('rental.report.assignedIncomeHint') }}</small>
      </article>
    </div>

    <el-card class="mb-16px" shadow="never">
      <template #header>
        <div class="card-title">
          <span>{{ t('rental.report.sourceBreakdown') }}</span>
          <small>{{ t('rental.report.sourceBreakdownHint') }}</small>
        </div>
      </template>
      <el-table :data="overview?.sources || []">
        <el-table-column :label="t('rental.report.sourceType')" min-width="140">
          <template #default="{ row }">{{ sourceLabel(row.sourceType) }}</template>
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
    </el-card>

    <el-tabs v-model="activeTab" class="report-tabs">
      <el-tab-pane :label="t('rental.report.productTab')" name="product">
        <el-table v-loading="productLoading" :data="productList">
          <el-table-column prop="shopId" :label="t('rental.report.shopId')" width="100" />
          <el-table-column
            prop="goodsTitle"
            :label="t('rental.report.goodsTitle')"
            min-width="220"
            show-overflow-tooltip
          />
          <el-table-column :label="t('rental.report.externalProductId')" min-width="160">
            <template #default="{ row }">{{ maskChannelIdentifier(row.externalProductId) }}</template>
          </el-table-column>
          <el-table-column :label="t('rental.report.externalSkuId')" min-width="150">
            <template #default="{ row }">{{ maskChannelIdentifier(row.externalSkuId) }}</template>
          </el-table-column>
          <el-table-column prop="orderCount" :label="t('rental.report.orderCount')" width="110" />
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
        <el-table v-loading="deviceLoading" :data="deviceList">
          <el-table-column prop="deviceNo" :label="t('rental.device.deviceNo')" min-width="150" />
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
              <span class="progress-label">{{ formatPercent(row.utilizationBasisPoints) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="occupiedDays" :label="t('rental.report.occupiedDays')" width="110" />
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
.report-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 24px;
  margin-bottom: 16px;
  overflow: hidden;
  background:
    radial-gradient(circle at 92% 12%, rgb(34 197 94 / 18%), transparent 34%),
    linear-gradient(135deg, var(--el-fill-color-light), var(--el-bg-color));
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px;
}

.report-hero h2 {
  margin: 4px 0 6px;
  font-family: 'Avenir Next', 'PingFang SC', sans-serif;
  font-size: 26px;
  letter-spacing: -0.02em;
}

.report-hero p,
.card-title small {
  margin: 0;
  color: var(--el-text-color-secondary);
}

.report-kicker {
  color: var(--el-color-success);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.report-filter {
  flex-shrink: 0;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  min-height: 160px;
  margin-bottom: 16px;
}

.metric-card {
  display: flex;
  min-height: 112px;
  padding: 18px;
  flex-direction: column;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgb(15 23 42 / 5%);
}

.metric-card--income {
  background: linear-gradient(145deg, rgb(34 197 94 / 12%), var(--el-bg-color) 72%);
}

.metric-card--utilization {
  background: linear-gradient(145deg, rgb(14 165 233 / 12%), var(--el-bg-color) 72%);
}

.metric-card span,
.metric-card small {
  color: var(--el-text-color-secondary);
}

.metric-card strong {
  margin: 9px 0 5px;
  color: var(--el-text-color-primary);
  font-family: 'Avenir Next', 'PingFang SC', sans-serif;
  font-size: 25px;
  font-variant-numeric: tabular-nums;
}

.card-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
}

.report-tabs {
  padding: 0 16px 12px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
}

.empty-state {
  padding: 24px 0;
  color: var(--el-text-color-secondary);
}

.progress-label {
  display: inline-block;
  margin-top: 4px;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 900px) {
  .report-hero {
    align-items: stretch;
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 600px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
