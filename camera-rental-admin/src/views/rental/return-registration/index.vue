<template>
  <ContentWrap>
    <div class="registration-heading">
      <div>
        <p>RETURN REGISTRATION</p>
        <h2>客户退回登记</h2>
        <span>创建微信分享链接，核对客户提交的物流、设备序列号和归还照片。</span>
      </div>
      <el-button
        v-hasPermi="['rental:return-registration:create']"
        type="primary"
        @click="createVisible = true"
      >
        <Icon icon="ep:link" class="mr-5px" />创建退回链接
      </el-button>
    </div>

    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="关键词">
        <el-input
          v-model="query.keyword"
          clearable
          class="!w-240px"
          placeholder="登记编号 / 订单号 / 运单号"
          @keyup.enter="load"
        />
      </el-form-item>
      <el-form-item label="设备序列号">
        <el-input
          v-model="query.serial"
          clearable
          class="!w-210px"
          placeholder="例如 A6-08-4L5H"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable class="!w-180px" placeholder="全部状态">
          <el-option v-for="status in statuses" :key="status" :label="statusLabel(status)" :value="status" />
        </el-select>
      </el-form-item>
      <el-form-item label="内部订单 ID">
        <el-input-number v-model="query.rentalOrderId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="提交时间">
        <el-date-picker
          v-model="submittedRange"
          type="datetimerange"
          value-format="YYYY-MM-DDTHH:mm:ss"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          class="!w-380px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="rows">
      <el-table-column prop="formNo" label="登记编号" min-width="165" />
      <el-table-column prop="orderNo" label="订单号" min-width="175" show-overflow-tooltip />
      <el-table-column label="状态" width="125">
        <template #default="{ row }">
          <el-tag :type="tagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="寄回物流" min-width="190">
        <template #default="{ row }">
          <div>{{ row.carrierName || '—' }}</div>
          <small class="muted">{{ row.waybillNo || '尚未提交' }}</small>
        </template>
      </el-table-column>
      <el-table-column prop="expiresAt" label="链接有效期" width="180" />
      <el-table-column prop="submittedAt" label="提交时间" width="180">
        <template #default="{ row }">{{ row.submittedAt || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" width="250">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
          <el-button
            v-if="canReissueReturnRegistration(row.status)"
            v-hasPermi="['rental:return-registration:create']"
            link
            type="primary"
            @click="reissue(row)"
          >重新签发</el-button>
          <el-button
            v-if="canRevokeReturnRegistration(row.status)"
            v-hasPermi="['rental:return-registration:revoke']"
            link
            type="danger"
            @click="revoke(row)"
          >撤销</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="query.pageNo"
      v-model:limit="query.pageSize"
      @pagination="load"
    />

    <el-dialog v-model="createVisible" title="创建客户退回链接" width="520px">
      <el-alert type="info" :closable="false" title="只能为已经分配具体设备的内部租赁订单创建链接。" class="mb-18px" />
      <el-form label-width="120px">
        <el-form-item label="内部订单 ID" required>
          <el-input-number v-model="createForm.rentalOrderId" :min="1" class="!w-100%" controls-position="right" />
        </el-form-item>
        <el-form-item label="有效天数">
          <el-input-number v-model="createForm.validDays" :min="1" :max="30" class="!w-100%" controls-position="right" />
        </el-form-item>
      </el-form>
      <div v-if="createdUrl" class="created-link">
        <small>链接只在本次创建后显示，请立即发送给客户</small>
        <code>{{ createdUrl }}</code>
        <el-button type="primary" plain @click="copy(createdUrl)">复制链接</el-button>
      </div>
      <template #footer>
        <el-button @click="createVisible = false">关闭</el-button>
        <el-button type="primary" :loading="creating" :disabled="!createForm.rentalOrderId" @click="createLink">
          创建链接
        </el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="退回登记详情" size="min(720px, 94vw)">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="登记编号">{{ detail.formNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
            <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="运单号">{{ detail.waybillNo || '—' }}</el-descriptions-item>
            <el-descriptions-item label="寄出日期">{{ detail.shippedDate || '—' }}</el-descriptions-item>
            <el-descriptions-item label="Return Delivery">{{ detail.deliveryId || '尚未创建' }}</el-descriptions-item>
            <el-descriptions-item label="异常说明" :span="2">{{ detail.issueDescription || '无' }}</el-descriptions-item>
          </el-descriptions>
          <h3>订单客户信息</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="收货人">{{ detail.customer?.name || '—' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ detail.customer?.mobile || '—' }}</el-descriptions-item>
            <el-descriptions-item label="收货地址" :span="2">{{ detail.customer?.address || '—' }}</el-descriptions-item>
          </el-descriptions>
          <h3>设备匹配</h3>
          <el-table :data="detail.devices" size="small">
            <el-table-column prop="submittedSerial" label="客户填写" />
            <el-table-column prop="normalizedSerial" label="规范序列号" />
            <el-table-column label="匹配结果">
              <template #default="{ row }">
                <el-tag :type="row.matchStatus === 'MATCHED' ? 'success' : 'warning'">{{ row.matchStatus }}</el-tag>
                <div class="muted">{{ row.matchMessage }}</div>
              </template>
            </el-table-column>
          </el-table>
          <h3>归还照片</h3>
          <div class="attachment-grid">
            <a v-for="file in detail.attachments" :key="file.attachmentId" :href="file.previewUrl" target="_blank">
              <el-image :src="file.previewUrl" fit="cover" />
              <span>{{ categoryLabel(file.category) }}</span>
            </a>
          </div>
          <div v-if="canReviewReturnRegistration(detail.status)" class="review-box">
            <el-input v-model="reviewNote" type="textarea" :rows="3" maxlength="1000" placeholder="填写审核说明" />
            <div v-hasPermi="['rental:return-registration:review']">
              <el-button type="danger" plain @click="review(false)">驳回</el-button>
              <el-button type="primary" @click="review(true)">重新校验并接受</el-button>
            </div>
          </div>
        </template>
      </div>
    </el-drawer>
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  createReturnRegistration,
  getReturnRegistration,
  getReturnRegistrationPage,
  reissueReturnRegistration,
  reviewReturnRegistration,
  revokeReturnRegistration,
  type ReturnRegistrationDetail,
  type ReturnRegistrationPageParams,
  type ReturnRegistrationRow
} from '@/api/rental/returnRegistration'
import { useMessage } from '@/hooks/web/useMessage'
import {
  buildReturnRegistrationPageParams,
  canReissueReturnRegistration,
  canReviewReturnRegistration,
  canRevokeReturnRegistration,
  RETURN_REGISTRATION_STATUSES,
  returnRegistrationStatusLabel
} from './returnRegistrationModel'

defineOptions({ name: 'RentalReturnRegistration' })
const message = useMessage()
const statuses = RETURN_REGISTRATION_STATUSES
const loading = ref(false)
const rows = ref<ReturnRegistrationRow[]>([])
const total = ref(0)
const query = reactive<ReturnRegistrationPageParams>({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  serial: '',
  status: '',
  rentalOrderId: undefined
})
const submittedRange = ref<[string, string]>()
const createVisible = ref(false)
const creating = ref(false)
const createdUrl = ref('')
const createForm = reactive({ rentalOrderId: undefined as number | undefined, validDays: 7 })
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<ReturnRegistrationDetail>()
const reviewNote = ref('')

const statusLabel = returnRegistrationStatusLabel
const tagType = (value: string) => value === 'ACCEPTED' ? 'success' : value === 'REVIEW_REQUIRED' ? 'warning' : value === 'REJECTED' || value === 'REVOKED' ? 'danger' : 'info'
const categoryLabel = (value: string) => ({ DEVICE_EXTERIOR: '设备外观', SERIAL_LABEL: '序列号铭牌', PACKAGING: '打包状态', DAMAGE_DETAIL: '异常细节' }[value] || value)

async function load() {
  loading.value = true
  try {
    const data = await getReturnRegistrationPage(
      buildReturnRegistrationPageParams(query, submittedRange.value)
    )
    rows.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
function handleQuery() { query.pageNo = 1; load() }
function reset() {
  Object.assign(query, {
    pageNo: 1,
    keyword: '',
    serial: '',
    status: '',
    rentalOrderId: undefined
  })
  submittedRange.value = undefined
  load()
}
async function createLink() {
  if (!createForm.rentalOrderId) return
  creating.value = true
  try {
    const result = await createReturnRegistration({ rentalOrderId: createForm.rentalOrderId, validDays: createForm.validDays })
    createdUrl.value = `${window.location.origin}${result.sharePath}`
    await load()
  } finally {
    creating.value = false
  }
}
async function copy(value: string) { await navigator.clipboard.writeText(value); message.success('链接已复制') }
async function openDetail(id: number) {
  detailVisible.value = true
  detailLoading.value = true
  try { detail.value = await getReturnRegistration(id); reviewNote.value = detail.value.reviewNote || '' }
  finally { detailLoading.value = false }
}
async function revoke(row: ReturnRegistrationRow) {
  await message.confirm(`确认撤销 ${row.formNo}？撤销后客户无法继续填写。`)
  await revokeReturnRegistration(row.id)
  message.success('已撤销')
  await load()
}
async function reissue(row: ReturnRegistrationRow) {
  await message.confirm(`确认重新签发 ${row.formNo}？旧链接会立即失效。`)
  const result = await reissueReturnRegistration(row.id, 7)
  const url = `${window.location.origin}${result.sharePath}`
  await copy(url)
  message.success('新链接已签发，旧链接已失效')
  await load()
}
async function review(accept: boolean) {
  if (!detail.value) return
  await reviewReturnRegistration(detail.value.id, { accept, note: reviewNote.value })
  message.success(accept ? '已接受登记' : '已驳回登记')
  await openDetail(detail.value.id)
  await load()
}
onMounted(load)
</script>

<style scoped>
.registration-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 24px; padding: 22px; border-radius: 18px; background: linear-gradient(135deg, #edf5ef, #f5efe5); }
.registration-heading p { margin: 0; color: #33775b; font-size: 11px; font-weight: 800; letter-spacing: .16em; }
.registration-heading h2 { margin: 6px 0; font-size: 27px; }
.registration-heading span, .muted { color: var(--el-text-color-secondary); font-size: 12px; }
.created-link { display: grid; gap: 12px; padding: 16px; border-radius: 12px; background: var(--el-fill-color-light); }
.created-link code { overflow-wrap: anywhere; line-height: 1.6; }
.detail-body h3 { margin: 28px 0 12px; }
.attachment-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.attachment-grid a { display: grid; gap: 7px; color: inherit; text-decoration: none; }
.attachment-grid :deep(.el-image) { aspect-ratio: 1; border-radius: 10px; background: var(--el-fill-color); }
.review-box { display: grid; gap: 14px; margin-top: 24px; padding: 16px; border: 1px solid var(--el-border-color); border-radius: 14px; }
.review-box > div { display: flex; justify-content: flex-end; gap: 10px; }
@media (max-width: 720px) { .registration-heading { align-items: flex-start; flex-direction: column; } .attachment-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
