<template>
  <Error v-if="!isSuperAdmin" type="403" />
  <ContentWrap v-else>
    <el-alert
      v-if="loadError"
      class="mb-16px"
      type="error"
      :closable="false"
      :title="t('rental.common.loadError')"
    >
      <el-button link type="primary" @click="retryAll">
        {{ t('rental.common.retry') }}
      </el-button>
    </el-alert>
    <XianyuConfigPanel ref="configPanelRef" @load-error="loadError = true" />

    <ContentWrap>
      <div class="mb-12px flex flex-wrap items-center gap-8px">
        <el-button
          type="primary"
          @click="handleSyncShops"
          v-hasPermi="['rental:xianyu:sync']"
          :loading="syncing"
        >
          {{ t('rental.xianyu.syncShops') }}
        </el-button>
        <el-button @click="getList">{{ t('common.query') }}</el-button>
        <span class="text-13px text-[var(--el-text-color-secondary)]">{{
          t('rental.xianyu.shopListHint')
        }}</span>
      </div>
      <el-table v-loading="loading" :data="list">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="shopName" :label="t('rental.xianyu.shopName')" min-width="140" />
        <el-table-column :label="t('rental.xianyu.externalShopId')" min-width="160">
          <template #default="{ row }">
            {{ maskChannelIdentifier(row.externalShopId) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.xianyu.authorizeId')" min-width="160">
          <template #default="{ row }">
            {{ maskChannelIdentifier(row.authorizeId) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.xianyu.authStatus')" width="120">
          <template #default="{ row }">
            <el-tag :type="getRentalTagType('auth', row.authorizationStatus)">
              {{ rentalLabel('auth', row.authorizationStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.xianyu.authorizationExpiresAt')" width="180">
          <template #default="{ row }">
            {{ formatNullableDate(row.authorizationExpiresAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.xianyu.guaranteeStatus')" width="140">
          <template #default="{ row }">
            {{ xianyuValueLabel('guaranteeStatuses', row.guaranteeStatus) }}
          </template>
        </el-table-column>
        <template #empty>
          <div class="py-24px text-[var(--el-text-color-secondary)]">
            {{ t('rental.xianyu.emptyHint') }}
          </div>
        </template>
      </el-table>
      <Pagination
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </ContentWrap>

    <ContentWrap v-if="canReplayPushEvent">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ t('rental.xianyu.replayTitle') }}</span>
          <span class="text-13px text-[var(--el-text-color-secondary)]">{{
            t('rental.xianyu.replayHint')
          }}</span>
        </div>
      </template>
      <el-form class="-mb-15px" :inline="true" :model="replayForm">
        <el-form-item :label="t('rental.xianyu.pushEventId')">
          <el-input-number
            v-model="replayForm.eventId"
            class="!w-220px"
            :min="1"
            :precision="0"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="warning"
            @click="handleReplayPushEvent"
            :loading="replayLoading"
            v-hasPermi="['rental:xianyu:replay']"
          >
            {{ t('rental.xianyu.replayPushEvent') }}
          </el-button>
        </el-form-item>
        <el-form-item :label="t('rental.xianyu.rawPayloadId')">
          <el-input-number
            v-model="replayForm.rawPayloadId"
            class="!w-220px"
            :min="1"
            :precision="0"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="warning"
            plain
            @click="handleReplayRawPayload"
            :loading="replayLoading"
            v-hasPermi="['rental:xianyu:replay']"
          >
            {{ t('rental.xianyu.replayRawPayload') }}
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <ContentWrap v-if="canViewRawPayload">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ t('rental.xianyu.rawPayloadTitle') }}</span>
          <el-button
            @click="getRawPayloadList"
            :loading="rawPayloadLoading"
            v-hasPermi="['rental:xianyu:raw']"
          >
            <Icon icon="ep:refresh" class="mr-5px" />{{ t('common.refresh') }}
          </el-button>
        </div>
      </template>
      <el-alert
        class="mb-12px"
        type="warning"
        :closable="false"
        :title="t('rental.xianyu.rawPayloadHint')"
      />
      <el-form class="-mb-15px" :inline="true" :model="rawPayloadQuery">
        <el-form-item :label="t('rental.xianyu.rawSourceType')">
          <el-input
            v-model="rawPayloadQuery.sourceType"
            class="!w-180px"
            clearable
            :placeholder="t('common.inputText')"
          />
        </el-form-item>
        <el-form-item :label="t('rental.xianyu.rawSourceIdentifier')">
          <el-input
            v-model="rawPayloadQuery.sourceIdentifier"
            class="!w-220px"
            clearable
            :placeholder="t('common.inputText')"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="getRawPayloadList" v-hasPermi="['rental:xianyu:raw']">
            {{ t('common.query') }}
          </el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="rawPayloadLoading" :data="rawPayloadList">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="sourceType" :label="t('rental.xianyu.rawSourceType')" width="160" />
        <el-table-column :label="t('rental.xianyu.rawSourceIdentifier')" min-width="180">
          <template #default="{ row }">
            {{ maskChannelIdentifier(row.sourceIdentifier) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="payloadHash"
          :label="t('rental.xianyu.payloadHash')"
          min-width="220"
        />
        <el-table-column
          prop="schemaVersion"
          :label="t('rental.xianyu.schemaVersion')"
          width="130"
        />
        <el-table-column
          prop="redactionVersion"
          :label="t('rental.xianyu.redactionVersion')"
          width="130"
        />
        <el-table-column :label="t('rental.xianyu.receivedAt')" width="180">
          <template #default="{ row }">
            {{ formatNullableDate(row.receivedAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('common.action')" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="handleViewRawPayload(row)"
              v-hasPermi="['rental:xianyu:raw']"
            >
              {{ t('rental.xianyu.viewMaskedPayload') }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="py-24px text-[var(--el-text-color-secondary)]">
            {{ t('rental.xianyu.rawPayloadEmptyHint') }}
          </div>
        </template>
      </el-table>
      <Pagination
        :total="rawPayloadTotal"
        v-model:page="rawPayloadQuery.pageNo"
        v-model:limit="rawPayloadQuery.pageSize"
        @pagination="getRawPayloadList"
      />
    </ContentWrap>

    <ContentWrap>
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ t('rental.xianyu.alertTitle') }}</span>
          <el-button @click="getAlertList" :loading="alertLoading">
            <Icon icon="ep:refresh" class="mr-5px" />{{ t('common.refresh') }}
          </el-button>
        </div>
      </template>
      <el-table v-loading="alertLoading" :data="alertList">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="shopId" :label="t('rental.order.shopId')" width="100" />
        <el-table-column :label="t('rental.xianyu.alertType')" min-width="160">
          <template #default="{ row }">
            {{ xianyuValueLabel('alertTypes', row.alertType) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.xianyu.alertSeverity')" width="120">
          <template #default="{ row }">
            <el-tag :type="row.severity === 'ERROR' ? 'danger' : 'warning'">
              {{ xianyuValueLabel('alertSeverities', row.severity) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.xianyu.alertStatus')" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'OPEN' ? 'danger' : 'success'">
              {{ xianyuValueLabel('alertStatuses', row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.xianyu.alertSource')" min-width="160">
          <template #default="{ row }">
            {{ maskChannelIdentifier(row.sourceIdentifier) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.xianyu.alertMessage')" min-width="240">
          <template #default="{ row }">
            {{ maskSensitiveText(row.message) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.xianyu.lastSeenAt')" width="180">
          <template #default="{ row }">
            {{ formatNullableDate(row.lastSeenAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('common.action')" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'OPEN'"
              link
              type="primary"
              v-hasPermi="['rental:xianyu:sync']"
              @click="handleResolveAlert(row)"
            >
              {{ t('rental.xianyu.resolveAlert') }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="py-24px text-[var(--el-text-color-secondary)]">
            {{ t('rental.xianyu.alertEmptyHint') }}
          </div>
        </template>
      </el-table>
      <Pagination
        :total="alertTotal"
        v-model:page="alertQuery.pageNo"
        v-model:limit="alertQuery.pageSize"
        @pagination="getAlertList"
      />
    </ContentWrap>

    <ContentWrap>
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ t('rental.xianyu.productSyncTitle') }}</span>
          <span class="text-13px text-[var(--el-text-color-secondary)]">{{
            t('rental.xianyu.productSyncHint')
          }}</span>
        </div>
      </template>
      <el-form class="-mb-15px" :inline="true" :model="productSyncForm">
        <el-form-item :label="t('rental.xianyu.shopName')">
          <el-select
            v-model="productSyncForm.shopId"
            class="!w-220px"
            filterable
            :placeholder="t('rental.order.shopPlaceholder')"
          >
            <el-option
              v-for="shop in list"
              :key="shop.id"
              :label="shop.shopName || String(shop.id)"
              :value="shop.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('rental.xianyu.productUpdateTimeRange')">
          <el-date-picker
            v-model="productSyncForm.updateRange"
            type="datetimerange"
            value-format="x"
            :range-separator="t('rental.xianyu.dateSeparator')"
            :start-placeholder="t('rental.xianyu.startTime')"
            :end-placeholder="t('rental.xianyu.endTime')"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleSyncProducts"
            v-hasPermi="['rental:xianyu:sync']"
            :loading="productSyncing"
          >
            {{ t('rental.xianyu.syncProducts') }}
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <ContentWrap>
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ t('rental.xianyu.afterSaleTitle') }}</span>
          <span class="text-13px text-[var(--el-text-color-secondary)]">{{
            t('rental.xianyu.afterSaleHint')
          }}</span>
        </div>
      </template>
      <el-form class="-mb-15px" :inline="true" :model="afterSaleSyncForm">
        <el-form-item :label="t('rental.xianyu.shopName')">
          <el-select
            v-model="afterSaleSyncForm.shopId"
            class="!w-220px"
            filterable
            :placeholder="t('rental.order.shopPlaceholder')"
          >
            <el-option
              v-for="shop in list"
              :key="shop.id"
              :label="shop.shopName || String(shop.id)"
              :value="shop.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('rental.xianyu.applyTimeRange')">
          <el-date-picker
            v-model="afterSaleSyncForm.applyRange"
            type="datetimerange"
            value-format="x"
            :range-separator="t('rental.xianyu.dateSeparator')"
            :start-placeholder="t('rental.xianyu.startTime')"
            :end-placeholder="t('rental.xianyu.endTime')"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleSyncAfterSales"
            v-hasPermi="['rental:xianyu:sync']"
            :loading="afterSaleSyncing"
          >
            {{ t('rental.xianyu.syncAfterSales') }}
          </el-button>
          <el-button @click="getAfterSaleList">{{ t('common.query') }}</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="afterSaleLoading" :data="afterSaleList">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="shopId" :label="t('rental.order.shopId')" width="100" />
        <el-table-column :label="t('rental.xianyu.externalAfterSaleId')" min-width="180">
          <template #default="{ row }">
            {{ maskChannelIdentifier(row.externalAfterSaleId) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="externalOrderId"
          :label="t('rental.order.externalOrderId')"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column
          prop="afterSaleStatus"
          :label="t('rental.xianyu.afterSaleStatus')"
          width="130"
        />
        <el-table-column :label="t('rental.xianyu.refundAmountFen')" width="140">
          <template #default="{ row }">
            {{ row.refundAmount ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="amountUnitStatus"
          :label="t('rental.xianyu.amountUnitStatus')"
          width="150"
        />
        <el-table-column :label="t('rental.xianyu.timeoutAt')" width="180">
          <template #default="{ row }">
            {{ formatNullableDate(row.timeoutAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.xianyu.sourceUpdatedAt')" width="180">
          <template #default="{ row }">
            {{ formatNullableDate(row.sourceUpdatedAt) }}
          </template>
        </el-table-column>
        <template #empty>
          <div class="py-24px text-[var(--el-text-color-secondary)]">
            {{ t('rental.xianyu.afterSaleEmptyHint') }}
          </div>
        </template>
      </el-table>
      <Pagination
        :total="afterSaleTotal"
        v-model:page="afterSaleQuery.pageNo"
        v-model:limit="afterSaleQuery.pageSize"
        @pagination="getAfterSaleList"
      />
    </ContentWrap>

    <ContentWrap>
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ t('rental.xianyu.expressCompanyTitle') }}</span>
          <el-button @click="loadExpressCompanies" :loading="expressLoading">
            <Icon icon="ep:refresh" class="mr-5px" />{{ t('common.refresh') }}
          </el-button>
        </div>
      </template>
      <el-table v-loading="expressLoading" :data="expressList">
        <el-table-column prop="code" :label="t('rental.xianyu.expressCode')" width="160" />
        <el-table-column
          prop="expressName"
          :label="t('rental.xianyu.expressName')"
          min-width="180"
        />
        <el-table-column
          prop="expressAlias"
          :label="t('rental.xianyu.expressAlias')"
          min-width="160"
        />
        <el-table-column :label="t('rental.xianyu.hotExpress')" width="120">
          <template #default="{ row }">
            <el-tag :type="row.hot ? 'success' : 'info'">
              {{ row.hot ? t('common.yes') : t('common.no') }}
            </el-tag>
          </template>
        </el-table-column>
        <template #empty>
          <div class="py-24px text-[var(--el-text-color-secondary)]">
            {{ t('rental.xianyu.expressEmptyHint') }}
          </div>
        </template>
      </el-table>
    </ContentWrap>

    <el-dialog
      v-model="rawPayloadDialogVisible"
      :title="t('rental.xianyu.rawPayloadDetailTitle')"
      width="760px"
    >
      <el-descriptions v-if="rawPayloadDetail" :column="2" border class="mb-12px">
        <el-descriptions-item label="ID">{{ rawPayloadDetail.id }}</el-descriptions-item>
        <el-descriptions-item :label="t('rental.xianyu.rawSourceType')">
          {{ rawPayloadDetail.sourceType }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('rental.xianyu.rawSourceIdentifier')">
          {{ maskChannelIdentifier(rawPayloadDetail.sourceIdentifier) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('rental.xianyu.receivedAt')">
          {{ formatNullableDate(rawPayloadDetail.receivedAt) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('rental.xianyu.payloadHash')" :span="2">
          {{ rawPayloadDetail.payloadHash }}
        </el-descriptions-item>
      </el-descriptions>
      <pre
        class="max-h-520px overflow-auto rounded bg-[var(--el-fill-color-light)] p-12px text-12px leading-5"
        >{{ rawPayloadDetail?.maskedPayload || '-' }}</pre
      >
      <template #footer>
        <el-button @click="rawPayloadDialogVisible = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'
import { hasPermission } from '@/directives/permission/hasPermi'
import { useUserStore } from '@/store/modules/user'
import {
  getXianyuAfterSalePage,
  getXianyuAlertPage,
  getXianyuExpressCompanyList,
  getXianyuRawPayload,
  getXianyuRawPayloadPage,
  getXianyuShopPage,
  replayXianyuPushEvent,
  replayXianyuRawPayload,
  resolveXianyuAlert,
  syncXianyuAfterSalePage,
  syncAuthorizedShops,
  syncXianyuProductPage,
  type XianyuAlertVO,
  type XianyuAfterSaleVO,
  type XianyuExpressCompanyVO,
  type XianyuRawPayloadVO,
  type XianyuShopVO
} from '@/api/rental/xianyu'
import { getRentalLabelKey, getRentalTagType, type RentalLabelGroup } from '@/utils/rentalLabels'
import { formatNullableDate } from '@/utils/formatTime'
import { toEpochMillis } from '@/utils/rentalDate'
import { maskChannelIdentifier, maskSensitiveText } from '@/utils/rentalPrivacy'
import XianyuConfigPanel from './components/XianyuConfigPanel.vue'

defineOptions({ name: 'RentalXianyuOps' })
const { t } = useI18n()
const message = useMessage()
const userStore = useUserStore()

const configPanelRef = ref<{ reload: () => Promise<void> }>()
const isSuperAdmin = computed(() => userStore.getRoles.includes('super_admin'))
const loadError = ref(false)
const loading = ref(false)
const syncing = ref(false)
const list = ref<XianyuShopVO[]>([])
const total = ref(0)
const queryParams = reactive({ pageNo: 1, pageSize: 10 })
const afterSaleLoading = ref(false)
const afterSaleSyncing = ref(false)
const afterSaleList = ref<XianyuAfterSaleVO[]>([])
const afterSaleTotal = ref(0)
const afterSaleQuery = reactive({ pageNo: 1, pageSize: 10 })
const productSyncing = ref(false)
const productSyncForm = reactive<{
  shopId?: number
  updateRange: string[] | null
}>({
  shopId: undefined,
  updateRange: []
})
const alertLoading = ref(false)
const alertList = ref<XianyuAlertVO[]>([])
const alertTotal = ref(0)
const alertQuery = reactive({ pageNo: 1, pageSize: 10, status: 'OPEN' })
const rawPayloadLoading = ref(false)
const rawPayloadList = ref<XianyuRawPayloadVO[]>([])
const rawPayloadTotal = ref(0)
const rawPayloadQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  sourceType: '',
  sourceIdentifier: ''
})
const rawPayloadDialogVisible = ref(false)
const rawPayloadDetail = ref<XianyuRawPayloadVO>()
const afterSaleSyncForm = reactive<{
  shopId?: number
  applyRange: string[] | null
}>({
  shopId: undefined,
  applyRange: []
})
const expressLoading = ref(false)
const expressList = ref<XianyuExpressCompanyVO[]>([])
const canViewRawPayload = computed(() => hasPermission(['rental:xianyu:raw']))
const canReplayPushEvent = computed(() => hasPermission(['rental:xianyu:replay']))
const replayLoading = ref(false)
const replayForm = reactive<{ eventId?: number; rawPayloadId?: number }>({
  eventId: undefined,
  rawPayloadId: undefined
})

const rentalLabel = (group: RentalLabelGroup, value?: string | null) => {
  return t(getRentalLabelKey(group, value), { code: value ?? '' })
}

const xianyuValueLabel = (
  map: 'alertTypes' | 'alertSeverities' | 'alertStatuses' | 'guaranteeStatuses',
  value?: string | null
) => {
  if (!value) return '-'
  const key = `rental.xianyu.${map}.${value}`
  const translated = t(key)
  return translated === key ? value : translated
}

const getList = async () => {
  loading.value = true
  try {
    const data = await getXianyuShopPage(queryParams)
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

const handleSyncShops = async () => {
  syncing.value = true
  try {
    const count = await syncAuthorizedShops()
    message.success(t('rental.xianyu.syncShopsSuccess', { count }))
    await getList()
    await getAlertList()
  } finally {
    syncing.value = false
  }
}

const getAlertList = async () => {
  alertLoading.value = true
  try {
    const data = await getXianyuAlertPage(alertQuery)
    alertList.value = data.list || []
    alertTotal.value = data.total || 0
  } catch {
    alertList.value = []
    alertTotal.value = 0
    loadError.value = true
  } finally {
    alertLoading.value = false
  }
}

const handleResolveAlert = async (row: XianyuAlertVO) => {
  await resolveXianyuAlert(row.id)
  message.success(t('rental.xianyu.resolveAlertSuccess'))
  await getAlertList()
}

const getRawPayloadList = async () => {
  rawPayloadLoading.value = true
  try {
    const data = await getXianyuRawPayloadPage(rawPayloadQuery)
    rawPayloadList.value = data.list || []
    rawPayloadTotal.value = data.total || 0
  } catch {
    rawPayloadList.value = []
    rawPayloadTotal.value = 0
    loadError.value = true
  } finally {
    rawPayloadLoading.value = false
  }
}

const handleViewRawPayload = async (row: XianyuRawPayloadVO) => {
  rawPayloadDetail.value = await getXianyuRawPayload(row.id)
  rawPayloadDialogVisible.value = true
}

const handleReplayPushEvent = async () => {
  if (!replayForm.eventId) {
    message.warning(t('rental.xianyu.replayEventRequired'))
    return
  }
  replayLoading.value = true
  try {
    const result = await replayXianyuPushEvent(replayForm.eventId)
    showReplayResult(result.status, result.message || result.safeErrorCode || '-')
  } finally {
    replayLoading.value = false
  }
}

const handleReplayRawPayload = async () => {
  if (!replayForm.rawPayloadId) {
    message.warning(t('rental.xianyu.replayRawPayloadRequired'))
    return
  }
  replayLoading.value = true
  try {
    const result = await replayXianyuRawPayload(replayForm.rawPayloadId)
    showReplayResult(result.status, result.message || result.safeErrorCode || '-')
    await getList()
  } finally {
    replayLoading.value = false
  }
}

const showReplayResult = (status: string, text: string) => {
  const content = t('rental.xianyu.replayResult', { status, message: text })
  if (status === 'FAILED') {
    message.error(content)
    return
  }
  if (status === 'SKIPPED') {
    message.warning(content)
    return
  }
  message.success(content)
}

const getAfterSaleList = async () => {
  afterSaleLoading.value = true
  try {
    const data = await getXianyuAfterSalePage({
      pageNo: afterSaleQuery.pageNo,
      pageSize: afterSaleQuery.pageSize,
      shopId: afterSaleSyncForm.shopId
    })
    afterSaleList.value = data.list || []
    afterSaleTotal.value = data.total || 0
  } catch {
    afterSaleList.value = []
    afterSaleTotal.value = 0
    loadError.value = true
  } finally {
    afterSaleLoading.value = false
  }
}

const handleSyncProducts = async () => {
  const [windowStart, windowEnd] = productSyncForm.updateRange ?? []
  const windowStartMillis = toEpochMillis(windowStart)
  const windowEndMillis = toEpochMillis(windowEnd)
  if (!productSyncForm.shopId || !windowStartMillis || !windowEndMillis) {
    message.warning(t('rental.xianyu.productSyncRequired'))
    return
  }
  if (windowStartMillis >= windowEndMillis) {
    message.warning(t('rental.order.windowInvalid'))
    return
  }
  productSyncing.value = true
  try {
    const result = await syncXianyuProductPage({
      shopId: productSyncForm.shopId,
      windowStart: windowStartMillis,
      windowEnd: windowEndMillis,
      pageNo: 1,
      pageSize: 50
    })
    message.success(
      t('rental.xianyu.syncProductsSuccess', {
        received: result.receivedCount,
        succeeded: result.succeededCount,
        deduplicated: result.deduplicatedCount,
        skus: result.skuCount
      })
    )
    if (canViewRawPayload.value) {
      await getRawPayloadList()
    }
  } finally {
    productSyncing.value = false
  }
}

const handleSyncAfterSales = async () => {
  const [applyStart, applyEnd] = afterSaleSyncForm.applyRange ?? []
  const applyStartMillis = toEpochMillis(applyStart)
  const applyEndMillis = toEpochMillis(applyEnd)
  if (!afterSaleSyncForm.shopId || !applyStartMillis || !applyEndMillis) {
    message.warning(t('rental.xianyu.afterSaleSyncRequired'))
    return
  }
  if (applyStartMillis >= applyEndMillis) {
    message.warning(t('rental.order.windowInvalid'))
    return
  }
  afterSaleSyncing.value = true
  try {
    const result = await syncXianyuAfterSalePage({
      shopId: afterSaleSyncForm.shopId,
      applyStart: applyStartMillis,
      applyEnd: applyEndMillis,
      pageNo: 1,
      pageSize: 50
    })
    message.success(
      t('rental.xianyu.syncAfterSalesSuccess', {
        received: result.receivedCount,
        succeeded: result.succeededCount,
        hasNextPage: result.hasNextPage ? t('common.yes') : t('common.no')
      })
    )
    afterSaleQuery.pageNo = 1
    await getAfterSaleList()
  } finally {
    afterSaleSyncing.value = false
  }
}

const loadExpressCompanies = async () => {
  expressLoading.value = true
  try {
    expressList.value = await getXianyuExpressCompanyList()
  } catch {
    expressList.value = []
    loadError.value = true
  } finally {
    expressLoading.value = false
  }
}

const retryAll = async () => {
  loadError.value = false
  await configPanelRef.value?.reload()
  await getList()
  await getAlertList()
  if (canViewRawPayload.value) {
    await getRawPayloadList()
  }
  await getAfterSaleList()
  await loadExpressCompanies()
}

onMounted(() => {
  if (isSuperAdmin.value) {
    retryAll()
  }
})
</script>
