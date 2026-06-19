<template>
  <div class="personal-page">
    <section class="profile-hero">
      <div class="hero-main">
        <div class="avatar-ring">
          <el-avatar :size="104" :src="userInfo.avatar || defaultAvatar">
            {{ avatarText }}
          </el-avatar>
        </div>

        <div class="profile-copy">
          <h1>{{ userInfo.nickname || userInfo.username || 'Pixel Creator' }}</h1>
          <p class="username">@{{ userInfo.username || 'creator' }}</p>
          <p class="profile-desc">
            记录灵感、整理作品、分享创作，让每一次像素实验都沉淀成你的视觉主页。
          </p>

          <div class="hero-actions">
            <el-button type="primary" @click="handleUpload">
              <el-icon><Upload /></el-icon>
              上传图片
            </el-button>
            <el-button @click="router.push('/settings')">
              <el-icon><Setting /></el-icon>
              编辑资料
            </el-button>
          </div>
        </div>
      </div>

      <div class="hero-side">
        <div class="stat-grid">
          <div v-for="item in heroStats" :key="item.label" class="hero-stat">
            <span class="stat-value">{{ formatNumber(item.value) }}</span>
            <span class="stat-label">{{ item.label }}</span>
          </div>
        </div>
      </div>
    </section>

    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      class="visually-hidden"
      @change="onFileSelected"
    >

    <el-tabs v-model="activeTab" class="creator-tabs" @tab-change="handleTabChange">
      <el-tab-pane name="works">
        <template #label>
          <span class="tab-label"><el-icon><Picture /></el-icon>作品</span>
        </template>

        <section class="toolbar-panel">
          <div>
            <h2>我的作品流</h2>
            <p>{{ filteredImages.length }} 个作品正在展示</p>
          </div>
          <el-radio-group v-model="filterType" size="small">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="public">公开</el-radio-button>
            <el-radio-button label="private">私有</el-radio-button>
          </el-radio-group>
        </section>

        <div v-loading="loadingImages" class="feed-shell">
          <div v-if="filteredImages.length" class="masonry-grid">
            <article v-for="image in filteredImages" :key="image.id" class="work-card">
              <div class="cover-shell">
                <button class="work-cover" type="button" @click="viewImage(image)">
                  <img :src="image.url" :alt="imageTitle(image)" loading="lazy">
                  <span class="visibility-badge" :class="{ public: isPublic(image) }">
                    {{ isPublic(image) ? '公开' : '私有' }}
                  </span>
                </button>

                <el-dropdown
                  class="card-menu"
                  trigger="click"
                  placement="bottom-end"
                  @command="(command) => handleImageCommand(image, command)"
                >
                  <button
                    class="icon-button"
                    type="button"
                    aria-label="作品操作"
                    @click.stop
                  >
                    <el-icon><MoreFilled /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="view">
                        <el-icon><View /></el-icon>
                        查看
                      </el-dropdown-item>
                      <el-dropdown-item command="visibility">
                        <el-icon>
                          <Lock v-if="isPublic(image)" />
                          <Unlock v-else />
                        </el-icon>
                        {{ isPublic(image) ? '设为私有' : '设为公开' }}
                      </el-dropdown-item>
                      <el-dropdown-item command="workbench">
                        <el-icon><EditPen /></el-icon>
                        在图像工坊中编辑
                      </el-dropdown-item>
                      <el-dropdown-item command="delete" divided>
                        <el-icon><Delete /></el-icon>
                        删除
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>

              <div class="work-meta">
                <div>
                  <h3 :title="imageTitle(image)">{{ imageTitle(image) }}</h3>
                  <p v-if="image.description" class="image-desc">{{ image.description }}</p>
                  <button class="desc-edit-btn" type="button" @click.stop="openDescriptionEdit(image)">
                    <el-icon><Edit /></el-icon>
                    {{ image.description ? '编辑说明' : '添加说明' }}
                  </button>
                  <p>{{ formatDate(image.created_at) }}</p>
                </div>
              </div>

              <div class="metric-row">
                <span><el-icon><View /></el-icon>{{ formatNumber(image.view_count) }}</span>
                <span><el-icon><Star /></el-icon>{{ formatNumber(image.like_count) }}</span>
                <span><el-icon><FolderOpened /></el-icon>{{ formatNumber(image.collect_count) }}</span>
              </div>
            </article>
          </div>
          <div v-else class="empty-panel">
            <el-icon><Picture /></el-icon>
            <h3>还没有作品</h3>
            <p>上传图片后，作品会自动沉淀到你的个人主页。</p>
            <el-button type="primary" @click="handleUpload">上传图片</el-button>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane name="collections">
        <template #label>
          <span class="tab-label"><el-icon><FolderOpened /></el-icon>收藏</span>
        </template>
        <content-feed
          :items="collections"
          :loading="loadingCollections"
          empty-title="暂无收藏"
          empty-desc="去社区发现值得反复看的作品。"
          action-text="逛逛社区"
          @item-click="goToCommunity"
          @empty-action="goToCommunity"
        />
      </el-tab-pane>

      <el-tab-pane name="likes">
        <template #label>
          <span class="tab-label"><el-icon><Star /></el-icon>点赞</span>
        </template>
        <content-feed
          :items="likes"
          :loading="loadingLikes"
          empty-title="暂无点赞"
          empty-desc="遇到喜欢的作品，点亮它们后会出现在这里。"
          action-text="发现作品"
          @item-click="goToCommunity"
          @empty-action="goToCommunity"
        />
      </el-tab-pane>

      <el-tab-pane name="following">
        <template #label>
          <span class="tab-label"><el-icon><User /></el-icon>关注作者</span>
        </template>

        <section class="toolbar-panel">
          <div>
            <h2>关注作者</h2>
            <p>{{ followingCreators.length }} 位创作者正在关注</p>
          </div>
          <el-button @click="router.push('/creators?tab=following')">
            <el-icon><User /></el-icon>
            查看全部
          </el-button>
        </section>

        <div v-loading="loadingFollowing" class="feed-shell">
          <div v-if="followingCreators.length" class="following-grid">
            <article
              v-for="creator in followingCreators"
              :key="creator.id"
              class="following-card"
              @click="openFollowingCreator(creator)"
            >
              <div class="following-avatar">
                <el-avatar :size="58" :src="creator.avatar">{{ creator.name.charAt(0) }}</el-avatar>
              </div>
              <div class="following-info">
                <h3>{{ creator.name }}</h3>
                <p v-if="creator.followedAt">关注于 {{ formatDate(creator.followedAt) }}</p>
                <p v-else>已关注的创作者</p>
                <div class="following-metrics">
                  <span>{{ formatNumber(creator.workCount) }} 作品</span>
                  <span>{{ formatNumber(creator.likeCount) }} 获赞</span>
                  <span>{{ formatNumber(creator.collectCount) }} 收藏</span>
                </div>
              </div>
              <div class="following-actions">
                <el-button text @click.stop="openFollowingCreator(creator)">主页</el-button>
                <el-button
                  plain
                  :loading="unfollowingId === creator.id"
                  @click.stop="handleUnfollowCreator(creator)"
                >
                  取消关注
                </el-button>
              </div>
            </article>
          </div>

          <div v-else class="empty-panel">
            <el-icon><User /></el-icon>
            <h3>还没有关注作者</h3>
            <p>去社区发现喜欢的创作者，关注后会出现在这里。</p>
            <el-button type="primary" @click="router.push('/creators')">发现创作者</el-button>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane name="albums">
        <template #label>
          <span class="tab-label"><el-icon><Files /></el-icon>作品集</span>
        </template>

        <section v-if="!selectedAlbum" class="toolbar-panel">
          <div>
            <h2>我的作品集</h2>
            <p>{{ albums.length }} 个作品集</p>
          </div>
          <el-button type="primary" @click="showCreateAlbumDialog = true">
            <el-icon><Plus /></el-icon>
            新建作品集
          </el-button>
        </section>
        <section v-else class="toolbar-panel">
          <div>
            <el-button text @click="selectedAlbum = null">
              <el-icon><ArrowLeft /></el-icon>
              返回作品集列表
            </el-button>
          </div>
          <div>
            <h2>{{ selectedAlbum.title }}</h2>
            <p>{{ selectedAlbum.images?.length || 0 }} 张图片 · {{ selectedAlbum.description || '暂无描述' }}</p>
          </div>
          <div class="album-detail-actions">
            <el-button @click="openAlbumEdit(selectedAlbum)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button type="danger" plain @click="confirmDeleteAlbum(selectedAlbum)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
            <el-button type="primary" @click="showAddImageDialog = true">
              <el-icon><Plus /></el-icon>
              添加图片
            </el-button>
          </div>
        </section>

        <div v-loading="loadingAlbums" class="feed-shell">
          <!-- Album list view -->
          <div v-if="!selectedAlbum">
            <div v-if="albums.length" class="album-grid">
              <article v-for="album in albums" :key="album.id" class="album-card" @click="openAlbumDetail(album)">
                <div class="album-cover">
                  <img v-if="albumCoverUrl(album)" :src="albumCoverUrl(album)" :alt="album.title" loading="lazy">
                  <div v-else class="album-cover-placeholder">
                    <el-icon><FolderOpened /></el-icon>
                  </div>
                  <span class="album-count">{{ album.image_count || 0 }} 张</span>
                  <el-dropdown
                    class="album-menu"
                    trigger="click"
                    placement="bottom-end"
                    @command="(command) => handleAlbumCommand(album, command)"
                  >
                    <button class="icon-button" type="button" aria-label="作品集操作" @click.stop>
                      <el-icon><MoreFilled /></el-icon>
                    </button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="open">
                          <el-icon><View /></el-icon>
                          查看
                        </el-dropdown-item>
                        <el-dropdown-item command="edit">
                          <el-icon><Edit /></el-icon>
                          编辑
                        </el-dropdown-item>
                        <el-dropdown-item command="delete" divided>
                          <el-icon><Delete /></el-icon>
                          删除
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
                <div class="album-meta">
                  <h3 :title="album.title">{{ album.title }}</h3>
                  <p v-if="album.description" class="album-desc-snippet">{{ album.description }}</p>
                  <p class="album-date">{{ formatDate(album.created_at) }}</p>
                </div>
              </article>
            </div>
            <div v-else class="empty-panel">
              <el-icon><FolderOpened /></el-icon>
              <h3>还没有作品集</h3>
              <p>创建作品集，将相关作品整理在一起。</p>
              <el-button type="primary" @click="showCreateAlbumDialog = true">新建作品集</el-button>
            </div>
          </div>

          <!-- Album detail view -->
          <div v-else>
            <div v-if="selectedAlbum.images?.length" class="album-image-list">
              <article v-for="(img, index) in selectedAlbum.images" :key="img.id" class="album-image-item">
                <div class="album-image-cover" @click="viewImage(img)">
                  <img :src="img.url" :alt="img.title || '图片'" loading="lazy">
                  <span v-if="selectedAlbum.cover_image_id === img.id" class="album-cover-badge">封面</span>
                </div>
                <div class="album-image-info">
                  <h4>{{ img.title || img.original_name || '未命名' }}</h4>
                  <p v-if="albumImageDescription(img)" class="album-image-desc">{{ albumImageDescription(img) }}</p>
                  <button class="desc-edit-btn" type="button" @click.stop="openAlbumImageDescEdit(img)">
                    <el-icon><Edit /></el-icon>
                    {{ albumImageDescription(img) ? '编辑描述' : '添加描述' }}
                  </button>
                </div>
                <div class="album-image-actions">
                  <el-button text size="small" :disabled="index === 0" @click.stop="moveAlbumImage(index, -1)">上移</el-button>
                  <el-button text size="small" :disabled="index === selectedAlbum.images.length - 1" @click.stop="moveAlbumImage(index, 1)">下移</el-button>
                  <el-button text size="small" :disabled="selectedAlbum.cover_image_id === img.id" @click.stop="setAlbumCover(img)">设封面</el-button>
                  <el-button type="danger" text size="small" @click.stop="confirmRemoveImage(img)">
                    <el-icon><Delete /></el-icon>
                    移除
                  </el-button>
                </div>
              </article>
            </div>
            <div v-else class="empty-panel">
              <el-icon><Picture /></el-icon>
              <h3>作品集为空</h3>
              <p>添加图片到这个作品集中。</p>
              <el-button type="primary" @click="showAddImageDialog = true">添加图片</el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane name="insights">
        <template #label>
          <span class="tab-label"><el-icon><TrendCharts /></el-icon>数据</span>
        </template>

        <section class="insights-grid">
          <div class="chart-card">
            <div class="section-title compact">
              <div>
                <span class="eyebrow">30 天趋势</span>
                <h2>互动走势</h2>
              </div>
            </div>
            <div ref="trendChartRef" class="trend-chart" />
          </div>

          <div class="hot-card">
            <div class="section-title compact">
              <div>
                <span class="eyebrow">Top Works</span>
                <h2>热门作品</h2>
              </div>
            </div>
            <div v-if="hotWorks.length" class="hot-list">
              <button v-for="work in hotWorks" :key="work.id" class="hot-item" type="button" @click="viewImage(work)">
                <img :src="work.url" :alt="imageTitle(work)" loading="lazy">
                <span class="hot-rank">#{{ hotWorks.indexOf(work) + 1 }}</span>
                <div>
                  <strong :title="imageTitle(work)">{{ imageTitle(work) }}</strong>
                  <small>{{ formatNumber(work.view_count) }} 浏览 · {{ formatNumber(work.like_count) }} 赞</small>
                </div>
              </button>
            </div>
            <div v-else class="soft-empty small">暂无热门作品数据。</div>
          </div>
        </section>
      </el-tab-pane>
    </el-tabs>


    <!-- Description edit dialog -->
    <el-dialog v-model="descDialogVisible" title="编辑说明" width="480px">
      <el-input
        v-model="descEditValue"
        type="textarea"
        :rows="4"
        placeholder="输入图片说明..."
        maxlength="500"
        show-word-limit
      />
      <template #footer>
        <el-button @click="descDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="descSaving" @click="saveDescription">保存</el-button>
      </template>
    </el-dialog>

    <!-- Create album dialog -->
    <el-dialog v-model="showCreateAlbumDialog" title="新建作品集" width="560px">
      <el-form label-position="top">
        <el-form-item label="标题" required>
          <el-input v-model="newAlbum.title" placeholder="作品集标题" maxlength="50" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="newAlbum.description"
            type="textarea"
            :rows="3"
            placeholder="简要描述这个作品集..."
            maxlength="200"
          />
        </el-form-item>
        <el-form-item label="封面图片">
          <div v-if="newAlbum.coverImage" class="cover-preview">
            <img :src="newAlbum.coverImage.url" alt="封面预览">
            <el-button text type="danger" size="small" @click="newAlbum.coverImage = null">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <div v-else class="cover-picker">
            <p>从你的作品中选择封面：</p>
            <div class="cover-option-grid">
              <button
                v-for="img in images"
                :key="img.id"
                type="button"
                class="cover-option"
                :class="{ active: newAlbum.coverImage?.id === img.id }"
                @click="newAlbum.coverImage = img"
              >
                <img :src="img.url" :alt="imageTitle(img)" loading="lazy">
              </button>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateAlbumDialog = false">取消</el-button>
        <el-button type="primary" :loading="albumSaving" :disabled="!newAlbum.title.trim()" @click="handleCreateAlbum">
          创建
        </el-button>
      </template>
    </el-dialog>

    <!-- Edit album dialog -->
    <el-dialog v-model="showEditAlbumDialog" title="编辑作品集" width="560px">
      <el-form label-position="top">
        <el-form-item label="标题" required>
          <el-input v-model="editAlbum.title" placeholder="作品集标题" maxlength="50" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="editAlbum.description"
            type="textarea"
            :rows="3"
            placeholder="简要描述这个作品集..."
            maxlength="200"
          />
        </el-form-item>
        <el-form-item label="封面图片">
          <div v-if="editAlbum.coverImage" class="cover-preview">
            <img :src="editAlbum.coverImage.url" alt="封面预览">
            <el-button text type="danger" size="small" @click="editAlbum.coverImage = null; editAlbum.coverImageId = null">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <div v-else class="cover-picker">
            <p>从你的作品中选择封面：</p>
            <div class="cover-option-grid">
              <button
                v-for="img in images"
                :key="img.id"
                type="button"
                class="cover-option"
                :class="{ active: editAlbum.coverImageId === img.id }"
                @click="editAlbum.coverImage = img; editAlbum.coverImageId = img.id"
              >
                <img :src="img.url" :alt="imageTitle(img)" loading="lazy">
              </button>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditAlbumDialog = false">取消</el-button>
        <el-button type="primary" :loading="albumEditSaving" :disabled="!editAlbum.title.trim()" @click="saveAlbumEdit">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- Album image description edit dialog -->
    <el-dialog v-model="albumDescDialogVisible" title="编辑作品集图片描述" width="480px">
      <el-input
        v-model="albumDescEditValue"
        type="textarea"
        :rows="4"
        placeholder="输入这张图片在作品集中的描述..."
        maxlength="500"
        show-word-limit
      />
      <template #footer>
        <el-button @click="albumDescDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="albumDescSaving" @click="saveAlbumImageDescription">保存</el-button>
      </template>
    </el-dialog>

    <!-- Add image to album dialog -->
    <el-dialog v-model="showAddImageDialog" title="添加图片到作品集" width="640px">
      <p class="add-image-hint">选择要添加到「{{ selectedAlbum?.title }}」的图片：</p>
      <div class="add-image-grid">
        <button
          v-for="img in availableImagesForAlbum"
          :key="img.id"
          type="button"
          class="add-image-option"
          :class="{ selected: imagesToAdd.includes(img.id) }"
          @click="toggleImageToAdd(img.id)"
        >
          <img :src="img.url" :alt="imageTitle(img)" loading="lazy">
          <span v-if="imagesToAdd.includes(img.id)" class="add-check">
            <el-icon><Check /></el-icon>
          </span>
        </button>
      </div>
      <div v-if="!availableImagesForAlbum.length" class="soft-empty small">
        没有可添加的图片。
      </div>
      <template #footer>
        <el-button @click="showAddImageDialog = false">取消</el-button>
        <el-button type="primary" :loading="addingImages" :disabled="!imagesToAdd.length" @click="handleAddImages">
          添加 ({{ imagesToAdd.length }})
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton, ElIcon, ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Check,
  Delete,
  Edit,
  EditPen,
  Files,
  FolderOpened,
  Lock,
  MoreFilled,
  Picture,
  Plus,
  Setting,
  Star,
  TrendCharts,
  Unlock,
  Upload,
  User,
  View
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { useUserStore } from '@/store/user'
import { getUserStats } from '@/api/auth'
import { uploadImage, getUserImages, deleteImage, updateImageVisibility, updateImageDescription } from '@/api/image'
import { getFollowingCreators, getUserCollections, getUserLikes } from '@/api/community'
import { unfollowUser } from '@/api/social'
import {
  createAlbum,
  getAlbums,
  getAlbumDetail,
  updateAlbum,
  deleteAlbum,
  addImageToAlbum,
  removeImageFromAlbum,
  updateAlbumImageDescription,
  reorderAlbumImages
} from '@/api/album'

const ContentFeed = defineComponent({
  name: 'ContentFeed',
  props: {
    items: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
    emptyTitle: { type: String, default: '暂无内容' },
    emptyDesc: { type: String, default: '' },
    actionText: { type: String, default: '去看看' }
  },
  emits: ['item-click', 'empty-action'],
  setup(props, { emit }) {
    const title = (item) => item.title || item.original_name || item.filename || '未命名作品'
    const format = (value) => Number(value || 0).toLocaleString('zh-CN')
    return () => h('div', { class: 'feed-shell compact-feed', directives: [] }, [
      props.items.length
        ? h('div', { class: 'masonry-grid' }, props.items.map(item => h('article', {
            class: 'work-card',
            key: item.id
          }, [
            h('button', {
              class: 'work-cover',
              type: 'button',
              onClick: () => emit('item-click', item.id)
            }, [
              h('img', { src: item.url, alt: title(item), loading: 'lazy' })
            ]),
            h('div', { class: 'work-meta' }, [
              h('div', null, [
                h('h3', { title: title(item) }, title(item)),
                h('p', null, item.author_name ? `by ${item.author_name}` : '来自社区')
              ])
            ]),
            h('div', { class: 'metric-row' }, [
              h('span', null, [h(ElIcon, null, () => h(View)), format(item.view_count)]),
              h('span', null, [h(ElIcon, null, () => h(Star)), format(item.like_count)]),
              h('span', null, [h(ElIcon, null, () => h(FolderOpened)), format(item.collect_count)])
            ])
          ])))
        : h('div', { class: 'empty-panel' }, [
            h(ElIcon, null, () => h(Picture)),
            h('h3', null, props.emptyTitle),
            h('p', null, props.emptyDesc),
            h(ElButton, { type: 'primary', onClick: () => emit('empty-action') }, () => props.actionText)
          ])
    ])
  }
})

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo || {})

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const fileInput = ref(null)
const activeTab = ref('works')
const filterType = ref('all')

const images = ref([])
const collections = ref([])
const likes = ref([])
const followingCreators = ref([])
const hotWorks = ref([])
const trendData = ref([])
const insightStats = ref({})

const loadingImages = ref(false)
const loadingCollections = ref(false)
const loadingLikes = ref(false)
const loadingFollowing = ref(false)
const uploading = ref(false)
const unfollowingId = ref(null)

const trendChartRef = ref(null)
let trendChart = null

// Description editing
const descDialogVisible = ref(false)
const descEditValue = ref('')
const descSaving = ref(false)
let descEditTarget = null

// Album state
const albums = ref([])
const loadingAlbums = ref(false)
const selectedAlbum = ref(null)
const showCreateAlbumDialog = ref(false)
const showEditAlbumDialog = ref(false)
const albumSaving = ref(false)
const albumEditSaving = ref(false)
const newAlbum = ref({
  title: '',
  description: '',
  coverImage: null
})
const editAlbum = ref({
  id: null,
  title: '',
  description: '',
  coverImage: null,
  coverImageId: null
})

// Album image description editing
const albumDescDialogVisible = ref(false)
const albumDescEditValue = ref('')
const albumDescSaving = ref(false)
let albumDescEditTarget = null

// Add image to album
const showAddImageDialog = ref(false)
const imagesToAdd = ref([])
const addingImages = ref(false)

const avatarText = computed(() => {
  const source = userInfo.value.nickname || userInfo.value.username || 'U'
  return source.charAt(0).toUpperCase()
})

const toNumber = (value) => Number(value || 0)

const formatNumber = (value) => {
  const num = toNumber(value)
  if (num >= 10000) return `${(num / 10000).toFixed(1)}w`
  if (num >= 1000) return `${(num / 1000).toFixed(1)}k`
  return String(num)
}

const imageTitle = (image = {}) => image.title || image.original_name || image.filename || '未命名作品'

const isPublic = (image = {}) => image.is_public === true || image.is_public === 1 || image.is_public === '1'

const asList = (res) => Array.isArray(res) ? res : (res?.list || [])

const albumCoverUrl = (album = {}) => album.cover_url || album.cover_thumbnail_url || ''

const albumImageDescription = (image = {}) => image.album_description || ''

const albumPayload = (form = {}) => ({
  title: form.title.trim(),
  description: form.description?.trim() || '',
  coverImageId: form.coverImage?.id ?? form.coverImageId ?? null
})

const normalizeStats = (stats = {}) => ({
  works: toNumber(stats.works ?? stats.imageCount),
  publicWorks: toNumber(stats.publicImageCount),
  receivedLikes: toNumber(stats.likes ?? stats.receivedLikeCount),
  receivedCollects: toNumber(stats.collects ?? stats.receivedCollectCount),
  views: toNumber(stats.views ?? stats.viewCount),
  likedWorks: toNumber(stats.likeCount),
  collectedWorks: toNumber(stats.collectionCount)
})

const normalizeTrend = (trend = []) => trend.map(item => ({
  label: item.label || item.date || '',
  views: toNumber(item.views),
  likes: toNumber(item.likes),
  collects: toNumber(item.collects),
  works: toNumber(item.works)
}))

const normalizeFollowingCreator = (creator = {}) => ({
  id: creator.id || creator.user_id || creator.author_id,
  name: creator.nickname || creator.username || creator.name || '创作者',
  avatar: creator.avatar || creator.author_avatar || '',
  followedAt: creator.followed_at || creator.created_at,
  workCount: toNumber(creator.work_count),
  likeCount: toNumber(creator.like_count),
  collectCount: toNumber(creator.collect_count)
})

const heroStats = computed(() => [
  { label: '作品', value: insightStats.value.works ?? images.value.length },
  { label: '公开', value: insightStats.value.publicWorks ?? images.value.filter(isPublic).length },
  { label: '获赞', value: insightStats.value.receivedLikes },
  { label: '收藏', value: insightStats.value.receivedCollects },
  { label: '浏览', value: insightStats.value.views }
])

const filteredImages = computed(() => {
  if (filterType.value === 'public') return images.value.filter(isPublic)
  if (filterType.value === 'private') return images.value.filter(image => !isPublic(image))
  return images.value
})

const formatDate = (dateStr) => {
  if (!dateStr) return '未知时间'
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric'
  })
}

const fetchImages = async () => {
  loadingImages.value = true
  try {
    const res = await getUserImages({ pageSize: 80 })
    images.value = res.list || []
  } catch (error) {
    console.error('获取图片列表失败:', error)
  } finally {
    loadingImages.value = false
  }
}

const fetchStats = async () => {
  try {
    const res = await getUserStats({ days: 30 })
    insightStats.value = normalizeStats(res)
    trendData.value = normalizeTrend(res.trend || [])
    hotWorks.value = res.hotWorks || []
    await nextTick()
    renderTrendChart()
  } catch (error) {
    console.error('获取统计数据失败:', error)
    insightStats.value = normalizeStats({})
    trendData.value = []
    hotWorks.value = []
  }
}

const fetchCollections = async () => {
  loadingCollections.value = true
  try {
    const res = await getUserCollections({ pageSize: 50 })
    collections.value = res.list || []
  } catch (error) {
    console.error('获取收藏列表失败:', error)
  } finally {
    loadingCollections.value = false
  }
}

const fetchLikes = async () => {
  loadingLikes.value = true
  try {
    const res = await getUserLikes({ pageSize: 50 })
    likes.value = res.list || []
  } catch (error) {
    console.error('获取点赞列表失败:', error)
  } finally {
    loadingLikes.value = false
  }
}

const fetchFollowingCreators = async () => {
  loadingFollowing.value = true
  try {
    const res = await getFollowingCreators()
    followingCreators.value = (res || [])
      .map(normalizeFollowingCreator)
      .filter(creator => creator.id)
  } catch (error) {
    console.error('获取关注作者失败:', error)
    ElMessage.error('获取关注作者失败')
  } finally {
    loadingFollowing.value = false
  }
}

const fetchInitialData = async () => {
  await Promise.all([
    fetchImages(),
    fetchStats(),
    fetchCollections(),
    fetchLikes(),
    fetchFollowingCreators(),
    fetchAlbums()
  ])
}

const handleTabChange = async (tab) => {
  if (tab === 'insights') {
    await nextTick()
    renderTrendChart()
  } else if (tab === 'albums') {
    fetchAlbums()
  } else if (tab === 'following' && !followingCreators.value.length) {
    fetchFollowingCreators()
  }
}

const handleUpload = () => {
  fileInput.value?.click()
}

const onFileSelected = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    event.target.value = ''
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    event.target.value = ''
    return
  }

  uploading.value = true
  try {
    await uploadImage(file)
    ElMessage.success('上传成功')
    await Promise.all([fetchImages(), fetchStats()])
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

const viewImage = (image) => {
  if (image?.id) router.push(`/post/${image.id}`)
}

const openInWorkbench = (image) => {
  if (image?.url) {
    localStorage.setItem('pixel_lab_workbench_image', image.url)
  }
  router.push('/workbench')
}

const handleImageCommand = (image, command) => {
  if (command === 'view') viewImage(image)
  if (command === 'visibility') toggleVisibility(image)
  if (command === 'workbench') openInWorkbench(image)
  if (command === 'delete') confirmDelete(image)
}

const toggleVisibility = async (image) => {
  try {
    const nextPublic = !isPublic(image)
    await updateImageVisibility(image.id, nextPublic)
    image.is_public = nextPublic ? 1 : 0
    await fetchStats()
    ElMessage.success(nextPublic ? '已设为公开' : '已设为私有')
  } catch (error) {
    console.error('更新可见性失败:', error)
    ElMessage.error('操作失败')
  }
}

const confirmDelete = (image) => {
  ElMessageBox.confirm(
    `确定删除「${imageTitle(image)}」吗？此操作不可恢复。`,
    '确认删除',
    {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteImage(image.id)
      ElMessage.success('删除成功')
      await Promise.all([fetchImages(), fetchStats()])
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const goToCommunity = (imageId) => {
  if (imageId) {
    router.push(`/post/${imageId}`)
  } else {
    router.push('/community')
  }
}

const openFollowingCreator = (creator) => {
  if (creator?.id) router.push(`/user/${creator.id}`)
}

const handleUnfollowCreator = async (creator) => {
  if (!creator?.id) return
  unfollowingId.value = creator.id
  try {
    await unfollowUser(creator.id)
    followingCreators.value = followingCreators.value.filter(item => item.id !== creator.id)
    ElMessage.success('已取消关注')
  } catch (error) {
    console.error('取消关注失败:', error)
    ElMessage.error('取消关注失败')
  } finally {
    unfollowingId.value = null
  }
}

// Description editing functions
const openDescriptionEdit = (image) => {
  descEditTarget = image
  descEditValue.value = image.description || ''
  descDialogVisible.value = true
}

const saveDescription = async () => {
  if (!descEditTarget) return
  descSaving.value = true
  try {
    await updateImageDescription(descEditTarget.id, descEditValue.value)
    descEditTarget.description = descEditValue.value
    ElMessage.success('说明已更新')
    descDialogVisible.value = false
  } catch (error) {
    console.error('更新说明失败:', error)
    ElMessage.error('更新失败')
  } finally {
    descSaving.value = false
  }
}

// Album functions
const fetchAlbums = async () => {
  loadingAlbums.value = true
  try {
    const res = await getAlbums({ pageSize: 50 })
    albums.value = asList(res)
  } catch (error) {
    console.error('获取作品集失败:', error)
  } finally {
    loadingAlbums.value = false
  }
}

const openAlbumDetail = async (album) => {
  try {
    const res = await getAlbumDetail(album.id)
    selectedAlbum.value = res
  } catch (error) {
    console.error('获取作品集详情失败:', error)
    ElMessage.error('获取详情失败')
  }
}

const handleCreateAlbum = async () => {
  if (!newAlbum.value.title.trim()) return
  albumSaving.value = true
  try {
    const data = albumPayload(newAlbum.value)
    await createAlbum(data)
    await fetchAlbums()
    ElMessage.success('作品集创建成功')
    showCreateAlbumDialog.value = false
    newAlbum.value = { title: '', description: '', coverImage: null }
  } catch (error) {
    console.error('创建作品集失败:', error)
    ElMessage.error('创建失败')
  } finally {
    albumSaving.value = false
  }
}

const openAlbumEdit = (album = selectedAlbum.value) => {
  if (!album) return
  editAlbum.value = {
    id: album.id,
    title: album.title || '',
    description: album.description || '',
    coverImage: null,
    coverImageId: album.cover_image_id || null
  }
  showEditAlbumDialog.value = true
}

const saveAlbumEdit = async () => {
  if (!editAlbum.value.id || !editAlbum.value.title.trim()) return
  albumEditSaving.value = true
  try {
    await updateAlbum(editAlbum.value.id, albumPayload(editAlbum.value))
    ElMessage.success('作品集已更新')
    showEditAlbumDialog.value = false
    await fetchAlbums()
    if (selectedAlbum.value?.id === editAlbum.value.id) {
      selectedAlbum.value = await getAlbumDetail(editAlbum.value.id)
    }
  } catch (error) {
    console.error('更新作品集失败:', error)
    ElMessage.error('更新失败')
  } finally {
    albumEditSaving.value = false
  }
}

const confirmDeleteAlbum = (album) => {
  if (!album?.id) return
  ElMessageBox.confirm(
    `确定删除「${album.title}」吗？作品集内的图片不会被删除。`,
    '确认删除',
    {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteAlbum(album.id)
      albums.value = albums.value.filter(item => item.id !== album.id)
      if (selectedAlbum.value?.id === album.id) selectedAlbum.value = null
      ElMessage.success('作品集已删除')
    } catch (error) {
      console.error('删除作品集失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const handleAlbumCommand = (album, command) => {
  if (command === 'open') openAlbumDetail(album)
  if (command === 'edit') openAlbumEdit(album)
  if (command === 'delete') confirmDeleteAlbum(album)
}

const confirmRemoveImage = (img) => {
  ElMessageBox.confirm(
    `确定将此图片从作品集中移除吗？`,
    '确认移除',
    {
      confirmButtonText: '移除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await removeImageFromAlbum(selectedAlbum.value.id, img.id)
      selectedAlbum.value.images = selectedAlbum.value.images.filter(i => i.id !== img.id)
      await fetchAlbums()
      ElMessage.success('已移除')
    } catch (error) {
      console.error('移除图片失败:', error)
      ElMessage.error('移除失败')
    }
  }).catch(() => {})
}

// Album image description editing
const openAlbumImageDescEdit = (img) => {
  albumDescEditTarget = img
  albumDescEditValue.value = albumImageDescription(img)
  albumDescDialogVisible.value = true
}

const saveAlbumImageDescription = async () => {
  if (!albumDescEditTarget || !selectedAlbum.value) return
  albumDescSaving.value = true
  try {
    await updateAlbumImageDescription(selectedAlbum.value.id, albumDescEditTarget.id, albumDescEditValue.value)
    albumDescEditTarget.album_description = albumDescEditValue.value
    ElMessage.success('描述已更新')
    albumDescDialogVisible.value = false
  } catch (error) {
    console.error('更新描述失败:', error)
    ElMessage.error('更新失败')
  } finally {
    albumDescSaving.value = false
  }
}

const setAlbumCover = async (img) => {
  if (!selectedAlbum.value || !img?.id) return
  try {
    await updateAlbum(selectedAlbum.value.id, {
      title: selectedAlbum.value.title,
      description: selectedAlbum.value.description || '',
      coverImageId: img.id
    })
    selectedAlbum.value.cover_image_id = img.id
    selectedAlbum.value.cover_url = img.url
    albums.value = albums.value.map(album => album.id === selectedAlbum.value.id
      ? { ...album, cover_image_id: img.id, cover_url: img.url }
      : album)
    ElMessage.success('封面已更新')
  } catch (error) {
    console.error('设置作品集封面失败:', error)
    ElMessage.error('设置封面失败')
  }
}

const moveAlbumImage = async (index, direction) => {
  if (!selectedAlbum.value?.images?.length) return
  const nextIndex = index + direction
  if (nextIndex < 0 || nextIndex >= selectedAlbum.value.images.length) return
  const nextImages = [...selectedAlbum.value.images]
  const [item] = nextImages.splice(index, 1)
  nextImages.splice(nextIndex, 0, item)
  const previousImages = selectedAlbum.value.images
  selectedAlbum.value.images = nextImages
  try {
    await reorderAlbumImages(selectedAlbum.value.id, nextImages.map(img => img.id))
    ElMessage.success('排序已更新')
  } catch (error) {
    selectedAlbum.value.images = previousImages
    console.error('更新作品集排序失败:', error)
    ElMessage.error('排序失败')
  }
}

// Add images to album
const availableImagesForAlbum = computed(() => {
  if (!selectedAlbum.value?.images) return images.value
  const existingIds = new Set(selectedAlbum.value.images.map(img => img.id))
  return images.value.filter(img => !existingIds.has(img.id))
})

const toggleImageToAdd = (id) => {
  const idx = imagesToAdd.value.indexOf(id)
  if (idx >= 0) {
    imagesToAdd.value.splice(idx, 1)
  } else {
    imagesToAdd.value.push(id)
  }
}

const handleAddImages = async () => {
  if (!selectedAlbum.value || !imagesToAdd.value.length) return
  addingImages.value = true
  try {
    await addImageToAlbum(selectedAlbum.value.id, { imageIds: imagesToAdd.value })
    // Refresh album detail
    const res = await getAlbumDetail(selectedAlbum.value.id)
    selectedAlbum.value = res
    await fetchAlbums()
    imagesToAdd.value = []
    showAddImageDialog.value = false
    ElMessage.success('图片已添加')
  } catch (error) {
    console.error('添加图片失败:', error)
    ElMessage.error('添加失败')
  } finally {
    addingImages.value = false
  }
}

const renderTrendChart = () => {
  if (!trendChartRef.value) return

  if (trendChart) {
    trendChart.dispose()
  }

  trendChart = echarts.init(trendChartRef.value)
  const labels = trendData.value.map(item => item.label)

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(10, 10, 10, 0.92)',
      borderColor: 'rgba(255, 255, 255, 0.12)',
      textStyle: { color: '#fff' }
    },
    legend: {
      top: 0,
      data: ['浏览', '获赞', '收藏'],
      textStyle: { color: '#a0a0a0' }
    },
    grid: {
      top: 44,
      left: 12,
      right: 16,
      bottom: 8,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.12)' } },
      axisLabel: { color: '#a0a0a0' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.08)' } },
      axisLabel: { color: '#a0a0a0' }
    },
    series: [
      {
        name: '浏览',
        type: 'line',
        smooth: true,
        data: trendData.value.map(item => item.views),
        lineStyle: { color: '#00d4ff', width: 3 },
        itemStyle: { color: '#00d4ff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0, 212, 255, 0.25)' },
            { offset: 1, color: 'rgba(0, 212, 255, 0.02)' }
          ])
        }
      },
      {
        name: '获赞',
        type: 'line',
        smooth: true,
        data: trendData.value.map(item => item.likes),
        lineStyle: { color: '#ff4f8b', width: 2 },
        itemStyle: { color: '#ff4f8b' }
      },
      {
        name: '收藏',
        type: 'line',
        smooth: true,
        data: trendData.value.map(item => item.collects),
        lineStyle: { color: '#00ff88', width: 2 },
        itemStyle: { color: '#00ff88' }
      }
    ]
  })
}

const handleResize = () => {
  trendChart?.resize()
}

watch(activeTab, async (tab) => {
  if (tab === 'insights') {
    await nextTick()
    renderTrendChart()
  } else if (tab === 'albums') {
    fetchAlbums()
  } else if (tab === 'following' && !followingCreators.value.length) {
    fetchFollowingCreators()
  }
})

watch(showAddImageDialog, (val) => {
  if (!val) imagesToAdd.value = []
})

onMounted(() => {
  fetchInitialData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  trendChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.personal-page {
  width: min(1440px, 100%);
  margin: 0 auto;
  padding: 0;
}

.profile-hero,
.toolbar-panel,
.chart-card,
.hot-card {
  border: 0;
  border-radius: var(--radius-lg);
  background: var(--background-card);
  box-shadow: var(--shadow-sm);
}

.profile-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: var(--space-6);
  padding: clamp(var(--space-6), 4vw, var(--space-10));
  overflow: hidden;
}

.hero-main {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: var(--space-6);
}

.avatar-ring {
  flex: 0 0 auto;
  padding: 5px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--primary), var(--secondary));
}

.avatar-ring :deep(.el-avatar) {
  border: 5px solid var(--background-card);
  background: var(--background-muted);
  color: var(--foreground);
  font-size: 36px;
  font-weight: 800;
}

.profile-copy {
  min-width: 0;
}

.eyebrow {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
}

.visibility-badge {
  border: 0;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.86);
  color: var(--foreground-muted);
  padding: 4px 10px;
  font-size: 12px;
  line-height: 1;
}

.profile-copy h1 {
  margin: 0;
  color: var(--foreground);
  font-size: clamp(30px, 4vw, 44px);
  line-height: 1.1;
  letter-spacing: 0;
}

.username {
  margin-top: var(--space-2);
  color: var(--foreground-muted);
  font-size: 16px;
}

.profile-desc {
  max-width: 660px;
  margin: var(--space-4) 0 0;
  color: var(--foreground-muted);
  font-size: 15px;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-top: var(--space-6);
}

.hero-side {
  display: flex;
  align-items: stretch;
}

.stat-grid {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.hero-stat {
  min-height: 78px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border: 0;
  border-radius: var(--radius-md);
  background: var(--background-muted);
  padding: var(--space-4);
}

.hero-stat:last-child {
  grid-column: span 2;
}

.stat-value {
  color: var(--foreground);
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
}

.stat-label {
  margin-top: var(--space-2);
  color: var(--foreground-muted);
  font-size: 12px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.section-title.compact {
  margin-bottom: var(--space-4);
}

.section-title h2,
.toolbar-panel h2 {
  margin: 0;
  color: var(--foreground);
  font-size: 22px;
  line-height: 1.2;
}

.work-cover img,
.hot-item img,
.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.creator-tabs {
  margin-top: var(--space-5);
}

.creator-tabs :deep(.el-tabs__header) {
  margin-bottom: var(--space-5);
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.toolbar-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
  padding: var(--space-5) var(--space-6);
}

.toolbar-panel p {
  margin-top: 4px;
  color: var(--foreground-muted);
}

.feed-shell {
  min-height: 320px;
}

.masonry-grid {
  column-count: 4;
  column-gap: var(--space-5);
}

.work-card {
  display: inline-block;
  width: 100%;
  margin: 0 0 var(--space-5);
  overflow: hidden;
  border: 0;
  border-radius: var(--radius-lg);
  background: var(--background-card);
  box-shadow: var(--shadow-sm);
  break-inside: avoid;
  transition:
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.work-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-5px);
}

.cover-shell {
  position: relative;
}

.work-cover {
  position: relative;
  display: block;
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
  border: 0;
  background: var(--background-muted);
  cursor: pointer;
}

.work-cover img {
  transition: transform var(--transition-base);
}

.work-card:hover .work-cover img {
  transform: scale(1.05);
}

.visibility-badge {
  position: absolute;
  top: var(--space-2);
  left: var(--space-2);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: var(--shadow-sm);
}

.visibility-badge.public {
  color: var(--primary);
}

.card-menu {
  position: absolute;
  top: var(--space-2);
  right: var(--space-2);
  z-index: 2;
  opacity: 0;
  transform: translateY(-2px);
  transition: opacity var(--transition-fast), transform var(--transition-fast);
}

.cover-shell:hover .card-menu,
.cover-shell:focus-within .card-menu {
  opacity: 1;
  transform: translateY(0);
}

.work-meta {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-4);
}

.work-meta > div {
  min-width: 0;
}

.work-meta h3 {
  max-width: 100%;
  overflow: hidden;
  color: var(--foreground);
  font-size: 14px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.work-meta p {
  margin-top: 4px;
  color: var(--foreground-muted);
  font-size: 12px;
}

.icon-button {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.9);
  color: var(--foreground);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition:
    color var(--transition-fast),
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.icon-button:hover {
  color: var(--primary);
  transform: translateY(-1px);
  box-shadow: var(--shadow);
}

.metric-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: 0 var(--space-4) var(--space-4);
  color: var(--foreground-muted);
  font-size: 12px;
}

.metric-row span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.compact-feed .masonry-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: var(--space-4);
}

.compact-feed .work-card {
  display: block;
  margin: 0;
}

.compact-feed .work-cover {
  aspect-ratio: 4 / 3;
}

.compact-feed .work-meta {
  padding: var(--space-3);
}

.compact-feed .metric-row {
  padding: 0 var(--space-3) var(--space-3);
}

.following-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--space-4);
}

.following-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: var(--space-4);
  align-items: center;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--background-card);
  box-shadow: var(--shadow-sm);
  padding: var(--space-5);
  cursor: pointer;
  transition:
    border-color var(--transition-fast),
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.following-card:hover {
  border-color: var(--primary);
  box-shadow: var(--shadow-md);
  transform: translateY(-3px);
}

.following-card:focus-within {
  border-color: var(--primary);
}

.following-avatar {
  align-self: flex-start;
}

.following-info {
  min-width: 0;
}

.following-info h3 {
  overflow: hidden;
  color: var(--foreground);
  font-size: 16px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.following-info p {
  margin-top: 4px;
  color: var(--foreground-muted);
  font-size: 13px;
}

.following-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: var(--space-3);
}

.following-metrics span {
  border-radius: var(--radius-full);
  background: var(--background-muted);
  color: var(--foreground-muted);
  padding: 4px 9px;
  font-size: 12px;
}

.following-actions {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
  padding-top: var(--space-2);
}

.empty-panel,
.soft-empty {
  min-height: 260px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px dashed var(--border-light);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.025);
  color: var(--foreground-muted);
  padding: var(--space-8);
  text-align: center;
}

.empty-panel .el-icon {
  margin-bottom: var(--space-4);
  color: var(--foreground-subtle);
  font-size: 52px;
}

.empty-panel h3 {
  color: var(--foreground);
  font-size: 20px;
}

.empty-panel p {
  margin: var(--space-2) 0 var(--space-5);
}

.soft-empty.small {
  min-height: 180px;
}

.insights-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.8fr);
  gap: var(--space-5);
}

.chart-card,
.hot-card {
  padding: var(--space-6);
}

.trend-chart {
  width: 100%;
  height: 360px;
}

.hot-list {
  display: grid;
  gap: var(--space-3);
}

.hot-item {
  position: relative;
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  align-items: center;
  gap: var(--space-3);
  min-height: 84px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--background-muted);
  color: var(--foreground);
  padding: var(--space-2);
  cursor: pointer;
  text-align: left;
}

.hot-item img {
  width: 72px;
  height: 72px;
  border-radius: var(--radius);
}

.hot-rank {
  position: absolute;
  top: 6px;
  left: 6px;
  border-radius: var(--radius-full);
  background: var(--primary);
  color: var(--background);
  padding: 2px 7px;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 800;
}

.hot-item strong,
.hot-item small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-item small {
  margin-top: 4px;
  color: var(--foreground-muted);
}

.preview-dialog :deep(.el-dialog__body) {
  height: min(68vh, 720px);
}

.preview-image {
  max-height: 68vh;
  object-fit: contain;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
}

/* Image description editing */
.image-desc {
  margin-top: 4px;
  overflow: hidden;
  color: var(--foreground-muted);
  font-size: 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.desc-edit-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  border: 0;
  background: none;
  color: var(--primary);
  font-size: 12px;
  cursor: pointer;
  padding: 0;
}

.desc-edit-btn:hover {
  text-decoration: underline;
}

/* Album grid */
.album-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-5);
}

.album-card {
  border: 0;
  border-radius: var(--radius-lg);
  background: var(--background-card);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  cursor: pointer;
  transition:
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.album-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-3px);
}

.album-card:focus-within {
  box-shadow: var(--shadow-md);
}

.album-detail-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-2);
}

.album-detail-actions .el-button {
  margin-left: 0;
}

.album-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background: var(--background-muted);
}

.album-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.album-cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--foreground-subtle);
  font-size: 48px;
}

.album-count {
  position: absolute;
  bottom: var(--space-2);
  right: var(--space-2);
  border-radius: var(--radius-full);
  background: rgba(0, 0, 0, 0.65);
  color: #fff;
  padding: 4px 10px;
  font-size: 12px;
}

.album-menu {
  position: absolute;
  top: var(--space-2);
  right: var(--space-2);
  opacity: 0;
  transform: translateY(-2px);
  transition: opacity var(--transition-fast), transform var(--transition-fast);
}

.album-card:hover .album-menu,
.album-card:focus-within .album-menu {
  opacity: 1;
  transform: translateY(0);
}

.album-meta {
  padding: var(--space-4);
}

.album-meta h3 {
  margin: 0;
  overflow: hidden;
  color: var(--foreground);
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.album-desc-snippet {
  margin-top: 4px;
  overflow: hidden;
  color: var(--foreground-muted);
  font-size: 13px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.album-date {
  margin-top: var(--space-2);
  color: var(--foreground-subtle);
  font-size: 12px;
}

/* Album image list */
.album-image-list {
  display: grid;
  gap: var(--space-4);
}

.album-image-item {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-4);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--background-muted);
  padding: var(--space-3);
}

.album-image-cover {
  flex: 0 0 100px;
  width: 100px;
  height: 100px;
  position: relative;
  border-radius: var(--radius);
  overflow: hidden;
  cursor: pointer;
}

.album-image-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.album-cover-badge {
  position: absolute;
  left: 6px;
  bottom: 6px;
  border-radius: var(--radius-full);
  background: rgba(0, 0, 0, 0.68);
  color: #fff;
  padding: 2px 7px;
  font-size: 11px;
}

.album-image-info {
  flex: 1;
  min-width: 0;
}

.album-image-info h4 {
  margin: 0;
  overflow: hidden;
  color: var(--foreground);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.album-image-desc {
  margin-top: 4px;
  overflow: hidden;
  color: var(--foreground-muted);
  font-size: 13px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.album-image-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-1);
  min-width: 210px;
}

/* Cover picker in album creation */
.cover-preview {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.cover-preview img {
  width: 80px;
  height: 80px;
  border-radius: var(--radius);
  object-fit: cover;
}

.cover-picker p {
  margin: 0 0 var(--space-2);
  color: var(--foreground-muted);
  font-size: 13px;
}

.cover-option-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(64px, 1fr));
  gap: var(--space-2);
  max-height: 200px;
  overflow-y: auto;
}

.cover-option {
  width: 100%;
  aspect-ratio: 1;
  border: 2px solid transparent;
  border-radius: var(--radius);
  overflow: hidden;
  background: none;
  cursor: pointer;
  padding: 0;
}

.cover-option.active {
  border-color: var(--primary);
}

.cover-option img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Add image to album */
.add-image-hint {
  margin: 0 0 var(--space-4);
  color: var(--foreground-muted);
  font-size: 14px;
}

.add-image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: var(--space-2);
  max-height: 400px;
  overflow-y: auto;
}

.add-image-option {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  border: 2px solid transparent;
  border-radius: var(--radius);
  overflow: hidden;
  background: none;
  cursor: pointer;
  padding: 0;
}

.add-image-option.selected {
  border-color: var(--primary);
}

.add-image-option img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.add-check {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  background: var(--primary);
  color: #fff;
  font-size: 12px;
}

@media (max-width: 1180px) {
  .profile-hero,
  .insights-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .personal-page {
    padding: 0;
  }

  .profile-hero {
    padding: var(--space-5);
  }

  .hero-main {
    flex-direction: column;
    align-items: flex-start;
  }

  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .masonry-grid {
    column-count: 2;
    column-gap: var(--space-3);
  }

  .hero-stat:last-child {
    grid-column: span 2;
  }

  .toolbar-panel,
  .section-title {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-panel > *,
  .album-detail-actions {
    width: 100%;
  }

  .album-detail-actions {
    justify-content: flex-start;
  }

  .trend-chart {
    height: 300px;
  }

  .album-grid {
    grid-template-columns: 1fr;
  }

  .album-image-item {
    grid-template-columns: 84px minmax(0, 1fr);
    align-items: start;
  }

  .album-image-cover {
    width: 84px;
    height: 84px;
    flex-basis: 84px;
  }

  .album-image-actions {
    grid-column: 1 / -1;
    min-width: 0;
    justify-content: flex-start;
  }
}

@media (max-width: 520px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }

  .masonry-grid {
    column-count: 1;
  }

  .hero-stat:last-child {
    grid-column: auto;
  }
}
</style>
