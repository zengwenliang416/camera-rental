<template>
  <section class="rental-command-center">
    <header class="command-header">
      <div>
        <div class="command-kicker">RENTAL OPERATIONS</div>
        <h1>租赁运营大屏</h1>
        <p> {{ dateRangeLabel }} · 收入口径来自渠道订单，设备利用率按有效占用排期统计 </p>
      </div>
      <div class="header-actions">
        <div class="operator-meta">
          <span>{{ todayLabel }}</span>
          <strong>{{ username || '租赁运营' }}</strong>
        </div>
        <el-button :loading="loading" type="primary" @click="loadDashboard">
          <Icon class="mr-6px" icon="ep:refresh" />
          刷新数据
        </el-button>
      </div>
    </header>

    <el-alert
      v-if="failedSections.length"
      class="mb-12px"
      :closable="false"
      show-icon
      type="warning"
      title="部分运营数据暂不可用"
    >
      <template #default>
        {{ failedSections.join('、') }}加载失败或当前账号无权限，其他数据已正常展示。
      </template>
    </el-alert>

    <div class="metric-grid">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <div class="metric-icon" :class="`is-${metric.tone}`">
          <Icon :icon="metric.icon" />
        </div>
        <div class="metric-content">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
          <small>{{ metric.helper }}</small>
        </div>
      </article>
    </div>

    <div class="dashboard-grid">
      <el-card class="dashboard-card revenue-card" shadow="never">
        <template #header>
          <div class="card-heading">
            <div>
              <strong>收入结构</strong>
              <span>近 30 天渠道实付租金</span>
            </div>
            <el-button link type="primary" @click="goTo('/rental/report')">经营报表</el-button>
          </div>
        </template>
        <el-skeleton :loading="loading && !overview" animated>
          <div v-if="sourceChartData.length" class="chart-layout">
            <Echart :height="292" :options="sourceChartOptions" />
            <div class="source-list">
              <div v-for="source in sourceChartData" :key="source.name" class="source-row">
                <span>
                  <i :style="{ backgroundColor: source.color }"></i>
                  {{ source.name }}
                </span>
                <strong>{{ formatMoney(source.value) }}</strong>
              </div>
            </div>
          </div>
          <el-empty v-else description="统计区间内暂无收入数据" :image-size="96" />
        </el-skeleton>
      </el-card>

      <el-card class="dashboard-card queue-card" shadow="never">
        <template #header>
          <div class="card-heading">
            <div>
              <strong>今日待办</strong>
              <span>按后端待处理队列实时汇总</span>
            </div>
          </div>
        </template>
        <div class="queue-list">
          <button
            v-for="item in queueItems"
            :key="item.label"
            class="queue-item"
            :disabled="!item.available"
            type="button"
            @click="goTo(item.path)"
          >
            <span class="queue-icon" :class="`is-${item.tone}`">
              <Icon :icon="item.icon" />
            </span>
            <span class="queue-copy">
              <strong>{{ item.label }}</strong>
              <small>{{ item.helper }}</small>
            </span>
            <b>{{ item.available ? item.value : '—' }}</b>
            <Icon icon="ep:arrow-right" />
          </button>
        </div>
      </el-card>

      <el-card class="dashboard-card device-card" shadow="never">
        <template #header>
          <div class="card-heading">
            <div>
              <strong>设备效能</strong>
              <span>按设备占用周期计算利用率</span>
            </div>
            <el-button link type="primary" @click="goTo('/rental/schedule')">排期中心</el-button>
          </div>
        </template>
        <el-skeleton :loading="loading && !devicePerformance.length" animated>
          <Echart v-if="devicePerformance.length" :height="292" :options="deviceChartOptions" />
          <el-empty v-else description="暂无设备效能数据" :image-size="96" />
        </el-skeleton>
      </el-card>

      <el-card class="dashboard-card sync-card" shadow="never">
        <template #header>
          <div class="card-heading">
            <div>
              <strong>最近同步</strong>
              <span>闲管家订单与售后同步运行状态</span>
            </div>
            <el-button link type="primary" @click="goTo('/rental/sync-run')">运行历史</el-button>
          </div>
        </template>
        <el-table v-if="syncRuns.length" :data="syncRuns" height="292" size="small">
          <el-table-column label="资源" min-width="86">
            <template #default="{ row }">{{ syncResourceLabel(row.resourceType) }}</template>
          </el-table-column>
          <el-table-column label="状态" min-width="90">
            <template #default="{ row }">
              <el-tag :type="syncStatusType(row.status)" size="small">
                {{ syncStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="成功 / 失败" min-width="105">
            <template #default="{ row }">
              {{ row.succeededCount || 0 }} / {{ row.failedCount || 0 }}
            </template>
          </el-table-column>
          <el-table-column label="完成时间" min-width="150">
            <template #default="{ row }">{{
              formatSyncTime(row.finishedAt || row.startedAt)
            }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无同步运行记录" :image-size="96" />
      </el-card>
    </div>

    <el-card class="dashboard-card shortcuts-card" shadow="never">
      <template #header>
        <div class="card-heading">
          <div>
            <strong>运营工作台</strong>
            <span>进入对应模块处理订单、设备、排期和客户退回</span>
          </div>
        </div>
      </template>
      <div class="shortcut-grid">
        <button
          v-for="item in shortcuts"
          :key="item.path"
          class="shortcut-item"
          type="button"
          @click="goTo(item.path)"
        >
          <span :class="`is-${item.tone}`"><Icon :icon="item.icon" /></span>
          <div>
            <strong>{{ item.label }}</strong>
            <small>{{ item.helper }}</small>
          </div>
          <Icon icon="ep:right" />
        </button>
      </div>
    </el-card>
  </section>
</template>

<script lang="ts" setup>
import type { EChartsOption } from 'echarts'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import {
  getRentalDevicePerformanceReportPage,
  getRentalReportOverview,
  type RentalDevicePerformanceReportVO,
  type RentalReportOverviewVO
} from '@/api/rental/report'
import { getManualReviewPage } from '@/api/rental/review'
import {
  getReturnRegistrationPage,
  type ReturnRegistrationPageParams
} from '@/api/rental/returnRegistration'
import { getRentalSyncRunPage, type RentalSyncRunVO } from '@/api/rental/syncRun'
import { getXianyuPendingShipOrderPage } from '@/api/rental/xianyu'
import { useUserStore } from '@/store/modules/user'

defineOptions({ name: 'Index' })

type Tone = 'blue' | 'green' | 'orange' | 'red'
type QueueItem = {
  label: string
  helper: string
  value: number
  icon: string
  path: string
  tone: Tone
  available: boolean
}

const router = useRouter()
const userStore = useUserStore()
const username = computed(() => userStore.getUser.nickname)
const permissions = computed(() => userStore.getPermissions)
const loading = ref(false)
const overview = ref<RentalReportOverviewVO>()
const devicePerformance = ref<RentalDevicePerformanceReportVO[]>([])
const syncRuns = ref<RentalSyncRunVO[]>([])
const pendingShipCount = ref(0)
const pendingReviewCount = ref(0)
const pendingReturnCount = ref(0)
const failedSections = ref<string[]>([])

const endDate = dayjs()
const startDate = endDate.subtract(29, 'day')
const reportQuery = {
  startDate: startDate.format('YYYY-MM-DD'),
  endDate: endDate.format('YYYY-MM-DD'),
  pageNo: 1,
  pageSize: 8
}

const todayLabel = computed(() => dayjs().format('YYYY 年 MM 月 DD 日'))
const dateRangeLabel = computed(
  () => `${startDate.format('YYYY-MM-DD')} 至 ${endDate.format('YYYY-MM-DD')}`
)

const hasPermission = (permission: string) =>
  permissions.value.has('*:*:*') || permissions.value.has(permission)

const formatMoney = (fen = 0) =>
  `¥${(fen / 100).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`

const formatPercent = (basisPoints = 0) => `${(basisPoints / 100).toFixed(1)}%`

const metrics = computed(() => [
  {
    label: '实付租金',
    value: overview.value ? formatMoney(overview.value.rentAmountFen) : '—',
    helper: `近 30 天 · ${overview.value?.orderCount || 0} 笔订单`,
    icon: 'solar:wallet-money-bold-duotone',
    tone: 'blue'
  },
  {
    label: '退款金额',
    value: overview.value ? formatMoney(overview.value.refundAmountFen) : '—',
    helper: '退款独立统计，不冲减租金口径',
    icon: 'solar:card-recive-bold-duotone',
    tone: 'red'
  },
  {
    label: '设备利用率',
    value: overview.value ? formatPercent(overview.value.utilizationBasisPoints) : '—',
    helper: `${overview.value?.occupiedDeviceDays || 0} 占用设备日`,
    icon: 'solar:camera-bold-duotone',
    tone: 'green'
  },
  {
    label: '设备规模',
    value: overview.value ? `${overview.value.deviceCount} 台` : '—',
    helper: `${overview.value?.idleDeviceDays || 0} 空闲设备日`,
    icon: 'solar:box-minimalistic-bold-duotone',
    tone: 'orange'
  }
])

const sourcePalette = ['#2563eb', '#0f9f6e', '#f59e0b', '#ef4444', '#64748b']
const sourceLabel = (sourceType: string) => {
  const labels: Record<string, string> = {
    XIANYU: '闲鱼',
    WEB: '官网',
    MINI_PROGRAM: '小程序',
    APP: 'App',
    MANUAL: '人工录入'
  }
  return labels[sourceType] || sourceType
}

const sourceChartData = computed(() =>
  (overview.value?.sources || [])
    .filter((source) => source.rentAmountFen > 0)
    .map((source, index) => ({
      name: sourceLabel(source.sourceType),
      value: source.rentAmountFen,
      color: sourcePalette[index % sourcePalette.length]
    }))
)

const sourceChartOptions = computed<EChartsOption>(() => ({
  color: sourceChartData.value.map((item) => item.color),
  tooltip: {
    trigger: 'item',
    formatter: (params: any) => `${params.name}<br/>${formatMoney(params.value)}`
  },
  series: [
    {
      type: 'pie',
      radius: ['56%', '78%'],
      center: ['45%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 4, borderRadius: 8 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 700 } },
      data: sourceChartData.value.map(({ name, value }) => ({ name, value }))
    }
  ]
}))

const deviceChartOptions = computed<EChartsOption>(() => ({
  color: ['#2563eb'],
  grid: { left: 12, right: 28, top: 18, bottom: 10, containLabel: true },
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    formatter: (params: any) => {
      const item = params?.[0]
      return item ? `${item.name}<br/>利用率 ${item.value}%` : ''
    }
  },
  xAxis: {
    type: 'value',
    max: 100,
    axisLabel: { formatter: '{value}%' },
    splitLine: { lineStyle: { color: '#e5e7eb', type: 'dashed' } }
  },
  yAxis: {
    type: 'category',
    inverse: true,
    axisTick: { show: false },
    axisLine: { show: false },
    data: devicePerformance.value.map((item) => item.deviceNo)
  },
  series: [
    {
      type: 'bar',
      barWidth: 14,
      data: devicePerformance.value.map((item) => item.utilizationBasisPoints / 100),
      itemStyle: { borderRadius: [0, 7, 7, 0] }
    }
  ]
}))

const queueItems = computed<QueueItem[]>(() => [
  {
    label: '待发货订单',
    helper: '已同步、等待设备和物流确认',
    value: pendingShipCount.value,
    icon: 'solar:delivery-bold-duotone',
    path: '/rental/order',
    tone: 'blue',
    available: hasPermission('rental:xianyu:query')
  },
  {
    label: '备注待复核',
    helper: '日期冲突或关键信息不足',
    value: pendingReviewCount.value,
    icon: 'solar:clipboard-check-bold-duotone',
    path: '/rental/review',
    tone: 'orange',
    available: hasPermission('rental:review:query')
  },
  {
    label: '客户退回待核验',
    helper: '物流、设备编码和照片待确认',
    value: pendingReturnCount.value,
    icon: 'solar:box-bold-duotone',
    path: '/rental/return-registration',
    tone: 'red',
    available: hasPermission('rental:return-registration:query')
  }
])

const shortcutDefinitions = [
  {
    label: '渠道订单',
    helper: '订单同步、备注解析与发货',
    icon: 'solar:cart-large-2-bold-duotone',
    path: '/rental/order',
    tone: 'blue' as Tone,
    permission: 'rental:xianyu:query'
  },
  {
    label: '设备排期',
    helper: '查看计租周期与设备占用周期',
    icon: 'solar:calendar-date-bold-duotone',
    path: '/rental/schedule',
    tone: 'green' as Tone,
    permission: 'rental:schedule:query'
  },
  {
    label: '租赁设备',
    helper: '设备实例、状态与二维码',
    icon: 'solar:camera-bold-duotone',
    path: '/rental/device',
    tone: 'orange' as Tone,
    permission: 'rental:device:query'
  },
  {
    label: '客户退回',
    helper: '审核客户提交的退回登记',
    icon: 'solar:inbox-in-bold-duotone',
    path: '/rental/return-registration',
    tone: 'red' as Tone,
    permission: 'rental:return-registration:query'
  }
]

const shortcuts = computed(() =>
  shortcutDefinitions.filter((item) => hasPermission(item.permission))
)

const runSection = async (label: string, task: () => Promise<void>) => {
  try {
    await task()
  } catch {
    failedSections.value.push(label)
  }
}

const loadDashboard = async () => {
  loading.value = true
  failedSections.value = []
  const tasks: Promise<void>[] = []

  if (hasPermission('rental:report:query')) {
    tasks.push(
      runSection('经营概览', async () => {
        overview.value = await getRentalReportOverview(reportQuery)
      }),
      runSection('设备效能', async () => {
        const data = await getRentalDevicePerformanceReportPage(reportQuery)
        devicePerformance.value = data.list || []
      })
    )
  }

  if (hasPermission('rental:xianyu:query')) {
    tasks.push(
      runSection('待发货订单', async () => {
        const data = await getXianyuPendingShipOrderPage({ pageNo: 1, pageSize: 1 })
        pendingShipCount.value = data.total || 0
      }),
      runSection('同步历史', async () => {
        const data = await getRentalSyncRunPage({ pageNo: 1, pageSize: 5 })
        syncRuns.value = data.list || []
      })
    )
  }

  if (hasPermission('rental:review:query')) {
    tasks.push(
      runSection('人工复核', async () => {
        const data = await getManualReviewPage({ pageNo: 1, pageSize: 1, status: 'PENDING' })
        pendingReviewCount.value = data.total || 0
      })
    )
  }

  if (hasPermission('rental:return-registration:query')) {
    tasks.push(
      runSection('客户退回', async () => {
        const params: ReturnRegistrationPageParams = {
          pageNo: 1,
          pageSize: 1,
          status: 'REVIEW_REQUIRED'
        }
        const data = await getReturnRegistrationPage(params)
        pendingReturnCount.value = data.total || 0
      })
    )
  }

  await Promise.all(tasks)
  loading.value = false
}

const syncResourceLabel = (value: string) =>
  ({ ORDER: '订单', AFTER_SALE: '售后', PRODUCT: '商品' })[value] || value

const syncStatusLabel = (value: string) =>
  ({ RUNNING: '运行中', SUCCEEDED: '成功', FAILED: '失败' })[value] || value

const syncStatusType = (value: string) => {
  if (value === 'SUCCEEDED') return 'success'
  if (value === 'FAILED') return 'danger'
  return 'warning'
}

const formatSyncTime = (value?: string) => (value ? dayjs(value).format('MM-DD HH:mm') : '—')
const goTo = (path: string) => router.push(path)

onMounted(loadDashboard)
</script>

<style scoped>
.rental-command-center {
  --dashboard-border: color-mix(in srgb, var(--el-border-color) 82%, transparent);

  min-height: calc(100vh - 110px);
  padding: 18px;
  background:
    radial-gradient(circle at 10% 0%, rgb(37 99 235 / 8%), transparent 28%),
    radial-gradient(circle at 92% 8%, rgb(15 159 110 / 8%), transparent 24%),
    var(--el-bg-color-page);
}

.command-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 8px 2px 20px;
}

.command-kicker {
  margin-bottom: 5px;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  color: var(--el-color-primary);
}

.command-header h1 {
  margin: 0;
  font-size: clamp(24px, 2.3vw, 36px);
  font-weight: 760;
  letter-spacing: -0.04em;
  color: var(--el-text-color-primary);
}

.command-header p {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.operator-meta {
  display: grid;
  gap: 3px;
  text-align: right;
}

.operator-meta span {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.operator-meta strong {
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.metric-card {
  display: flex;
  min-height: 132px;
  padding: 20px;
  background: color-mix(in srgb, var(--el-bg-color) 96%, transparent);
  border: 1px solid var(--dashboard-border);
  border-radius: 12px;
  box-shadow: 0 10px 28px rgb(15 23 42 / 4%);
  align-items: center;
  gap: 16px;
}

.metric-icon,
.queue-icon,
.shortcut-item > span {
  display: grid;
  flex: none;
  place-items: center;
  border-radius: 12px;
}

.metric-icon {
  width: 48px;
  height: 48px;
  font-size: 25px;
}

.is-blue {
  color: #2563eb;
  background: #eff6ff;
}

.is-green {
  color: #0f9f6e;
  background: #ecfdf5;
}

.is-orange {
  color: #d97706;
  background: #fffbeb;
}

.is-red {
  color: #dc2626;
  background: #fef2f2;
}

.metric-content {
  min-width: 0;
}

.metric-content span,
.metric-content small {
  display: block;
  color: var(--el-text-color-secondary);
}

.metric-content span {
  font-size: 13px;
}

.metric-content strong {
  display: block;
  margin: 6px 0;
  font-size: clamp(23px, 2vw, 31px);
  letter-spacing: -0.04em;
  color: var(--el-text-color-primary);
  font-variant-numeric: tabular-nums;
}

.metric-content small {
  overflow: hidden;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(330px, 0.75fr);
  gap: 12px;
}

.dashboard-card {
  border-color: var(--dashboard-border);
  border-radius: 12px;
}

.dashboard-card :deep(.el-card__header) {
  padding: 16px 18px;
}

.dashboard-card :deep(.el-card__body) {
  padding: 16px 18px;
}

.card-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-heading > div {
  display: grid;
  gap: 3px;
}

.card-heading strong {
  font-size: 15px;
  color: var(--el-text-color-primary);
}

.card-heading span {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.chart-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 185px;
  align-items: center;
}

.source-list {
  display: grid;
  gap: 12px;
}

.source-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.source-row span {
  display: flex;
  min-width: 0;
  font-size: 12px;
  color: var(--el-text-color-regular);
  align-items: center;
  gap: 8px;
}

.source-row i {
  width: 8px;
  height: 8px;
  flex: none;
  border-radius: 50%;
}

.source-row strong {
  font-size: 12px;
  color: var(--el-text-color-primary);
  font-variant-numeric: tabular-nums;
}

.queue-list {
  display: grid;
  gap: 10px;
}

.queue-item {
  display: grid;
  width: 100%;
  min-height: 78px;
  padding: 12px;
  color: var(--el-text-color-secondary);
  text-align: left;
  background: var(--el-fill-color-blank);
  border: 1px solid var(--dashboard-border);
  border-radius: 10px;
  transition:
    border-color 0.2s ease,
    transform 0.2s ease;
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 12px;
}

.queue-item:not(:disabled) {
  cursor: pointer;
}

.queue-item:not(:disabled):hover {
  border-color: var(--el-color-primary-light-5);
  transform: translateY(-1px);
}

.queue-item:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.queue-icon {
  width: 40px;
  height: 40px;
  font-size: 21px;
}

.queue-copy {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.queue-copy strong {
  font-size: 13px;
  color: var(--el-text-color-primary);
}

.queue-copy small {
  overflow: hidden;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.queue-item b {
  font-size: 22px;
  color: var(--el-text-color-primary);
  font-variant-numeric: tabular-nums;
}

.shortcuts-card {
  margin-top: 12px;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.shortcut-item {
  display: grid;
  min-height: 74px;
  padding: 12px;
  color: var(--el-text-color-secondary);
  text-align: left;
  cursor: pointer;
  background: var(--el-fill-color-blank);
  border: 1px solid var(--dashboard-border);
  border-radius: 10px;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
}

.shortcut-item:hover {
  background: var(--el-fill-color-light);
  border-color: var(--el-color-primary-light-5);
}

.shortcut-item > span {
  width: 40px;
  height: 40px;
  font-size: 21px;
}

.shortcut-item > div {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.shortcut-item strong {
  font-size: 13px;
  color: var(--el-text-color-primary);
}

.shortcut-item small {
  overflow: hidden;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (width <= 1180px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .shortcut-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (width <= 680px) {
  .rental-command-center {
    padding: 12px;
  }

  .command-header {
    align-items: stretch;
    flex-direction: column;
  }

  .header-actions {
    justify-content: space-between;
  }

  .operator-meta {
    text-align: left;
  }

  .metric-grid,
  .shortcut-grid {
    grid-template-columns: 1fr;
  }

  .metric-card {
    min-height: 112px;
  }

  .chart-layout {
    grid-template-columns: 1fr;
  }

  .source-list {
    padding: 0 10px 10px;
  }
}
</style>
