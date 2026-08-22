<template>
  <section class="rental-command-center">
    <div class="screen-grid-bg"></div>
    <header class="screen-header">
      <div class="header-side header-left">
        <span class="live-dot"></span>
        <div>
          <strong>运营数据在线</strong>
          <small>{{ dateRangeLabel }}</small>
        </div>
      </div>
      <div class="screen-title">
        <i></i>
        <div>
          <span>RENTAL OPERATIONS COMMAND</span>
          <h1>相机租赁运营指挥中心</h1>
        </div>
        <i></i>
      </div>
      <div class="header-side header-right">
        <div>
          <strong>{{ clockLabel }}</strong>
          <small>{{ todayLabel }} · {{ username || '租赁运营' }}</small>
        </div>
        <button class="refresh-button" type="button" :disabled="loading" @click="loadDashboard">
          <Icon :class="{ 'is-rotating': loading }" icon="ep:refresh" />
        </button>
      </div>
    </header>

    <div v-if="failedSections.length" class="screen-warning">
      <Icon icon="ep:warning" />
      {{ failedSections.join('、') }}暂不可用，其余运营数据已正常展示
    </div>

    <div class="metric-grid">
      <article v-for="(metric, index) in metrics" :key="metric.label" class="metric-card">
        <div class="metric-index">0{{ index + 1 }}</div>
        <div class="metric-icon" :class="`is-${metric.tone}`">
          <Icon :icon="metric.icon" />
        </div>
        <div class="metric-content">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
          <small>{{ metric.helper }}</small>
        </div>
      </article>

      <article class="metric-card">
        <div class="metric-index">05</div>
        <div class="metric-icon is-purple">
          <Icon icon="solar:delivery-bold-duotone" />
        </div>
        <div class="metric-content">
          <span>当日发货金额</span>
          <strong>{{ shipSummary ? formatMoney(shipSummary.shipAmountFen) : '—' }}</strong>
          <div class="ship-date-row">
            <small>{{ shipSummary?.shipOrderCount || 0 }} 单发货</small>
            <div class="ship-date-control">
              <button type="button" :disabled="loading" @click="shiftShipDate(-1)">
                <Icon icon="ep:arrow-left" />
              </button>
              <input
                v-model="shipDate"
                type="date"
                :max="todayIso"
                aria-label="发货日期"
                @change="onShipDateChange"
              />
              <button type="button" :disabled="loading" @click="shiftShipDate(1)">
                <Icon icon="ep:arrow-right" />
              </button>
            </div>
          </div>
        </div>
      </article>
    </div>

    <div class="command-layout">
      <div class="command-column">
        <section class="screen-panel source-panel">
          <div class="panel-corners"></div>
          <header class="panel-title">
            <div>
              <span>CHANNEL REVENUE</span>
              <strong>渠道收入结构</strong>
            </div>
            <button type="button" @click="goTo('/rental/report')">详情</button>
          </header>
          <div v-if="sourceChartData.length" class="source-content">
            <Echart :height="226" :options="sourceChartOptions" />
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
          <div v-else class="screen-empty">统计区间内暂无收入数据</div>
        </section>

        <section class="screen-panel queue-panel">
          <div class="panel-corners"></div>
          <header class="panel-title">
            <div>
              <span>OPERATION QUEUE</span>
              <strong>实时待办队列</strong>
            </div>
          </header>
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
            </button>
          </div>
        </section>
      </div>

      <div class="command-column center-column">
        <section class="screen-panel core-panel">
          <div class="panel-corners"></div>
          <header class="panel-title is-centered">
            <div>
              <span>CORE OPERATING INDEX</span>
              <strong>设备运营核心指数</strong>
            </div>
          </header>
          <div class="core-visual">
            <div class="radar-ring radar-ring-one"></div>
            <div class="radar-ring radar-ring-two"></div>
            <Echart :height="286" :options="utilizationGaugeOptions" />
          </div>
          <div class="core-stats">
            <div>
              <span>已归集设备收入</span>
              <strong>{{ formatMoney(overview?.assignedIncomeFen || 0) }}</strong>
            </div>
            <div>
              <span>有效订单数</span>
              <strong>{{ overview?.orderCount || 0 }}</strong>
            </div>
            <div>
              <span>占用 / 总设备日</span>
              <strong
                >{{ overview?.occupiedDeviceDays || 0 }}/{{
                  overview?.totalDeviceDays || 0
                }}</strong
              >
            </div>
          </div>
        </section>

        <section class="screen-panel device-panel">
          <div class="panel-corners"></div>
          <header class="panel-title">
            <div>
              <span>DEVICE PERFORMANCE</span>
              <strong>设备利用率排行</strong>
            </div>
            <button type="button" @click="goTo('/rental/schedule')">排期中心</button>
          </header>
          <Echart v-if="devicePerformance.length" :height="236" :options="deviceChartOptions" />
          <div v-else class="screen-empty">暂无设备效能数据</div>
        </section>
      </div>

      <div class="command-column">
        <section class="screen-panel sync-panel">
          <div class="panel-corners"></div>
          <header class="panel-title">
            <div>
              <span>SYNC MONITOR</span>
              <strong>闲管家同步监控</strong>
            </div>
            <button type="button" @click="goTo('/rental/sync-run')">历史</button>
          </header>
          <div v-if="syncRuns.length" class="sync-list">
            <div v-for="run in syncRuns" :key="run.id" class="sync-row">
              <span class="sync-state" :class="`is-${run.status.toLowerCase()}`"></span>
              <div>
                <strong>{{ syncResourceLabel(run.resourceType) }}同步</strong>
                <small>{{ formatSyncTime(run.finishedAt || run.startedAt) }}</small>
              </div>
              <div class="sync-count">
                <strong>{{ run.succeededCount || 0 }}</strong>
                <small>成功</small>
              </div>
              <span class="sync-status">{{ syncStatusLabel(run.status) }}</span>
            </div>
          </div>
          <div v-else class="screen-empty">暂无同步运行记录</div>
        </section>

        <section class="screen-panel shortcut-panel">
          <div class="panel-corners"></div>
          <header class="panel-title">
            <div>
              <span>OPERATION WORKBENCH</span>
              <strong>运营工作台</strong>
            </div>
          </header>
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
        </section>
      </div>
    </div>

    <footer class="screen-footer">
      <span>数据口径：渠道实付租金独立统计退款</span>
      <i></i>
      <span>排期口径：设备占用周期从出库至回仓检测完成</span>
      <i></i>
      <span>系统时区：Asia/Shanghai</span>
    </footer>
  </section>
</template>

<script lang="ts" setup>
import type { EChartsOption } from 'echarts'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import {
  getRentalDevicePerformanceReportPage,
  getRentalReportOverview,
  getRentalReportShipDateSummary,
  type RentalDevicePerformanceReportVO,
  type RentalReportOverviewVO,
  type RentalShipDateSummaryVO
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

type Tone = 'blue' | 'green' | 'orange' | 'red' | 'purple'
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
const todayIso = dayjs().format('YYYY-MM-DD')
const shipDate = ref(todayIso)
const shipSummary = ref<RentalShipDateSummaryVO>()
const failedSections = ref<string[]>([])
const now = ref(dayjs())
let clockTimer: ReturnType<typeof setInterval> | undefined

const endDate = dayjs()
const startDate = endDate.subtract(29, 'day')
const reportQuery = {
  startDate: startDate.format('YYYY-MM-DD'),
  endDate: endDate.format('YYYY-MM-DD'),
  pageNo: 1,
  pageSize: 8
}

const todayLabel = computed(() => dayjs().format('YYYY 年 MM 月 DD 日'))
const clockLabel = computed(() => now.value.format('HH:mm:ss'))
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

const sourcePalette = ['#21d4fd', '#4effc4', '#ffbf5b', '#ff6584', '#7c8cff']
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
    backgroundColor: 'rgba(4, 18, 38, 0.96)',
    borderColor: '#1b6f91',
    textStyle: { color: '#dff7ff' },
    formatter: (params: any) => `${params.name}<br/>${formatMoney(params.value)}`
  },
  series: [
    {
      type: 'pie',
      radius: ['54%', '76%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#061428', borderWidth: 4, borderRadius: 4 },
      label: { show: false },
      emphasis: {
        scaleSize: 8,
        label: { show: true, color: '#ffffff', fontSize: 13, fontWeight: 700 }
      },
      data: sourceChartData.value.map(({ name, value }) => ({ name, value }))
    }
  ]
}))

const utilizationGaugeOptions = computed<EChartsOption>(() => ({
  series: [
    {
      type: 'gauge',
      min: 0,
      max: 100,
      startAngle: 220,
      endAngle: -40,
      radius: '88%',
      center: ['50%', '52%'],
      progress: {
        show: true,
        roundCap: true,
        width: 14,
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 1,
            y2: 0,
            colorStops: [
              { offset: 0, color: '#20c8ff' },
              { offset: 1, color: '#52ffc7' }
            ]
          }
        }
      },
      axisLine: {
        lineStyle: {
          width: 14,
          color: [[1, 'rgba(44, 115, 147, 0.24)']]
        }
      },
      axisTick: { show: false },
      splitLine: {
        distance: 8,
        length: 8,
        lineStyle: { width: 1, color: '#3b8eaa' }
      },
      axisLabel: {
        distance: 24,
        color: '#6396ad',
        fontSize: 9
      },
      pointer: {
        show: true,
        length: '52%',
        width: 4,
        itemStyle: { color: '#dffcff' }
      },
      anchor: {
        show: true,
        size: 11,
        itemStyle: { color: '#50ffd0', borderColor: '#071a2e', borderWidth: 3 }
      },
      title: {
        show: true,
        offsetCenter: [0, '58%'],
        color: '#73a9bd',
        fontSize: 11
      },
      detail: {
        valueAnimation: true,
        offsetCenter: [0, '25%'],
        color: '#ffffff',
        fontSize: 34,
        fontWeight: 700,
        formatter: '{value}%'
      },
      data: [
        {
          name: '设备利用率',
          value: overview.value
            ? Number((overview.value.utilizationBasisPoints / 100).toFixed(1))
            : 0
        }
      ]
    }
  ]
}))

const deviceChartOptions = computed<EChartsOption>(() => ({
  grid: { left: 10, right: 22, top: 16, bottom: 8, containLabel: true },
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    backgroundColor: 'rgba(4, 18, 38, 0.96)',
    borderColor: '#1b6f91',
    textStyle: { color: '#dff7ff' },
    formatter: (params: any) => {
      const item = params?.[0]
      return item ? `${item.name}<br/>利用率 ${item.value}%` : ''
    }
  },
  xAxis: {
    type: 'value',
    max: 100,
    axisLabel: { color: '#638da2', fontSize: 9, formatter: '{value}%' },
    axisLine: { lineStyle: { color: '#17425d' } },
    splitLine: { lineStyle: { color: 'rgba(41, 101, 128, 0.24)', type: 'dashed' } }
  },
  yAxis: {
    type: 'category',
    inverse: true,
    axisTick: { show: false },
    axisLine: { show: false },
    axisLabel: { color: '#a8d5e7', fontSize: 10 },
    data: devicePerformance.value.map((item) => item.deviceNo)
  },
  series: [
    {
      type: 'bar',
      barWidth: 10,
      showBackground: true,
      backgroundStyle: { color: 'rgba(32, 87, 113, 0.18)', borderRadius: 5 },
      data: devicePerformance.value.map((item) => item.utilizationBasisPoints / 100),
      itemStyle: {
        borderRadius: [0, 5, 5, 0],
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 1,
          y2: 0,
          colorStops: [
            { offset: 0, color: '#1e86ff' },
            { offset: 1, color: '#53ffca' }
          ]
        }
      }
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

const loadShipSummary = async () => {
  shipSummary.value = await getRentalReportShipDateSummary({ date: shipDate.value })
}

const onShipDateChange = async () => {
  if (!shipDate.value) {
    shipDate.value = todayIso
  }
  try {
    await loadShipSummary()
  } catch {
    shipSummary.value = undefined
  }
}

const shiftShipDate = (offset: number) => {
  const next = dayjs(shipDate.value).add(offset, 'day')
  if (next.isAfter(dayjs(), 'day')) return
  shipDate.value = next.format('YYYY-MM-DD')
  onShipDateChange()
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
      }),
      runSection('发货金额', loadShipSummary)
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

const formatSyncTime = (value?: string) => (value ? dayjs(value).format('MM-DD HH:mm') : '—')
const goTo = (path: string) => router.push(path)

onMounted(() => {
  loadDashboard()
  clockTimer = setInterval(() => {
    now.value = dayjs()
  }, 1000)
})

onBeforeUnmount(() => {
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<style scoped>
.rental-command-center {
  --screen-cyan: #24d7ff;
  --screen-green: #50ffc9;
  --screen-orange: #ffbc57;
  --screen-red: #ff647d;
  --screen-border: rgb(49 162 203 / 38%);
  --screen-panel: rgb(5 26 50 / 82%);

  position: relative;
  min-height: calc(100vh - 112px);
  padding: 14px 16px 10px;
  overflow: hidden;
  font-family: 'Avenir Next', 'DIN Alternate', 'PingFang SC', sans-serif;
  color: #dff7ff;
  background:
    radial-gradient(circle at 50% 20%, rgb(17 105 157 / 22%), transparent 35%),
    radial-gradient(circle at 8% 80%, rgb(20 209 185 / 10%), transparent 24%),
    radial-gradient(circle at 92% 76%, rgb(30 126 255 / 12%), transparent 26%),
    linear-gradient(180deg, #071529 0%, #030b18 100%);
}

.screen-grid-bg {
  position: absolute;
  z-index: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgb(41 126 167 / 12%) 1px, transparent 1px),
    linear-gradient(90deg, rgb(41 126 167 / 12%) 1px, transparent 1px);
  background-size: 32px 32px;
  opacity: 0.3;
  inset: 0;
  mask-image: linear-gradient(to bottom, rgb(0 0 0 / 72%), transparent 94%);
}

.rental-command-center > :not(.screen-grid-bg) {
  position: relative;
  z-index: 1;
}

.screen-header {
  display: grid;
  min-height: 76px;
  padding: 0 6px 12px;
  border-bottom: 1px solid rgb(55 176 219 / 28%);
  grid-template-columns: minmax(180px, 1fr) minmax(360px, 1.5fr) minmax(180px, 1fr);
  align-items: center;
}

.screen-header::after {
  position: absolute;
  bottom: -2px;
  left: 50%;
  width: 44%;
  height: 3px;
  background: linear-gradient(90deg, transparent, var(--screen-cyan), transparent);
  content: '';
  transform: translateX(-50%);
  box-shadow: 0 0 15px rgb(36 215 255 / 80%);
}

.header-side {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-side > div {
  display: grid;
  gap: 3px;
}

.header-side strong {
  font-size: 13px;
  color: #d8f5ff;
}

.header-side small {
  font-size: 10px;
  letter-spacing: 0.04em;
  color: #628fa6;
}

.live-dot {
  width: 8px;
  height: 8px;
  background: var(--screen-green);
  border-radius: 50%;
  box-shadow:
    0 0 0 4px rgb(80 255 201 / 10%),
    0 0 12px var(--screen-green);
  animation: screen-pulse 1.8s ease-in-out infinite;
}

.screen-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  text-align: center;
}

.screen-title i {
  width: clamp(42px, 6vw, 88px);
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--screen-cyan));
  box-shadow: 0 0 8px rgb(36 215 255 / 60%);
}

.screen-title i:last-child {
  background: linear-gradient(90deg, var(--screen-cyan), transparent);
}

.screen-title > div {
  display: grid;
  gap: 2px;
}

.screen-title span {
  font-size: 8px;
  letter-spacing: 0.32em;
  color: #4c91ad;
}

.screen-title h1 {
  margin: 0;
  font-size: clamp(20px, 2.2vw, 30px);
  font-weight: 700;
  letter-spacing: 0.12em;
  color: #f1fbff;
  text-shadow:
    0 0 12px rgb(36 215 255 / 42%),
    0 0 26px rgb(27 113 191 / 28%);
}

.header-right {
  justify-content: flex-end;
  text-align: right;
}

.header-right strong {
  font-size: 20px;
  letter-spacing: 0.08em;
  color: var(--screen-cyan);
  font-variant-numeric: tabular-nums;
}

.refresh-button {
  display: grid;
  width: 34px;
  height: 34px;
  padding: 0;
  font-size: 16px;
  color: var(--screen-cyan);
  cursor: pointer;
  background: rgb(18 76 106 / 28%);
  border: 1px solid rgb(65 196 236 / 38%);
  clip-path: polygon(8px 0, 100% 0, 100% calc(100% - 8px), calc(100% - 8px) 100%, 0 100%, 0 8px);
  place-items: center;
}

.refresh-button:disabled {
  cursor: wait;
  opacity: 0.6;
}

.is-rotating {
  animation: screen-rotate 1s linear infinite;
}

.screen-warning {
  display: flex;
  min-height: 30px;
  padding: 6px 12px;
  margin-top: 10px;
  font-size: 11px;
  color: #ffdca0;
  background: rgb(129 82 10 / 24%);
  border: 1px solid rgb(255 188 87 / 34%);
  align-items: center;
  gap: 8px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin: 12px 0 10px;
}

.metric-card {
  position: relative;
  display: grid;
  min-height: 96px;
  padding: 14px 12px;
  overflow: hidden;
  background: linear-gradient(135deg, rgb(25 91 125 / 22%), transparent 58%), rgb(5 27 51 / 76%);
  border: 1px solid rgb(46 146 184 / 36%);
  box-shadow: inset 0 0 24px rgb(16 94 132 / 12%);
  grid-template-columns: 38px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  clip-path: polygon(
    10px 0,
    100% 0,
    100% calc(100% - 10px),
    calc(100% - 10px) 100%,
    0 100%,
    0 10px
  );
}

.metric-card::after {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 46%;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--screen-cyan));
  content: '';
}

.metric-index {
  position: absolute;
  top: 7px;
  right: 9px;
  font-size: 9px;
  letter-spacing: 0.08em;
  color: rgb(88 163 190 / 48%);
}

.metric-icon,
.queue-icon,
.shortcut-item > span {
  display: grid;
  flex: none;
  place-items: center;
}

.metric-icon {
  width: 36px;
  height: 36px;
  font-size: 21px;
  background: rgb(13 64 91 / 62%);
  border: 1px solid currentcolor;
  transform: rotate(45deg);
}

.metric-icon :deep(svg) {
  transform: rotate(-45deg);
}

.is-blue {
  color: var(--screen-cyan);
}

.is-green {
  color: var(--screen-green);
}

.is-orange {
  color: var(--screen-orange);
}

.is-red {
  color: var(--screen-red);
}

.is-purple {
  color: #8ea2ff;
}

.metric-content {
  min-width: 0;
}

.metric-content span {
  display: block;
  font-size: 11px;
  letter-spacing: 0.08em;
  color: #75a6ba;
}

.metric-content strong {
  display: block;
  margin: 3px 0 2px;
  overflow: hidden;
  font-size: clamp(21px, 2vw, 29px);
  letter-spacing: 0.01em;
  color: #f5fcff;
  text-overflow: ellipsis;
  text-shadow: 0 0 12px rgb(36 215 255 / 20%);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.metric-content small {
  display: block;
  overflow: hidden;
  font-size: 9px;
  color: #4d7c91;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ship-date-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-top: 4px;
}

.ship-date-control {
  display: flex;
  flex: none;
  align-items: center;
  gap: 4px;
}

.ship-date-control button {
  display: grid;
  width: 20px;
  height: 20px;
  padding: 0;
  font-size: 11px;
  color: var(--screen-cyan);
  cursor: pointer;
  background: rgb(18 76 106 / 28%);
  border: 1px solid rgb(65 196 236 / 38%);
  place-items: center;
}

.ship-date-control button:hover:not(:disabled) {
  border-color: rgb(36 215 255 / 70%);
  box-shadow: 0 0 8px rgb(36 215 255 / 30%);
}

.ship-date-control button:disabled {
  cursor: wait;
  opacity: 0.5;
}

.ship-date-control input {
  width: 106px;
  height: 20px;
  padding: 0 4px;
  font-size: 10px;
  letter-spacing: 0.02em;
  color: #a8d5e7;
  color-scheme: dark;
  background: rgb(13 64 91 / 45%);
  border: 1px solid rgb(60 163 200 / 30%);
  font-variant-numeric: tabular-nums;
}

.ship-date-control input:focus {
  outline: none;
  border-color: var(--screen-cyan);
  box-shadow: 0 0 6px rgb(36 215 255 / 35%);
}

.command-layout {
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(245px, 0.88fr) minmax(320px, 1.24fr) minmax(245px, 0.88fr);
}

.command-column {
  display: grid;
  align-content: start;
  gap: 10px;
}

.screen-panel {
  position: relative;
  padding: 12px;
  overflow: hidden;
  background: linear-gradient(180deg, rgb(11 44 72 / 35%), transparent 48%), var(--screen-panel);
  border: 1px solid var(--screen-border);
  box-shadow:
    inset 0 0 32px rgb(18 90 129 / 8%),
    0 0 18px rgb(0 0 0 / 14%);
}

.panel-corners::before,
.panel-corners::after,
.screen-panel::before,
.screen-panel::after {
  position: absolute;
  z-index: 2;
  width: 18px;
  height: 18px;
  pointer-events: none;
  content: '';
}

.screen-panel::before {
  top: -1px;
  left: -1px;
  border-top: 2px solid var(--screen-cyan);
  border-left: 2px solid var(--screen-cyan);
}

.screen-panel::after {
  top: -1px;
  right: -1px;
  border-top: 2px solid var(--screen-cyan);
  border-right: 2px solid var(--screen-cyan);
}

.panel-corners::before {
  bottom: -1px;
  left: -1px;
  border-bottom: 2px solid var(--screen-cyan);
  border-left: 2px solid var(--screen-cyan);
}

.panel-corners::after {
  right: -1px;
  bottom: -1px;
  border-right: 2px solid var(--screen-cyan);
  border-bottom: 2px solid var(--screen-cyan);
}

.panel-title {
  display: flex;
  min-height: 38px;
  padding: 0 8px 6px 12px;
  margin: -2px 0 6px;
  border-bottom: 1px solid rgb(47 146 184 / 24%);
  align-items: center;
  justify-content: space-between;
}

.panel-title::before {
  position: absolute;
  left: 12px;
  width: 3px;
  height: 24px;
  background: var(--screen-cyan);
  content: '';
  box-shadow: 0 0 12px rgb(36 215 255 / 74%);
}

.panel-title > div {
  display: grid;
  gap: 1px;
}

.panel-title span {
  font-size: 7px;
  letter-spacing: 0.18em;
  color: #3f7d98;
}

.panel-title strong {
  font-size: 13px;
  letter-spacing: 0.08em;
  color: #dff8ff;
}

.panel-title button {
  padding: 3px 8px;
  font-size: 9px;
  color: #63c9e9;
  cursor: pointer;
  background: rgb(17 79 107 / 24%);
  border: 1px solid rgb(60 163 200 / 30%);
}

.panel-title.is-centered {
  justify-content: center;
  text-align: center;
}

.panel-title.is-centered::before {
  display: none;
}

.source-panel {
  min-height: 332px;
}

.source-content {
  position: relative;
}

.source-list {
  display: grid;
  padding: 0 8px 6px;
  margin-top: -18px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 5px 12px;
}

.source-row {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-top: 5px;
  font-size: 9px;
  border-top: 1px solid rgb(58 135 164 / 18%);
}

.source-row span {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 5px;
  color: #7daabd;
}

.source-row i {
  width: 6px;
  height: 6px;
  flex: none;
  border-radius: 50%;
  box-shadow: 0 0 6px currentcolor;
}

.source-row strong {
  overflow: hidden;
  color: #d8f3fc;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.queue-list {
  display: grid;
  gap: 7px;
  padding-top: 2px;
}

.queue-item {
  display: grid;
  width: 100%;
  min-height: 60px;
  padding: 8px 10px;
  color: #6f9caf;
  text-align: left;
  background: linear-gradient(90deg, rgb(24 95 126 / 28%), transparent 72%), rgb(6 30 54 / 58%);
  border: 1px solid rgb(49 134 169 / 24%);
  transition:
    border-color 0.2s ease,
    background 0.2s ease;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 9px;
}

.queue-item:not(:disabled) {
  cursor: pointer;
}

.queue-item:not(:disabled):hover {
  background: linear-gradient(90deg, rgb(28 129 167 / 36%), rgb(4 24 44 / 40%));
  border-color: rgb(36 215 255 / 54%);
}

.queue-item:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.queue-icon {
  width: 32px;
  height: 32px;
  font-size: 17px;
  background: rgb(10 50 77 / 70%);
  border: 1px solid currentcolor;
  border-radius: 50%;
}

.queue-copy {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.queue-copy strong {
  font-size: 11px;
  color: #d7f3fc;
}

.queue-copy small {
  overflow: hidden;
  font-size: 9px;
  color: #527b8e;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.queue-item b {
  font-size: 24px;
  color: #f1fcff;
  text-shadow: 0 0 9px rgb(36 215 255 / 36%);
  font-variant-numeric: tabular-nums;
}

.core-panel {
  min-height: 378px;
  background:
    radial-gradient(circle at 50% 48%, rgb(14 116 160 / 20%), transparent 38%),
    linear-gradient(180deg, rgb(13 49 78 / 32%), transparent 64%), rgb(4 21 42 / 86%);
}

.core-visual {
  position: relative;
  height: 286px;
  max-width: 380px;
  margin: -4px auto -10px;
}

.core-visual :deep(.echart) {
  position: relative;
  z-index: 2;
}

.radar-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  border: 1px solid rgb(42 171 211 / 18%);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  box-shadow:
    0 0 20px rgb(28 141 187 / 10%),
    inset 0 0 20px rgb(28 141 187 / 8%);
}

.radar-ring::before,
.radar-ring::after {
  position: absolute;
  background: rgb(49 162 202 / 12%);
  content: '';
}

.radar-ring::before {
  top: 50%;
  left: -8%;
  width: 116%;
  height: 1px;
}

.radar-ring::after {
  top: -8%;
  left: 50%;
  width: 1px;
  height: 116%;
}

.radar-ring-one {
  width: 220px;
  height: 220px;
  animation: screen-radar 18s linear infinite;
}

.radar-ring-two {
  width: 168px;
  height: 168px;
  border-style: dashed;
  animation: screen-radar-reverse 12s linear infinite;
}

.core-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  padding: 0 4px 2px;
}

.core-stats > div {
  display: grid;
  min-width: 0;
  gap: 4px;
  padding: 8px 5px;
  text-align: center;
  background: rgb(7 36 62 / 58%);
  border-top: 1px solid rgb(45 155 196 / 28%);
  border-bottom: 1px solid rgb(45 155 196 / 14%);
}

.core-stats span {
  overflow: hidden;
  font-size: 8px;
  color: #577f91;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.core-stats strong {
  overflow: hidden;
  font-size: 14px;
  color: var(--screen-green);
  text-overflow: ellipsis;
  text-shadow: 0 0 8px rgb(80 255 201 / 30%);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.device-panel {
  min-height: 306px;
}

.sync-panel {
  min-height: 332px;
}

.sync-list {
  display: grid;
  gap: 6px;
  padding-top: 3px;
}

.sync-row {
  display: grid;
  min-height: 47px;
  padding: 7px 8px;
  background: rgb(6 31 55 / 52%);
  border-bottom: 1px solid rgb(38 113 145 / 20%);
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 8px;
}

.sync-state {
  width: 7px;
  height: 7px;
  background: var(--screen-orange);
  border-radius: 50%;
  box-shadow: 0 0 8px currentcolor;
}

.sync-state.is-succeeded {
  color: var(--screen-green);
  background: currentcolor;
}

.sync-state.is-failed {
  color: var(--screen-red);
  background: currentcolor;
}

.sync-row > div:not(.sync-count) {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.sync-row strong {
  font-size: 10px;
  color: #c9edf8;
}

.sync-row small {
  font-size: 8px;
  color: #4f7a8c;
}

.sync-count {
  display: grid;
  gap: 1px;
  text-align: right;
}

.sync-count strong {
  font-size: 14px;
  color: var(--screen-green);
  font-variant-numeric: tabular-nums;
}

.sync-status {
  min-width: 42px;
  padding: 2px 5px;
  font-size: 8px;
  color: #7fc7de;
  text-align: center;
  border: 1px solid rgb(57 152 187 / 30%);
}

.shortcut-panel {
  min-height: 352px;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px;
  padding-top: 3px;
}

.shortcut-item {
  display: grid;
  min-height: 112px;
  padding: 10px 8px;
  color: #5f8da0;
  text-align: center;
  cursor: pointer;
  background: linear-gradient(180deg, rgb(21 89 120 / 24%), transparent), rgb(6 31 55 / 54%);
  border: 1px solid rgb(48 137 171 / 24%);
  transition:
    border-color 0.2s ease,
    transform 0.2s ease;
  place-items: center;
}

.shortcut-item:hover {
  border-color: rgb(36 215 255 / 54%);
  transform: translateY(-2px);
}

.shortcut-item > span {
  width: 34px;
  height: 34px;
  font-size: 19px;
  border: 1px solid currentcolor;
  border-radius: 50%;
}

.shortcut-item > div {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.shortcut-item strong {
  font-size: 10px;
  color: #d3f2fc;
}

.shortcut-item small {
  overflow: hidden;
  font-size: 8px;
  color: #4d788a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shortcut-item > svg {
  display: none;
}

.screen-empty {
  display: grid;
  min-height: 210px;
  font-size: 10px;
  letter-spacing: 0.08em;
  color: #476f82;
  place-items: center;
}

.screen-footer {
  display: flex;
  min-height: 28px;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding-top: 8px;
  font-size: 8px;
  letter-spacing: 0.05em;
  color: #365f73;
}

.screen-footer i {
  width: 3px;
  height: 3px;
  background: var(--screen-cyan);
  border-radius: 50%;
  box-shadow: 0 0 6px var(--screen-cyan);
}

@keyframes screen-pulse {
  50% {
    opacity: 0.45;
    transform: scale(0.82);
  }
}

@keyframes screen-rotate {
  to {
    transform: rotate(360deg);
  }
}

@keyframes screen-radar {
  to {
    transform: translate(-50%, -50%) rotate(360deg);
  }
}

@keyframes screen-radar-reverse {
  to {
    transform: translate(-50%, -50%) rotate(-360deg);
  }
}

@media (width <= 1280px) {
  .command-layout {
    grid-template-columns: minmax(220px, 0.85fr) minmax(300px, 1.3fr) minmax(220px, 0.85fr);
  }

  .panel-title span,
  .metric-content small,
  .queue-copy small {
    display: none;
  }
}

@media (width <= 980px) {
  .screen-header {
    grid-template-columns: 1fr auto;
  }

  .header-left {
    display: none;
  }

  .screen-title {
    justify-content: flex-start;
  }

  .command-layout {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .center-column {
    order: -1;
    grid-column: 1 / -1;
    grid-template-columns: minmax(0, 1.25fr) minmax(0, 0.75fr);
  }
}

@media (width <= 760px) {
  .rental-command-center {
    padding: 10px;
  }

  .screen-header {
    grid-template-columns: 1fr;
  }

  .screen-title {
    justify-content: center;
  }

  .screen-title i,
  .header-right {
    display: none;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .command-layout,
  .center-column {
    grid-template-columns: 1fr;
  }

  .center-column {
    grid-column: auto;
  }

  .metric-card {
    min-height: 84px;
  }

  .screen-footer {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .live-dot,
  .is-rotating,
  .radar-ring-one,
  .radar-ring-two {
    animation: none;
  }
}
</style>
