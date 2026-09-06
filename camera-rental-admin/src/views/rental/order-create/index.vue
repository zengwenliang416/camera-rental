<template>
  <div class="rental-order-create-page">
    <div class="page-heading">
      <span class="page-eyebrow">OFFLINE MANUAL ORDER</span>
      <h1>{{ t('rental.orderCreate.pageTitle') }}</h1>
      <p>{{ t('rental.orderCreate.pageDescription') }}</p>
    </div>

    <el-form
      ref="formRef"
      v-loading="submitting"
      :model="formData"
      :rules="rules"
      label-width="110px"
      class="order-create-layout"
    >
      <div class="form-main">
        <section class="form-card">
          <header class="form-card-header">
            <span class="form-card-index">01</span>
            <h3>{{ t('rental.orderCreate.customerTitle') }}</h3>
          </header>
          <div class="form-grid">
            <el-form-item :label="t('rental.orderCreate.customerName')" prop="customerName">
              <el-input
                v-model="formData.customerName"
                maxlength="64"
                :placeholder="t('rental.orderCreate.customerNamePlaceholder')"
              />
            </el-form-item>
            <el-form-item :label="t('rental.orderCreate.customerMobile')" prop="customerMobile">
              <el-input
                v-model.trim="formData.customerMobile"
                maxlength="11"
                :placeholder="t('rental.orderCreate.customerMobilePlaceholder')"
                @blur="handleMobileBlur"
              />
            </el-form-item>
            <el-form-item :label="t('rental.orderCreate.customerWechatId')" prop="customerWechatId">
              <el-input
                v-model="formData.customerWechatId"
                maxlength="64"
                :placeholder="t('rental.orderCreate.optional')"
              />
            </el-form-item>
          </div>
        </section>

        <section class="form-card">
          <header class="form-card-header">
            <span class="form-card-index">02</span>
            <h3>{{ t('rental.orderCreate.periodTitle') }}</h3>
          </header>
          <div class="form-grid">
            <el-form-item
              :label="t('rental.orderCreate.billableStartDate')"
              prop="billableStartDate"
            >
              <el-date-picker
                v-model="formData.billableStartDate"
                class="w-full"
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
                class="w-full"
                type="date"
                value-format="YYYY-MM-DD"
                :disabled-date="disabledPastDate"
                :placeholder="t('rental.orderCreate.billableEndDatePlaceholder')"
                @change="validateDatePair"
              />
            </el-form-item>
          </div>
        </section>

        <section class="form-card">
          <header class="form-card-header">
            <span class="form-card-index">03</span>
            <h3>{{ t('rental.orderCreate.itemsTitle') }}</h3>
          </header>
          <el-alert
            class="mb-16px"
            type="info"
            :closable="false"
            show-icon
            :title="t('rental.orderCreate.itemDeviceHint')"
          />
          <div v-for="(item, index) in formData.items" :key="item.key" class="item-card">
            <div class="item-card-header">
              <span class="item-card-title">
                {{ t('rental.orderCreate.itemLabel', { index: index + 1 }) }}
              </span>
              <el-tag
                v-if="item.devices[0]?.equipmentModelCode"
                size="small"
                effect="plain"
                class="item-card-model"
              >
                {{ item.devices[0].equipmentModelCode }}
              </el-tag>
              <el-button
                class="item-card-remove"
                link
                type="danger"
                :disabled="formData.items.length <= 1"
                @click="removeItem(index)"
              >
                {{ t('rental.orderCreate.removeItem') }}
              </el-button>
            </div>
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
            <div class="item-card-footer">
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
            </div>
          </div>
          <el-button class="add-item-btn" @click="addItem">
            <Icon icon="ep:plus" class="mr-5px" />
            {{ t('rental.orderCreate.addItem') }}
          </el-button>
        </section>

        <section class="form-card">
          <header class="form-card-header">
            <span class="form-card-index">04</span>
            <h3>{{ t('rental.orderCreate.deliveryTitle') }}</h3>
          </header>
          <el-form-item :label="t('rental.orderCreate.deliveryMethod')" prop="deliveryMethod">
            <el-radio-group v-model="formData.deliveryMethod" @change="handleDeliveryMethodChange">
              <el-radio-button value="EXPRESS">
                {{ t('rental.orderCreate.deliveryMethodExpress') }}
              </el-radio-button>
              <el-radio-button value="ERRAND">
                {{ t('rental.orderCreate.deliveryMethodErrand') }}
              </el-radio-button>
              <el-radio-button value="SELF_DELIVERY">
                {{ t('rental.orderCreate.deliveryMethodSelfDelivery') }}
              </el-radio-button>
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
          <div v-else class="form-grid">
            <el-form-item :label="t('rental.orderCreate.receiverName')" prop="receiverName">
              <el-input
                v-model="formData.receiverName"
                maxlength="64"
                :placeholder="t('rental.orderCreate.receiverNamePlaceholder')"
              />
            </el-form-item>
            <el-form-item :label="t('rental.orderCreate.receiverMobile')" prop="receiverMobile">
              <el-input
                v-model.trim="formData.receiverMobile"
                maxlength="11"
                :placeholder="t('rental.orderCreate.receiverMobilePlaceholder')"
              />
            </el-form-item>
            <el-form-item
              class="form-grid-span"
              :label="t('rental.orderCreate.receiverAddress')"
              prop="receiverAddress"
            >
              <el-input
                v-model="formData.receiverAddress"
                maxlength="200"
                :placeholder="t('rental.orderCreate.receiverAddressPlaceholder')"
              />
            </el-form-item>
          </div>
          <el-form-item :label="t('rental.orderCreate.deliveryRemark')" prop="deliveryRemark">
            <el-input
              v-model="formData.deliveryRemark"
              type="textarea"
              :rows="2"
              maxlength="200"
              :placeholder="deliveryRemarkPlaceholder"
            />
          </el-form-item>
        </section>
      </div>

      <aside class="form-aside">
        <section class="form-card summary-card">
          <header class="form-card-header">
            <h3>{{ t('rental.orderCreate.summaryTitle') }}</h3>
          </header>
          <dl class="summary-list">
            <div class="summary-row">
              <dt>{{ t('rental.orderCreate.summaryPeriod') }}</dt>
              <dd v-if="rentalPeriodReady">
                {{ formData.billableStartDate }} ~ {{ formData.billableEndDate }}
                <span class="summary-sub">
                  {{ t('rental.orderCreate.summaryDays', { days: rentalDays }) }}
                </span>
              </dd>
              <dd v-else class="summary-empty">
                {{ t('rental.orderCreate.summaryPeriodEmpty') }}
              </dd>
            </div>
            <div class="summary-row">
              <dt>{{ t('rental.orderCreate.summaryDevices') }}</dt>
              <dd> {{ totalDeviceCount }} {{ t('rental.orderCreate.summaryDeviceUnit') }} </dd>
            </div>
            <div class="summary-row">
              <dt>{{ t('rental.orderCreate.summaryRent') }}</dt>
              <dd>{{ formatYuan(totalRentAmount) }}</dd>
            </div>
          </dl>
          <div class="summary-deposit">
            <span class="summary-deposit-label">{{ t('rental.orderCreate.depositAmount') }}</span>
            <el-form-item prop="depositAmount">
              <el-input-number
                v-model="formData.depositAmount"
                class="w-full"
                :min="0"
                :precision="2"
                :controls="false"
                placeholder="0.00"
              />
            </el-form-item>
            <p class="summary-hint">{{ t('rental.orderCreate.depositHint') }}</p>
          </div>
          <div class="summary-total">
            <span>{{ t('rental.orderCreate.summaryTotal') }}</span>
            <strong>{{ formatYuan(totalPayable) }}</strong>
          </div>
          <el-button
            v-if="canCreateBoundOrder"
            class="summary-submit"
            type="primary"
            size="large"
            :loading="submitting"
            @click="submit"
          >
            {{ t('rental.orderCreate.submit') }}
          </el-button>
        </section>
      </aside>
    </el-form>
  </div>
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

// 摘要区展示用的计租天数（闭区间），仅展示，不参与提交
const rentalDays = computed(() => {
  if (!rentalPeriodReady.value) return 0
  const start = Date.parse(`${formData.billableStartDate}T00:00:00Z`)
  const end = Date.parse(`${formData.billableEndDate}T00:00:00Z`)
  return Math.round((end - start) / 86400000) + 1
})

const totalDeviceCount = computed(() =>
  formData.items.reduce((sum, item) => sum + item.devices.length, 0)
)

const totalRentAmount = computed(() =>
  formData.items.reduce((sum, item) => sum + (item.rentAmount || 0), 0)
)

const totalPayable = computed(() => totalRentAmount.value + (formData.depositAmount || 0))

const formatYuan = (value: number) => `¥ ${value.toFixed(2)}`

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
.rental-order-create-page {
  padding-bottom: 24px;
}

.page-heading {
  margin: 4px 4px 20px;
}

.page-eyebrow {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: var(--el-color-primary);
}

.page-heading h1 {
  margin: 6px 0 4px;
  font-size: 22px;
  font-weight: 600;
}

.page-heading p {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.order-create-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
  align-items: start;
}

.form-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.form-card {
  padding: 20px 20px 4px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
}

.form-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
}

.form-card-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.form-card-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-radius: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  column-gap: 24px;
}

.form-grid-span {
  grid-column: 1 / -1;
}

.form-grid .el-form-item .el-input,
.form-grid .el-form-item .el-date-editor {
  width: 100%;
}

.item-card {
  padding: 12px 16px 0;
  margin-bottom: 12px;
  background: var(--el-fill-color-extra-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.item-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.item-card-title {
  font-size: 13px;
  font-weight: 600;
}

.item-card-remove {
  margin-left: auto;
}

.item-card-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 0 24px;
}

.add-item-btn {
  width: 100%;
  margin-bottom: 16px;
  border-style: dashed;
}

.form-aside {
  position: sticky;
  top: 12px;
}

.summary-list {
  margin: 0 0 4px;
}

.summary-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}

.summary-row dt {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.summary-row dd {
  margin: 0;
  font-size: 13px;
  text-align: right;
}

.summary-sub {
  margin-left: 6px;
  color: var(--el-text-color-secondary);
}

.summary-empty {
  color: var(--el-text-color-placeholder);
}

.summary-deposit {
  margin-top: 16px;
}

.summary-deposit-label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.summary-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.summary-total {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding-top: 16px;
  margin-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.summary-total span {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.summary-total strong {
  font-size: 22px;
  font-weight: 700;
  color: var(--el-color-primary);
}

.summary-submit {
  width: 100%;
  margin: 16px 0;
}

@media (width <= 1100px) {
  .order-create-layout {
    grid-template-columns: 1fr;
  }

  .form-aside {
    position: static;
  }
}
</style>
