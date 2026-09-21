<template>
  <view class="auth-container">
    <!-- 顶部 -->
    <Header />

    <!-- 表单区域 -->
    <view class="form-container">
      <TenantPicker
        ref="tenantPickerRef"
        :disabled="Boolean(socialBindingContext) || authLoading"
        :preferred-tenant-id="socialBindingContext?.tenantId"
      />
      <view v-if="socialBindingContext" class="mb-24rpx rounded-12rpx bg-[#e8f4ff] px-24rpx py-20rpx text-26rpx text-[#1890ff]">
        三方授权成功，请使用账号密码登录完成绑定
      </view>
      <view class="input-item">
        <wd-icon name="user" size="20px" color="#111111" />
        <wd-input
          v-model="formData.username"
          placeholder="请输入用户名"
          clearable
          clear-trigger="focus"
        />
      </view>
      <view class="input-item">
        <wd-icon name="lock" size="20px" color="#111111" />
        <wd-input
          v-model="formData.password"
          placeholder="请输入密码"
          clearable
          clear-trigger="focus"
          show-password
        />
      </view>
      <view v-if="captchaEnabled">
        <Verify
          ref="verifyRef"
          :captcha-type="captchaType"
          explain="向右滑动完成验证"
          :img-size="{ width: '300px', height: '150px' }"
          mode="pop"
          @success="verifySuccess"
        />
      </view>

      <!-- 登录按钮 -->
      <view class="mb-2 mt-2 flex justify-between">
        <text v-if="!authLoading" class="text-28rpx text-[#6b6b6b]" @click="goToForgetPassword">
          忘记密码？
        </text>
      </view>
      <wd-button block :disabled="authLoading" :loading="loading" type="primary" @click="handleLogin">
        登录
      </wd-button>

      <view class="mt-48rpx border-t border-[#e5e5e5] pt-28rpx text-center text-24rpx text-[#6b6b6b]">
        首次使用请联系管理员开通仓务权限
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import type { SocialLoginBindingContext } from '@/utils/social-login'
import { useToast } from '@wot-ui/ui/components/wd-toast'
import { computed, reactive, ref } from 'vue'
import {
  FORGET_PASSWORD_PAGE,
} from '@/router/config'
import { useTokenStore } from '@/store/token'
import { ensureDecodeURIComponent, redirectAfterLogin } from '@/utils'
import Header from './components/header.vue'
import TenantPicker from './components/tenant-picker.vue'
import Verify from './components/verifition/verify.vue'

defineOptions({
  name: 'LoginPage',
  style: {
    navigationStyle: 'custom',
  },
})

const pageProps = defineProps<{
  redirect?: string
  socialBind?: string
}>()

definePage({
  style: {
    navigationStyle: 'custom',
  },
})

const toast = useToast()
const loading = ref(false) // 表单提交状态
const redirectUrl = ref(pageProps.redirect ? ensureDecodeURIComponent(pageProps.redirect) : undefined) // 重定向地址
const tenantPickerRef = ref<InstanceType<typeof TenantPicker>>() // 租户选择器引用
const captchaEnabled = import.meta.env.VITE_APP_CAPTCHA_ENABLE === 'true' // 验证码开关
const verifyRef = ref()
const captchaType = ref('blockPuzzle') // 滑块验证码 blockPuzzle|clickWord

const formData = reactive({
  username: import.meta.env.VITE_APP_DEFAULT_LOGIN_USERNAME || '',
  password: import.meta.env.VITE_APP_DEFAULT_LOGIN_PASSWORD || '',
  captchaVerification: '', // 验证码校验值
}) // 表单数据
const socialBindingContext = ref<SocialLoginBindingContext>() // 待绑定的三方授权上下文
const socialLoginLoading = ref(false) // 三方登录进行状态
const authLoading = computed(() => loading.value || socialLoginLoading.value) // 任一登录流程进行状态
const socialAuth = computed(() => { // 待绑定的三方授权参数
  const context = socialBindingContext.value
  return context
    ? {
        socialType: context.socialType,
        socialCode: context.socialCode,
        socialState: context.socialState,
      }
    : undefined
})

/** 获取验证码 */
async function getCode() {
  // 情况一，未开启：则直接登录
  if (!captchaEnabled) {
    await verifySuccess({})
  } else {
    // 情况二，已开启：则展示验证码；只有完成验证码的情况，才进行登录
    // 弹出验证码
    verifyRef.value.show()
  }
}

/** 登录处理 */
async function handleLogin() {
  if (authLoading.value) {
    return
  }
  if (!validateTenant()) {
    return
  }
  if (!formData.username) {
    toast.warning('请输入用户名')
    return
  }
  if (!formData.password) {
    toast.warning('请输入密码')
    return
  }
  await getCode()
}

/** 验证成功后登录 */
async function verifySuccess(params: any) {
  loading.value = true
  try {
    // 调用登录接口
    const tokenStore = useTokenStore()
    formData.captchaVerification = params.captchaVerification
    await tokenStore.login({
      type: 'username',
      ...formData,
      ...socialAuth.value,
    })
    // 处理跳转
    redirectAfterLogin(socialBindingContext.value?.redirect || redirectUrl.value)
  } finally {
    loading.value = false
  }
}

/** 跳转到忘记密码 */
function goToForgetPassword() {
  uni.navigateTo({ url: FORGET_PASSWORD_PAGE })
}

/** 校验当前租户 */
function validateTenant() {
  return Boolean(tenantPickerRef.value?.validate())
}
</script>

<style lang="scss" scoped>
@import './styles/auth.scss';
</style>
