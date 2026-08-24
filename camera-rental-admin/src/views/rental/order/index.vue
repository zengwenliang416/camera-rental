<template>
  <ContentWrap>
    <!-- Errors only; long help text moved out of the filter chrome -->
    <el-alert
      v-if="loadError"
      class="mb-12px"
      type="error"
      :closable="false"
      :title="t('rental.common.loadError')"
    >
      <el-button link type="primary" @click="getList">
        {{ t('rental.common.retry') }}
      </el-button>
    </el-alert>
    <el-alert
      v-if="shopLoadError"
      class="mb-12px"
      type="error"
      :closable="false"
      :title="t('rental.order.shopLoadError')"
    >
      <el-button link type="primary" @click="loadShops">
        {{ t('rental.common.retry') }}
      </el-button>
    </el-alert>

    <!-- Primary filter: local list only -->
    <el-form class="order-filter-form" :inline="true" :model="queryParams" @submit.prevent>
      <el-form-item :label="t('rental.order.externalOrderId')">
        <el-input
          v-model="queryParams.externalOrderId"
          class="!w-200px"
          clearable
          :placeholder="t('rental.order.externalOrderIdPlaceholder')"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="t('rental.order.filterShop')">
        <el-select
          v-model="queryParams.shopId"
          class="!w-200px"
          clearable
          filterable
          :placeholder="t('rental.order.shopPlaceholder')"
        >
          <el-option
            v-for="shop in shops"
            :key="shop.id"
            :label="shopLabel(shop)"
            :value="shop.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('rental.order.orderStatus')">
        <el-select
          v-model="queryParams.orderStatus"
          class="!w-150px"
          clearable
          :placeholder="t('common.selectText')"
        >
          <el-option
            v-for="option in orderStatusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('rental.order.conversionStatus')">
        <el-select
          v-model="queryParams.conversionStatus"
          class="!w-150px"
          clearable
          :placeholder="t('common.selectText')"
        >
          <el-option
            v-for="option in conversionStatusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('rental.order.reportDateRange')">
        <el-date-picker
          v-model="queryParams.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          class="!w-240px"
          :start-placeholder="t('rental.report.startDate')"
          :end-placeholder="t('rental.report.endDate')"
        />
      </el-form-item>
      <el-form-item :label="t('rental.order.rentalDateRange')">
        <el-date-picker
          v-model="queryParams.rentalDateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          class="!w-240px"
          :start-placeholder="t('rental.report.startDate')"
          :end-placeholder="t('rental.report.endDate')"
        />
      </el-form-item>
      <el-form-item :label="t('rental.order.shipDate')">
        <el-date-picker
          v-model="queryParams.shipDate"
          type="date"
          value-format="YYYY-MM-DD"
          class="!w-160px"
          clearable
          :placeholder="t('rental.order.shipDatePlaceholder')"
          :shortcuts="shipDateShortcuts"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" />{{ t('common.query') }}
        </el-button>
        <el-button @click="resetQuery">{{ t('common.reset') }}</el-button>
        <el-button link type="primary" @click="showAdvanced = !showAdvanced">
          {{ showAdvanced ? t('rental.order.hideAdvanced') : t('rental.order.showAdvanced') }}
          <Icon :icon="showAdvanced ? 'ep:arrow-up' : 'ep:arrow-down'" class="ml-4px" />
        </el-button>
        <el-button v-hasPermi="['rental:xianyu:sync']" @click="openSyncDialog">
          <Icon icon="ep:refresh" class="mr-5px" />{{ t('rental.order.syncAction') }}
        </el-button>
        <el-button
          v-hasPermi="['rental:xianyu:sync']"
          :loading="reparsing"
          @click="handleReparseRemarks"
        >
          <Icon icon="ep:magic-stick" class="mr-5px" />{{ t('rental.order.reparseAction') }}
        </el-button>
        <XianyuOrderColumnSettings v-model="selectedColumnKeys" />
      </el-form-item>
    </el-form>

    <!-- Secondary filter: rarely used IDs -->
    <el-form
      v-show="showAdvanced"
      class="order-advanced-form mb-12px"
      :inline="true"
      :model="queryParams"
      @submit.prevent
    >
      <el-form-item :label="t('rental.order.externalProductId')">
        <el-input
          v-model="queryParams.externalProductId"
          class="!w-180px"
          clearable
          :placeholder="t('rental.order.externalProductId')"
        />
      </el-form-item>
      <el-form-item :label="t('rental.order.externalSkuId')">
        <el-input
          v-model="queryParams.externalSkuId"
          class="!w-180px"
          clearable
          :placeholder="t('rental.order.externalSkuId')"
        />
      </el-form-item>
    </el-form>

    <p class="order-hint mb-12px">
      {{ t('rental.order.listHintShort') }}
    </p>

    <el-table v-loading="loading" :data="list">
      <el-table-column
        v-for="column in visibleOrderColumns"
        :key="column.key"
        :prop="column.key === 'shopName' ? undefined : column.key"
        :label="orderColumnLabel(column)"
        :width="column.minWidth ? undefined : column.width"
        :min-width="column.minWidth || column.width"
        :fixed="column.key === 'id' ? 'left' : undefined"
        :show-overflow-tooltip="isOverflowColumn(column)"
      >
        <template #default="{ row }">
          <el-button v-if="column.key === 'id'" link type="primary" @click="openOrderDetail(row)">
            {{ row.id }}
          </el-button>
          <span v-else-if="column.key === 'shopName'">{{ shopNameById(row.shopId) }}</span>
          <el-tag
            v-else-if="column.key === 'orderStatus'"
            :type="getRentalTagType('channelOrder', row.orderStatus)"
          >
            {{ rentalLabel('channelOrder', row.orderStatus) }}
          </el-tag>
          <el-tag
            v-else-if="column.key === 'conversionStatus'"
            :type="getRentalTagType('conversion', row.conversionStatus)"
          >
            {{ rentalLabel('conversion', row.conversionStatus) }}
          </el-tag>
          <el-tooltip
            v-else-if="column.key === 'remarkParseStatus'"
            :content="
              [remarkReasonLabel(row.rentalPeriodReasonCode), row.remarkParseModel]
                .filter(Boolean)
                .join(' / ')
            "
            placement="top"
          >
            <div class="flex items-center gap-4px">
              <el-tag :type="remarkParseTagType(row.remarkParseStatus)">
                {{ remarkParseStatusLabel(row.remarkParseStatus) }}
              </el-tag>
              <span class="text-12px text-[var(--el-text-color-secondary)]">
                {{
                  row.remarkParseSource === 'AI'
                    ? 'AI'
                    : row.remarkParseSource === 'RULE'
                      ? '规则'
                      : '-'
                }}
                <template v-if="row.remarkParseConfidence">
                  {{ Math.round(row.remarkParseConfidence * 100) }}%
                </template>
              </span>
            </div>
          </el-tooltip>
          <span v-else>{{ formatOrderColumnValue(row, column) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('table.action')" width="320" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openOrderDetail(row)">
            {{ t('rental.order.viewDetail') }}
          </el-button>
          <el-button
            v-if="canShipOrder(row)"
            v-hasPermi="['rental:xianyu:ship']"
            link
            type="primary"
            @click="openShipWorkbench(row)"
          >
            {{ t('rental.order.shipAction') }}
          </el-button>
          <el-button
            v-if="canRetryConvert(row.conversionStatus)"
            link
            type="primary"
            v-hasPermi="['rental:order:convert']"
            :title="t('rental.order.convertHint')"
            @click="handleConvert(row.id)"
          >
            {{ t('rental.order.convert') }}
          </el-button>
          <el-button
            v-if="canBackfillDispatch(row)"
            v-hasPermi="['rental:xianyu:ship']"
            link
            type="warning"
            @click="openBackfillDialog(row)"
          >
            {{ t('rental.order.backfillAction') }}
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="py-24px text-[var(--el-text-color-secondary)]">
          {{ t('rental.order.emptyHint') }}
        </div>
      </template>
    </el-table>

    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-drawer
      v-model="detailVisible"
      :title="t('rental.order.detailTitle')"
      size="min(840px, 96vw)"
      destroy-on-close
    >
      <XianyuOrderDetailPanel v-if="detailOrder" :order="detailOrder" />
    </el-drawer>

    <XianyuDispatchBackfillDialog
      v-model="backfillVisible"
      :order="backfillOrder"
      @completed="handleBackfillCompleted"
    />

    <el-drawer
      v-model="shipDrawerVisible"
      :title="t('rental.order.shipDrawerTitle')"
      size="min(960px, 96vw)"
      destroy-on-close
    >
      <XianyuShipWorkbench
        v-if="shipDrawerOrder"
        :initial-order="shipDrawerOrder"
        @shipped="handleOrderShipped"
      />
    </el-drawer>

    <!-- Manual catch-up: secondary, not mixed into filter bar -->
    <el-dialog
      v-model="syncVisible"
      :title="t('rental.order.syncDialogTitle')"
      width="560px"
      destroy-on-close
    >
      <el-alert class="mb-16px" type="info" :closable="false" :title="t('rental.order.syncHint')" />
      <el-form ref="syncFormRef" :model="syncForm" label-width="110px" @submit.prevent>
        <el-form-item :label="t('rental.order.syncShop')" required>
          <el-select
            v-model="syncForm.shopId"
            class="!w-100%"
            filterable
            :placeholder="t('rental.order.shopPlaceholder')"
          >
            <el-option
              v-for="shop in shops"
              :key="shop.id"
              :label="shopLabel(shop)"
              :value="shop.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('rental.order.windowStart')" required>
          <el-date-picker
            v-model="syncForm.windowStart"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            class="!w-100%"
            :placeholder="t('rental.order.windowStart')"
          />
        </el-form-item>
        <el-form-item :label="t('rental.order.windowEnd')" required>
          <el-date-picker
            v-model="syncForm.windowEnd"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            class="!w-100%"
            :placeholder="t('rental.order.windowEnd')"
          />
        </el-form-item>
        <el-form-item :label="t('rental.order.pageSize')">
          <el-input-number
            v-model="syncForm.pageSize"
            :min="1"
            :max="100"
            controls-position="right"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fillLast30Days">{{ t('rental.order.fillLast30Days') }}</el-button>
        <el-button @click="syncVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="syncing" @click="handleSync">
          {{ t('rental.order.syncPage') }}
        </el-button>
      </template>
    </el-dialog>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { computed, ref, reactive, onMounted, watch } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'
import { useCache } from '@/hooks/web/useCache'
import {
  convertXianyuOrder,
  getXianyuOrderPage,
  getXianyuShopPage,
  reparseXianyuSellerRemarks,
  syncXianyuOrderPage,
  type XianyuOrderVO,
  type XianyuShopVO
} from '@/api/rental/xianyu'
import { toEpochMillis } from '@/utils/rentalDate'
import { formatDate, formatNullableDate } from '@/utils/formatTime'
import { fenToYuan } from '@/utils'
import {
  getRentalLabelKey,
  getRentalStatusValues,
  getRentalTagType,
  type RentalLabelGroup
} from '@/utils/rentalLabels'
import XianyuShipWorkbench from './components/XianyuShipWorkbench.vue'
import XianyuDispatchBackfillDialog from './components/XianyuDispatchBackfillDialog.vue'
import XianyuOrderDetailPanel from './components/XianyuOrderDetailPanel.vue'
import XianyuOrderColumnSettings from './components/XianyuOrderColumnSettings.vue'
import {
  XIANYU_ORDER_COLUMNS,
  sanitizePersistedXianyuOrderColumnKeys,
  type XianyuOrderColumnDefinition,
  type XianyuOrderColumnKey
} from './components/xianyuOrderColumns'

defineOptions({ name: 'RentalChannelOrder' })
const { t } = useI18n()
const message = useMessage()
const { wsCache } = useCache()
const orderColumnCacheKey = 'rental:xianyu:order:visible-columns:v1'

const loading = ref(false)
const syncing = ref(false)
const reparsing = ref(false)
const loadError = ref(false)
const shopLoadError = ref(false)
const showAdvanced = ref(false)
const syncVisible = ref(false)
const detailVisible = ref(false)
const shipDrawerVisible = ref(false)
const backfillVisible = ref(false)
const detailOrder = ref<XianyuOrderVO>()
const shipDrawerOrder = ref<XianyuOrderVO>()
const backfillOrder = ref<XianyuOrderVO>()
const list = ref<XianyuOrderVO[]>([])
const shops = ref<XianyuShopVO[]>([])
const total = ref(0)
const selectedColumnKeys = ref<XianyuOrderColumnKey[]>(
  sanitizePersistedXianyuOrderColumnKeys(wsCache.get(orderColumnCacheKey))
)
const visibleOrderColumns = computed(() => {
  const selected = new Set(selectedColumnKeys.value)
  return XIANYU_ORDER_COLUMNS.filter((column) => selected.has(column.key))
})
const orderStatusOptions = computed(() =>
  getRentalStatusValues('channelOrder').map((value) => ({
    value,
    label: rentalLabel('channelOrder', value)
  }))
)
const conversionStatusOptions = computed(() =>
  getRentalStatusValues('conversion').map((value) => ({
    value,
    label: rentalLabel('conversion', value)
  }))
)
const shipDateShortcuts = computed(() => [
  {
    text: t('rental.order.today'),
    value: () => new Date()
  }
])
const route = useRoute()
const queryParams = reactive<{
  pageNo: number
  pageSize: number
  shopId?: number
  orderStatus?: string
  conversionStatus?: string
  externalOrderId?: string
  externalProductId?: string
  externalSkuId?: string
  dateRange?: [string, string]
  rentalDateRange?: [string, string]
  shipDate?: string
}>({
  pageNo: 1,
  pageSize: 10
})

const syncForm = reactive({
  shopId: undefined as number | undefined,
  windowStart: '',
  windowEnd: '',
  pageNo: 1,
  pageSize: 20
})

const shopLabel = (shop: XianyuShopVO) => {
  const auth = rentalLabel('auth', shop.authorizationStatus)
  return `${shop.shopName || '-'} (#${shop.id} · ${auth})`
}

const rentalLabel = (group: RentalLabelGroup, value?: string | number | null) => {
  return t(getRentalLabelKey(group, value), { code: value ?? '' })
}

const formatYuan = (amount?: number) => {
  return amount == null ? '-' : t('rental.common.yuanAmount', { amount: fenToYuan(amount) })
}

const orderColumnLabel = (column: XianyuOrderColumnDefinition) => {
  if (!column.labelKey) return column.label
  const translated = t(column.labelKey)
  return translated === column.labelKey ? column.label : translated
}

const isOverflowColumn = (column: XianyuOrderColumnDefinition) => {
  return column.format === 'text' || column.format === 'array'
}

const normalizeDateValue = (value: unknown, includeTime: boolean) => {
  if (Array.isArray(value) && value.length >= 3) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value
    const date = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    if (!includeTime) return date
    return `${date} ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(
      second
    ).padStart(2, '0')}`
  }
  if (typeof value !== 'string' || !value.trim()) return '-'
  return includeTime ? formatNullableDate(value) : value.trim()
}

const formatOrderColumnValue = (order: XianyuOrderVO, column: XianyuOrderColumnDefinition) => {
  const value =
    column.key === 'shopName'
      ? shopNameById(order.shopId)
      : order[column.key as keyof XianyuOrderVO]
  if (column.key === 'rentalPeriodReasonCode') {
    return remarkReasonLabel(typeof value === 'string' ? value : undefined)
  }
  if (column.format === 'amount-fen') {
    return typeof value === 'number' ? formatYuan(value) : '-'
  }
  if (column.format === 'date') {
    return normalizeDateValue(value, false)
  }
  if (column.format === 'datetime') {
    return normalizeDateValue(value, true)
  }
  if (column.format === 'boolean') {
    return typeof value === 'boolean' ? t(value ? 'common.yes' : 'common.no') : '-'
  }
  if (column.format === 'confidence') {
    return typeof value === 'number' ? `${Math.round(value * 100)}%` : '-'
  }
  if (column.format === 'array') {
    return Array.isArray(value) && value.length > 0 ? value.join(', ') : '-'
  }
  if (typeof value === 'string') {
    return value.trim() || '-'
  }
  return value ?? '-'
}

const shopNameById = (id?: number) => {
  if (id == null) return '-'
  return shops.value.find((s) => s.id === id)?.shopName || String(id)
}

const openOrderDetail = (order: XianyuOrderVO) => {
  detailOrder.value = order
  detailVisible.value = true
}

const pendingShipStatuses = new Set(['12', 'WAIT_SHIP', 'WAIT_SEND', 'WAIT_SELLER_SEND_GOODS'])

const canShipOrder = (order: XianyuOrderVO) => {
  return pendingShipStatuses.has(order.orderStatus)
}

const backfillableStatuses = new Set(['21', '22'])

const canBackfillDispatch = (order: XianyuOrderVO) => {
  return backfillableStatuses.has(order.orderStatus) && !order.cancelTime
}

const openShipWorkbench = (order: XianyuOrderVO) => {
  shipDrawerOrder.value = order
  shipDrawerVisible.value = true
}

const openBackfillDialog = (order: XianyuOrderVO) => {
  backfillOrder.value = order
  backfillVisible.value = true
}

const handleBackfillCompleted = async () => {
  await getList()
}

const handleOrderShipped = async () => {
  await getList()
}

watch(selectedColumnKeys, (keys) => {
  const sanitized = sanitizePersistedXianyuOrderColumnKeys(keys)
  wsCache.set(orderColumnCacheKey, sanitized)
})

const fillLast30Days = () => {
  const end = Date.now()
  const start = end - 30 * 24 * 60 * 60 * 1000
  syncForm.windowStart = formatDate(start)
  syncForm.windowEnd = formatDate(end)
}

const openSyncDialog = () => {
  if (!syncForm.windowStart || !syncForm.windowEnd) {
    fillLast30Days()
  }
  if (!syncForm.shopId && shops.value.length > 0) {
    const preferred = shops.value.find((s) => s.authorizationStatus === 'VALID') || shops.value[0]
    syncForm.shopId = preferred.id
  }
  // Prefer aligning with current list filter when set
  if (queryParams.shopId) {
    syncForm.shopId = queryParams.shopId
  }
  syncVisible.value = true
}

const loadShops = async () => {
  shopLoadError.value = false
  try {
    const data = await getXianyuShopPage({ pageNo: 1, pageSize: 100 })
    shops.value = data.list || []
    if (!syncForm.shopId && shops.value.length > 0) {
      // Prefer VALID shops under current app (ignore legacy INVALID / expired auth).
      const preferred = shops.value.find((s) => s.authorizationStatus === 'VALID') || shops.value[0]
      syncForm.shopId = preferred.id
    }
  } catch {
    shops.value = []
    shopLoadError.value = true
  }
}

const remarkParseStatusLabel = (status?: string) => {
  if (status && ['PENDING', 'SUCCESS', 'FAILED'].includes(status)) {
    return t(`rental.order.remarkParse.${status}`)
  }
  return t('rental.order.remarkParse.UNKNOWN')
}

const remarkParseTagType = (status?: string) => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

const remarkReasonLabel = (reason?: string) => {
  if (!reason) return '-'
  const key = `rental.order.remarkReason.${reason}`
  const translated = t(key, { code: reason })
  return translated === key ? reason : translated
}

const getList = async () => {
  loading.value = true
  loadError.value = false
  try {
    const data = await getXianyuOrderPage({
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      shopId: queryParams.shopId,
      orderStatus: queryParams.orderStatus || undefined,
      conversionStatus: queryParams.conversionStatus || undefined,
      externalOrderId: queryParams.externalOrderId?.trim() || undefined,
      externalProductId: queryParams.externalProductId || undefined,
      externalSkuId: queryParams.externalSkuId || undefined,
      startDate: queryParams.dateRange?.[0],
      endDate: queryParams.dateRange?.[1],
      shipDate: queryParams.shipDate,
      rentalStartDate: queryParams.rentalDateRange?.[0],
      rentalEndDate: queryParams.rentalDateRange?.[1]
    })
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
  queryParams.shopId = undefined
  queryParams.orderStatus = undefined
  queryParams.conversionStatus = undefined
  queryParams.externalOrderId = undefined
  queryParams.externalProductId = undefined
  queryParams.externalSkuId = undefined
  queryParams.dateRange = undefined
  queryParams.rentalDateRange = undefined
  queryParams.shipDate = undefined
  queryParams.pageNo = 1
  showAdvanced.value = false
  await getList()
}

const handleSync = async () => {
  const windowStart = toEpochMillis(syncForm.windowStart)
  const windowEnd = toEpochMillis(syncForm.windowEnd)
  if (!syncForm.shopId || windowStart == null || windowEnd == null) {
    message.warning(t('rental.order.syncRequired'))
    return
  }
  if (windowStart >= windowEnd) {
    message.warning(t('rental.order.windowInvalid'))
    return
  }
  syncing.value = true
  try {
    const result = await syncXianyuOrderPage({
      shopId: Number(syncForm.shopId),
      windowStart,
      windowEnd,
      pageNo: syncForm.pageNo,
      pageSize: syncForm.pageSize
    })
    message.success(
      t('rental.order.syncSuccess', {
        received: result.receivedCount,
        succeeded: result.succeededCount
      })
    )
    syncVisible.value = false
    queryParams.shopId = syncForm.shopId
    queryParams.pageNo = 1
    await getList()
  } finally {
    syncing.value = false
  }
}

const handleReparseRemarks = async () => {
  try {
    await message.confirm(t('rental.order.reparseConfirm'))
  } catch {
    return
  }
  reparsing.value = true
  try {
    const processed = await reparseXianyuSellerRemarks()
    message.success(t('rental.order.reparseSuccess', { processed }))
    await getList()
  } finally {
    reparsing.value = false
  }
}

/** Hermes-style auto-convert is primary; manual retry only for pending/review rows. */
const canRetryConvert = (status?: string) => status === 'PENDING' || status === 'REVIEW_REQUIRED'

const handleConvert = async (id: number) => {
  const result = await convertXianyuOrder(id)
  const statusText = rentalLabel('conversion', result.status)
  message.success(
    result.reviewId
      ? t('rental.order.conversionReviewResult', {
          status: statusText,
          reasonCode: result.reasonCode || '-',
          reviewId: result.reviewId
        })
      : result.reasonCode
        ? t('rental.order.conversionResult', {
            status: statusText,
            reasonCode: result.reasonCode
          })
        : statusText
  )
  await getList()
}

onMounted(async () => {
  const shopId = Number(route.query.shopId)
  queryParams.shopId = Number.isInteger(shopId) && shopId > 0 ? shopId : undefined
  queryParams.orderStatus =
    typeof route.query.orderStatus === 'string' ? route.query.orderStatus : undefined
  queryParams.externalOrderId =
    typeof route.query.externalOrderId === 'string' ? route.query.externalOrderId : undefined
  queryParams.externalProductId =
    typeof route.query.externalProductId === 'string' ? route.query.externalProductId : undefined
  queryParams.externalSkuId =
    typeof route.query.externalSkuId === 'string' ? route.query.externalSkuId : undefined
  if (queryParams.externalProductId || queryParams.externalSkuId) {
    showAdvanced.value = true
  }
  if (typeof route.query.startDate === 'string' && typeof route.query.endDate === 'string') {
    queryParams.dateRange = [route.query.startDate, route.query.endDate]
  }
  if (
    typeof route.query.rentalStartDate === 'string' &&
    typeof route.query.rentalEndDate === 'string'
  ) {
    queryParams.rentalDateRange = [route.query.rentalStartDate, route.query.rentalEndDate]
  }
  queryParams.shipDate = typeof route.query.shipDate === 'string' ? route.query.shipDate : undefined
  fillLast30Days()
  await loadShops()
  await getList()
})
</script>

<style scoped>
.order-filter-form {
  margin-bottom: 4px;
}

.order-advanced-form {
  padding: 12px 12px 0;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
}

.order-hint {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
}
</style>
