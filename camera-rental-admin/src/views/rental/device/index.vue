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
    <el-form class="-mb-15px" :inline="true" :model="queryParams">
      <el-form-item :label="t('rental.device.modelCode')">
        <el-input v-model="queryParams.equipmentModelCode" class="!w-180px" clearable />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">{{ t('common.query') }}</el-button>
        <el-button type="primary" v-hasPermi="['rental:device:create']" @click="openCreate">
          {{ t('action.create') }}
        </el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="deviceNo" :label="t('rental.device.deviceNo')" min-width="120" />
      <el-table-column
        prop="serialNumber"
        :label="t('rental.device.serialNumber')"
        min-width="120"
      />
      <el-table-column
        prop="equipmentModelCode"
        :label="t('rental.device.modelCode')"
        min-width="140"
      />
      <el-table-column :label="t('rental.device.status')" width="120">
        <template #default="{ row }">
          <el-tag :type="getRentalTagType('device', row.status)">
            {{ t(getRentalLabelKey('device', row.status)) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="enabled" :label="t('rental.device.enabled')" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
            {{ row.enabled ? t('common.yes') : t('common.no') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('table.action')" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            v-hasPermi="['rental:device:assign']"
            @click="openAssign(row)"
          >
            {{ t('rental.device.assign') }}
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="py-24px text-[var(--el-text-color-secondary)]">
          {{ t('rental.device.emptyHint') }}
        </div>
      </template>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog v-model="createVisible" :title="t('rental.device.createTitle')" width="480px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="120px">
        <el-form-item :label="t('rental.device.deviceNo')" prop="deviceNo">
          <el-input v-model="createForm.deviceNo" />
        </el-form-item>
        <el-form-item :label="t('rental.device.serialNumber')">
          <el-input v-model="createForm.serialNumber" />
        </el-form-item>
        <el-form-item :label="t('rental.device.modelCode')" prop="equipmentModelCode">
          <el-input v-model="createForm.equipmentModelCode" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">
          {{ t('common.ok') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" :title="t('rental.device.assignTitle')" width="520px">
      <el-form ref="assignFormRef" :model="assignForm" :rules="assignRules" label-width="140px">
        <el-form-item :label="t('rental.device.deviceId')">
          <el-input v-model="assignForm.deviceId" disabled />
        </el-form-item>
        <el-form-item :label="t('rental.device.orderItemId')" prop="rentalOrderItemId">
          <el-input v-model="assignForm.rentalOrderItemId" />
        </el-form-item>
        <el-form-item :label="t('rental.device.occupyStart')" prop="occupyStartDate">
          <el-date-picker
            v-model="assignForm.occupyStartDate"
            type="date"
            value-format="YYYY-MM-DD"
            class="!w-100%"
          />
        </el-form-item>
        <el-form-item :label="t('rental.device.occupyEndExclusive')" prop="occupyEndDateExclusive">
          <el-date-picker
            v-model="assignForm.occupyEndDateExclusive"
            type="date"
            value-format="YYYY-MM-DD"
            class="!w-100%"
          />
        </el-form-item>
        <el-form-item :label="t('rental.device.idempotencyKey')" prop="idempotencyKey">
          <el-input v-model="assignForm.idempotencyKey" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="assigning" @click="submitAssign">{{
          t('common.ok')
        }}</el-button>
      </template>
    </el-dialog>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { computed, ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'
import {
  assignRentalDevice,
  createRentalDevice,
  getRentalDevicePage,
  type RentalDeviceVO
} from '@/api/rental/device'
import { getRentalLabelKey, getRentalTagType } from '@/utils/rentalLabels'

defineOptions({ name: 'RentalDevice' })
const { t } = useI18n()
const message = useMessage()

const loading = ref(false)
const loadError = ref(false)
const creating = ref(false)
const assigning = ref(false)
const list = ref<RentalDeviceVO[]>([])
const total = ref(0)
const queryParams = reactive({ pageNo: 1, pageSize: 10, equipmentModelCode: '' })
const createVisible = ref(false)
const createForm = reactive({ deviceNo: '', serialNumber: '', equipmentModelCode: '' })
const createFormRef = ref<FormInstance>()
const createRules = computed<FormRules>(() => ({
  deviceNo: [{ required: true, message: t('rental.device.deviceNoRequired'), trigger: 'blur' }],
  equipmentModelCode: [
    { required: true, message: t('rental.device.modelCodeRequired'), trigger: 'blur' }
  ]
}))
const assignVisible = ref(false)
const assignForm = reactive({
  deviceId: undefined as number | undefined,
  rentalOrderItemId: '',
  occupyStartDate: '',
  occupyEndDateExclusive: '',
  idempotencyKey: ''
})
const assignFormRef = ref<FormInstance>()
const assignRules = computed<FormRules>(() => ({
  rentalOrderItemId: [
    { required: true, message: t('rental.device.assignRequired'), trigger: 'blur' },
    {
      pattern: /^[1-9]\d*$/,
      message: t('rental.device.orderItemIdInvalid'),
      trigger: 'blur'
    }
  ],
  occupyStartDate: [
    { required: true, message: t('rental.device.assignRequired'), trigger: 'change' }
  ],
  occupyEndDateExclusive: [
    { required: true, message: t('rental.device.assignRequired'), trigger: 'change' }
  ],
  idempotencyKey: [{ required: true, message: t('rental.device.assignRequired'), trigger: 'blur' }]
}))

const getList = async () => {
  loading.value = true
  loadError.value = false
  try {
    const data = await getRentalDevicePage(queryParams)
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

const openCreate = () => {
  createForm.deviceNo = ''
  createForm.serialNumber = ''
  createForm.equipmentModelCode = ''
  createVisible.value = true
}

const submitCreate = async () => {
  const valid = await createFormRef.value?.validate()
  if (!valid) return
  creating.value = true
  try {
    await createRentalDevice({
      ...createForm,
      deviceNo: createForm.deviceNo.trim(),
      equipmentModelCode: createForm.equipmentModelCode.trim(),
      serialNumber: createForm.serialNumber.trim() || undefined
    })
    message.success(t('common.createSuccess'))
    createVisible.value = false
    await getList()
  } finally {
    creating.value = false
  }
}

const openAssign = (row: RentalDeviceVO) => {
  assignForm.deviceId = row.id
  assignForm.rentalOrderItemId = ''
  assignForm.occupyStartDate = ''
  assignForm.occupyEndDateExclusive = ''
  assignForm.idempotencyKey = `assign-${row.id}-${Date.now()}`
  assignVisible.value = true
}

const submitAssign = async () => {
  const valid = await assignFormRef.value?.validate()
  if (!valid || !assignForm.deviceId) return
  const rentalOrderItemId = Number(assignForm.rentalOrderItemId.trim())
  if (!Number.isSafeInteger(rentalOrderItemId) || rentalOrderItemId <= 0) {
    message.warning(t('rental.device.orderItemIdInvalid'))
    return
  }
  assigning.value = true
  try {
    const result = await assignRentalDevice({
      deviceId: assignForm.deviceId,
      rentalOrderItemId,
      occupyStartDate: assignForm.occupyStartDate,
      occupyEndDateExclusive: assignForm.occupyEndDateExclusive,
      idempotencyKey: assignForm.idempotencyKey.trim()
    })
    message.success(
      t('rental.device.assignSuccess', {
        assignmentId: result.assignmentId,
        scheduleId: result.scheduleId
      })
    )
    assignVisible.value = false
  } finally {
    assigning.value = false
  }
}

onMounted(getList)
</script>
