<template>
  <ContentWrap>
    <el-collapse v-model="activePanels">
      <el-collapse-item name="ship">
        <template #title>
          <div class="flex flex-wrap items-center gap-8px">
            <span>{{ t('rental.xianyu.shipWorkbenchTitle') }}</span>
            <el-tag :type="config?.writeEnabled ? 'warning' : 'info'">
              {{ t('rental.xianyu.writeSwitch') }}:
              {{ config?.writeEnabled ? t('common.yes') : t('common.no') }}
            </el-tag>
          </div>
        </template>

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
          :title="t('rental.xianyu.shipWorkbenchHint')"
        />

        <el-steps class="mb-16px" :active="currentStep" finish-status="success" simple>
          <el-step :title="t('rental.xianyu.shipStepWaybill')" />
          <el-step :title="t('rental.xianyu.shipStepDevice')" />
          <el-step :title="t('rental.xianyu.shipStepOrder')" />
          <el-step :title="t('rental.xianyu.shipStepSubmit')" />
        </el-steps>

        <el-card shadow="never">
          <template #header>
            <div class="flex flex-wrap items-center justify-between gap-8px">
              <span>{{ stepTitle }}</span>
              <span class="text-13px text-[var(--el-text-color-secondary)]">
                {{ currentStep + 1 }} / 4
              </span>
            </div>
          </template>

          <div v-if="currentStep === 0">
            <el-upload
              v-model:file-list="shipmentUploadFiles"
              accept="image/*"
              :auto-upload="false"
              :limit="1"
              :on-change="handleShipmentImageChange"
              :on-exceed="handleImageExceed"
              :on-remove="handleShipmentImageRemove"
              drag
            >
              <Icon
                icon="ep:upload-filled"
                class="mb-8px text-32px text-[var(--el-color-primary)]"
              />
              <div class="el-upload__text">{{ t('rental.xianyu.shipUploadHint') }}</div>
              <template #tip>
                <div class="el-upload__tip">{{ t('rental.xianyu.shipUploadTip') }}</div>
              </template>
            </el-upload>
            <div class="mt-12px flex flex-wrap gap-8px">
              <el-button
                v-hasPermi="['rental:xianyu:ship:ocr']"
                type="primary"
                :disabled="!shipmentImageFile"
                :loading="shipmentOcrLoading"
                @click="handleShipmentOcr"
              >
                {{ t('rental.xianyu.shipOcr') }}
              </el-button>
              <el-tag v-if="shipmentOcr?.extractionSource" type="info">
                {{ shipmentOcr.extractionSource }}
              </el-tag>
              <el-tag v-if="shipmentOcr?.confidence" type="success">
                {{ t('rental.xianyu.shipOcrConfidence', { value: shipmentOcr.confidence }) }}
              </el-tag>
            </div>
            <el-form class="mt-16px" label-position="top" :model="shipmentForm">
              <el-form-item :label="t('rental.xianyu.waybillNo')">
                <el-input
                  v-model.trim="shipmentForm.waybillNo"
                  clearable
                  :placeholder="t('rental.xianyu.waybillNoPlaceholder')"
                />
              </el-form-item>
              <el-form-item :label="t('rental.xianyu.expressName')">
                <el-select
                  v-model="shipmentForm.expressCode"
                  class="!w-1/1"
                  filterable
                  clearable
                  :placeholder="t('rental.xianyu.expressPlaceholder')"
                  @change="handleShipmentExpressChange"
                >
                  <el-option
                    v-for="express in expressList"
                    :key="express.code"
                    :label="`${express.expressName} (${express.code})`"
                    :value="express.code"
                  />
                </el-select>
              </el-form-item>
              <el-form-item :label="t('rental.xianyu.expressNameManual')">
                <el-input
                  v-model.trim="shipmentForm.expressName"
                  clearable
                  :placeholder="t('rental.xianyu.expressNameManualPlaceholder')"
                />
              </el-form-item>
            </el-form>
          </div>

          <div v-else-if="currentStep === 1">
            <el-alert
              class="mb-12px"
              type="info"
              :closable="false"
              :title="t('rental.xianyu.deviceQrHint')"
            />
            <el-upload
              v-model:file-list="deviceQrUploadFiles"
              accept="image/*"
              :auto-upload="false"
              :limit="1"
              :on-change="handleDeviceQrImageChange"
              :on-exceed="handleImageExceed"
              :on-remove="handleDeviceQrImageRemove"
              drag
            >
              <Icon
                icon="ep:upload-filled"
                class="mb-8px text-32px text-[var(--el-color-primary)]"
              />
              <div class="el-upload__text">{{ t('rental.xianyu.deviceQrUploadHint') }}</div>
              <template #tip>
                <div class="el-upload__tip">{{ t('rental.xianyu.deviceQrUploadTip') }}</div>
              </template>
            </el-upload>
            <div class="mt-12px flex flex-wrap gap-8px">
              <el-button
                type="primary"
                :disabled="!deviceQrImageFile"
                :loading="deviceQrLoading"
                @click="handleDeviceQrImageDecode"
              >
                {{ t('rental.xianyu.deviceQrDecode') }}
              </el-button>
              <el-button
                :disabled="!deviceQrPayload.trim()"
                :loading="deviceQrResolving"
                @click="handleResolveDeviceQrPayload"
              >
                {{ t('rental.xianyu.deviceQrResolve') }}
              </el-button>
            </div>
            <el-form class="mt-16px" label-position="top" :model="shipmentForm">
              <el-form-item :label="t('rental.xianyu.deviceQrPayloadLabel')">
                <el-input
                  v-model.trim="deviceQrPayload"
                  clearable
                  :placeholder="t('rental.xianyu.deviceQrPayloadPlaceholder')"
                  @keyup.enter="handleResolveDeviceQrPayload"
                />
              </el-form-item>
              <el-form-item :label="t('rental.device.deviceNo')">
                <el-input
                  v-model.trim="shipmentForm.deviceNo"
                  clearable
                  autofocus
                  :placeholder="t('rental.xianyu.deviceScanPlaceholder')"
                />
              </el-form-item>
            </el-form>
            <el-descriptions v-if="selectedDevice" class="mt-12px" :column="3" border>
              <el-descriptions-item :label="t('rental.device.deviceNo')">
                {{ selectedDevice.deviceNo }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('rental.device.serialNumber')">
                {{ selectedDevice.serialNumber || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('common.status')">
                {{ selectedDevice.status }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div v-else-if="currentStep === 2">
            <el-form label-position="top" :model="shipmentForm">
              <el-row :gutter="16">
                <el-col :xs="24" :md="10">
                  <el-form-item :label="t('rental.order.filterShop')">
                    <el-select
                      v-model="shipmentForm.shopId"
                      class="!w-1/1"
                      filterable
                      clearable
                      :placeholder="t('rental.order.shopPlaceholder')"
                    >
                      <el-option
                        v-for="shop in shops"
                        :key="shop.id"
                        :label="shop.shopName || String(shop.id)"
                        :value="shop.id"
                      />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="10">
                  <el-form-item :label="t('rental.xianyu.pendingKeyword')">
                    <el-input
                      v-model.trim="shipmentForm.keyword"
                      clearable
                      :placeholder="t('rental.xianyu.pendingKeywordPlaceholder')"
                      @keyup.enter="handleSearchPendingShipOrders(true)"
                    />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="4">
                  <el-form-item label=" ">
                    <el-button
                      v-hasPermi="['rental:xianyu:ship']"
                      class="!w-1/1"
                      type="primary"
                      :loading="pendingShipLoading"
                      @click="handleSearchPendingShipOrders(true)"
                    >
                      {{ t('rental.xianyu.searchPendingShip') }}
                    </el-button>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
            <el-table
              v-loading="pendingShipLoading"
              :data="pendingShipList"
              highlight-current-row
              @current-change="handleSelectPendingShipOrder"
            >
              <el-table-column width="52">
                <template #default="{ row }">
                  <el-radio
                    :model-value="selectedPendingShipOrder?.id"
                    :value="row.id"
                    @change="handleSelectPendingShipOrder(row)"
                  />
                </template>
              </el-table-column>
              <el-table-column :label="t('rental.order.externalOrderId')" min-width="180">
                <template #default="{ row }">
                  {{ maskOrderId(row.externalOrderId) }}
                </template>
              </el-table-column>
              <el-table-column
                prop="goodsTitle"
                :label="t('rental.order.goodsTitle')"
                min-width="220"
              />
              <el-table-column :label="t('rental.xianyu.buyerNick')" min-width="120">
                <template #default="{ row }">
                  {{ maskChannelIdentifier(row.buyerNick) }}
                </template>
              </el-table-column>
              <el-table-column :label="t('rental.order.payAmountFen')" width="120">
                <template #default="{ row }">
                  {{ formatFen(row.payAmount) }}
                </template>
              </el-table-column>
              <el-table-column
                prop="conversionStatus"
                :label="t('rental.order.conversionStatus')"
                width="140"
              />
              <el-table-column :label="t('rental.xianyu.sourceUpdatedAt')" width="180">
                <template #default="{ row }">
                  {{ formatNullableDate(row.sourceUpdatedAt) }}
                </template>
              </el-table-column>
              <template #empty>
                <div class="py-24px text-[var(--el-text-color-secondary)]">
                  {{ t('rental.xianyu.pendingShipEmptyHint') }}
                </div>
              </template>
            </el-table>
            <Pagination
              :total="pendingShipTotal"
              v-model:page="shipmentForm.pageNo"
              v-model:limit="shipmentForm.pageSize"
              @pagination="handleSearchPendingShipOrders(false)"
            />
          </div>

          <div v-else>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="t('rental.xianyu.shipConfirmOrder')">
                {{ maskOrderId(selectedPendingShipOrder?.externalOrderId) || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('rental.order.goodsTitle')">
                {{ selectedPendingShipOrder?.goodsTitle || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('rental.xianyu.shipConfirmDevice')">
                {{ shipmentForm.deviceNo || '-' }}
                <span v-if="selectedDevice?.serialNumber">
                  / {{ selectedDevice.serialNumber }}
                </span>
              </el-descriptions-item>
              <el-descriptions-item :label="t('rental.xianyu.shipConfirmWaybill')">
                {{ shipmentForm.expressName || '-' }} {{ shipmentForm.waybillNo || '-' }}
              </el-descriptions-item>
            </el-descriptions>
            <el-alert
              class="mt-12px"
              type="warning"
              :closable="false"
              :title="t('rental.xianyu.shipSubmitBackendHint')"
            />
            <el-alert
              v-if="shipmentResult"
              class="mt-12px"
              type="success"
              :closable="false"
              :title="
                t('rental.xianyu.shipSuccess', {
                  shipmentId: shipmentResult.shipmentId,
                  deviceNo: shipmentResult.deviceNo,
                  waybillNo: shipmentResult.maskedWaybillNo
                })
              "
            />
          </div>

          <div class="mt-16px flex flex-wrap justify-between gap-8px">
            <div class="flex flex-wrap gap-8px">
              <el-button @click="resetShipmentWorkbench">
                {{ t('common.reset') }}
              </el-button>
              <el-button :disabled="currentStep === 0" @click="currentStep -= 1">
                {{ t('common.prevLabel') }}
              </el-button>
            </div>
            <div class="flex flex-wrap gap-8px">
              <el-button v-if="currentStep < 3" type="primary" @click="handleNextStep">
                {{ t('common.nextLabel') }}
              </el-button>
              <el-button
                v-else
                type="danger"
                :loading="shipping"
                v-hasPermi="['rental:xianyu:ship']"
                @click="handleShipXianyuOrder"
              >
                {{ t('rental.xianyu.confirmShip') }}
              </el-button>
            </div>
          </div>
        </el-card>
      </el-collapse-item>
    </el-collapse>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
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
  getXianyuPendingShipOrderPage,
  getXianyuShopPage,
  recognizeXianyuShipmentImage,
  shipXianyuOrder,
  type XianyuConfigVO,
  type XianyuExpressCompanyVO,
  type XianyuOrderShipRespVO,
  type XianyuPendingShipOrderVO,
  type XianyuShipmentOcrRespVO,
  type XianyuShopVO
} from '@/api/rental/xianyu'
import { resolveRentalDeviceQr, type RentalDeviceVO } from '@/api/rental/device'
import { formatNullableDate } from '@/utils/formatTime'
import { maskChannelIdentifier } from '@/utils/rentalPrivacy'

defineOptions({ name: 'XianyuShipWorkbench' })

type BarcodeDetectorLike = {
  detect: (image: ImageBitmapSource) => Promise<Array<{ rawValue?: string }>>
}

type BarcodeDetectorConstructor = new (options?: { formats?: string[] }) => BarcodeDetectorLike

const { t } = useI18n()
const message = useMessage()

const activePanels = ref<string[]>(['ship'])
const currentStep = ref(0)
const config = ref<XianyuConfigVO>()
const loadError = ref(false)
const shops = ref<XianyuShopVO[]>([])
const expressList = ref<XianyuExpressCompanyVO[]>([])
const shipmentUploadFiles = ref<UploadUserFile[]>([])
const shipmentImageFile = ref<UploadRawFile>()
const shipmentOcr = ref<XianyuShipmentOcrRespVO>()
const shipmentOcrLoading = ref(false)
const deviceQrUploadFiles = ref<UploadUserFile[]>([])
const deviceQrImageFile = ref<UploadRawFile>()
const deviceQrPayload = ref('')
const deviceQrLoading = ref(false)
const deviceQrResolving = ref(false)
const selectedDevice = ref<RentalDeviceVO>()
const pendingShipLoading = ref(false)
const shipping = ref(false)
const pendingShipList = ref<XianyuPendingShipOrderVO[]>([])
const pendingShipTotal = ref(0)
const selectedPendingShipOrder = ref<XianyuPendingShipOrderVO>()
const shipmentResult = ref<XianyuOrderShipRespVO>()
const canOcrShipment = computed(() => hasPermission(['rental:xianyu:ship:ocr']))
const canShipXianyuOrder = computed(() => hasPermission(['rental:xianyu:ship']))

const shipmentForm = reactive<{
  shopId?: number
  keyword: string
  waybillNo: string
  expressCode: string
  expressName: string
  deviceNo: string
  pageNo: number
  pageSize: number
}>({
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
    t('rental.xianyu.shipOrderTitle'),
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

const loadShops = async () => {
  try {
    const data = await getXianyuShopPage({ pageNo: 1, pageSize: 100 })
    shops.value = data.list || []
    if (!shipmentForm.shopId && shops.value.length > 0) {
      const preferred =
        shops.value.find((shop) => shop.authorizationStatus === 'VALID') || shops.value[0]
      shipmentForm.shopId = preferred.id
    }
  } catch {
    shops.value = []
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
    deviceQrPayload.value = payload
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

const handleResolveDeviceQrPayload = async () => {
  const payload = deviceQrPayload.value.trim()
  if (!payload) {
    message.warning(t('rental.xianyu.deviceQrPayloadRequired'))
    return
  }
  deviceQrResolving.value = true
  try {
    await resolveDeviceQrPayload(payload)
  } finally {
    deviceQrResolving.value = false
  }
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

const maskOrderId = (orderId?: string) => {
  const value = orderId?.trim()
  if (!value) return ''
  return value.length <= 10 ? '***' : `${value.slice(0, 6)}***${value.slice(-4)}`
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
}

const handleSearchPendingShipOrders = async (resetPage: boolean) => {
  if (!canShipXianyuOrder.value) {
    message.warning(t('error.noPermission'))
    return
  }
  if (resetPage) {
    shipmentForm.pageNo = 1
  }
  pendingShipLoading.value = true
  try {
    const data = await getXianyuPendingShipOrderPage({
      pageNo: shipmentForm.pageNo,
      pageSize: shipmentForm.pageSize,
      shopId: shipmentForm.shopId,
      keyword: shipmentForm.keyword
    })
    pendingShipList.value = data.list || []
    pendingShipTotal.value = data.total || 0
    selectedPendingShipOrder.value = undefined
  } catch {
    pendingShipList.value = []
    pendingShipTotal.value = 0
    loadError.value = true
  } finally {
    pendingShipLoading.value = false
  }
}

const handleSelectPendingShipOrder = (row?: XianyuPendingShipOrderVO) => {
  selectedPendingShipOrder.value = row
  shipmentResult.value = undefined
}

const handleNextStep = () => {
  if (currentStep.value === 0 && !hasWaybillReady()) {
    return
  }
  if (currentStep.value === 1 && !shipmentForm.deviceNo.trim()) {
    message.warning(t('rental.xianyu.deviceNoRequired'))
    return
  }
  if (currentStep.value === 2 && !selectedPendingShipOrder.value) {
    message.warning(t('rental.xianyu.pendingShipRequired'))
    return
  }
  currentStep.value += 1
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
  const selectedOrder = selectedPendingShipOrder.value
  if (!selectedOrder) {
    message.warning(t('rental.xianyu.pendingShipRequired'))
    return
  }
  if (!hasWaybillReady()) {
    currentStep.value = 0
    return
  }
  const deviceNo = shipmentForm.deviceNo.trim()
  if (!deviceNo) {
    currentStep.value = 1
    message.warning(t('rental.xianyu.deviceNoRequired'))
    return
  }
  const waybillNo = shipmentForm.waybillNo.trim()
  const expressCode = shipmentForm.expressCode.trim()
  const expressName = shipmentForm.expressName.trim()
  try {
    await message.confirm(
      t('rental.xianyu.shipConfirmMessage', {
        orderNo: maskOrderId(selectedOrder.externalOrderId),
        deviceNo,
        expressName,
        waybillNo
      }),
      t('rental.xianyu.confirmShip')
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
      ocrConfirmed: Boolean(shipmentOcr.value?.waybillNo)
    })
    shipmentResult.value = result
    message.success(
      t('rental.xianyu.shipSuccess', {
        shipmentId: result.shipmentId,
        deviceNo: result.deviceNo,
        waybillNo: result.maskedWaybillNo
      })
    )
    await handleSearchPendingShipOrders(false)
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
  deviceQrPayload.value = ''
  selectedDevice.value = undefined
  selectedPendingShipOrder.value = undefined
  shipmentResult.value = undefined
  pendingShipList.value = []
  pendingShipTotal.value = 0
  shipmentForm.keyword = ''
  shipmentForm.waybillNo = ''
  shipmentForm.expressCode = ''
  shipmentForm.expressName = ''
  shipmentForm.deviceNo = ''
  shipmentForm.pageNo = 1
  shipmentForm.pageSize = 5
}

const formatFen = (value?: number | null) => {
  if (value === undefined || value === null) {
    return '-'
  }
  return `¥${(value / 100).toFixed(2)}`
}

const retryWorkbench = async () => {
  loadError.value = false
  await Promise.all([loadConfig(), loadShops(), loadExpressCompanies()])
}

onMounted(retryWorkbench)
</script>
