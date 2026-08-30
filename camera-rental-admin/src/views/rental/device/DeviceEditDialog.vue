<template>
  <Dialog
    v-model="dialogVisible"
    :title="t('rental.device.editTitle')"
    width="560px"
    :fullscreen="false"
  >
    <el-form
      ref="formRef"
      v-loading="saving"
      :model="formData"
      :rules="rules"
      label-width="130px"
    >
      <el-form-item :label="t('rental.device.deviceNo')">
        <el-input :model-value="device?.deviceNo" disabled />
      </el-form-item>
      <el-form-item :label="t('rental.device.category')">
        <el-input :model-value="device?.categoryCode || t('rental.device.uncategorized')" disabled />
      </el-form-item>
      <el-form-item :label="t('rental.device.modelCode')">
        <el-input :model-value="device?.equipmentModelCode" disabled />
      </el-form-item>
      <el-form-item :label="t('rental.device.status')">
        <el-input :model-value="device ? statusLabel(device.status) : ''" disabled />
      </el-form-item>
      <el-form-item :label="t('rental.device.serialNumber')" prop="serialNumber">
        <el-input v-model="formData.serialNumber" maxlength="128" clearable />
      </el-form-item>
      <el-form-item :label="t('rental.device.warehouseCode')" prop="warehouseCode">
        <el-input v-model="formData.warehouseCode" maxlength="128" clearable />
      </el-form-item>
      <el-form-item :label="t('rental.device.purchaseAmount')" prop="purchaseAmountYuan">
        <el-input-number
          v-model="formData.purchaseAmountYuan"
          class="!w-100%"
          :min="0"
          :precision="2"
          :step="100"
          controls-position="right"
        />
      </el-form-item>
      <el-form-item :label="t('rental.device.enabled')">
        <el-switch v-model="formData.enabled" />
        <span class="ml-12px text-12px text-[var(--el-text-color-secondary)]">
          {{ t('rental.device.disableHint') }}
        </span>
      </el-form-item>
      <el-alert
        type="info"
        :closable="false"
        :title="t('rental.device.immutableHint')"
      />
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="saving" @click="submit">
        {{ t('common.ok') }}
      </el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import type { FormInstance, FormRules } from 'element-plus'
import {
  updateRentalDevice,
  type RentalDeviceVO
} from '@/api/rental/device'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'
import { getRentalLabelKey } from '@/utils/rentalLabels'
import {
  buildDeviceUpdatePayload,
  createDeviceMaintenanceForm,
  type DeviceMaintenanceForm
} from './deviceMaintenanceModel'

const emit = defineEmits<{ success: [] }>()
const { t } = useI18n()
const message = useMessage()
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const device = ref<RentalDeviceVO>()
const formData = reactive<DeviceMaintenanceForm>({
  id: 0,
  serialNumber: '',
  warehouseCode: '',
  purchaseAmountYuan: undefined,
  enabled: true
})
const rules = computed<FormRules>(() => ({
  purchaseAmountYuan: [
    {
      validator: (_rule, value, callback) => {
        if (value === undefined || (Number.isFinite(value) && value >= 0)) {
          callback()
          return
        }
        callback(new Error(t('rental.device.purchaseAmountInvalid')))
      },
      trigger: 'change'
    }
  ]
}))

const statusLabel = (status: string) => t(getRentalLabelKey('device', status))

const open = (row: RentalDeviceVO) => {
  device.value = row
  Object.assign(formData, createDeviceMaintenanceForm(row))
  formRef.value?.clearValidate()
  dialogVisible.value = true
}
defineExpose({ open })

const submit = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    await updateRentalDevice(buildDeviceUpdatePayload(formData))
    dialogVisible.value = false
    message.success(t('rental.device.updateSuccess'))
    emit('success')
  } finally {
    saving.value = false
  }
}
</script>
