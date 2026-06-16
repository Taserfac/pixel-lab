export const IMAGE_CATEGORIES = [
  { label: '手绘', value: 'drawing' },
  { label: '编辑作品', value: 'edit' },
  { label: 'AI 创作', value: 'ai' },
  { label: '像素画', value: 'pixel' },
  { label: '摄影', value: 'photo' },
  { label: '角色', value: 'character' },
  { label: '风景', value: 'landscape' },
  { label: '头像', value: 'avatar' },
  { label: '其他', value: 'other' }
]

export const ALL_IMAGE_CATEGORIES = [
  { label: '全部', value: '' },
  ...IMAGE_CATEGORIES
]

export const DEFAULT_IMAGE_CATEGORY = 'other'

export const getImageCategoryLabel = (value) => {
  return IMAGE_CATEGORIES.find(category => category.value === value)?.label || '未分类'
}
