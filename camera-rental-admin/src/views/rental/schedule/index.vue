<template>
  <ContentWrap>
    <el-alert
      class="mb-16px"
      type="info"
      :closable="false"
      :title="t('rental.schedule.rangeHint')"
    />
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
    <el-form class="-mb-15px" :inline="true" :model="queryParams">
      <el-form-item :label="t('rental.schedule.deviceId')">
        <el-input-number
          v-model="queryParams.deviceId"
          class="!w-160px"
          :min="1"
          controls-position="right"
        />
      </el-form-item>
      <el-form-item :label="t('rental.schedule.rentalOrderId')">
        <el-input-number
          v-model="queryParams.rentalOrderId"
          class="!w-160px"
          :min="1"
          controls-position="right"
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
      <el-form-item :label="t('rental.schedule.occupyStartDate')">
        <el-date-picker
          v-model="queryParams.occupyStartDate"
          type="date"
          value-format="YYYY-MM-DD"
          class="!w-160px"
        />
      </el-form-item>
      <el-form-item :label="t('rental.schedule.occupyEndDateExclusive')">
        <el-date-picker
          v-model="queryParams.occupyEndDateExclusive"
          type="date"
          value-format="YYYY-MM-DD"
          class="!w-160px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">{{ t('common.query') }}</el-button>
        <el-button @click="resetQuery">{{ t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>

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
        width="180"
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
import { computed, onMounted, reactive, ref } from 'vue'
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
const list = ref<RentalScheduleVO[]>([])
const total = ref(0)
const queryParams = reactive<RentalSchedulePageReqVO>({ pageNo: 1, pageSize: 10 })
const statusOptions = computed(() =>
  getRentalStatusValues('schedule').map((value) => ({
    value,
    label: t(getRentalLabelKey('schedule', value))
  }))
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
  queryParams.pageNo = 1
  await getList()
}

onMounted(() => {
  const deviceId = Number(route.query.deviceId)
  queryParams.deviceId = Number.isInteger(deviceId) && deviceId > 0 ? deviceId : undefined
  queryParams.occupyStartDate =
    typeof route.query.occupyStartDate === 'string' ? route.query.occupyStartDate : undefined
  queryParams.occupyEndDateExclusive =
    typeof route.query.occupyEndDateExclusive === 'string'
      ? route.query.occupyEndDateExclusive
      : undefined
  return getList()
})
</script>
