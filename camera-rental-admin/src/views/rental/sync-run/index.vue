<template>
  <ContentWrap>
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
    <el-form class="-mb-15px" :inline="true" :model="queryParams" @submit.prevent>
      <el-form-item :label="t('rental.syncRun.filters.shopId')">
        <el-input
          v-model="queryParams.shopId"
          class="!w-180px"
          clearable
          :placeholder="t('rental.syncRun.filters.shopId')"
        />
      </el-form-item>
      <el-form-item :label="t('rental.syncRun.filters.resourceType')">
        <el-select
          v-model="queryParams.resourceType"
          class="!w-180px"
          clearable
          :placeholder="t('rental.syncRun.filters.resourceType')"
        >
          <el-option
            v-for="value in resourceTypes"
            :key="value"
            :label="resourceTypeLabel(value)"
            :value="value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('rental.syncRun.filters.status')">
        <el-select
          v-model="queryParams.status"
          class="!w-180px"
          clearable
          :placeholder="t('rental.syncRun.filters.status')"
        >
          <el-option
            v-for="value in statuses"
            :key="value"
            :label="statusLabel(value)"
            :value="value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('rental.syncRun.filters.triggerType')">
        <el-select
          v-model="queryParams.triggerType"
          class="!w-180px"
          clearable
          :placeholder="t('rental.syncRun.filters.triggerType')"
        >
          <el-option
            v-for="value in triggerTypes"
            :key="value"
            :label="triggerTypeLabel(value)"
            :value="value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">{{ t('rental.syncRun.actions.query') }}</el-button>
        <el-button @click="resetQuery">{{ t('rental.syncRun.actions.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list">
      <el-table-column prop="id" :label="t('rental.syncRun.table.id')" width="90" />
      <el-table-column prop="shopId" :label="t('rental.syncRun.table.shopId')" width="100" />
      <el-table-column :label="t('rental.syncRun.table.resourceType')" min-width="140">
        <template #default="{ row }">{{ resourceTypeLabel(row.resourceType) }}</template>
      </el-table-column>
      <el-table-column :label="t('rental.syncRun.table.triggerType')" min-width="140">
        <template #default="{ row }">{{ triggerTypeLabel(row.triggerType) }}</template>
      </el-table-column>
      <el-table-column :label="t('rental.syncRun.table.status')" min-width="130">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('rental.syncRun.table.windowStart')" width="180">
        <template #default="{ row }">{{ formatTime(row.windowStart) }}</template>
      </el-table-column>
      <el-table-column :label="t('rental.syncRun.table.windowEnd')" width="180">
        <template #default="{ row }">{{ formatTime(row.windowEnd) }}</template>
      </el-table-column>
      <el-table-column
        prop="receivedCount"
        :label="t('rental.syncRun.table.receivedCount')"
        width="120"
      />
      <el-table-column
        prop="deduplicatedCount"
        :label="t('rental.syncRun.table.deduplicatedCount')"
        width="150"
      />
      <el-table-column
        prop="succeededCount"
        :label="t('rental.syncRun.table.succeededCount')"
        width="120"
      />
      <el-table-column
        prop="reviewRequiredCount"
        :label="t('rental.syncRun.table.reviewRequiredCount')"
        width="150"
      />
      <el-table-column
        prop="failedCount"
        :label="t('rental.syncRun.table.failedCount')"
        width="120"
      />
      <el-table-column
        :label="t('rental.syncRun.table.lastErrorCode')"
        min-width="160"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          {{ formatText(row.lastErrorCode) }}
        </template>
      </el-table-column>
      <el-table-column
        :label="t('rental.syncRun.table.lastErrorMessage')"
        min-width="220"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          {{ formatText(row.lastErrorMessage) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('rental.syncRun.table.startedAt')" width="180">
        <template #default="{ row }">{{ formatTime(row.startedAt) }}</template>
      </el-table-column>
      <el-table-column :label="t('rental.syncRun.table.finishedAt')" width="180">
        <template #default="{ row }">{{ formatTime(row.finishedAt) }}</template>
      </el-table-column>
      <template #empty>
        <div class="py-24px text-[var(--el-text-color-secondary)]">
          {{ t('rental.syncRun.empty') }}
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
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import {
  getRentalSyncRunPage,
  type RentalSyncRunPageReqVO,
  type RentalSyncRunVO
} from '@/api/rental/syncRun'
import { formatNullableDate } from '@/utils/formatTime'

defineOptions({ name: 'RentalXianyuSyncRun' })

type SyncRunQueryForm = {
  pageNo: number
  pageSize: number
  shopId: string
  resourceType: string
  status: string
  triggerType: string
}

const { t } = useI18n()
const resourceTypes = ['ORDER', 'AFTER_SALE'] as const
const statuses = ['RUNNING', 'SUCCEEDED', 'FAILED'] as const
const triggerTypes = ['MANUAL', 'SCHEDULED'] as const

const loading = ref(false)
const loadError = ref(false)
const list = ref<RentalSyncRunVO[]>([])
const total = ref(0)
const queryParams = reactive<SyncRunQueryForm>({
  pageNo: 1,
  pageSize: 10,
  shopId: '',
  resourceType: '',
  status: '',
  triggerType: ''
})

const normalizeText = (value: string) => {
  const trimmed = value.trim()
  return trimmed ? trimmed : undefined
}

const normalizeShopId = (value: string) => {
  const trimmed = value.trim()
  if (!trimmed) return undefined
  return /^[1-9]\d*$/.test(trimmed) ? trimmed : null
}

const buildParams = (): RentalSyncRunPageReqVO | null => {
  const shopId = normalizeShopId(queryParams.shopId)
  if (shopId === null) {
    ElMessage.warning(t('rental.syncRun.filters.shopIdInvalid'))
    return null
  }
  return {
    pageNo: queryParams.pageNo,
    pageSize: queryParams.pageSize,
    shopId,
    resourceType: normalizeText(queryParams.resourceType),
    status: normalizeText(queryParams.status),
    triggerType: normalizeText(queryParams.triggerType)
  }
}

const formatText = (value?: string) => {
  return value && value.trim() ? value : t('rental.syncRun.common.emptyValue')
}

const formatTime = (value?: string) => {
  return formatNullableDate(value, 'YYYY-MM-DD HH:mm:ss', t('rental.syncRun.common.emptyValue'))
}

const resourceTypeLabel = (value: string) => {
  return resourceTypes.includes(value as (typeof resourceTypes)[number])
    ? t(`rental.syncRun.resourceType.${value}`)
    : value
}

const statusLabel = (value: string) => {
  return statuses.includes(value as (typeof statuses)[number])
    ? t(`rental.syncRun.status.${value}`)
    : value
}

const triggerTypeLabel = (value: string) => {
  return triggerTypes.includes(value as (typeof triggerTypes)[number])
    ? t(`rental.syncRun.triggerType.${value}`)
    : value
}

const statusTagType = (value: string) => {
  if (value === 'SUCCEEDED') return 'success'
  if (value === 'FAILED') return 'danger'
  return 'warning'
}

const getList = async () => {
  const params = buildParams()
  if (!params) return
  loading.value = true
  loadError.value = false
  try {
    const data = await getRentalSyncRunPage(params)
    list.value = data.list || []
    total.value = data.total || 0
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
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.shopId = ''
  queryParams.resourceType = ''
  queryParams.status = ''
  queryParams.triggerType = ''
  await getList()
}

onMounted(() => {
  getList()
})
</script>
