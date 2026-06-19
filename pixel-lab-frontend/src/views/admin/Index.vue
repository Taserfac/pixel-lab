<template>
  <div class="admin-page">
    <h1>后台管理</h1>
    
    <!-- Tab 切换 -->
    <el-tabs v-model="activeTab" class="admin-tabs">
      <!-- 平台统计 -->
      <el-tab-pane label="平台统计" name="stats">
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon users">
              <el-icon :size="24"><User /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ platformStats.users.total }}</span>
              <span class="stat-label">用户总数</span>
              <span class="stat-today">今日 +{{ platformStats.users.today }}</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon images">
              <el-icon :size="24"><Picture /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ platformStats.images.total }}</span>
              <span class="stat-label">作品总数</span>
              <span class="stat-today">今日 +{{ platformStats.images.today }}</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon views">
              <el-icon :size="24"><View /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ formatNumber(platformStats.interactions.views) }}</span>
              <span class="stat-label">总浏览量</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon likes">
              <el-icon :size="24"><Star /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ platformStats.interactions.likes }}</span>
              <span class="stat-label">总点赞数</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon reports">
              <el-icon :size="24"><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ platformStats.interactions.reports }}</span>
              <span class="stat-label">待处理举报</span>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 用户管理 -->
      <el-tab-pane label="用户管理" name="users">
        <div class="search-bar">
          <el-input
            v-model="userSearch"
            placeholder="搜索用户名/昵称"
            clearable
            @keyup.enter="fetchUsers"
            @clear="fetchUsers"
          />
          <el-button @click="fetchUsers">搜索</el-button>
        </div>
        <el-table :data="users" v-loading="loadingUsers" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="用户" min-width="150">
            <template #default="{ row }">
              <div class="user-cell">
                <el-avatar :size="32" :src="row.avatar">
                  {{ row.username?.charAt(0).toUpperCase() }}
                </el-avatar>
                <span>{{ row.nickname || row.username }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">
              <el-tag :type="row.role === 'admin' ? 'danger' : 'info'">
                {{ row.role === 'admin' ? '管理员' : '用户' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="140">
            <template #default="{ row }">
              <el-tag v-if="row.status === 1" type="success">正常</el-tag>
              <el-tag v-else-if="row.ban_end_at" type="danger">
                封禁至 {{ formatDate(row.ban_end_at) }}
              </el-tag>
              <el-tag v-else type="danger">永久封禁</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="created_at" label="注册时间" width="160">
            <template #default="{ row }">
              {{ formatDate(row.created_at) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <template v-if="row.id === userStore.userInfo?.id">
                <span class="self-label">当前用户</span>
              </template>
              <template v-else>
                <el-button
                  v-if="row.status === 1"
                  type="danger"
                  size="small"
                  @click="handleBanUser(row)"
                >
                  封禁
                </el-button>
                <el-button
                  v-else
                  type="success"
                  size="small"
                  @click="handleUnbanUser(row)"
                >
                  解封
                </el-button>
                <el-button
                  v-if="row.role !== 'admin'"
                  type="warning"
                  size="small"
                  @click="handleSetAdmin(row)"
                >
                  设为管理员
                </el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <el-pagination
            v-model:current-page="userPage"
            :page-size="10"
            :total="userTotal"
            layout="total, prev, pager, next"
            @current-change="fetchUsers"
          />
        </div>
      </el-tab-pane>

      <!-- 作品管理 -->
      <el-tab-pane label="作品管理" name="images">
        <div class="search-bar">
          <el-input
            v-model="imageSearch"
            placeholder="搜索作品名称"
            clearable
            @keyup.enter="fetchImages"
            @clear="fetchImages"
          />
          <el-select v-model="imageStatus" placeholder="状态" clearable @change="fetchImages">
            <el-option label="全部" value="" />
            <el-option label="正常" value="1" />
            <el-option label="已封禁" value="2" />
            <el-option label="已删除" value="0" />
          </el-select>
          <el-button @click="fetchImages">搜索</el-button>
        </div>
        <el-table :data="images" v-loading="loadingImages" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="预览" width="100">
            <template #default="{ row }">
              <img :src="row.url" class="image-thumb" />
            </template>
          </el-table-column>
          <el-table-column label="作品名称" min-width="150">
            <template #default="{ row }">
              {{ row.title || row.original_name || '未命名' }}
            </template>
          </el-table-column>
          <el-table-column prop="author_name" label="作者" width="100" />
          <el-table-column label="数据" width="140">
            <template #default="{ row }">
              <span class="data-item">👀 {{ row.view_count || 0 }}</span>
              <span class="data-item">❤️ {{ row.like_count || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.status === 1" type="success" size="small">正常</el-tag>
              <el-tag v-else-if="row.status === 2" type="danger" size="small">封禁</el-tag>
              <el-tag v-else type="info" size="small">已删除</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="公开" width="80">
            <template #default="{ row }">
              <el-tag :type="row.is_public ? 'success' : 'info'" size="small">
                {{ row.is_public ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="created_at" label="创建时间" width="160">
            <template #default="{ row }">
              {{ formatDate(row.created_at) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 1">
                <el-button type="danger" size="small" @click="handleDeleteImage(row)">删除</el-button>
                <el-button type="warning" size="small" @click="handleBanImage(row)">封禁</el-button>
              </template>
              <template v-else-if="row.status === 2">
                <el-button type="success" size="small" @click="handleUnbanImage(row)">解封</el-button>
              </template>
              <span v-else class="deleted-text">已删除</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <el-pagination
            v-model:current-page="imagePage"
            :page-size="10"
            :total="imageTotal"
            layout="total, prev, pager, next"
            @current-change="fetchImages"
          />
        </div>
      </el-tab-pane>

      <!-- 作品集管理 -->
      <el-tab-pane label="作品集管理" name="albums">
        <div class="search-bar">
          <el-input
            v-model="albumSearch"
            placeholder="搜索作品集标题"
            clearable
            @keyup.enter="fetchAlbums"
            @clear="fetchAlbums"
          />
          <el-button @click="fetchAlbums">搜索</el-button>
        </div>
        <el-table :data="albums" v-loading="loadingAlbums" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="标题" min-width="150">
            <template #default="{ row }">
              {{ row.title || '未命名' }}
            </template>
          </el-table-column>
          <el-table-column prop="author_name" label="作者" width="100" />
          <el-table-column label="图片数" width="80">
            <template #default="{ row }">
              {{ row.image_count || 0 }}
            </template>
          </el-table-column>
          <el-table-column label="公开" width="80">
            <template #default="{ row }">
              <el-tag :type="row.is_public ? 'success' : 'info'" size="small">
                {{ row.is_public ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.status === 1" type="success" size="small">正常</el-tag>
              <el-tag v-else-if="row.status === 2" type="danger" size="small">封禁</el-tag>
              <el-tag v-else type="info" size="small">已删除</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="created_at" label="创建时间" width="160">
            <template #default="{ row }">
              {{ formatDate(row.created_at) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 1">
                <el-button type="danger" size="small" @click="handleDeleteAlbum(row)">删除</el-button>
                <el-button type="warning" size="small" @click="handleBanAlbum(row)">封禁</el-button>
              </template>
              <template v-else-if="row.status === 2">
                <el-button type="success" size="small" @click="handleUnbanAlbum(row)">解封</el-button>
              </template>
              <span v-else class="deleted-text">已删除</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <el-pagination
            v-model:current-page="albumPage"
            :page-size="10"
            :total="albumTotal"
            layout="total, prev, pager, next"
            @current-change="fetchAlbums"
          />
        </div>
      </el-tab-pane>

      <!-- 举报处理 -->
      <el-tab-pane label="举报处理" name="reports">
        <div class="search-bar">
          <el-select v-model="reportStatus" placeholder="处理状态" clearable @change="fetchReports">
            <el-option label="全部" value="" />
            <el-option label="待处理" value="0" />
            <el-option label="已处理" value="1" />
            <el-option label="已驳回" value="2" />
          </el-select>
          <el-button @click="fetchReports">刷新</el-button>
        </div>
        <el-table :data="reports" v-loading="loadingReports" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="作品" min-width="220">
            <template #default="{ row }">
              <div class="report-work">
                <img :src="row.url" class="image-thumb" />
                <div>
                  <strong>{{ row.title || row.original_name || '未命名' }}</strong>
                  <span>作者：{{ row.author_name || '匿名' }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="reporter_name" label="举报人" width="120" />
          <el-table-column label="原因" min-width="220">
            <template #default="{ row }">
              <strong>{{ row.reason }}</strong>
              <p v-if="row.detail" class="report-detail">{{ row.detail }}</p>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.status === 0" type="warning" size="small">待处理</el-tag>
              <el-tag v-else-if="row.status === 1" type="success" size="small">已处理</el-tag>
              <el-tag v-else type="info" size="small">已驳回</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="created_at" label="举报时间" width="160">
            <template #default="{ row }">
              {{ formatDate(row.created_at) }}
            </template>
          </el-table-column>
          <el-table-column label="办理人" width="120">
            <template #default="{ row }">
              {{ row.handler_name || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 0">
                <el-button type="warning" size="small" @click="handleBanReportedImage(row)">封禁作品</el-button>
                <el-button type="success" size="small" @click="handleResolveReport(row)">标记处理</el-button>
                <el-button size="small" @click="handleRejectReport(row)">驳回</el-button>
              </template>
              <span v-else class="deleted-text">已办理</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <el-pagination
            v-model:current-page="reportPage"
            :page-size="10"
            :total="reportTotal"
            layout="total, prev, pager, next"
            @current-change="fetchReports"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 封禁用户对话框 -->
    <el-dialog v-model="banDialogVisible" title="封禁用户" width="420px">
      <el-form label-position="top">
        <el-form-item label="封禁天数">
          <el-radio-group v-model="banDays">
            <el-radio :value="3">3 天</el-radio>
            <el-radio :value="7">7 天</el-radio>
            <el-radio :value="30">30 天</el-radio>
            <el-radio :value="0">永久</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="封禁原因">
          <el-input v-model="banReason" type="textarea" :rows="3" placeholder="输入封禁原因（可选）" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="banDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmBanUser">确认封禁</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { User, Picture, View, Star, Warning } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getUsers, updateUserStatus, updateUserRole, getImages, deleteImage, getStats, banUser, banImage, getAlbums, banAlbum, deleteAlbum, getReports, updateReportStatus, banReportedImage } from '@/api/admin'

const userStore = useUserStore()
const activeTab = ref('stats')

// 平台统计
const platformStats = ref({
  users: { total: 0, today: 0 },
  images: { total: 0, today: 0 },
  interactions: { views: 0, likes: 0, collects: 0, comments: 0, reports: 0 }
})

// 用户管理
const users = ref([])
const userSearch = ref('')
const userPage = ref(1)
const userTotal = ref(0)
const loadingUsers = ref(false)

// 作品管理
const images = ref([])
const imageSearch = ref('')
const imageStatus = ref('')
const imagePage = ref(1)
const imageTotal = ref(0)
const loadingImages = ref(false)

// 作品集管理
const albums = ref([])
const albumSearch = ref('')
const albumPage = ref(1)
const albumTotal = ref(0)
const loadingAlbums = ref(false)

// 举报处理
const reports = ref([])
const reportStatus = ref('0')
const reportPage = ref(1)
const reportTotal = ref(0)
const loadingReports = ref(false)

// 封禁对话框
const banDialogVisible = ref(false)
const banDays = ref(7)
const banReason = ref('')
const banTargetUser = ref(null)

// 格式化数字
const formatNumber = (num) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取平台统计
const fetchStats = async () => {
  try {
    const res = await getStats()
    platformStats.value = res
  } catch (error) {
    console.error('获取统计失败:', error)
  }
}

// 获取用户列表
const fetchUsers = async () => {
  loadingUsers.value = true
  try {
    const res = await getUsers({
      page: userPage.value,
      pageSize: 10,
      keyword: userSearch.value
    })
    users.value = res.list || []
    userTotal.value = res.total || 0
  } catch (error) {
    console.error('获取用户失败:', error)
  } finally {
    loadingUsers.value = false
  }
}

// 封禁用户
const handleBanUser = (user) => {
  banTargetUser.value = user
  banDays.value = 7
  banReason.value = ''
  banDialogVisible.value = true
}

const confirmBanUser = async () => {
  if (!banTargetUser.value) return
  try {
    await banUser(banTargetUser.value.id, banDays.value, banReason.value)
    ElMessage.success('已封禁')
    banDialogVisible.value = false
    fetchUsers()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '操作失败')
  }
}

// 解封用户
const handleUnbanUser = async (user) => {
  try {
    await ElMessageBox.confirm(`确定要解封用户 "${user.nickname || user.username}" 吗？`, '确认', { type: 'warning' })
    await updateUserStatus(user.id, 1)
    ElMessage.success('已解封')
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '操作失败')
    }
  }
}

// 设为管理员
const handleSetAdmin = async (user) => {
  try {
    await ElMessageBox.confirm(`确定要将 "${user.nickname || user.username}" 设为管理员吗？`, '确认', { type: 'warning' })
    await updateUserRole(user.id, 'admin')
    ElMessage.success('已设为管理员')
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '操作失败')
    }
  }
}

// 获取作品列表
const fetchImages = async () => {
  loadingImages.value = true
  try {
    const res = await getImages({
      page: imagePage.value,
      pageSize: 10,
      keyword: imageSearch.value,
      status: imageStatus.value
    })
    images.value = res.list || []
    imageTotal.value = res.total || 0
  } catch (error) {
    console.error('获取作品失败:', error)
  } finally {
    loadingImages.value = false
  }
}

// 删除作品
const handleDeleteImage = async (image) => {
  try {
    await ElMessageBox.confirm('确定要删除这个作品吗？', '确认', { type: 'warning' })
    await deleteImage(image.id)
    ElMessage.success('已删除')
    fetchImages()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '操作失败')
    }
  }
}

// 封禁作品
const handleBanImage = async (image) => {
  try {
    await ElMessageBox.confirm(`确定要封禁作品 "${image.title || image.original_name || '未命名'}" 吗？`, '确认封禁', { type: 'warning' })
    await banImage(image.id, 2)
    ElMessage.success('已封禁')
    fetchImages()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '操作失败')
    }
  }
}

// 解封作品
const handleUnbanImage = async (image) => {
  try {
    await ElMessageBox.confirm(`确定要解封作品 "${image.title || image.original_name || '未命名'}" 吗？`, '确认解封', { type: 'warning' })
    await banImage(image.id, 1)
    ElMessage.success('已解封')
    fetchImages()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '操作失败')
    }
  }
}

// 获取作品集列表
const fetchAlbums = async () => {
  loadingAlbums.value = true
  try {
    const res = await getAlbums({
      page: albumPage.value,
      pageSize: 10,
      keyword: albumSearch.value
    })
    albums.value = res.list || []
    albumTotal.value = res.total || 0
  } catch (error) {
    console.error('获取作品集失败:', error)
  } finally {
    loadingAlbums.value = false
  }
}

// 删除作品集
const handleDeleteAlbum = async (album) => {
  try {
    await ElMessageBox.confirm(`确定要删除作品集 "${album.title}" 吗？`, '确认', { type: 'warning' })
    await deleteAlbum(album.id)
    ElMessage.success('已删除')
    fetchAlbums()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '操作失败')
    }
  }
}

// 封禁作品集
const handleBanAlbum = async (album) => {
  try {
    await ElMessageBox.confirm(`确定要封禁作品集 "${album.title}" 吗？`, '确认封禁', { type: 'warning' })
    await banAlbum(album.id, 2)
    ElMessage.success('已封禁')
    fetchAlbums()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '操作失败')
    }
  }
}

// 解封作品集
const handleUnbanAlbum = async (album) => {
  try {
    await ElMessageBox.confirm(`确定要解封作品集 "${album.title}" 吗？`, '确认解封', { type: 'warning' })
    await banAlbum(album.id, 1)
    ElMessage.success('已解封')
    fetchAlbums()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '操作失败')
    }
  }
}

// 获取举报列表
const fetchReports = async () => {
  loadingReports.value = true
  try {
    const res = await getReports({
      page: reportPage.value,
      pageSize: 10,
      status: reportStatus.value
    })
    reports.value = res.list || []
    reportTotal.value = res.total || 0
  } catch (error) {
    console.error('获取举报失败:', error)
  } finally {
    loadingReports.value = false
  }
}

// 封禁被举报作品
const handleBanReportedImage = async (report) => {
  const title = report.title || report.original_name || '未命名'
  try {
    await ElMessageBox.confirm(`确定要封禁被举报作品 "${title}" 吗？`, '确认封禁', { type: 'warning' })
    await banReportedImage(report.id)
    ElMessage.success('已封禁作品并处理举报')
    fetchReports()
    fetchImages()
    fetchStats()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '操作失败')
    }
  }
}

// 标记举报已处理
const handleResolveReport = async (report) => {
  try {
    await ElMessageBox.confirm('确定将该举报标记为已处理吗？', '确认处理', { type: 'warning' })
    await updateReportStatus(report.id, 1)
    ElMessage.success('已处理')
    fetchReports()
    fetchStats()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '操作失败')
    }
  }
}

// 驳回举报
const handleRejectReport = async (report) => {
  try {
    await ElMessageBox.confirm('确定驳回该举报吗？', '确认驳回', { type: 'warning' })
    await updateReportStatus(report.id, 2)
    ElMessage.success('已驳回')
    fetchReports()
    fetchStats()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '操作失败')
    }
  }
}

onMounted(() => {
  fetchStats()
  fetchUsers()
  fetchImages()
  fetchAlbums()
  fetchReports()
})
</script>

<style scoped>
.admin-page {
  padding: var(--space-6);
  max-width: 1400px;
  margin: 0 auto;
}

.admin-page h1 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: var(--space-6);
}

.admin-tabs {
  background: var(--background-soft);
  padding: var(--space-4);
  border: 3px solid var(--border);
  box-shadow: 6px 6px 0 var(--border);
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: var(--space-4);
  margin-top: var(--space-4);
}

.stat-card {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-5);
  background: var(--background);
  border: 3px solid var(--border);
}

.stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid var(--border);
}

.stat-icon.users { background: #39c5bb; color: white; }
.stat-icon.images { background: #8b5cf6; color: white; }
.stat-icon.views { background: #f59e0b; color: white; }
.stat-icon.likes { background: #ef4444; color: white; }
.stat-icon.reports { background: #f97316; color: white; }

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  font-family: var(--font-mono);
}

.stat-label {
  font-size: 12px;
  color: var(--foreground-muted);
}

.stat-today {
  font-size: 11px;
  color: var(--primary);
}

/* 搜索栏 */
.search-bar {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.search-bar .el-input {
  width: 200px;
}

/* 表格 */
.user-cell {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.image-thumb {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border: 2px solid var(--border);
}

.report-work {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.report-work strong,
.report-work span {
  display: block;
}

.report-work span,
.report-detail {
  color: var(--foreground-muted);
  font-size: 12px;
}

.report-detail {
  margin: 4px 0 0;
  line-height: 1.5;
}

.data-item {
  margin-right: var(--space-2);
  font-size: 12px;
}

.deleted-text {
  color: var(--foreground-muted);
  font-size: 12px;
}

.self-label {
  display: inline-block;
  padding: 4px 8px;
  background: var(--primary-muted);
  color: var(--primary);
  font-size: 12px;
  font-weight: 600;
  border-radius: 4px;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-4);
}

/* 表格样式修复 */
:deep(.el-table) {
  background-color: var(--background-soft) !important;
  --el-table-bg-color: var(--background-soft);
  --el-table-tr-bg-color: var(--background-soft);
  --el-table-header-bg-color: var(--background-muted);
  --el-table-row-hover-bg-color: var(--background-muted);
  --el-table-border-color: var(--border);
  --el-table-text-color: var(--foreground);
  --el-table-header-text-color: var(--foreground-muted);
}

:deep(.el-table th.el-table__cell) {
  background-color: var(--background-muted) !important;
  color: var(--foreground-muted) !important;
}

:deep(.el-table td.el-table__cell) {
  background-color: var(--background-soft) !important;
  color: var(--foreground) !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background-color: var(--background) !important;
}

/* 响应式 */
@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
