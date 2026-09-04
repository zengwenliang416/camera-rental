<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('rental.order.backfillTitle')"
    width="min(620px, calc(100vw - 32px))"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-alert
      class="mb-16px"
      type="warning"
      :closable="false"
      :title="t('rental.order.backfillHint')"
    />

    <el-descriptions v-if="order" class="mb-16px" :column="2" border>
      <el-descriptions-item :label="t('rental.order.externalOrderId')">
        {{ order.externalOrderId }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.orderStatus')">
        {{ rentalLabel(order.orderStatus) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.goodsTitle')" :span="2">
        {{ order.goodsTitle || '-' }}
      </el-descriptions-item>
    </el-descriptions>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="118px" @submit.prevent>
      <el-form-item :label="t('rental.order.backfillDeviceNo')" prop="deviceNo">
        <el-input
          v-model.trim="form.deviceNo"
          :placeholder="t('rental.order.backfillDevicePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('rental.xianyu.waybillNo')" prop="waybillNo">
        <el-input v-model.trim="form.waybillNo" />
      </el-form-item>
      <el-form-item :label="t('rental.xianyu.expressCode')" prop="expressCode">
        <el-input v-model.trim="form.expressCode" />
      </el-form-item>
      <el-form-item :label="t('rental.xianyu.expressName')" prop="expressName">
        <el-input v-model.trim="form.expressName" />
      </el-form-item>
      <el-form-item :label="t('rental.order.backfillConsignTime')" prop="consignTime">
        <el-date-picker
          v-model="form.consignTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          class="!w-100%"
        />
      </el-form-item>
      <el-form-item :label="t('rental.order.backfillReason')" prop="reason">
        <el-input
          v-model.trim="form.reason"
          type="textarea"
          :rows="3"
          maxlength="480"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">
        {{ t('rental.order.backfillSubmit') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'
import { getRentalLabelKey } from '@/utils/rentalLabels'
import {
  backfillXianyuOrderDispatch,
  type XianyuOrderShipRespVO,
  type XianyuOrderVO
} from '@/api/rental/xianyu'

const props = defineProps<{
  modelValue: boolean
  order?: XianyuOrderVO
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'completed', result: XianyuOrderShipRespVO): void
}>()

const { t } = useI18n()
const message = useMessage()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const idempotencyKey = ref('')
const rentalLabel = (value?: string | number | null) => t(getRentalLabelKey('channelOrder', value))
const form = reactive({
  deviceNo: '',
  waybillNo: '',
  expressCode: '',
  expressName: '',
  consignTime: '',
  reason: ''
})

const rules = computed<FormRules>(() => ({
  deviceNo: [
    { required: true, message: t('rental.order.backfillDeviceRequired'), trigger: 'blur' }
  ],
  waybillNo: [
    { required: true, message: t('rental.order.backfillWaybillRequired'), trigger: 'blur' },
    {
      pattern: /^\w{10,}$/,
      message: t('rental.xianyu.waybillNoInvalid'),
      trigger: 'blur'
    }
  ],
  expressCode: [{ required: true, message: t('common.required'), trigger: 'blur' }],
  expressName: [{ required: true, message: t('common.required'), trigger: 'blur' }],
  consignTime: [{ required: true, message: t('common.required'), trigger: 'change' }],
  reason: [{ required: true, message: t('rental.order.backfillReasonRequired'), trigger: 'blur' }]
}))

const normalizeDateTime = (value?: string) => {
  if (value?.trim()) {
    return value.replace('T', ' ').slice(0, 19)
  }
  const now = new Date()
  const pad = (input: number) => String(input).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(
    now.getHours()
  )}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

const resetForm = () => {
  const order = props.order
  form.deviceNo = ''
  form.waybillNo = order?.waybillNo || ''
  form.expressCode = order?.expressCode || ''
  form.expressName = order?.expressName || ''
  form.consignTime = normalizeDateTime(order?.consignTime)
  form.reason = t('rental.order.backfillDefaultReason')
  idempotencyKey.value = `dispatch-backfill-${order?.id || 'unknown'}-${Date.now()}`
  formRef.value?.clearValidate()
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) resetForm()
  }
)

const submit = async () => {
  const order = props.order
  if (!order || !(await formRef.value?.validate())) return
  submitting.value = true
  try {
    const result = await backfillXianyuOrderDispatch({
      channelOrderId: order.id,
      deviceNo: form.deviceNo,
      idempotencyKey: idempotencyKey.value,
      expressCode: form.expressCode,
      expressName: form.expressName,
      waybillNo: form.waybillNo,
      consignTime: form.consignTime,
      reason: form.reason
    })
    message.success(
      t('rental.order.backfillSuccess', {
        deviceNo: result.deviceNo || form.deviceNo
      })
    )
    emit('completed', result)
    emit('update:modelValue', false)
  } finally {
    submitting.value = false
  }
}
</script>
