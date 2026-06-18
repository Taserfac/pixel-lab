<template>
  <div class="publish-page page-shell">
    <header class="publish-header">
      <div>
        <h1 class="page-heading">Publish</h1>
        <p class="page-description">补充作品信息，然后把它分享给社区。</p>
      </div>
      <span v-if="draftSaved" class="draft-status"><el-icon><CircleCheck /></el-icon>草稿已保存</span>
    </header>

    <form class="publish-layout" @submit.prevent="publish">
      <UiCard padding="lg" class="upload-panel">
        <UploadBox v-model="form.file" :max-size="8" />
        <div class="upload-hint">
          <span>建议使用宽度不低于 1200px 的图片</span>
          <UiButton v-if="form.file" variant="ghost" size="sm" @click="sendToStudio">
            <template #icon><el-icon><Brush /></el-icon></template>
            先去 Studio 编辑
          </UiButton>
        </div>
      </UiCard>

      <UiCard padding="lg" class="form-panel">
        <div class="form-heading">
          <h2>作品信息</h2>
          <span>带 * 为必填项</span>
        </div>

        <UiInput
          v-model="form.title"
          label="标题 *"
          placeholder="给作品起一个容易记住的名字"
          :maxlength="60"
          :error="errors.title"
        />

        <label class="field-group">
          <span>作品说明</span>
          <textarea v-model="form.description" rows="5" maxlength="500" placeholder="写下创作背景、灵感或使用的工具" />
          <small>{{ form.description.length }}/500</small>
        </label>

        <div class="field-group">
          <span>标签</span>
          <div class="tag-editor">
            <UiInput
              v-model="tagInput"
              placeholder="输入标签后按 Enter"
              :error="errors.tags"
              @keydown.enter.prevent="addTag"
            />
            <UiButton variant="secondary" @click="addTag">添加</UiButton>
          </div>
          <div v-if="form.tags.length" class="selected-tags">
            <UiTag v-for="tag in form.tags" :key="tag" removable @remove="removeTag(tag)">#{{ tag }}</UiTag>
          </div>
          <small>最多 5 个标签，每个不超过 12 个字</small>
        </div>

        <label class="visibility-row">
          <span>
            <strong>公开到社区</strong>
            <small>关闭后仅自己在 Profile 中可见</small>
          </span>
          <el-switch v-model="form.isPublic" />
        </label>

        <div class="publish-actions">
          <UiButton variant="secondary" :disabled="publishing" @click="saveDraft(true)">保存草稿</UiButton>
          <UiButton type="submit" :loading="publishing">
            <template #icon><el-icon><Upload /></el-icon></template>
            发布作品
          </UiButton>
        </div>
      </UiCard>
    </form>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Brush, CircleCheck, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiInput from '@/components/ui/UiInput.vue'
import UiTag from '@/components/ui/UiTag.vue'
import UploadBox from '@/components/ui/UploadBox.vue'
import { updateImageMetadata, updateImageVisibility, uploadImage } from '@/api/image'

const DRAFT_KEY = 'pixel_lab_publish_draft'
const router = useRouter()
const publishing = ref(false)
const draftSaved = ref(false)
const tagInput = ref('')
const form = reactive({ file: null, title: '', description: '', tags: [], isPublic: true })
const errors = reactive({ title: '', tags: '' })
let autosaveTimer

const serializableDraft = () => ({
  title: form.title,
  description: form.description,
  tags: form.tags,
  isPublic: form.isPublic,
  savedAt: Date.now()
})

const saveDraft = showMessage => {
  localStorage.setItem(DRAFT_KEY, JSON.stringify(serializableDraft()))
  draftSaved.value = true
  if (showMessage) ElMessage.success('草稿已保存到本机')
  window.clearTimeout(autosaveTimer)
  autosaveTimer = window.setTimeout(() => { draftSaved.value = false }, 1800)
}

const restoreDraft = () => {
  try {
    const draft = JSON.parse(localStorage.getItem(DRAFT_KEY) || 'null')
    if (!draft) return
    form.title = draft.title || ''
    form.description = draft.description || ''
    form.tags = Array.isArray(draft.tags) ? draft.tags.slice(0, 5) : []
    form.isPublic = draft.isPublic !== false
  } catch {
    localStorage.removeItem(DRAFT_KEY)
  }
}

const addTag = () => {
  const value = tagInput.value.trim().replace(/^#/, '')
  errors.tags = ''
  if (!value) return
  if (value.length > 12) {
    errors.tags = '单个标签不能超过 12 个字'
    return
  }
  if (form.tags.includes(value)) {
    tagInput.value = ''
    return
  }
  if (form.tags.length >= 5) {
    errors.tags = '最多添加 5 个标签'
    return
  }
  form.tags.push(value)
  tagInput.value = ''
}

const removeTag = tag => {
  form.tags = form.tags.filter(item => item !== tag)
  errors.tags = ''
}

const validate = () => {
  errors.title = form.title.trim() ? '' : '请填写作品标题'
  if (!form.file) ElMessage.warning('请先选择要发布的图片')
  return Boolean(form.file && !errors.title && !errors.tags)
}

const publish = async () => {
  if (!validate()) return
  publishing.value = true
  try {
    const uploaded = await uploadImage(form.file, form.description.trim())
    const imageId = uploaded?.id
    if (!imageId) throw new Error('上传结果缺少作品 ID')
    await updateImageMetadata(imageId, {
      title: form.title.trim(),
      description: form.description.trim(),
      tags: form.tags
    })
    await updateImageVisibility(imageId, form.isPublic)
    localStorage.removeItem(DRAFT_KEY)
    ElMessage.success(form.isPublic ? '作品已发布到社区' : '作品已保存为私有')
    await router.push(`/post/${imageId}`)
  } catch (error) {
    console.error('发布作品失败:', error)
    ElMessage.error('发布失败，草稿已保留')
    saveDraft(false)
  } finally {
    publishing.value = false
  }
}

const sendToStudio = () => {
  if (!form.file) return
  const reader = new FileReader()
  reader.onload = () => {
    localStorage.setItem('pixel_lab_workbench_image', reader.result)
    saveDraft(false)
    router.push('/studio')
  }
  reader.readAsDataURL(form.file)
}

watch(() => [form.title, form.description, form.tags.join(','), form.isPublic], () => {
  window.clearTimeout(autosaveTimer)
  autosaveTimer = window.setTimeout(() => saveDraft(false), 600)
})

onMounted(restoreDraft)
onBeforeUnmount(() => window.clearTimeout(autosaveTimer))
</script>

<style scoped>
.publish-page { max-width: 1180px; }

.publish-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
  margin-bottom: var(--space-8);
}

.draft-status {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  color: var(--primary-active);
  font-size: 12px;
}

.publish-layout { display: grid; grid-template-columns: minmax(0, 1.1fr) minmax(360px, 0.9fr); gap: var(--space-6); align-items: start; }
.upload-panel,
.form-panel { min-width: 0; }

.upload-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-top: var(--space-3);
  color: var(--text-tertiary);
  font-size: 12px;
}

.form-panel { display: grid; gap: var(--space-6); }
.form-heading { display: flex; align-items: baseline; justify-content: space-between; gap: var(--space-3); }
.form-heading h2 { margin: 0; font-size: 19px; }
.form-heading span { color: var(--text-tertiary); font-size: 11px; }

.field-group { display: grid; gap: var(--space-2); position: relative; }
.field-group > span { color: var(--text-primary); font-size: 13px; font-weight: 680; }
.field-group textarea {
  resize: vertical;
  min-height: 120px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: var(--space-3);
  outline: 0;
  color: var(--text-primary);
  background: var(--card);
}
.field-group textarea:focus { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-soft); }
.field-group small { color: var(--text-tertiary); font-size: 11px; }
.field-group > small { justify-self: end; }

.tag-editor { display: grid; grid-template-columns: 1fr auto; gap: var(--space-2); }
.selected-tags { display: flex; flex-wrap: wrap; gap: var(--space-2); }

.visibility-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  padding: var(--space-4) 0;
}
.visibility-row > span { display: grid; gap: 2px; }
.visibility-row strong { font-size: 13px; }
.visibility-row small { color: var(--text-tertiary); font-size: 11px; }

.publish-actions { display: flex; justify-content: flex-end; gap: var(--space-2); }

@media (max-width: 900px) {
  .publish-layout { grid-template-columns: 1fr; }
}
</style>
