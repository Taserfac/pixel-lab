<!--
  【文件路径】src/views/auth/Register.vue
  【文件功能说明】注册页面
  - 账号密码注册表单
  - 表单校验（用户名规则、密码强度）
  - 注册成功后跳转到登录页
-->

<template>
  <div class="register-page">
    <div class="register-box">
      <!-- Logo -->
      <div class="logo">
        <h1>创建账号</h1>
        <p>加入 Pixel Lab 开始创作</p>
      </div>
      
      <!-- 注册表单 -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="register-form"
        @keyup.enter="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名（4-20位字母数字下划线）"
            size="large"
          >
            <template #prefix>
              <el-icon :size="16">
                <User />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="nickname">
          <el-input
            v-model="form.nickname"
            placeholder="请输入昵称（显示名称）"
            size="large"
          >
            <template #prefix>
              <el-icon :size="16">
                <Avatar />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码（至少6位）"
            size="large"
            show-password
            @input="checkPassword"
          >
            <template #prefix>
              <el-icon :size="16">
                <Lock />
              </el-icon>
            </template>
          </el-input>
          <!-- 密码强度提示 -->
          <div
            v-if="form.password"
            class="password-strength"
          >
            <span>密码强度：</span>
            <el-tag
              :type="passwordStrength.type"
              size="small"
            >
              {{ passwordStrength.text }}
            </el-tag>
          </div>
        </el-form-item>
        
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请确认密码"
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
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="register-btn"
            :loading="loading"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>
      
      <!-- 登录链接 -->
      <div class="login-link">
        已有账号？<el-link
          type="primary"
          @click="goLogin"
        >
          立即登录
        </el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Avatar } from '@element-plus/icons-vue'
import { register } from '@/api/auth'
import { checkPasswordStrength } from '@/utils/validate'

const router = useRouter()

// 表单引用
const formRef = ref(null)
// 加载状态
const loading = ref(false)

// 表单数据
const form = ref({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

// 密码强度
const passwordStrength = ref({
  type: 'info',
  text: '请输入密码'
})

// 检查密码强度
const checkPassword = () => {
  const result = checkPasswordStrength(form.value.password)
  
  if (result.level === 0) {
    passwordStrength.value = { type: 'danger', text: '太弱' }
  } else if (result.level <= 2) {
    passwordStrength.value = { type: 'warning', text: '较弱' }
  } else if (result.level === 3) {
    passwordStrength.value = { type: 'success', text: '中等' }
  } else {
    passwordStrength.value = { type: 'success', text: '很强' }
  }
}

// 确认密码校验
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.value.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 表单校验规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '长度在 4 到 20 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母、数字、下划线', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 注册
const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  
  try {
    await register({
      username: form.value.username,
      nickname: form.value.nickname,
      password: form.value.password
    })
    
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    loading.value = false
  }
}

// 跳转到登录页
const goLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  padding: var(--space-4);
}

.register-box {
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

.register-form {
  margin-bottom: var(--space-6);
}

.password-strength {
  margin-top: var(--space-2);
  font-size: 12px;
  color: var(--foreground-muted);
}

.register-btn {
  width: 100%;
}

.login-link {
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
