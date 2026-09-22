/** 仅响应员工主动点击；不自动复制、不记录客户信息。 */
export function copyStaffField(value?: string) {
  if (!value?.trim())
    return
  uni.setClipboardData({ data: value.trim() })
}
export function callReceiver(value?: string) {
  const phone = (value || '').replace(/[\s()-]/g, '')
  if (!/^\+?\d{7,15}$/.test(phone)) {
    uni.showToast({ title: '电话号码不完整，请核对订单', icon: 'none' })
    return
  }
  uni.makePhoneCall({ phoneNumber: phone, fail: () => uni.showToast({ title: '未拨出电话，可复制号码后拨号', icon: 'none' }) })
}
