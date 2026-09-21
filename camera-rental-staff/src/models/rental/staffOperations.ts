/** 仅做展示和提交前完整性校验，业务许可仍由后端决定。 */
export function staffError(error: unknown, fallback: string) {
  if (error instanceof Error && error.message)
    return error.message
  if (error && typeof error === 'object') {
    for (const key of ['msg', 'message', 'errMsg']) {
      const value = (error as Record<string, unknown>)[key]
      if (typeof value === 'string' && value)
        return value
    }
  }
  return fallback
}

export function deviceStatusLabel(status?: string) {
  const labels: Record<string, string> = {
    AVAILABLE: '在库可租',
    RENTED: '在租',
    MAINTENANCE: '维修中',
    DISABLED: '已停用',
  }
  return status ? labels[status] || status : '状态未获取'
}

export function inspectionStateLabel(status?: string) {
  const labels: Record<string, string> = { PENDING: '待检测', FAILED: '检测不通过', PASSED: '检测通过', NOT_RECORDED: '无检测记录' }
  return status ? labels[status] || status : '检测状态未获取'
}

export function inspectionBlocker(checks: Array<{ value: string }>, passed: boolean) {
  if (!checks.length || checks.some(item => !item.value || item.value === '待确认'))
    return '请逐项完成检测后再提交'
  const allPassed = checks.every(item => ['正常', '齐全'].includes(item.value))
  if (passed && !allPassed)
    return '检测存在问题，不能选择通过'
  if (!passed && allPassed)
    return '所有检测项均正常，请先记录异常项再转维修'
  return ''
}

export function operationKey(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`
}

export function sanitizeStaffMessage(value: string) {
  return value.replace(/CRD1\|[^\s，；]+/g, '[设备二维码]')
    .replace(/\b1\d{10}\b/g, text => `${text.slice(0, 3)}****${text.slice(-4)}`)
    .replace(/\b\d{16,}\b/g, text => `${text.slice(0, 6)}***${text.slice(-4)}`)
}
