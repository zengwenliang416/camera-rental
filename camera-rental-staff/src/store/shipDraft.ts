import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { operationKey } from '@/models/rental/staffOperations'
import { useUserStore } from '@/store/user'

export interface ShipDraft {
  channelOrderId: number
  rentalOrderId?: number
  receiverName?: string
  receiverMobile?: string
  receiverAddress?: string
  orderNo: string
  goodsTitle?: string
  deviceNo: string
  deviceStatus?: string
  expressCode: string
  expressName: string
  waybillNo: string
  ocrConfirmed: boolean
  idempotencyKey: string
}

export const useShipDraftStore = defineStore('staff-ship-draft', () => {
  const draft = ref<ShipDraft | null>(null)
  const user = useUserStore()
  watch(() => [user.tenantId, user.visitTenantId, user.userInfo.userId], clear, { flush: 'sync' })

  function setDraft(value: Omit<ShipDraft, 'idempotencyKey'>) {
    const old = draft.value
    const unchanged = old && Object.entries(value).every(([key, val]) => old[key as keyof ShipDraft] === val)
    draft.value = { ...value, idempotencyKey: unchanged ? old.idempotencyKey : operationKey('staff-ship') }
  }

  function clear() {
    draft.value = null
  }

  return { draft, setDraft, clear }
})
