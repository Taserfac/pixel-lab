<template>
  <div class="login-page">
    <section class="brand-panel">
      <div class="brand-shell">
        <div class="brand-top">
          <img class="brand-mark" :src="pixelLabLogo" alt="Pixel Lab logo">
          <div>
            <p class="eyebrow">Pixel Lab</p>
            <h1>进入你的像素创作空间</h1>
          </div>
        </div>

        <p class="brand-copy">
          管理作品、上传图片、探索社区灵感，并把每一次创作沉淀成自己的作品集。
        </p>

        <div class="preview-stage">
          <img
            class="hero-illustration"
            :src="heroIllustration"
            alt="Pixel Lab 创作工作台插画"
          >
        </div>

        <div class="feature-grid">
          <div v-for="item in features" :key="item.title" class="feature-item">
            <el-icon><component :is="item.icon" /></el-icon>
            <div>
              <strong>{{ item.title }}</strong>
              <span>{{ item.desc }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="form-panel">
      <div class="login-card">
        <div class="card-header">
          <div>
            <p class="eyebrow">欢迎回来</p>
            <h2>登录 Pixel Lab</h2>
          </div>
          <el-button
            class="ghost-icon"
            circle
            :icon="isDarkTheme ? Sunny : Moon"
            :title="themeToggleLabel"
            @click="themeStore.toggleTheme"
          />
        </div>

        <div v-if="lastUsername" class="account-hint">
          <el-icon><UserFilled /></el-icon>
          <span>上次登录：{{ lastUsername }}</span>
          <button type="button" @click="useLastUsername">使用</button>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model.trim="form.username"
              placeholder="用户名"
              size="large"
              autocomplete="username"
              clearable
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              size="large"
              autocomplete="current-password"
              show-password
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="rememberMe">记住账号</el-checkbox>
            <el-link type="primary" :underline="false" @click="showPasswordTip">
              忘记密码？
            </el-link>
          </div>

          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form>

        <div class="quick-row single">
          <button type="button" @click="goRegister">
            <el-icon><Plus /></el-icon>
            创建账号
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, markRaw, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Lock,
  Moon,
  Picture,
  Plus,
  Sunny,
  TrendCharts,
  User,
  UserFilled,
  Upload
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useThemeStore } from '@/store/theme'
import * as storage from '@/utils/storage'
import { login } from '@/api/auth'
import heroIllustration from '@/assets/images/login-pixel-lab-hero.png'
import pixelLabLogo from '@/assets/images/pixel-lab-logo.svg'

const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()

const formRef = ref(null)
const loading = ref(false)
const rememberMe = ref(false)
const lastUsername = ref('')

const form = ref({
  username: '',
  password: ''
})

const features = [
  { icon: markRaw(Upload), title: '快速上传', desc: '导入图片并生成作品' },
  { icon: markRaw(Picture), title: '作品管理', desc: '整理公开与私有作品' },
  { icon: markRaw(TrendCharts), title: '创作数据', desc: '查看浏览、点赞与收藏' }
]

const isDarkTheme = computed(() => themeStore.theme === 'dark')
const themeToggleLabel = computed(() => isDarkTheme.value ? '切换到日间模式' : '切换到夜间模式')

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6 到 20 个字符', trigger: 'blur' }
  ]
}

onMounted(() => {
  lastUsername.value = storage.getItem('remember_username', '')
  if (lastUsername.value) {
    form.value.username = lastUsername.value
    rememberMe.value = true
  }
  storage.removeItem('remember_password')
})

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data = await login({
      username: form.value.username,
      password: form.value.password
    })

    userStore.login(data)

    if (rememberMe.value) {
      storage.setItem('remember_username', form.value.username)
    } else {
      storage.removeItem('remember_username')
    }

    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}

const useLastUsername = () => {
  form.value.username = lastUsername.value
}

const showPasswordTip = () => {
  ElMessage.info('当前版本请联系管理员重置密码')
}

const goRegister = () => {
  router.push('/register')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(440px, 0.95fr);
  background:
    radial-gradient(circle at 20% 15%, rgba(0, 255, 136, 0.16), transparent 28%),
    radial-gradient(circle at 80% 78%, rgba(0, 212, 255, 0.14), transparent 30%),
    var(--background);
  color: var(--foreground);
  overflow: hidden;
}

.brand-panel,
.form-panel {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  position: relative;
}

.brand-panel::before {
  content: '';
  position: absolute;
  inset: 40px 0 40px 40px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.08), transparent 45%),
    rgba(255, 255, 255, 0.02);
  pointer-events: none;
}

.brand-shell {
  width: min(720px, 100%);
  position: relative;
  z-index: 1;
}

.brand-top,
.card-header {
  display: flex;
  align-items: center;
  gap: 18px;
}

.brand-mark {
  width: 58px;
  height: 58px;
  border-radius: 18px;
  box-shadow: 0 18px 45px var(--primary-glow);
  flex-shrink: 0;
  display: block;
}

.eyebrow {
  color: var(--primary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0;
  margin: 0 0 6px;
}

h1,
h2 {
  margin: 0;
  letter-spacing: 0;
}

h1 {
  font-size: 56px;
  line-height: 1.06;
  max-width: 560px;
}

h2 {
  font-size: 30px;
  line-height: 1.2;
}

.brand-copy {
  max-width: 560px;
  margin: 28px 0 34px;
  color: var(--foreground-muted);
  font-size: 17px;
}

.preview-stage {
  height: 340px;
  position: relative;
  border-radius: var(--radius-xl);
  border: 1px solid var(--border);
  background:
    linear-gradient(135deg, rgba(0, 255, 136, 0.12), rgba(0, 212, 255, 0.08)),
    var(--background-card);
  overflow: hidden;
}

.hero-illustration {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-top: 18px;
}

.feature-item {
  min-height: 92px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
}

.feature-item .el-icon {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: var(--primary);
  background: var(--primary-muted);
  flex-shrink: 0;
}

.feature-item strong,
.feature-item span {
  display: block;
}

.feature-item strong {
  font-size: 14px;
}

.feature-item span {
  color: var(--foreground-muted);
  font-size: 12px;
  line-height: 1.4;
}

.form-panel {
  background: rgba(255, 255, 255, 0.03);
}

.login-card {
  width: min(460px, 100%);
  padding: 32px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  background: rgba(20, 20, 20, 0.86);
  box-shadow: var(--shadow-lg);
  backdrop-filter: blur(22px);
}

.card-header {
  justify-content: space-between;
  margin-bottom: 22px;
}

.ghost-icon {
  flex-shrink: 0;
}

.account-hint {
  min-height: 44px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  margin-bottom: 18px;
  border-radius: 14px;
  color: var(--foreground-muted);
  background: var(--background-muted);
}

.account-hint span {
  flex: 1;
}

.account-hint button,
.quick-row button {
  border: 0;
  cursor: pointer;
  color: var(--primary);
  background: transparent;
  font-weight: 700;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 2px 0 18px;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 800;
}

.quick-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 16px;
}

.quick-row.single {
  grid-template-columns: 1fr;
}

.quick-row button {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid var(--border);
  border-radius: 14px;
  color: var(--foreground);
  background: var(--background-muted);
}

.quick-row button:hover {
  border-color: var(--primary);
  color: var(--primary);
}

:deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 14px !important;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-checkbox__label) {
  color: var(--foreground-muted);
}

@media (max-width: 1180px) {
  .login-page {
    grid-template-columns: 1fr;
    overflow: auto;
  }

  .brand-panel,
  .form-panel {
    min-height: auto;
    padding: 28px;
  }

  .brand-panel::before {
    inset: 20px;
  }

  h1 {
    font-size: 42px;
  }
}

@media (max-width: 720px) {
  .brand-panel {
    display: none;
  }

  .form-panel {
    min-height: 100vh;
    padding: 18px;
  }

  .login-card {
    padding: 24px;
  }

  .quick-row {
    grid-template-columns: 1fr;
  }
}
</style>
