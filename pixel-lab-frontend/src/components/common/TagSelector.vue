<template>
  <div class="tag-selector">
    <!-- 已选标签胶囊 -->
    <div class="tag-selector__selected" v-if="modelValue.length > 0">
      <el-tag
        v-for="tag in modelValue"
        :key="tag.id"
        closable
        size="default"
        class="tag-selector__chip"
        @close="removeTag(tag)"
      >
        #{{ tag.name }}
      </el-tag>
    </div>

    <!-- 输入区域 -->
    <div class="tag-selector__input-wrap" :class="{ 'is-disabled': isMaxReached }">
      <el-input
        v-model="keyword"
        :placeholder="isMaxReached ? '最多只能选择3个标签' : placeholder"
        :disabled="isMaxReached"
        clearable
        @input="onInput"
        @focus="onFocus"
        @blur="onBlur"
        @clear="onClear"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <!-- 下拉推荐列表 -->
      <div
        v-show="showDropdown && (suggestions.length > 0 || loading || noResult)"
        class="tag-selector__dropdown"
      >
        <div v-if="loading" class="tag-selector__status">
          <el-icon class="is-loading"><Loading /></el-icon>
          搜索中...
        </div>
        <div v-else-if="noResult" class="tag-selector__status">
          无相关标签
        </div>
        <template v-else>
          <div
            v-for="tag in suggestions"
            :key="tag.id"
            class="tag-selector__option"
            :class="{ 'is-selected': isSelected(tag) }"
            @mousedown.prevent="selectTag(tag)"
          >
            <span class="tag-selector__option-name">#{{ tag.name }}</span>
            <span class="tag-selector__option-count">{{ tag.usage_count }}次使用</span>
          </div>
        </template>
      </div>
    </div>

    <!-- 限制提示 -->
    <div v-if="isMaxReached" class="tag-selector__limit-hint">
      <el-icon><InfoFilled /></el-icon>
      最多只能选择{{ maxTags }}个标签，删除已有标签后可重新添加
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { Search, Loading, InfoFilled } from '@element-plus/icons-vue'
import { useTagStore } from '@/store/tag'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  maxTags: {
    type: Number,
    default: 3
  },
  placeholder: {
    type: String,
    default: '搜索标签...'
  }
})

const emit = defineEmits(['update:modelValue'])

const tagStore = useTagStore()
const keyword = ref('')
const suggestions = ref([])
const loading = ref(false)
const showDropdown = ref(false)
const noResult = ref(false)
let searchTimer = null

// 是否达到上限
const isMaxReached = computed(() => props.modelValue.length >= props.maxTags)

// 是否已选
const isSelected = (tag) => {
  return props.modelValue.some(t => t.id === tag.id)
}

// 输入事件 — 防抖搜索
const onInput = () => {
  if (searchTimer) clearTimeout(searchTimer)
  if (!keyword.value.trim()) {
    suggestions.value = []
    noResult.value = false
    return
  }
  loading.value = true
  showDropdown.value = true
  searchTimer = setTimeout(async () => {
    try {
      const results = await tagStore.search(keyword.value, 20)
      // 过滤已选标签
      suggestions.value = results.filter(t => !isSelected(t))
      noResult.value = suggestions.value.length === 0
    } catch {
      suggestions.value = []
      noResult.value = true
    } finally {
      loading.value = false
    }
  }, 200)
}

// 聚焦时显示热门标签
const onFocus = async () => {
  if (isMaxReached.value) return
  showDropdown.value = true
  if (!keyword.value.trim() && suggestions.value.length === 0) {
    loading.value = true
    try {
      const results = await tagStore.search('', 20)
      suggestions.value = results.filter(t => !isSelected(t))
      noResult.value = false
    } catch {
      suggestions.value = []
    } finally {
      loading.value = false
    }
  }
}

// 失焦隐藏下拉
const onBlur = () => {
  setTimeout(() => {
    showDropdown.value = false
  }, 150)
}

// 清空输入
const onClear = () => {
  suggestions.value = []
  noResult.value = false
}

// 选择标签
const selectTag = (tag) => {
  if (isSelected(tag) || isMaxReached.value) return
  const newList = [...props.modelValue, { id: tag.id, name: tag.name }]
  emit('update:modelValue', newList)
  keyword.value = ''
  suggestions.value = []
  noResult.value = false
}

// 删除标签
const removeTag = (tag) => {
  const newList = props.modelValue.filter(t => t.id !== tag.id)
  emit('update:modelValue', newList)
}

// 预加载标签列表
onMounted(() => {
  tagStore.fetchAllTags()
})

// 当删除标签后恢复输入
watch(isMaxReached, (val) => {
  if (!val && showDropdown.value) {
    onInput()
  }
})
</script>

<style scoped>
.tag-selector {
  width: 100%;
}

.tag-selector__selected {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.tag-selector__chip {
  border-radius: 16px;
  font-size: 13px;
}

.tag-selector__input-wrap {
  position: relative;
}

.tag-selector__input-wrap.is-disabled {
  opacity: 0.6;
}

.tag-selector__dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 100;
  background: var(--el-bg-color, #fff);
  border: 1px solid var(--el-border-color-light, #e4e7ed);
  border-radius: 8px;
  margin-top: 4px;
  max-height: 240px;
  overflow-y: auto;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.tag-selector__status {
  padding: 12px 16px;
  text-align: center;
  color: var(--el-text-color-secondary, #909399);
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.tag-selector__option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.tag-selector__option:hover {
  background: var(--el-fill-color-light, #f5f7fa);
}

.tag-selector__option.is-selected {
  opacity: 0.5;
  cursor: not-allowed;
}

.tag-selector__option-name {
  font-size: 14px;
  color: var(--el-text-color-primary, #303133);
}

.tag-selector__option-count {
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
}

.tag-selector__limit-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-color-warning, #e6a23c);
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
