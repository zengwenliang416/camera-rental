<template>
  <ContentWrap class="rental-order-create-page">
    <div class="page-heading">
      <div>
        <span>OFFLINE MANUAL ORDER</span>
        <h1>{{ t('rental.orderCreate.pageTitle') }}</h1>
        <p>{{ t('rental.orderCreate.pageDescription') }}</p>
      </div>
    </div>

    <el-form
      ref="formRef"
      v-loading="submitting"
      :model="formData"
      :rules="rules"
      label-width="120px"
      class="order-create-form"
    >
      <section class="form-section">
        <h3>{{ t('rental.orderCreate.customerTitle') }}</h3>
        <el-form-item :label="t('rental.orderCreate.customerName')" prop="customerName">
          <el-input
            v-model="formData.customerName"
            class="!w-320px"
            maxlength="64"
            :placeholder="t('rental.orderCreate.customerNamePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('rental.orderCreate.customerMobile')" prop="customerMobile">
          <el-input
            v-model.trim="formData.customerMobile"
            class="!w-320px"
            maxlength="11"
            :placeholder="t('rental.orderCreate.customerMobilePlaceholder')"
            @blur="handleMobileBlur"
          />
        </el-form-item>
        <el-form-item :label="t('rental.orderCreate.customerWechatId')" prop="customerWechatId">
          <el-input
            v-model="formData.customerWechatId"
            class="!w-320px"
            maxlength="64"
            :placeholder="t('rental.orderCreate.optional')"
          />
        </el-form-item>
      </section>

      <section class="form-section">
        <h3>{{ t('rental.orderCreate.periodTitle') }}</h3>
        <el-form-item :label="t('rental.orderCreate.billableStartDate')" prop="billableStartDate">
          <el-date-picker
            v-model="formData.billableStartDate"
            type="date"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledPastDate"
            :placeholder="t('rental.orderCreate.billableStartDatePlaceholder')"
            @change="validateDatePair"
          />
        </el-form-item>
        <el-form-item :label="t('rental.orderCreate.billableEndDate')" prop="billableEndDate">
          <el-date-picker
            v-model="formData.billableEndDate"
            type="date"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledPastDate"
            :placeholder="t('rental.orderCreate.billableEndDatePlaceholder')"
            @change="validateDatePair"
          />
        </el-form-item>
      </section>

      <section class="form-section">
        <h3>{{ t('rental.orderCreate.itemsTitle') }}</h3>
        <el-alert
          class="mb-16px"
          type="info"
          :closable="false"
          show-icon
          :title="t('rental.orderCreate.itemDeviceHint')"
        />
        <div v-for="(item, index) in formData.items" :key="item.key" class="item-row">
          <el-form-item
            class="item-device"
            :label="t('rental.orderCreate.itemDevice')"
            :prop="`items.${index}.devices`"
            :rules="itemDeviceRules"
          >
            <OrderDeviceSelect
              :model-value="item.devices"
              :disabled="!rentalPeriodReady"
              :excluded-device-ids="excludedDeviceIds(index)"
              @update:model-value="updateItemDevices(index, $event)"
            />
          </el-form-item>
          <el-form-item :label="t('rental.orderCreate.itemModel')">
            <el-input
              class="!w-180px"
              disabled
              :model-value="item.devices[0]?.equipmentModelCode || ''"
              :placeholder="t('rental.orderCreate.itemModelAuto')"
            />
          </el-form-item>
          <el-form-item :label="t('rental.orderCreate.itemQuantity')">
            <el-input-number
              class="!w-120px"
              disabled
              :model-value="item.devices.length"
              :min="0"
            />
          </el-form-item>
          <el-form-item
            :label="t('rental.orderCreate.itemRentAmount')"
            :prop="`items.${index}.rentAmount`"
            :rules="itemRentRules"
          >
            <el-input-number
              v-model="item.rentAmount"
              class="!w-160px"
              :min="0"
              :precision="2"
              :controls="false"
              placeholder="0.00"
            />
          </el-form-item>
          <el-button
            class="item-remove"
            link
            type="danger"
            :disabled="formData.items.length <= 1"
            @click="removeItem(index)"
          >
            {{ t('rental.orderCreate.removeItem') }}
          </el-button>
        </div>
        <el-button link type="primary" @click="addItem">
          <Icon icon="ep:plus" class="mr-5px" />
          {{ t('rental.orderCreate.addItem') }}
        </el-button>
      </section>

      <section class="form-section">
        <h3>{{ t('rental.orderCreate.amountTitle') }}</h3>
        <el-form-item :label="t('rental.orderCreate.depositAmount')" prop="depositAmount">
          <el-input-number
            v-model="formData.depositAmount"
            class="!w-160px"
            :min="0"
            :precision="2"
            :controls="false"
            placeholder="0.00"
          />
          <div class="w-full text-12px text-[var(--el-text-color-secondary)]">
            {{ t('rental.orderCreate.depositHint') }}
          </div>
        </el-form-item>
      </section>

      <section class="form-section">
        <h3>{{ t('rental.orderCreate.deliveryTitle') }}</h3>
        <el-form-item :label="t('rental.orderCreate.deliveryMethod')" prop="deliveryMethod">
          <el-radio-group v-model="formData.deliveryMethod" @change="handleDeliveryMethodChange">
            <el-radio value="EXPRESS">{{ t('rental.orderCreate.deliveryMethodExpress') }}</el-radio>
            <el-radio value="ERRAND">{{ t('rental.orderCreate.deliveryMethodErrand') }}</el-radio>
            <el-radio value="SELF_DELIVERY">
              {{ t('rental.orderCreate.deliveryMethodSelfDelivery') }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-alert
          v-if="formData.deliveryMethod === 'EXPRESS'"
          class="mb-16px"
          type="info"
          :closable="false"
          show-icon
          :title="t('rental.orderCreate.expressHint')"
        />
        <template v-else>
          <el-form-item :label="t('rental.orderCreate.receiverName')" prop="receiverName">
            <el-input
              v-model="formData.receiverName"
              class="!w-320px"
              maxlength="64"
              :placeholder="t('rental.orderCreate.receiverNamePlaceholder')"
            />
          </el-form-item>
          <el-form-item :label="t('rental.orderCreate.receiverMobile')" prop="receiverMobile">
            <el-input
              v-model.trim="formData.receiverMobile"
              class="!w-320px"
              maxlength="11"
              :placeholder="t('rental.orderCreate.receiverMobilePlaceholder')"
            />
          </el-form-item>
          <el-form-item :label="t('rental.orderCreate.receiverAddress')" prop="receiverAddress">
            <el-input
              v-model="formData.receiverAddress"
              class="!w-480px"
              maxlength="200"
              :placeholder="t('rental.orderCreate.receiverAddressPlaceholder')"
            />
          </el-form-item>
        </template>
        <el-form-item :label="t('rental.orderCreate.deliveryRemark')" prop="deliveryRemark">
          <el-input
            v-model="formData.deliveryRemark"
            class="!w-480px"
            type="textarea"
            :rows="2"
            maxlength="200"
            :placeholder="deliveryRemarkPlaceholder"
          />
        </el-form-item>
      </section>

      <el-form-item>
        <el-button v-if="canCreateBoundOrder" type="primary" :loading="submitting" @click="submit">
          {{ t('rental.orderCreate.submit') }}
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormItemRule, FormRules } from 'element-plus'
import type { RentalDeviceVO } from '@/api/rental/device'
import {
  createRentalManualOrder,
  suggestRentalCustomer,
  type RentalDeliveryMethod,
  type RentalManualOrderCreateReqVO
} from '@/api/rental/orderCreate'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'
import { hasPermission } from '@/directives/permission/hasPermi'
import OrderDeviceSelect from './components/OrderDeviceSelect.vue'

defineOptions({ name: 'RentalOrderCreate' })

interface OrderItemForm {
  key: number
  devices: RentalDeviceVO[]
  /** 租金，单位：元，提交时换算为分 */
  rentAmount?: number
}

const MOBILE_PATTERN = /^1\d{10}$/

const { t } = useI18n()
const message = useMessage()
const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
let nextItemKey = 2
const canCreateBoundOrder = computed(
  () =>
    hasPermission(['rental:order:create']) &&
    hasPermission(['rental:device:query']) &&
    hasPermission(['rental:device:assign'])
)

const formData = reactive({
  customerName: '',
  customerMobile: '',
  customerWechatId: '',
  items: [{ key: 1, devices: [], rentAmount: undefined }] as OrderItemForm[],
  billableStartDate: '',
  billableEndDate: '',
  depositAmount: undefined as number | undefined,
  deliveryMethod: 'EXPRESS' as RentalDeliveryMethod,
  receiverName: '',
  receiverMobile: '',
  receiverAddress: '',
  deliveryRemark: ''
})

const receiverRequired = computed(() => formData.deliveryMethod !== 'EXPRESS')

const deliveryRemarkPlaceholder = computed(() =>
  formData.deliveryMethod === 'ERRAND'
    ? t('rental.orderCreate.deliveryRemarkErrandPlaceholder')
    : t('rental.orderCreate.deliveryRemarkPlaceholder')
)

const toShanghaiDate = (date: Date) =>
  new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai' }).format(date)

const disabledPastDate = (date: Date) => toShanghaiDate(date) < toShanghaiDate(new Date())

const rentalPeriodReady = computed(
  () =>
    Boolean(formData.billableStartDate) &&
    Boolean(formData.billableEndDate) &&
    formData.billableStartDate <= formData.billableEndDate &&
    formData.billableStartDate >= toShanghaiDate(new Date())
)

const validateDatePair = () => {
  formRef.value?.validateField(['billableStartDate', 'billableEndDate']).catch(() => undefined)
}

const dateValidator = (_rule: unknown, _value: unknown, callback: (error?: Error) => void) => {
  const { billableStartDate, billableEndDate } = formData
  if (!billableStartDate || !billableEndDate) {
    callback(new Error(t('rental.orderCreate.billableDateRequired')))
    return
  }
  if (billableStartDate > billableEndDate || billableStartDate < toShanghaiDate(new Date())) {
    callback(new Error(t('rental.orderCreate.billableDateInvalid')))
    return
  }
  callback()
}

const rules = computed<FormRules>(() => ({
  customerName: [
    { required: true, message: t('rental.orderCreate.customerNameRequired'), trigger: 'blur' }
  ],
  customerMobile: [
    { required: true, message: t('rental.orderCreate.customerMobileRequired'), trigger: 'blur' },
    {
      pattern: MOBILE_PATTERN,
      message: t('rental.orderCreate.customerMobileFormat'),
      trigger: 'blur'
    }
  ],
  billableStartDate: [{ validator: dateValidator, trigger: 'change' }],
  billableEndDate: [{ validator: dateValidator, trigger: 'change' }],
  receiverName: [
    {
      required: receiverRequired.value,
      message: t('rental.orderCreate.receiverNameRequired'),
      trigger: 'blur'
    }
  ],
  receiverMobile: [
    {
      required: receiverRequired.value,
      message: t('rental.orderCreate.receiverMobileRequired'),
      trigger: 'blur'
    }
  ],
  receiverAddress: [
    {
      required: receiverRequired.value,
      message: t('rental.orderCreate.receiverAddressRequired'),
      trigger: 'blur'
    }
  ]
}))

const itemDeviceRules = computed<FormItemRule[]>(() => [
  {
    type: 'array' as const,
    required: true,
    min: 1,
    message: t('rental.orderCreate.itemDeviceRequired'),
    trigger: 'change'
  }
])

const itemRentRules = computed(() => [
  { required: true, message: t('rental.orderCreate.itemRentAmountRequired'), trigger: 'blur' }
])

const addItem = () => {
  formData.items.push({ key: nextItemKey++, devices: [], rentAmount: undefined })
}

const removeItem = (index: number) => {
  if (formData.items.length <= 1) return
  formData.items.splice(index, 1)
}

const excludedDeviceIds = (currentIndex: number) =>
  formData.items.flatMap((item, index) =>
    index === currentIndex ? [] : item.devices.map((device) => device.id)
  )

const updateItemDevices = (index: number, devices: RentalDeviceVO[]) => {
  formData.items[index].devices = devices
  formRef.value?.validateField(`items.${index}.devices`).catch(() => undefined)
}

const handleDeliveryMethodChange = () => {
  formRef.value?.clearValidate(['receiverName', 'receiverMobile', 'receiverAddress'])
}

let suggestVersion = 0
let lastSuggestedMobile = ''
const handleMobileBlur = async () => {
  const mobile = formData.customerMobile.trim()
  if (!MOBILE_PATTERN.test(mobile) || mobile === lastSuggestedMobile) return
  const version = ++suggestVersion
  try {
    const customer = await suggestRentalCustomer(mobile)
    if (version !== suggestVersion || formData.customerMobile.trim() !== mobile || !customer) return
    lastSuggestedMobile = mobile
    formData.customerName = customer.name
    formData.customerWechatId = customer.wechatId || ''
    message.info(t('rental.orderCreate.customerMatched'))
  } catch {
    // 反查失败不阻断录单
  }
}

const yuanToFen = (value?: number) => (value === undefined ? undefined : Math.round(value * 100))

const buildRequest = (): RentalManualOrderCreateReqVO => ({
  customer: {
    name: formData.customerName.trim(),
    mobile: formData.customerMobile.trim(),
    wechatId: formData.customerWechatId.trim() || undefined
  },
  items: formData.items.map((item) => ({
    modelCode: item.devices[0]?.equipmentModelCode || '',
    quantity: item.devices.length,
    deviceIds: item.devices.map((device) => device.id),
    rentAmount: yuanToFen(item.rentAmount) ?? 0
  })),
  billableStartDate: formData.billableStartDate,
  billableEndDate: formData.billableEndDate,
  depositAmount: yuanToFen(formData.depositAmount),
  delivery: {
    method: formData.deliveryMethod,
    receiverName: receiverRequired.value ? formData.receiverName.trim() : undefined,
    receiverMobile: receiverRequired.value ? formData.receiverMobile.trim() : undefined,
    receiverAddress: receiverRequired.value ? formData.receiverAddress.trim() : undefined,
    remark: formData.deliveryRemark.trim() || undefined
  }
})

const submit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const result = await createRentalManualOrder(buildRequest())
    message.success(t('rental.orderCreate.createSuccess', { orderNo: result.orderNo }))
    router.push({
      name: 'RentalSchedule',
      query: {
        occupyStartDate: formData.billableStartDate,
        deviceId: formData.items[0]?.devices[0]?.id
      }
    })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.rental-order-create-page .page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.rental-order-create-page .page-heading span {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--el-text-color-secondary);
}

.rental-order-create-page .page-heading h1 {
  margin: 4px 0;
  font-size: 20px;
}

.rental-order-create-page .page-heading p {
  margin: 0;
  color: var(--el-text-color-secondary);
}

.form-section {
  padding: 4px 0 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.form-section:first-of-type {
  border-top: none;
}

.form-section h3 {
  margin: 8px 0 16px;
  font-size: 14px;
}

.item-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0 16px;
  align-items: flex-start;
  padding: 12px;
  margin-bottom: 12px;
  background: var(--el-fill-color-extra-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.item-device {
  flex: 1 1 520px;
}

.item-remove {
  margin-top: 5px;
}
</style>
