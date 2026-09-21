import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'
import { sanitizeStaffMessage } from '@/models/rental/staffOperations'
import { useUserStore } from '@/store/user'

export type StaffExceptionKind = 'scan' | 'order' | 'network'
export type StaffExceptionStatus = 'open' | 'resolved'

export interface StaffException {
  id: string
  kind: StaffExceptionKind
  title: string
  detail: string
  source: string
  status: StaffExceptionStatus
  createdAt: number
  resolvedAt?: number
}

export const useStaffExceptionStore = defineStore('staff-exception', () => {
  const items = ref<StaffException[]>([])
  const user = useUserStore()
  // 旧版本跨账号持久化了本机错误；此类信息只保留在当前会话。
  uni.removeStorageSync('staff-exception')
  watch(
    () => [user.tenantId, user.visitTenantId, user.userInfo.userId],
    () => {
      items.value = []
    },
    { flush: 'sync' },
  )

  const openItems = computed(() => items.value.filter(item => item.status === 'open'))
  const resolvedToday = computed(() => {
    const today = new Date().toLocaleDateString('en-CA', { timeZone: 'Asia/Shanghai' })
    return items.value.filter(item => item.status === 'resolved' && item.resolvedAt
      && new Date(item.resolvedAt).toLocaleDateString('en-CA', { timeZone: 'Asia/Shanghai' }) === today)
  })

  function record(input: Omit<StaffException, 'id' | 'status' | 'createdAt'> & { status?: StaffExceptionStatus }) {
    items.value = [{
      id: `ex-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      status: input.status || 'open',
      createdAt: Date.now(),
      kind: input.kind,
      title: input.title,
      detail: sanitizeStaffMessage(input.detail),
      source: input.source,
    }, ...items.value].slice(0, 50)
  }

  function resolve(id: string) {
    items.value = items.value.map(item => item.id === id ? { ...item, status: 'resolved', resolvedAt: Date.now() } : item)
  }

  return { items, openItems, resolvedToday, record, resolve }
})
