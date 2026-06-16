<template>
  <div class="settings-page">
    <h1>设置</h1>

    <!-- 基本信息 -->
    <div class="settings-section">
      <div class="section-header">
        <h2>基本信息</h2>
      </div>
      <div class="section-content">
        <div class="form-item">
          <label>头像</label>
          <div class="avatar-upload">
            <el-avatar :size="80" :src="form.avatar">
              {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
            </el-avatar>
            <el-upload
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
              accept="image/*"
            >
              <el-button size="small">更换头像</el-button>
            </el-upload>
          </div>
        </div>
        <div class="form-item">
          <label>昵称</label>
          <el-input v-model="form.nickname" placeholder="请输入昵称" style="max-width: 300px" />
        </div>
        <div class="form-item">
          <label>用户名</label>
          <el-input :value="userStore.userInfo?.username" disabled style="max-width: 300px" />
          <span class="hint">用户名不可修改</span>
        </div>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存修改</el-button>
      </div>
    </div>

    <!-- 修改密码 -->
    <div class="settings-section">
      <div class="section-header">
        <h2>修改密码</h2>
      </div>
      <div class="section-content">
        <div class="form-item">
          <label>原密码</label>
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" style="max-width: 300px" show-password />
        </div>
        <div class="form-item">
          <label>新密码</label>
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码（6-20位）" style="max-width: 300px" show-password />
        </div>
        <div class="form-item">
          <label>确认密码</label>
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" style="max-width: 300px" show-password />
        </div>
        <el-button type="primary" :loading="changingPassword" @click="handleChangePassword">修改密码</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { updateProfile, changePassword, uploadAvatar } from '@/api/auth'

const userStore = useUserStore()

const form = ref({
  nickname: '',
  avatar: ''
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const saving = ref(false)
const changingPassword = ref(false)

onMounted(() => {
  form.value.nickname = userStore.userInfo?.nickname || ''
  form.value.avatar = userStore.userInfo?.avatar || ''
})

// 头像上传前处理
const beforeAvatarUpload = async (file) => {
  console.log('上传文件:', file)
  
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }

  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }

  try {
    console.log('开始上传...')
    const res = await uploadAvatar(file)
    console.log('上传结果:', res)
    
    form.value.avatar = res.url
    
    // 自动保存头像
    console.log('保存头像...')
    if (res.user) {
      userStore.setUserInfo(res.user)
    } else {
      userStore.updateUserInfo({ avatar: res.url })
    }
    ElMessage.success('头像更新成功')
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('头像上传失败: ' + (error.message || '未知错误'))
  }

  return false
}

// 保存资料
const saveProfile = async () => {
  if (!form.value.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }

  saving.value = true
  try {
    const res = await updateProfile({
      nickname: form.value.nickname,
      avatar: form.value.avatar
    })
    userStore.setUserInfo(res)
    ElMessage.success('保存成功')
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    saving.value = false
  }
}

// 修改密码
const handleChangePassword = async () => {
  if (!passwordForm.value.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!passwordForm.value.newPassword || passwordForm.value.newPassword.length < 6) {
    ElMessage.warning('新密码长度需在 6-20 位之间')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  changingPassword.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    ElMessage.success('密码修改成功')
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (error) {
    console.error('修改密码失败:', error)
  } finally {
    changingPassword.value = false
  }
}
</script>

<style scoped>
.settings-page {
  padding: var(--space-6);
  max-width: 800px;
  margin: 0 auto;
}

.settings-page h1 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: var(--space-6);
}

.settings-section {
  background: var(--background-soft);
  border: 3px solid var(--border);
  box-shadow: 4px 4px 0 var(--border);
  margin-bottom: var(--space-6);
}

.section-header {
  padding: var(--space-4);
  border-bottom: 2px solid var(--border);
}

.section-header h2 {
  font-size: 14px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.section-content {
  padding: var(--space-5);
}

.form-item {
  margin-bottom: var(--space-5);
}

.form-item label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--foreground-muted);
  margin-bottom: var(--space-2);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.form-item .hint {
  display: block;
  font-size: 12px;
  color: var(--foreground-muted);
  margin-top: var(--space-1);
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}
</style>
