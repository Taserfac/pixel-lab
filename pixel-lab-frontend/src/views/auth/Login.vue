<!--
  【文件路径】src/views/auth/Login.vue
  【文件功能说明】登录页面
  - 账号密码登录表单
  - 表单校验
  - 记住密码功能
  - 登录成功后跳转首页
-->

<template>
  <div class="login-page">
    <div class="login-box">
      <!-- Logo -->
      <div class="logo">
        <h1>Pixel Lab Pro</h1>
        <p>数字图像创作与管理平台</p>
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
            记住密码
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import * as storage from '@/utils/storage'
import { login } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

// 表单引用
const formRef = ref(null)
// 加载状态
const loading = ref(false)
// 记住密码
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

// 页面加载时，读取记住的账号密码
onMounted(() => {
  const savedUsername = storage.getItem('remember_username', '')
  const savedPassword = storage.getItem('remember_password', '')
  
  if (savedUsername) {
    form.value.username = savedUsername
    form.value.password = savedPassword
    rememberMe.value = true
  }
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
    
    // 记住密码
    if (rememberMe.value) {
      storage.setItem('remember_username', form.value.username)
      storage.setItem('remember_password', form.value.password)
    } else {
      storage.removeItem('remember_username')
      storage.removeItem('remember_password')
    }
    
    ElMessage.success('登录成功')
    router.push('/')
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
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  padding: var(--space-4);
}

.login-box {
  width: 100%;
  max-width: 420px;
  padding: var(--space-10);
  background-color: var(--background);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
}

.logo {
  text-align: center;
  margin-bottom: var(--space-8);
}

.logo h1 {
  font-size: 28px;
  font-weight: 700;
  color: var(--primary);
  margin-bottom: var(--space-2);
}

.logo p {
  font-size: 14px;
  color: var(--foreground-muted);
}

.login-form {
  margin-bottom: var(--space-6);
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.login-btn {
  width: 100%;
}

.register-link {
  text-align: center;
  font-size: 14px;
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
</style>
