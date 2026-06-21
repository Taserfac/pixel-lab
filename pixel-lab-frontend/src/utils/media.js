export const withImageVariant = (url, variant) => {
  const value = String(url || '')
  if (!value || !variant || !value.includes('/uploads/') || /[?&]variant=/.test(value)) {
    return value
  }
  return `${value}${value.includes('?') ? '&' : '?'}variant=${encodeURIComponent(variant)}`
}

export const cardImageUrl = (work = {}) => withImageVariant(
  work.thumbnail_url || work.url || work.image_url || '',
  'card'
)

export const avatarImageUrl = (url) => withImageVariant(url, 'avatar')
