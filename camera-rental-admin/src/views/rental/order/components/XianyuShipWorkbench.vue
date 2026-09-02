<template>
  <div class="ship-workbench">
    <div class="mb-12px flex flex-wrap items-center justify-between gap-8px">
      <div class="flex flex-wrap items-center gap-8px">
        <strong>{{ t('rental.order.shipDrawerTitle') }}</strong>
        <el-tag :type="config?.writeEnabled ? 'warning' : 'info'">
          {{ t('rental.xianyu.writeSwitch') }}:
          {{ config?.writeEnabled ? t('common.yes') : t('common.no') }}
        </el-tag>
      </div>
      <el-tag v-if="initialOrder" type="primary">
        {{ t('rental.xianyu.shipConfirmOrder') }}: {{ initialOrder.externalOrderId }}
      </el-tag>
    </div>

    <el-alert
      v-if="loadError"
      class="mb-12px"
      type="error"
      :closable="false"
      :title="t('rental.common.loadError')"
    >
      <el-button link type="primary" @click="retryWorkbench">
        {{ t('rental.common.retry') }}
      </el-button>
    </el-alert>
    <el-alert
      class="mb-12px"
      type="warning"
      :closable="false"
      :title="t('rental.order.shipDrawerHint')"
    />

    <el-steps class="mb-16px" :active="currentStep" finish-status="success" simple>
      <el-step :title="t('rental.xianyu.shipStepWaybill')" />
      <el-step :title="t('rental.xianyu.shipStepDevice')" />
      <el-step :title="t('rental.xianyu.shipStepSubmit')" />
    </el-steps>

    <el-card shadow="never">
      <template #header>
        <div class="flex flex-wrap items-center justify-between gap-8px">
          <span>{{ stepTitle }}</span>
          <span class="text-13px text-[var(--el-text-color-secondary)]">
            {{ currentStep + 1 }} / 3
          </span>
        </div>
      </template>

      <XianyuWaybillStep
        v-if="currentStep === 0"
        v-model:upload-files="shipmentUploadFiles"
        :form="shipmentForm"
        :express-list="expressList"
        :ocr="shipmentOcr"
        :ocr-loading="shipmentOcrLoading"
        :has-image="Boolean(shipmentImageFile)"
        @image-change="handleShipmentImageChange"
        @image-remove="handleShipmentImageRemove"
        @image-exceed="handleImageExceed"
        @ocr="handleShipmentOcr"
        @express-change="handleShipmentExpressChange"
        @waybill-recognize="handleWaybillRecognize"
      />
      <XianyuDeviceStep
        v-else-if="currentStep === 1"
        v-model:upload-files="deviceQrUploadFiles"
        :form="shipmentForm"
        :selected-device="selectedDevice"
        :has-image="Boolean(deviceQrImageFile)"
        :decoding="deviceQrLoading"
        @image-change="handleDeviceQrImageChange"
        @image-remove="handleDeviceQrImageRemove"
        @image-exceed="handleImageExceed"
        @decode="handleDeviceQrImageDecode"
        @device-select="handleDeviceCascadeSelect"
      />
      <XianyuShipConfirmStep
        v-else
        :form="shipmentForm"
        :order="selectedPendingShipOrder"
        :device="selectedDevice"
        :result="shipmentResult"
        :requires-product-rule-binding="requiresProductRuleBinding"
        :can-bind-product-rule="canBindProductRule"
      />

      <div class="mt-16px flex flex-wrap justify-between gap-8px">
        <div class="flex flex-wrap gap-8px">
          <el-button @click="resetShipmentWorkbench">
            {{ t('common.reset') }}
          </el-button>
          <el-button :disabled="currentStep === 0" @click="handlePreviousStep">
            {{ t('common.prevLabel') }}
          </el-button>
        </div>
        <div class="flex flex-wrap gap-8px">
          <el-button v-if="currentStep < 2" type="primary" @click="handleNextStep">
            {{ t('common.nextLabel') }}
          </el-button>
          <el-button
            v-else
            type="danger"
            :loading="shipping"
            :disabled="
              Boolean(shipmentResult) || (requiresProductRuleBinding && !canBindProductRule)
            "
            v-hasPermi="['rental:xianyu:ship']"
            @click="handleShipXianyuOrder"
          >
            {{
              requiresProductRuleBinding
                ? t('rental.xianyu.confirmBindProductRuleAndShip')
                : t('rental.xianyu.confirmShip')
            }}
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type {
  UploadFile,
  UploadFiles,
  UploadProps,
  UploadRawFile,
  UploadUserFile
} from 'element-plus'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'
import { hasPermission } from '@/directives/permission/hasPermi'
import {
  getXianyuConfig,
  getXianyuExpressCompanyList,
  recognizeXianyuExpress,
  recognizeXianyuShipmentImage,
  shipXianyuOrder,
  type XianyuConfigVO,
  type XianyuExpressCompanyVO,
  type XianyuOrderShipRespVO,
  type XianyuPendingShipOrderVO,
  type XianyuShipmentOcrRespVO
} from '@/api/rental/xianyu'
import { resolveRentalDeviceQr, type RentalDeviceVO } from '@/api/rental/device'
import XianyuDeviceStep from './XianyuDeviceStep.vue'
import XianyuShipConfirmStep from './XianyuShipConfirmStep.vue'
import XianyuWaybillStep from './XianyuWaybillStep.vue'
import type { XianyuShipmentForm } from './xianyuShipWorkbenchTypes'

defineOptions({ name: 'XianyuShipWorkbench' })

const props = defineProps<{
  initialOrder: XianyuPendingShipOrderVO
}>()
const emit = defineEmits<{
  shipped: [result: XianyuOrderShipRespVO]
}>()

type BarcodeDetectorLike = {
  detect: (image: ImageBitmapSource) => Promise<Array<{ rawValue?: string }>>
}

type BarcodeDetectorConstructor = new (options?: { formats?: string[] }) => BarcodeDetectorLike

const { t } = useI18n()
const message = useMessage()

const currentStep = ref(0)
const config = ref<XianyuConfigVO>()
const loadError = ref(false)
const expressList = ref<XianyuExpressCompanyVO[]>([])
const shipmentUploadFiles = ref<UploadUserFile[]>([])
const shipmentImageFile = ref<UploadRawFile>()
const shipmentOcr = ref<XianyuShipmentOcrRespVO>()
const shipmentOcrLoading = ref(false)
const deviceQrUploadFiles = ref<UploadUserFile[]>([])
const deviceQrImageFile = ref<UploadRawFile>()
const deviceQrLoading = ref(false)
const selectedDevice = ref<RentalDeviceVO>()
const shipping = ref(false)
const selectedPendingShipOrder = ref<XianyuPendingShipOrderVO>()
const shipmentResult = ref<XianyuOrderShipRespVO>()
const canOcrShipment = computed(() => hasPermission(['rental:xianyu:ship:ocr']))
const canShipXianyuOrder = computed(() => hasPermission(['rental:xianyu:ship']))
const canBindProductRule = computed(() => hasPermission(['rental:configuration:update']))
const requiresProductRuleBinding = computed(
  () =>
    selectedPendingShipOrder.value?.preparationStatus === 'WAITING_MODEL' &&
    selectedPendingShipOrder.value?.preparationReasonCode === 'PRODUCT_RULE_NOT_CONFIGURED'
)

const shipmentForm = reactive<XianyuShipmentForm>({
  shopId: undefined,
  keyword: '',
  waybillNo: '',
  expressCode: '',
  expressName: '',
  deviceNo: '',
  pageNo: 1,
  pageSize: 5
})

const stepTitle = computed(() => {
  return [
    t('rental.xianyu.shipWaybillTitle'),
    t('rental.xianyu.shipDeviceTitle'),
    t('rental.xianyu.shipStepSubmit')
  ][currentStep.value]
})

const loadConfig = async () => {
  try {
    config.value = await getXianyuConfig()
  } catch {
    config.value = undefined
    loadError.value = true
  }
}

const loadExpressCompanies = async () => {
  try {
    expressList.value = await getXianyuExpressCompanyList()
  } catch {
    expressList.value = []
    loadError.value = true
  }
}

const getUploadRawImage = (uploadFile: UploadFile, uploadFiles: UploadFiles) => {
  const raw = uploadFile.raw
  if (!raw) {
    return undefined
  }
  if (!raw.type.startsWith('image/')) {
    message.warning(t('rental.xianyu.shipUploadImageOnly'))
    return undefined
  }
  if (raw.size > 8 * 1024 * 1024) {
    message.warning(t('rental.xianyu.shipUploadTooLarge'))
    return undefined
  }
  return { raw, files: uploadFiles.slice(-1) }
}

const handleShipmentImageChange: UploadProps['onChange'] = (
  uploadFile: UploadFile,
  uploadFiles: UploadFiles
) => {
  const image = getUploadRawImage(uploadFile, uploadFiles)
  shipmentImageFile.value = image?.raw
  shipmentUploadFiles.value = image?.files || []
  shipmentResult.value = undefined
}

const handleShipmentImageRemove = () => {
  shipmentImageFile.value = undefined
  shipmentOcr.value = undefined
}

const handleDeviceQrImageChange: UploadProps['onChange'] = (
  uploadFile: UploadFile,
  uploadFiles: UploadFiles
) => {
  const image = getUploadRawImage(uploadFile, uploadFiles)
  deviceQrImageFile.value = image?.raw
  deviceQrUploadFiles.value = image?.files || []
  selectedDevice.value = undefined
}

const handleDeviceQrImageRemove = () => {
  deviceQrImageFile.value = undefined
}

const handleImageExceed = () => {
  message.warning(t('rental.xianyu.shipUploadLimit'))
}

const handleShipmentOcr = async () => {
  if (!canOcrShipment.value) {
    message.warning(t('error.noPermission'))
    return
  }
  if (!shipmentImageFile.value) {
    message.warning(t('rental.xianyu.shipUploadRequired'))
    return
  }
  shipmentOcrLoading.value = true
  try {
    const result = await recognizeXianyuShipmentImage(shipmentImageFile.value)
    shipmentOcr.value = result
    if (result.waybillNo) {
      shipmentForm.waybillNo = result.waybillNo
    }
    applyShipmentExpress(result.expressCode, result.expressName)
    if (!result.waybillNo) {
      message.warning(t('rental.xianyu.shipOcrNoWaybill'))
      return
    }
    message.success(t('rental.xianyu.shipOcrSuccess'))
  } finally {
    shipmentOcrLoading.value = false
  }
}

const handleDeviceQrImageDecode = async () => {
  if (!deviceQrImageFile.value) {
    message.warning(t('rental.xianyu.deviceQrImageRequired'))
    return
  }
  deviceQrLoading.value = true
  try {
    const payload = await decodeBarcodeFromImage(deviceQrImageFile.value)
    await resolveDeviceQrPayload(
      payload,
      parseDeviceNoFromLabelFileName(deviceQrImageFile.value.name)
    )
  } catch (error) {
    message.warning(
      error instanceof Error ? error.message : t('rental.xianyu.deviceQrDecodeFailed')
    )
  } finally {
    deviceQrLoading.value = false
  }
}

const decodeBarcodeFromImage = async (file: File) => {
  const Detector = (
    globalThis as typeof globalThis & {
      BarcodeDetector?: BarcodeDetectorConstructor
    }
  ).BarcodeDetector
  if (!Detector) {
    throw new Error(t('rental.xianyu.deviceQrDecodeUnsupported'))
  }
  const detector = new Detector({ formats: ['qr_code', 'code_128', 'code_39'] })
  const image = await createImageBitmap(file)
  try {
    const codes = await detector.detect(image)
    const value = codes?.[0]?.rawValue?.trim()
    if (!value) {
      throw new Error(t('rental.xianyu.deviceQrNoCode'))
    }
    return value
  } finally {
    image.close()
  }
}

const handleDeviceCascadeSelect = (device: RentalDeviceVO) => {
  selectedDevice.value = device
  shipmentForm.deviceNo = device.deviceNo
  message.success(t('rental.xianyu.deviceNoFilled', { deviceNo: device.deviceNo }))
}

const resolveDeviceQrPayload = async (payload: string, fallbackDeviceNo?: string) => {
  if (payload.startsWith('CRD1|')) {
    try {
      const device = await resolveRentalDeviceQr(payload)
      selectedDevice.value = device
      shipmentForm.deviceNo = device.deviceNo
      message.success(t('rental.xianyu.deviceQrResolved', { deviceNo: device.deviceNo }))
    } catch (error) {
      if (!fallbackDeviceNo) {
        throw error
      }
      shipmentForm.deviceNo = fallbackDeviceNo
      selectedDevice.value = undefined
      message.warning(t('rental.xianyu.deviceQrFileNameFallback', { deviceNo: fallbackDeviceNo }))
    }
    return
  }
  shipmentForm.deviceNo = payload
  selectedDevice.value = undefined
  message.success(t('rental.xianyu.deviceNoFilled', { deviceNo: payload }))
}

const parseDeviceNoFromLabelFileName = (fileName: string) => {
  const baseName = fileName.replace(/\.[^.]+$/, '')
  const matched = baseName.match(/^(?:\d+-)?([A-Za-z0-9]+-\d{2}-[A-Za-z0-9]+)-(.+)$/)
  return matched?.[1]?.toUpperCase()
}

const applyShipmentExpress = (expressCode?: string, expressName?: string) => {
  const normalizedCode = expressCode?.trim()
  const normalizedName = expressName?.trim()
  const matched = expressList.value.find(
    (item) =>
      item.code === normalizedCode ||
      item.expressName === normalizedName ||
      item.expressAlias === normalizedName
  )
  if (matched) {
    shipmentForm.expressCode = matched.code
    shipmentForm.expressName = matched.expressName
    return
  }
  if (normalizedCode) {
    shipmentForm.expressCode = normalizedCode
  }
  if (normalizedName) {
    shipmentForm.expressName = normalizedName
  }
}

const handleShipmentExpressChange = (code?: string) => {
  const matched = expressList.value.find((item) => item.code === code)
  shipmentForm.expressName = matched?.expressName || ''
  autoRecognizedExpressCode.value = ''
}

// 自动识别出的快递编码；用户手工改动后不再覆盖
const autoRecognizedExpressCode = ref('')
const waybillRecognizing = ref(false)

const handleWaybillRecognize = async (waybillNo: string) => {
  const trimmed = waybillNo.trim()
  // 识别是辅助能力：单号太短、识别中或用户已手工选择过快递时跳过
  if (!/^\w{10,}$/.test(trimmed) || waybillRecognizing.value) {
    return
  }
  if (
    shipmentForm.expressCode.trim() &&
    shipmentForm.expressCode.trim() !== autoRecognizedExpressCode.value
  ) {
    return
  }
  waybillRecognizing.value = true
  try {
    const candidates = await recognizeXianyuExpress(trimmed)
    for (const candidate of candidates) {
      const matched = expressList.value.find(
        (item) =>
          item.code === candidate.code ||
          item.expressName === candidate.name ||
          item.expressAlias === candidate.name
      )
      if (matched) {
        shipmentForm.expressCode = matched.code
        shipmentForm.expressName = matched.expressName
        autoRecognizedExpressCode.value = matched.code
        message.success(t('rental.xianyu.expressRecognized', { name: matched.expressName }))
        return
      }
    }
  } catch {
    // 识别失败静默处理，由运营手工选择快递公司
  } finally {
    waybillRecognizing.value = false
  }
}

const ensureSelectedDeviceForProductRuleBinding = async () => {
  if (!requiresProductRuleBinding.value) {
    return true
  }
  const deviceNo = shipmentForm.deviceNo.trim()
  if (selectedDevice.value?.deviceNo === deviceNo) {
    return true
  }
  try {
    const device = await resolveRentalDeviceQr(deviceNo)
    selectedDevice.value = device
    shipmentForm.deviceNo = device.deviceNo
    return true
  } catch {
    message.warning(t('rental.xianyu.shipProductRuleDeviceResolveFailed'))
    return false
  }
}

const handleNextStep = async () => {
  if (currentStep.value === 0 && !hasWaybillReady()) {
    return
  }
  if (currentStep.value === 1 && !shipmentForm.deviceNo.trim()) {
    message.warning(t('rental.xianyu.deviceNoRequired'))
    return
  }
  if (currentStep.value === 1 && !(await ensureSelectedDeviceForProductRuleBinding())) {
    return
  }
  currentStep.value += 1
}

const handlePreviousStep = () => {
  currentStep.value -= 1
}

const hasWaybillReady = () => {
  const waybillNo = shipmentForm.waybillNo.trim()
  if (!waybillNo || !/^\w{10,}$/.test(waybillNo)) {
    message.warning(t('rental.xianyu.waybillNoInvalid'))
    return false
  }
  if (!shipmentForm.expressCode.trim() || !shipmentForm.expressName.trim()) {
    message.warning(t('rental.xianyu.expressRequired'))
    return false
  }
  return true
}

const handleShipXianyuOrder = async () => {
  if (!canShipXianyuOrder.value) {
    message.warning(t('error.noPermission'))
    return
  }
  if (requiresProductRuleBinding.value && !canBindProductRule.value) {
    message.warning(t('rental.xianyu.shipProductRuleBindPermissionRequired'))
    return
  }
  const selectedOrder = selectedPendingShipOrder.value
  if (!selectedOrder) {
    message.warning(t('rental.xianyu.pendingShipRequired'))
    return
  }
  if (!hasWaybillReady()) {
    currentStep.value = 0
    return
  }
  let deviceNo = shipmentForm.deviceNo.trim()
  if (!deviceNo) {
    currentStep.value = 1
    message.warning(t('rental.xianyu.deviceNoRequired'))
    return
  }
  if (!(await ensureSelectedDeviceForProductRuleBinding())) {
    currentStep.value = 1
    return
  }
  deviceNo = shipmentForm.deviceNo.trim()
  const waybillNo = shipmentForm.waybillNo.trim()
  const expressCode = shipmentForm.expressCode.trim()
  const expressName = shipmentForm.expressName.trim()
  try {
    await message.confirm(
      requiresProductRuleBinding.value
        ? t('rental.xianyu.shipBindProductRuleConfirmMessage', {
            itemId: selectedOrder.xianyuItemId || '-',
            modelCode: selectedDevice.value?.equipmentModelCode || '-',
            orderNo: selectedOrder.externalOrderId,
            deviceNo,
            expressName,
            waybillNo
          })
        : t('rental.xianyu.shipConfirmMessage', {
            orderNo: selectedOrder.externalOrderId,
            deviceNo,
            expressName,
            waybillNo
          }),
      requiresProductRuleBinding.value
        ? t('rental.xianyu.confirmBindProductRuleAndShip')
        : t('rental.xianyu.confirmShip')
    )
  } catch {
    return
  }
  shipping.value = true
  try {
    const result = await shipXianyuOrder({
      channelOrderId: selectedOrder.id,
      deviceNo,
      idempotencyKey: createIdempotencyKey(),
      expressCode,
      expressName,
      waybillNo,
      source: 'ADMIN',
      ocrConfirmed: Boolean(shipmentOcr.value?.waybillNo),
      bindProductRuleIfMissing: requiresProductRuleBinding.value
    })
    shipmentResult.value = result
    message.success(
      t('rental.xianyu.shipSuccess', {
        shipmentId: result.shipmentId,
        deviceNo: result.deviceNo,
        waybillNo: result.maskedWaybillNo
      })
    )
    emit('shipped', result)
  } finally {
    shipping.value = false
  }
}

const createIdempotencyKey = () => {
  if (globalThis.crypto?.randomUUID) {
    return `admin-xianyu-ship-${globalThis.crypto.randomUUID()}`
  }
  return `admin-xianyu-ship-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

const resetShipmentWorkbench = () => {
  currentStep.value = 0
  shipmentUploadFiles.value = []
  shipmentImageFile.value = undefined
  shipmentOcr.value = undefined
  deviceQrUploadFiles.value = []
  deviceQrImageFile.value = undefined
  selectedDevice.value = undefined
  selectedPendingShipOrder.value = undefined
  shipmentResult.value = undefined
  shipmentForm.keyword = ''
  shipmentForm.waybillNo = ''
  shipmentForm.expressCode = ''
  shipmentForm.expressName = ''
  shipmentForm.deviceNo = ''
  shipmentForm.pageNo = 1
  shipmentForm.pageSize = 5
  autoRecognizedExpressCode.value = ''
  applyInitialOrder()
}

const applyInitialOrder = () => {
  const order = props.initialOrder
  selectedPendingShipOrder.value = order
  shipmentForm.shopId = order.shopId
  shipmentForm.keyword = order.externalOrderId
}

const retryWorkbench = async () => {
  loadError.value = false
  await Promise.all([loadConfig(), loadExpressCompanies()])
}

watch(() => props.initialOrder, applyInitialOrder, { immediate: true })
onMounted(retryWorkbench)
</script>

<style scoped>
.ship-workbench {
  min-height: 100%;
  padding: 0 4px 20px;
}
</style>
