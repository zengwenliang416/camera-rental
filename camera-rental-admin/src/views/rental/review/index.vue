<template>
  <ContentWrap>
    <el-alert
      v-if="loadError"
      class="mb-12px"
      type="error"
      :closable="false"
      :title="t('rental.review.loadError')"
    >
      <el-button link type="primary" @click="getList">
        {{ t('rental.common.retry') }}
      </el-button>
    </el-alert>
    <el-form class="-mb-15px" :inline="true" :model="queryParams">
      <el-form-item :label="t('rental.review.status')">
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
      <el-form-item>
        <el-button @click="handleQuery">{{ t('common.query') }}</el-button>
        <el-button @click="resetQuery">{{ t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="reviewType" :label="t('rental.review.reviewType')" width="140" />
      <el-table-column prop="sourceType" :label="t('rental.review.sourceType')" width="120" />
      <el-table-column
        prop="sourceIdentifier"
        :label="t('rental.review.sourceIdentifier')"
        min-width="160"
      />
      <el-table-column :label="t('rental.review.status')" width="120">
        <template #default="{ row }">
          <el-tag :type="getRentalTagType('review', row.status)">
            {{ reviewStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reasonCode" :label="t('rental.review.reasonCode')" width="160" />
      <el-table-column :label="t('rental.review.reasonMessage')" min-width="220">
        <template #default="{ row }">
          {{ reviewReasonLabel(row.reasonCode, row.reasonMessage) }}
        </template>
      </el-table-column>
      <el-table-column
        prop="resolutionNote"
        :label="t('rental.review.resolutionNote')"
        min-width="180"
        show-overflow-tooltip
      />
      <el-table-column :label="t('rental.review.resolvedBy')" width="140">
        <template #default="{ row }">
          {{ row.resolvedByName || row.resolvedBy || '-' }}
        </template>
      </el-table-column>
      <el-table-column :label="t('rental.review.resolvedAt')" width="180">
        <template #default="{ row }">
          {{ formatNullableDate(row.resolvedAt) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('table.action')" width="150" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'OPEN'">
            <el-button
              link
              type="primary"
              v-hasPermi="['rental:review:update']"
              @click="openHandle(row, 'resolve')"
            >
              {{ t('rental.review.resolve') }}
            </el-button>
            <el-button
              link
              type="danger"
              v-hasPermi="['rental:review:update']"
              @click="openHandle(row, 'close')"
            >
              {{ t('rental.review.close') }}
            </el-button>
          </template>
        </template>
      </el-table-column>
      <template #empty>
        <div class="py-24px text-[var(--el-text-color-secondary)]">
          {{ t('rental.review.emptyHint') }}
        </div>
      </template>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog v-model="handleVisible" :title="handleTitle" width="520px">
      <el-form :model="handleForm" label-width="110px">
        <el-form-item :label="t('rental.review.sourceIdentifier')">
          <el-input :model-value="handleForm.sourceIdentifier" disabled />
        </el-form-item>
        <el-form-item :label="t('rental.review.resolutionNote')" required>
          <el-input
            v-model="handleForm.resolutionNote"
            type="textarea"
            :rows="4"
            maxlength="512"
            show-word-limit
            :placeholder="t('rental.review.resolutionNotePlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="handling" @click="submitHandle">{{
          t('common.ok')
        }}</el-button>
      </template>
    </el-dialog>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'
import {
  closeManualReview,
  getManualReviewPage,
  resolveManualReview,
  type RentalManualReviewVO
} from '@/api/rental/review'
import { formatNullableDate } from '@/utils/formatTime'
import { getRentalLabelKey, getRentalStatusValues, getRentalTagType } from '@/utils/rentalLabels'

defineOptions({ name: 'RentalManualReview' })
const { t } = useI18n()
const message = useMessage()

const loading = ref(false)
const loadError = ref(false)
const handling = ref(false)
const list = ref<RentalManualReviewVO[]>([])
const total = ref(0)
const queryParams = reactive({ pageNo: 1, pageSize: 10, status: '' })
const handleVisible = ref(false)
const handleType = ref<'resolve' | 'close'>('resolve')
const handleForm = reactive({ id: 0, sourceIdentifier: '', resolutionNote: '' })
const statusOptions = computed(() =>
  getRentalStatusValues('review').map((value) => ({
    value,
    label: t(getRentalLabelKey('review', value))
  }))
)
const handleTitle = computed(() =>
  t(handleType.value === 'resolve' ? 'rental.review.resolveTitle' : 'rental.review.closeTitle')
)
const reviewReasonCodes = new Set([
  'MISSING_REMARK',
  'MISSING_ORDER_DATE',
  'RENTAL_PERIOD_NOT_FOUND',
  'INVALID_RENTAL_DATE',
  'INVALID_RENTAL_RANGE',
  'INVALID_PAY_AMOUNT',
  'PRODUCT_MAPPING_REQUIRED'
])

const getList = async () => {
  loading.value = true
  loadError.value = false
  try {
    const data = await getManualReviewPage(queryParams)
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
  queryParams.status = ''
  queryParams.pageNo = 1
  await getList()
}

const reviewStatusLabel = (status: string) => {
  return t(getRentalLabelKey('review', status))
}

const reviewReasonLabel = (reasonCode?: string, reasonMessage?: string) => {
  if (reasonCode && reviewReasonCodes.has(reasonCode)) {
    return t(`rental.review.reason.${reasonCode}`)
  }
  return reasonMessage || t('rental.review.reason.UNKNOWN', { code: reasonCode || '-' })
}

const openHandle = (row: RentalManualReviewVO, type: 'resolve' | 'close') => {
  handleType.value = type
  handleForm.id = row.id
  handleForm.sourceIdentifier = row.sourceIdentifier
  handleForm.resolutionNote = ''
  handleVisible.value = true
}

const submitHandle = async () => {
  const resolutionNote = handleForm.resolutionNote.trim()
  if (!resolutionNote) {
    message.warning(t('rental.review.resolutionNoteRequired'))
    return
  }
  handling.value = true
  try {
    const request = { id: handleForm.id, resolutionNote }
    if (handleType.value === 'resolve') {
      await resolveManualReview(request)
    } else {
      await closeManualReview(request)
    }
    message.success(t('rental.review.handleSuccess'))
    handleVisible.value = false
    await getList()
  } finally {
    handling.value = false
  }
}

onMounted(getList)
</script>
