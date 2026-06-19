<template>
  <div ref="root" class="stable-masonry" :style="columnStyle">
    <div v-for="(column, index) in columns" :key="index" class="masonry-column">
      <PostCard
        v-for="work in column"
        :key="work.id"
        :work="work"
        :style="{ '--post-ratio': coverRatio(work) }"
        @select="emit('select', $event)"
        @tag-click="emit('tag-click', $event)"
        @author-select="emit('author-select', $event)"
      />
      <SkeletonCard
        v-for="item in skeletonColumns[index] || []"
        :key="`skeleton-${index}-${item}`"
        :style="{ '--post-ratio': skeletonRatio(item) }"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import PostCard from '@/components/community/PostCard.vue'

const props = defineProps({
  works: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  maxColumns: {
    type: Number,
    default: 4
  },
  skeletonCount: {
    type: Number,
    default: 8
  }
})

const emit = defineEmits(['select', 'tag-click', 'author-select'])
const root = ref(null)
const columnCount = ref(Math.min(4, props.maxColumns))
let resizeObserver

const resolveColumnCount = (width) => {
  if (width < 520) return 1
  if (width < 820) return Math.min(2, props.maxColumns)
  if (width < 1160) return Math.min(3, props.maxColumns)
  return Math.min(4, props.maxColumns)
}

const estimateHeight = (work) => {
  const width = Number(work.width || 0)
  const height = Number(work.height || 0)
  const imageHeight = width > 0 && height > 0 ? height / width : 0.78
  return Math.min(Math.max(imageHeight, 0.55), 1.8) + 0.34
}

const columns = computed(() => {
  const result = Array.from({ length: columnCount.value }, () => [])
  const heights = Array(columnCount.value).fill(0)

  props.works.forEach((work) => {
    const target = heights.indexOf(Math.min(...heights))
    result[target].push(work)
    heights[target] += estimateHeight(work)
  })

  return result
})

const skeletonColumns = computed(() => {
  if (!props.loading || props.works.length) {
    return Array.from({ length: columnCount.value }, () => [])
  }
  return Array.from({ length: columnCount.value }, (_, columnIndex) => (
    Array.from({ length: Math.ceil(props.skeletonCount / columnCount.value) }, (_, itemIndex) => (
      columnIndex + itemIndex * columnCount.value
    )).filter(index => index < props.skeletonCount)
  ))
})

const columnStyle = computed(() => ({
  gridTemplateColumns: `repeat(${columnCount.value}, minmax(0, 1fr))`
}))

const coverRatio = (work) => {
  const width = Number(work.width || 0)
  const height = Number(work.height || 0)
  return width > 0 && height > 0 ? `${width} / ${height}` : '4 / 3'
}

const skeletonRatio = (index) => ['4 / 5', '1 / 1', '5 / 4', '3 / 4'][index % 4]

onMounted(() => {
  resizeObserver = new ResizeObserver(([entry]) => {
    columnCount.value = resolveColumnCount(entry.contentRect.width)
  })
  if (root.value) resizeObserver.observe(root.value)
})

onBeforeUnmount(() => resizeObserver?.disconnect())
</script>

<style scoped>
.stable-masonry {
  min-height: 360px;
  display: grid;
  align-items: start;
  gap: var(--space-5);
}

.masonry-column {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.masonry-column :deep(.post-card),
.masonry-column :deep(.skeleton-card) {
  margin: 0;
}

@media (max-width: 640px) {
  .stable-masonry,
  .masonry-column {
    gap: var(--space-3);
  }
}
</style>
