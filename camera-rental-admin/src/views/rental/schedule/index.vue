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

    <!-- Primary: status + occupy window + device; IDs folded under advanced -->
    <el-form class="schedule-filter-form" :inline="true" :model="queryParams" @submit.prevent>
      <el-form-item :label="t('rental.schedule.deviceId')">
        <el-input-number
          v-model="queryParams.deviceId"
          class="!w-140px"
          :min="1"
          controls-position="right"
          :placeholder="t('rental.schedule.deviceId')"
        />
      </el-form-item>
      <el-form-item :label="t('rental.schedule.status')">
        <el-select
          v-model="queryParams.status"
          class="!w-150px"
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
          class="!w-260px"
          :start-placeholder="t('rental.schedule.occupyStartDate')"
          :end-placeholder="t('rental.schedule.occupyEndDateExclusive')"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" />{{ t('common.query') }}
        </el-button>
        <el-button @click="resetQuery">{{ t('common.reset') }}</el-button>
        <el-button link type="primary" @click="showAdvanced = !showAdvanced">
          {{ showAdvanced ? t('rental.schedule.hideAdvanced') : t('rental.schedule.showAdvanced') }}
          <Icon :icon="showAdvanced ? 'ep:arrow-up' : 'ep:arrow-down'" class="ml-4px" />
        </el-button>
      </el-form-item>
    </el-form>

    <el-form
      v-show="showAdvanced"
      class="schedule-advanced-form mb-12px"
      :inline="true"
      :model="queryParams"
      @submit.prevent
    >
      <el-form-item :label="t('rental.schedule.rentalOrderId')">
        <el-input-number
          v-model="queryParams.rentalOrderId"
          class="!w-160px"
          :min="1"
          controls-position="right"
        />
      </el-form-item>
    </el-form>

    <p class="schedule-hint mb-12px">
      {{ t('rental.schedule.rangeHintShort') }}
    </p>

    <el-table v-loading="loading" :data="list">
      <el-table-column prop="deviceNo" :label="t('rental.schedule.deviceNo')" min-width="130" />
      <el-table-column
        prop="equipmentModelCode"
        :label="t('rental.schedule.equipmentModelCode')"
        min-width="150"
      />
      <el-table-column
        prop="rentalOrderId"
        :label="t('rental.schedule.rentalOrderId')"
        width="130"
      />
      <el-table-column
        prop="rentalOrderItemId"
        :label="t('rental.schedule.rentalOrderItemId')"
        width="150"
      />
      <el-table-column :label="t('rental.schedule.status')" width="120">
        <template #default="{ row }">
          <el-tag :type="getRentalTagType('schedule', row.status)">
            {{ t(getRentalLabelKey('schedule', row.status)) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="billableStartDate"
        :label="t('rental.schedule.billableStartDate')"
        width="140"
      />
      <el-table-column
        prop="billableEndDate"
        :label="t('rental.schedule.billableEndDate')"
        width="140"
      />
      <el-table-column
        prop="occupyStartDate"
        :label="t('rental.schedule.occupyStartDate')"
        width="140"
      />
      <el-table-column
        prop="occupyEndDateExclusive"
        :label="t('rental.schedule.occupyEndDateExclusive')"
        width="160"
      />
      <template #empty>
        <div class="py-24px text-[var(--el-text-color-secondary)]">
          {{ t('rental.schedule.emptyHint') }}
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import {
  getRentalSchedulePage,
  type RentalSchedulePageReqVO,
  type RentalScheduleVO
} from '@/api/rental/schedule'
import { getRentalLabelKey, getRentalStatusValues, getRentalTagType } from '@/utils/rentalLabels'

defineOptions({ name: 'RentalSchedule' })
const { t } = useI18n()
const route = useRoute()

const loading = ref(false)
const loadError = ref(false)
const showAdvanced = ref(false)
const list = ref<RentalScheduleVO[]>([])
const total = ref(0)
const queryParams = reactive<RentalSchedulePageReqVO>({ pageNo: 1, pageSize: 10 })
/** UI-only daterange → occupyStartDate / occupyEndDateExclusive */
const occupyRange = ref<[string, string] | undefined>()

const statusOptions = computed(() =>
  getRentalStatusValues('schedule').map((value) => ({
    value,
    label: t(getRentalLabelKey('schedule', value))
  }))
)

watch(
  occupyRange,
  (range) => {
    queryParams.occupyStartDate = range?.[0]
    queryParams.occupyEndDateExclusive = range?.[1]
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

onMounted(() => {
  const deviceId = Number(route.query.deviceId)
  queryParams.deviceId = Number.isInteger(deviceId) && deviceId > 0 ? deviceId : undefined
  const start =
    typeof route.query.occupyStartDate === 'string' ? route.query.occupyStartDate : undefined
  const end =
    typeof route.query.occupyEndDateExclusive === 'string'
      ? route.query.occupyEndDateExclusive
      : undefined
  queryParams.occupyStartDate = start
  queryParams.occupyEndDateExclusive = end
  if (start && end) {
    occupyRange.value = [start, end]
  }
  const orderId = Number(route.query.rentalOrderId)
  if (Number.isInteger(orderId) && orderId > 0) {
    queryParams.rentalOrderId = orderId
    showAdvanced.value = true
  }
  return getList()
})
</script>

<style scoped>
.schedule-filter-form {
  margin-bottom: 4px;
}

.schedule-advanced-form {
  padding: 12px 12px 0;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
}

.schedule-hint {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
}
</style>
