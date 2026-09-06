<template>
  <div class="rental-order-create-page">
    <div class="order-sheet">
      <header class="sheet-head">
        <div class="sheet-title">
          <h1>{{ t('rental.orderCreate.pageTitle') }}</h1>
          <span class="sheet-desc">{{ t('rental.orderCreate.pageDescription') }}</span>
        </div>
        <div class="sheet-meta">
          <span>
            {{ t('rental.orderCreate.metaCreatedDate') }}
            <strong>{{ todayDate }}</strong>
          </span>
          <span>
            {{ t('rental.orderCreate.metaSourceLabel') }}
            <strong>{{ t('rental.orderCreate.metaSourceOffline') }}</strong>
          </span>
        </div>
      </header>

      <el-form
        ref="formRef"
        v-loading="submitting"
        :model="formData"
        :rules="rules"
        label-width="82px"
        class="sheet-body"
      >
        <div class="sheet-main">
          <section class="sheet-section">
            <div class="section-title">
              <h2>{{ t('rental.orderCreate.customerTitle') }}</h2>
            </div>
            <div class="field-grid">
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
              <el-form-item
                :label="t('rental.orderCreate.customerWechatId')"
                prop="customerWechatId"
              >
                <el-input
                  v-model="formData.customerWechatId"
                  maxlength="64"
                  :placeholder="t('rental.orderCreate.optional')"
                />
              </el-form-item>
            </div>
          </section>

          <section class="sheet-section">
            <div class="section-title">
              <h2>{{ t('rental.orderCreate.periodTitle') }}</h2>
              <span class="section-note">{{ t('rental.orderCreate.periodNote') }}</span>
            </div>
            <div class="field-grid">
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

          <section class="sheet-section">
            <div class="section-title">
              <h2>{{ t('rental.orderCreate.itemsTitle') }}</h2>
            </div>
            <el-alert
              class="mb-10px"
              type="info"
              :closable="false"
              show-icon
              :title="t('rental.orderCreate.itemDeviceHint')"
            />
            <div v-for="(item, index) in formData.items" :key="item.key" class="item-card">
              <div class="item-head">
                <span class="item-no">
                  {{ t('rental.orderCreate.itemLabel', { index: index + 1 }) }}
                </span>
                <el-tag v-if="item.devices[0]?.equipmentModelCode" size="small" effect="plain">
                  {{ item.devices[0].equipmentModelCode }}
                </el-tag>
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
              <el-form-item
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
              <div class="item-foot">
                <el-form-item :label="t('rental.orderCreate.itemQuantity')">
                  <el-input-number
                    class="!w-110px"
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
                    class="!w-140px"
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

          <section class="sheet-section">
            <div class="section-title">
              <h2>{{ t('rental.orderCreate.deliveryTitle') }}</h2>
            </div>
            <el-form-item
              class="delivery-method"
              :label="t('rental.orderCreate.deliveryMethod')"
              prop="deliveryMethod"
            >
              <el-radio-group
                v-model="formData.deliveryMethod"
                @change="handleDeliveryMethodChange"
              >
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
              type="info"
              :closable="false"
              show-icon
              :title="t('rental.orderCreate.expressHint')"
            />
            <div v-else class="field-grid">
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
                class="span-all"
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
            <el-form-item
              class="delivery-remark"
              :label="t('rental.orderCreate.deliveryRemark')"
              prop="deliveryRemark"
            >
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

        <aside class="sheet-stub">
          <h2>{{ t('rental.orderCreate.summaryTitle') }}</h2>
          <dl class="stub-rows">
            <div class="stub-row">
              <dt>{{ t('rental.orderCreate.summaryPeriod') }}</dt>
              <dd v-if="rentalPeriodReady">
                {{ formData.billableStartDate }} ~ {{ formData.billableEndDate }}
                <span class="stub-sub">
                  {{ t('rental.orderCreate.summaryDays', { days: rentalDays }) }}
                </span>
              </dd>
              <dd v-else class="stub-empty">{{ t('rental.orderCreate.summaryPeriodEmpty') }}</dd>
            </div>
            <div class="stub-row">
              <dt>{{ t('rental.orderCreate.summaryDevices') }}</dt>
              <dd>{{ totalDeviceCount }} {{ t('rental.orderCreate.summaryDeviceUnit') }}</dd>
            </div>
            <div class="stub-row">
              <dt>{{ t('rental.orderCreate.summaryRent') }}</dt>
              <dd>{{ formatYuan(totalRentAmount) }}</dd>
            </div>
          </dl>
          <div class="stub-deposit">
            <span class="stub-deposit-label">{{ t('rental.orderCreate.depositAmount') }}</span>
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
            <p class="stub-hint">{{ t('rental.orderCreate.depositHint') }}</p>
          </div>
          <div class="stub-total">
            <span>{{ t('rental.orderCreate.summaryTotal') }}</span>
            <strong>{{ formatYuan(totalPayable) }}</strong>
          </div>
          <el-button
            v-if="canCreateBoundOrder"
            class="stub-submit"
            type="primary"
            :loading="submitting"
            @click="submit"
          >
            {{ t('rental.orderCreate.submit') }}
          </el-button>
        </aside>
      </el-form>
    </div>
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

const todayDate = toShanghaiDate(new Date())

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
  padding-bottom: 16px;
  font-size: 13px;
}

.order-sheet {
  max-width: 1080px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
}

/* ---------- 单据抬头 ---------- */
.sheet-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 24px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.sheet-title h1 {
  display: inline;
  font-family: 'Songti SC', 'Noto Serif SC', STSong, SimSun, serif;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0.05em;
}

.sheet-desc {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.sheet-meta {
  flex-shrink: 0;
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-variant-numeric: tabular-nums;
}

.sheet-meta strong {
  font-weight: 500;
  color: var(--el-text-color-regular);
}

/* ---------- 主联 + 存根联 ---------- */
.sheet-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 272px;
  align-items: start;
}

.sheet-main {
  min-width: 0;
}

.sheet-section {
  padding: 12px 24px 14px;
}

.sheet-section + .sheet-section {
  border-top: 1px solid var(--el-border-color-lighter);
}

.section-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 10px;
}

.section-title h2 {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
}

.section-note {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 8px 20px;
}

.field-grid .span-all {
  grid-column: 1 / -1;
}

.field-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.field-grid :deep(.el-form-item__label) {
  font-size: 12px;
}

/* ---------- 设备明细 ---------- */
.item-card {
  padding: 8px 12px 10px;
  margin-bottom: 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-left: 3px solid var(--el-text-color-primary);
  border-radius: 4px;
}

.item-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.item-no {
  font-size: 12px;
  font-weight: 600;
}

.item-remove {
  margin-left: auto;
}

.item-card :deep(.el-form-item) {
  margin-bottom: 8px;
}

.item-foot {
  display: flex;
  flex-wrap: wrap;
  gap: 0 16px;
}

.item-foot :deep(.el-form-item) {
  margin-bottom: 0;
}

.add-item-btn {
  width: 100%;
  border-style: dashed;
}

.delivery-method {
  margin-bottom: 10px;
}

.delivery-remark {
  margin-top: 8px;
  margin-bottom: 0;
}

/* ---------- 存根联 ---------- */
.sheet-stub {
  position: sticky;
  top: 12px;
  padding: 12px 20px 16px;
  border-left: 2px dashed var(--el-border-color);
}

.sheet-stub h2 {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: var(--el-text-color-secondary);
}

.stub-rows {
  margin: 0;
  font-size: 12px;
}

.stub-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 10px;
  padding: 5px 0;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}

.stub-row dt {
  flex-shrink: 0;
  color: var(--el-text-color-secondary);
}

.stub-row dd {
  margin: 0;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.stub-sub {
  margin-left: 4px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.stub-empty {
  color: var(--el-text-color-placeholder);
}

.stub-deposit {
  margin-top: 10px;
}

.stub-deposit-label {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.stub-deposit :deep(.el-form-item) {
  margin-bottom: 0;
}

.stub-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.stub-total {
  display: flex;
  padding-top: 10px;
  margin-top: 12px;
  border-top: 2px solid var(--el-text-color-primary);
  justify-content: space-between;
  align-items: baseline;
}

.stub-total span {
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.stub-total strong {
  font-family: 'Songti SC', 'Noto Serif SC', STSong, SimSun, serif;
  font-size: 20px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.stub-submit {
  width: 100%;
  margin-top: 12px;
  letter-spacing: 0.1em;
}

/* ---------- 响应式 ---------- */
@media (width <= 900px) {
  .sheet-body {
    grid-template-columns: 1fr;
  }

  .sheet-stub {
    position: static;
    border-top: 2px dashed var(--el-border-color);
    border-left: none;
  }

  .sheet-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .sheet-section {
    padding: 12px 16px 14px;
  }
}
</style>
