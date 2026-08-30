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
    <el-alert
      v-if="catalogError"
      class="mb-12px"
      type="error"
      :closable="false"
      :title="t('rental.device.catalogLoadError')"
    >
      <el-button link type="primary" @click="loadCatalog">
        {{ t('rental.common.retry') }}
      </el-button>
    </el-alert>
    <el-form class="-mb-15px" :inline="true" :model="queryParams">
      <el-form-item :label="t('rental.device.category')">
        <el-select
          v-model="queryParams.categoryCode"
          class="!w-160px"
          clearable
          filterable
          @change="handleQueryCategoryChange"
        >
          <el-option
            v-for="category in deviceCatalog"
            :key="category.categoryCode"
            :label="categoryLabel(category.categoryCode)"
            :value="category.categoryCode"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('rental.device.modelCode')">
        <el-select
          v-model="queryParams.equipmentModelCode"
          class="!w-160px"
          clearable
          filterable
        >
          <el-option
            v-for="model in queryModelOptions"
            :key="model.id"
            :label="modelLabel(model)"
            :value="model.modelCode"
          />
        </el-select>
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
      <el-table-column prop="categoryCode" :label="t('rental.device.category')" min-width="110">
        <template #default="{ row }">
          {{ row.categoryCode ? categoryLabel(row.categoryCode) : t('rental.device.uncategorized') }}
        </template>
      </el-table-column>
      <el-table-column
        prop="equipmentModelCode"
        :label="t('rental.device.modelCode')"
        min-width="140"
      />
      <el-table-column
        prop="warehouseCode"
        :label="t('rental.device.warehouseCode')"
        min-width="120"
      />
      <el-table-column :label="t('rental.device.purchaseAmount')" min-width="130">
        <template #default="{ row }">
          {{ formatPurchaseAmount(row.purchaseAmount) }}
        </template>
      </el-table-column>
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
      <el-table-column :label="t('table.action')" width="390" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            v-hasPermi="['rental:device:update']"
            @click="openEdit(row)"
          >
            {{ t('action.edit') }}
          </el-button>
          <el-button
            link
            type="danger"
            v-hasPermi="['rental:device:delete']"
            :loading="deletingDeviceId === row.id"
            @click="handleDelete(row)"
          >
            {{ t('action.del') }}
          </el-button>
          <el-button
            link
            type="primary"
            v-hasPermi="['rental:device:query']"
            @click="openQr(row)"
          >
            {{ t('rental.device.qr') }}
          </el-button>
          <el-button
            link
            type="primary"
            v-hasPermi="['rental:device:assign']"
            @click="openAssign(row)"
          >
            {{ t('rental.device.assign') }}
          </el-button>
          <el-button
            v-if="row.status === 'AVAILABLE'"
            link
            type="warning"
            v-hasPermi="['rental:device:assign']"
            :loading="opsDeviceId === row.id"
            @click="handleDispatch(row)"
          >
            {{ t('rental.device.dispatch') }}
          </el-button>
          <el-button
            v-if="row.status === 'RENTED'"
            link
            type="success"
            v-hasPermi="['rental:device:assign']"
            @click="openReturn(row)"
          >
            {{ t('rental.device.return') }}
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
        <el-form-item :label="t('rental.device.category')" prop="categoryCode">
          <el-select
            v-model="createForm.categoryCode"
            class="!w-100%"
            filterable
            @change="handleCreateCategoryChange"
          >
            <el-option
              v-for="category in deviceCatalog"
              :key="category.id"
              :label="category.categoryName"
              :value="category.categoryCode"
            />
            <template #footer>
              <el-button
                link
                type="primary"
                v-hasPermi="['rental:device:create']"
                @click.stop="openCategoryCreate"
              >
                <Icon icon="ep:plus" class="mr-5px" />
                {{ t('rental.device.categoryQuickCreate') }}
              </el-button>
            </template>
          </el-select>
        </el-form-item>
        <el-form-item :label="t('rental.device.modelCode')" prop="equipmentModelCode">
          <el-select
            v-model="createForm.equipmentModelCode"
            class="!w-100%"
            :disabled="!createForm.categoryCode"
            filterable
            @change="handleCreateModelChange"
          >
            <el-option
              v-for="model in createModelOptions"
              :key="model.id"
              :label="modelLabel(model)"
              :value="model.modelCode"
            />
            <template #footer>
              <el-button
                link
                type="primary"
                v-hasPermi="['rental:device:create']"
                :disabled="!createForm.categoryCode"
                @click.stop="openModelCreate"
              >
                <Icon icon="ep:plus" class="mr-5px" />
                {{ t('rental.device.modelQuickCreate') }}
              </el-button>
            </template>
          </el-select>
        </el-form-item>
        <el-form-item :label="t('rental.device.deviceNo')" prop="deviceNoSuffix">
          <el-input
            v-model="createForm.deviceNoSuffix"
            maxlength="3"
            :disabled="!selectedCreateModel"
            :placeholder="t('rental.device.deviceNoSuffixPlaceholder')"
            @blur="normalizeCreateDeviceNoSuffix"
          >
            <template #prepend>
              {{ selectedCreateModel ? `${selectedCreateModel.deviceNoPrefix}-` : '--' }}
            </template>
          </el-input>
          <div class="text-12px text-[var(--el-text-color-secondary)]">
            {{
              createDeviceNoPreview
                ? t('rental.device.deviceNoComposeHint', { deviceNo: createDeviceNoPreview })
                : t('rental.device.deviceNoSuffixHint')
            }}
          </div>
        </el-form-item>
        <el-form-item :label="t('rental.device.serialNumber')">
          <el-input v-model="createForm.serialNumber" />
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

    <el-dialog v-model="qrVisible" :title="t('rental.device.qrTitle')" width="420px">
      <div v-loading="qrLoading" class="flex flex-col items-center gap-12px">
        <div class="text-14px">
          {{ qrInfo?.deviceNo }} / {{ qrInfo?.equipmentModelCode }}
        </div>
        <el-tag size="small" :type="qrInfo?.signed ? 'success' : 'info'">
          {{ qrInfo?.signed ? t('rental.device.qrSigned') : t('rental.device.qrUnsigned') }}
        </el-tag>
        <Qrcode v-if="qrInfo?.payload" :text="qrInfo.payload" :width="220" />
        <el-input
          v-if="qrInfo?.payload"
          type="textarea"
          :rows="3"
          :model-value="qrInfo.payload"
          readonly
        />
      </div>
      <template #footer>
        <el-button @click="qrVisible = false">{{ t('common.close') }}</el-button>
        <el-button type="primary" :disabled="!qrInfo?.payload" @click="copyQrPayload">
          {{ t('rental.device.qrCopy') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="returnVisible" :title="t('rental.device.returnTitle')" width="440px">
      <el-form label-width="120px">
        <el-form-item :label="t('rental.device.deviceNo')">
          <el-input :model-value="returnForm.deviceNo" disabled />
        </el-form-item>
        <el-form-item :label="t('rental.device.status')">
          <el-radio-group v-model="returnForm.inspectPassed">
            <el-radio :value="true">{{ t('rental.device.inspectPassed') }}</el-radio>
            <el-radio :value="false">{{ t('rental.device.inspectFailed') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('rental.device.returnNote')">
          <el-input v-model="returnForm.note" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="returning" @click="submitReturn">
          {{ t('common.ok') }}
        </el-button>
      </template>
    </el-dialog>

    <DeviceCategoryCreateDialog
      ref="categoryCreateDialogRef"
      @success="handleCategoryCreated"
    />
    <DeviceModelCreateDialog
      ref="modelCreateDialogRef"
      :categories="deviceCatalog"
      @success="handleModelCreated"
    />
    <DeviceEditDialog ref="deviceEditDialogRef" @success="getList" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { computed, ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'
import { Qrcode } from '@/components/Qrcode'
import {
  assignRentalDevice,
  createRentalDevice,
  deleteRentalDevice,
  dispatchRentalDevice,
  getRentalDeviceCatalog,
  getRentalDevicePage,
  getRentalDeviceQr,
  returnRentalDevice,
  type RentalDeviceCategoryVO,
  type RentalDeviceModelVO,
  type RentalDeviceQrVO,
  type RentalDeviceVO
} from '@/api/rental/device'
import { getRentalLabelKey, getRentalTagType } from '@/utils/rentalLabels'
import {
  buildDeviceNoPreview,
  findModel,
  getModelsForCategory,
  isModelInCategory,
  normalizeDeviceNoSuffix
} from './deviceCatalogModel'
import { formatPurchaseAmount } from './deviceMaintenanceModel'
import DeviceCategoryCreateDialog from './DeviceCategoryCreateDialog.vue'
import DeviceEditDialog from './DeviceEditDialog.vue'
import DeviceModelCreateDialog from './DeviceModelCreateDialog.vue'

defineOptions({ name: 'RentalDevice' })
const { t } = useI18n()
const message = useMessage()

const loading = ref(false)
const loadError = ref(false)
const catalogError = ref(false)
const creating = ref(false)
const assigning = ref(false)
const list = ref<RentalDeviceVO[]>([])
const deviceCatalog = ref<RentalDeviceCategoryVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  categoryCode: '',
  equipmentModelCode: ''
})
const createVisible = ref(false)
const createForm = reactive({
  serialNumber: '',
  categoryCode: '',
  equipmentModelCode: '',
  deviceNoSuffix: ''
})
const createFormRef = ref<FormInstance>()
const createRules = computed<FormRules>(() => ({
  categoryCode: [
    { required: true, message: t('rental.device.categoryRequired'), trigger: 'change' }
  ],
  equipmentModelCode: [
    { required: true, message: t('rental.device.modelCodeRequired'), trigger: 'change' }
  ],
  deviceNoSuffix: [
    { required: true, message: t('rental.device.deviceNoSuffixRequired'), trigger: 'blur' },
    {
      pattern: /^(?:0?[1-9]|[1-9][0-9]{1,2})$/,
      message: t('rental.device.deviceNoSuffixFormat'),
      trigger: 'blur'
    }
  ]
}))
const queryModelOptions = computed(() =>
  getModelsForCategory(deviceCatalog.value, queryParams.categoryCode)
)
const createModelOptions = computed(() =>
  getModelsForCategory(deviceCatalog.value, createForm.categoryCode)
)
const selectedCreateModel = computed(() =>
  findModel(deviceCatalog.value, createForm.categoryCode, createForm.equipmentModelCode)
)
const createDeviceNoPreview = computed(() =>
  buildDeviceNoPreview(
    selectedCreateModel.value?.deviceNoPrefix,
    createForm.deviceNoSuffix
  )
)

const categoryLabel = (categoryCode: string) =>
  deviceCatalog.value.find((category) => category.categoryCode === categoryCode)?.categoryName ??
  categoryCode
const modelLabel = (model: RentalDeviceModelVO) =>
  model.modelName === model.modelCode ? model.modelCode : `${model.modelName} (${model.modelCode})`

const loadCatalog = async () => {
  catalogError.value = false
  try {
    const catalog = await getRentalDeviceCatalog()
    deviceCatalog.value = catalog
    return catalog
  } catch {
    deviceCatalog.value = []
    catalogError.value = true
    return []
  }
}

const handleQueryCategoryChange = () => {
  if (
    queryParams.equipmentModelCode &&
    !isModelInCategory(
      deviceCatalog.value,
      queryParams.categoryCode,
      queryParams.equipmentModelCode
    )
  ) {
    queryParams.equipmentModelCode = ''
  }
}

const handleCreateCategoryChange = () => {
  createForm.equipmentModelCode = ''
  createForm.deviceNoSuffix = ''
}

const handleCreateModelChange = () => {
  createForm.deviceNoSuffix = ''
}

const categoryCreateDialogRef = ref<InstanceType<typeof DeviceCategoryCreateDialog>>()
const modelCreateDialogRef = ref<InstanceType<typeof DeviceModelCreateDialog>>()
const deviceEditDialogRef = ref<InstanceType<typeof DeviceEditDialog>>()
const deletingDeviceId = ref<number>()

const openCategoryCreate = () => {
  categoryCreateDialogRef.value?.open()
}

const openModelCreate = () => {
  const category = deviceCatalog.value.find(
    (item) => item.categoryCode === createForm.categoryCode
  )
  modelCreateDialogRef.value?.open(category?.id)
}

const openEdit = (row: RentalDeviceVO) => {
  deviceEditDialogRef.value?.open(row)
}

const handleDelete = async (row: RentalDeviceVO) => {
  try {
    await message.delConfirm(t('rental.device.deleteConfirm', { deviceNo: row.deviceNo }))
    deletingDeviceId.value = row.id
    await deleteRentalDevice(row.id)
    message.success(t('rental.device.deleteSuccess'))
    await getList()
  } catch {
    // Confirmation cancellation and request errors are already surfaced by shared UI services.
  } finally {
    deletingDeviceId.value = undefined
  }
}

const handleCategoryCreated = async (created: { id: number; categoryCode: string }) => {
  const catalog = await loadCatalog()
  const category = catalog.find(
    (item) => item.id === created.id || item.categoryCode === created.categoryCode
  )
  if (!category) return
  createForm.categoryCode = category.categoryCode
  createForm.equipmentModelCode = ''
  createForm.deviceNoSuffix = ''
}

const handleModelCreated = async (created: {
  id: number
  categoryId: number
  modelCode: string
}) => {
  const catalog = await loadCatalog()
  const category = catalog.find((item) => item.id === created.categoryId)
  const model = category?.models.find(
    (item) => item.id === created.id || item.modelCode === created.modelCode
  )
  if (!category || !model) return
  createForm.categoryCode = category.categoryCode
  createForm.equipmentModelCode = model.modelCode
  createForm.deviceNoSuffix = ''
}

const normalizeCreateDeviceNoSuffix = () => {
  const normalized = normalizeDeviceNoSuffix(createForm.deviceNoSuffix)
  if (normalized) createForm.deviceNoSuffix = normalized
}
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

const qrVisible = ref(false)
const qrLoading = ref(false)
const qrInfo = ref<RentalDeviceQrVO | null>(null)

const openQr = async (row: RentalDeviceVO) => {
  qrVisible.value = true
  qrLoading.value = true
  qrInfo.value = null
  try {
    qrInfo.value = await getRentalDeviceQr(row.id)
  } catch {
    qrVisible.value = false
  } finally {
    qrLoading.value = false
  }
}

const copyQrPayload = async () => {
  const payload = qrInfo.value?.payload
  if (!payload) return
  try {
    await navigator.clipboard.writeText(payload)
    message.success(t('rental.device.qrCopySuccess'))
  } catch {
    // HTTP / restricted clipboard: select textarea fallback is enough for ops
    message.warning(t('rental.device.qrPayload'))
  }
}

const opsDeviceId = ref<number | null>(null)
const returnVisible = ref(false)
const returning = ref(false)
const returnForm = reactive({
  deviceId: undefined as number | undefined,
  deviceNo: '',
  inspectPassed: true,
  note: ''
})

const statusLabel = (status: string) => t(getRentalLabelKey('device', status))

const handleDispatch = async (row: RentalDeviceVO) => {
  opsDeviceId.value = row.id
  try {
    const result = await dispatchRentalDevice({ deviceId: row.id })
    message.success(
      t('rental.device.dispatchSuccess', { status: statusLabel(result.deviceStatus) })
    )
    await getList()
  } finally {
    opsDeviceId.value = null
  }
}

const openReturn = (row: RentalDeviceVO) => {
  returnForm.deviceId = row.id
  returnForm.deviceNo = row.deviceNo
  returnForm.inspectPassed = true
  returnForm.note = ''
  returnVisible.value = true
}

const submitReturn = async () => {
  if (!returnForm.deviceId) return
  returning.value = true
  try {
    const result = await returnRentalDevice({
      deviceId: returnForm.deviceId,
      inspectPassed: returnForm.inspectPassed,
      note: returnForm.note.trim() || undefined
    })
    message.success(t('rental.device.returnSuccess', { status: statusLabel(result.deviceStatus) }))
    returnVisible.value = false
    await getList()
  } finally {
    returning.value = false
  }
}

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
  createForm.serialNumber = ''
  createForm.categoryCode = ''
  createForm.equipmentModelCode = ''
  createForm.deviceNoSuffix = ''
  createVisible.value = true
}

const submitCreate = async () => {
  const valid = await createFormRef.value?.validate()
  if (!valid) return
  creating.value = true
  try {
    await createRentalDevice({
      ...createForm,
      categoryCode: createForm.categoryCode,
      equipmentModelCode: createForm.equipmentModelCode.trim(),
      deviceNoSuffix: normalizeDeviceNoSuffix(createForm.deviceNoSuffix),
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

onMounted(async () => {
  await Promise.all([loadCatalog(), getList()])
})
</script>
