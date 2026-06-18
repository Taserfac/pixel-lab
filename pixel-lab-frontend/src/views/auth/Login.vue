<!--
  【文件路径】src/views/auth/Login.vue
  【文件功能说明】登录页面
  - 账号密码登录表单
  - 表单校验
  - 记住账号功能
  - 登录成功后跳转首页
-->

<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="glow-orb glow-orb-1" />
      <div class="glow-orb glow-orb-2" />
    </div>

    <div class="login-card">
      <!-- Logo -->
      <div class="logo">
        <div class="logo-icon">
          <span class="pixel-text">PX</span>
        </div>
        <div class="logo-text">
          <h1>Pixel Lab</h1>
          <p>数字图像创作与管理平台</p>
        </div>
      </div>
      
      <!-- 登录表单 -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
          >
            <template #prefix>
              <el-icon :size="16">
                <User />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
          >
            <template #prefix>
              <el-icon :size="16">
                <Lock />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <div class="form-options">
          <el-checkbox v-model="rememberMe">
            记住账号
          </el-checkbox>
          <el-link
            type="primary"
            :underline="false"
          >
            忘记密码？
          </el-link>
        </div>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      
      <!-- 注册链接 -->
      <div class="register-link">
        还没有账号？<el-link
          type="primary"
          @click="goRegister"
        >
          立即注册
        </el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import * as storage from '@/utils/storage'
import { login } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 表单引用
const formRef = ref(null)
// 加载状态
const loading = ref(false)
// 记住账号
const rememberMe = ref(false)

// 表单数据
const form = ref({
  username: '',
  password: ''
})

// 表单校验规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '长度在 4 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

// 页面加载时，读取记住的账号
onMounted(() => {
  const savedUsername = storage.getItem('remember_username', '')
  
  if (savedUsername) {
    form.value.username = savedUsername
    rememberMe.value = true
  }
  storage.removeItem('remember_password')
})

// 登录
const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  
  try {
    const data = await login({
      username: form.value.username,
      password: form.value.password
    })
    
    // 保存登录状态
    userStore.login(data)
    
    // 记住账号
    if (rememberMe.value) {
      storage.setItem('remember_username', form.value.username)
    } else {
      storage.removeItem('remember_username')
    }
    
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
      ? route.query.redirect
      : '/explore'
    await router.replace(redirect)
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}

// 跳转到注册页
const goRegister = () => {
  router.push('/register')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--background);
  padding: var(--space-4);
  position: relative;
  overflow: hidden;
}

/* 背景装饰 */
.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.glow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
}

.glow-orb-1 {
  width: 400px;
  height: 400px;
  background: var(--primary);
  top: -100px;
  right: -100px;
  animation: float 8s ease-in-out infinite;
}

.glow-orb-2 {
  width: 300px;
  height: 300px;
  background: var(--secondary);
  bottom: -50px;
  left: -50px;
  animation: float 10s ease-in-out infinite reverse;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(30px, 30px); }
}

/* 登录卡片 */
.login-card {
  width: 100%;
  max-width: 400px;
  padding: var(--space-10);
  background: var(--background-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  position: relative;
  z-index: 1;
}

/* 发光边框效果 */
.login-card::before {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: inherit;
  padding: 1px;
  background: linear-gradient(135deg, var(--primary-muted), transparent 50%, var(--secondary-muted));
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}

.logo {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin-bottom: var(--space-8);
}

.logo-icon {
  width: 48px;
  height: 48px;
  background: var(--primary);
  border-radius: var(--radius);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 30px var(--primary-glow);
  flex-shrink: 0;
}

.pixel-text {
  font-family: var(--font-mono);
  font-size: 16px;
  font-weight: 700;
  color: var(--background);
}

.logo-text h1 {
  font-size: 24px;
  font-weight: 600;
  color: var(--foreground);
  margin: 0;
  letter-spacing: -0.02em;
}

.logo-text p {
  font-size: 13px;
  color: var(--foreground-muted);
  margin: 4px 0 0 0;
}

.login-form {
  margin-bottom: var(--space-6);
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-5);
}

.form-options :deep(.el-checkbox__label) {
  color: var(--foreground-muted);
  font-size: 13px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-weight: 500;
  font-size: 15px;
}

.register-link {
  text-align: center;
  font-size: 13px;
  color: var(--foreground-muted);
}

/* 修复图标大小 */
:deep(.el-input__prefix-inner) {
  font-size: 16px;
}

:deep(.el-input__prefix-inner .el-icon) {
  width: 16px;
  height: 16px;
}

:deep(.el-input__prefix-inner svg) {
  width: 16px;
  height: 16px;
}

/* 响应式 */
@media (max-width: 480px) {
  .login-card {
    padding: var(--space-6);
  }

  .logo-text h1 {
    font-size: 20px;
  }
}
</style>
