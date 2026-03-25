import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

export function useImageEditor() {
  const canvas = ref(null)
  const canvasWrapper = ref(null)
  const originalImage = ref(null)
  const currentImage = ref(null)
  const thumbnailUrl = ref('')

  // 滤镜选项
  const filters = [
    { label: '原图', value: 'none' },
    { label: '黑白', value: 'grayscale' },
    { label: '复古', value: 'sepia' },
    { label: '反转', value: 'invert' },
    { label: '模糊', value: 'blur' },
    { label: '锐化', value: 'sharpen' },
    { label: '暖色', value: 'warm' },
    { label: '冷色', value: 'cool' }
  ]
  const currentFilter = ref('none')

  // 参数调整
  const adjustments = reactive({
    brightness: 0,
    contrast: 0,
    saturate: 0
  })

  // 像素画参数
  const pixelSize = ref(8)
  const colorCount = ref(32)
  const pixelPreviewApplied = ref(false)

  // 加载图片
  const loadImage = (url, name) => {
    return new Promise((resolve, reject) => {
      const img = new Image()
      img.crossOrigin = 'anonymous'
      img.onload = () => {
        originalImage.value = img
        currentImage.value = { url, name }
        thumbnailUrl.value = url
        resolve()
      }
      img.onerror = () => {
        ElMessage.error('图片加载失败')
        reject()
      }
      img.src = url
    })
  }

  // 初始化画布
  const initCanvas = () => {
    if (!canvas.value || !originalImage.value) return

    const wrapper = canvasWrapper.value
    const maxWidth = Math.max(wrapper.clientWidth - 20, 400)
    const maxHeight = Math.max(wrapper.clientHeight - 20, 300)

    let width = originalImage.value.width
    let height = originalImage.value.height

    const ratio = Math.min(maxWidth / width, maxHeight / height)

    if (ratio < 1) {
      width = width * ratio
      height = height * ratio
    }

    width = Math.max(width, 200)
    height = Math.max(height, 150)

    canvas.value.width = width
    canvas.value.height = height

    drawImage()
  }

  // 绘制图片
  const drawImage = () => {
    if (!canvas.value || !originalImage.value) return

    const ctx = canvas.value.getContext('2d')
    ctx.clearRect(0, 0, canvas.value.width, canvas.value.height)
    ctx.drawImage(
      originalImage.value,
      0, 0,
      originalImage.value.width,
      originalImage.value.height,
      0, 0,
      canvas.value.width,
      canvas.value.height
    )
  }

  // 获取滤镜预览样式
  const getFilterPreviewStyle = (filterValue) => {
    let filter = ''

    switch (filterValue) {
    case 'grayscale':
      filter = 'grayscale(100%) '
      break
    case 'sepia':
      filter = 'sepia(100%) '
      break
    case 'invert':
      filter = 'invert(100%) '
      break
    case 'blur':
      filter = 'blur(2px) '
      break
    case 'warm':
      filter = 'sepia(30%) saturate(140%) '
      break
    case 'cool':
      filter = 'saturate(80%) hue-rotate(20deg) '
      break
    }

    const brightness = 100 + adjustments.brightness
    const contrast = 100 + adjustments.contrast
    const saturate = 100 + adjustments.saturate

    filter += `brightness(${brightness}%) contrast(${contrast}%) saturate(${saturate}%)`

    return { filter }
  }

  // 重置调整参数
  const resetAdjustments = () => {
    adjustments.brightness = 0
    adjustments.contrast = 0
    adjustments.saturate = 0
    currentFilter.value = 'none'
    pixelPreviewApplied.value = false
  }

  // 像素化算法
  const pixelateImage = (ctx, width, height) => {
    const imageData = ctx.getImageData(0, 0, width, height)
    const data = imageData.data

    const scale = width / canvas.value.width
    const actualPixelSize = Math.max(1, Math.round(pixelSize.value * scale))
    const colorPalette = buildColorPalette(data, colorCount.value)

    for (let y = 0; y < height; y += actualPixelSize) {
      for (let x = 0; x < width; x += actualPixelSize) {
        const avgColor = getBlockAverageColor(data, x, y, actualPixelSize, width, height)
        const quantizedColor = findClosestColor(avgColor, colorPalette)

        for (let py = y; py < y + actualPixelSize && py < height; py++) {
          for (let px = x; px < x + actualPixelSize && px < width; px++) {
            const idx = (py * width + px) * 4
            data[idx] = quantizedColor.r
            data[idx + 1] = quantizedColor.g
            data[idx + 2] = quantizedColor.b
          }
        }
      }
    }

    return imageData
  }

  const buildColorPalette = (data, numColors) => {
    const colors = []
    for (let i = 0; i < data.length; i += 4) {
      colors.push({ r: data[i], g: data[i + 1], b: data[i + 2] })
    }
    const step = Math.max(1, Math.floor(colors.length / numColors))
    const palette = []
    for (let i = 0; i < colors.length && palette.length < numColors; i += step) {
      palette.push(colors[i])
    }
    return palette
  }

  const getBlockAverageColor = (data, startX, startY, blockSize, width, height) => {
    let r = 0, g = 0, b = 0, count = 0

    for (let y = startY; y < startY + blockSize && y < height; y++) {
      for (let x = startX; x < startX + blockSize && x < width; x++) {
        const idx = (y * width + x) * 4
        r += data[idx]
        g += data[idx + 1]
        b += data[idx + 2]
        count++
      }
    }

    return {
      r: Math.round(r / count),
      g: Math.round(g / count),
      b: Math.round(b / count)
    }
  }

  const findClosestColor = (color, palette) => {
    let minDist = Infinity
    let closest = palette[0]

    for (const p of palette) {
      const dist = Math.sqrt(
        Math.pow(color.r - p.r, 2) +
        Math.pow(color.g - p.g, 2) +
        Math.pow(color.b - p.b, 2)
      )
      if (dist < minDist) {
        minDist = dist
        closest = p
      }
    }

    return closest
  }

  // 应用滤镜到像素数据
  const applyFilterToData = (data) => {
    for (let i = 0; i < data.length; i += 4) {
      let r = data[i]
      let g = data[i + 1]
      let b = data[i + 2]

      if (currentFilter.value === 'grayscale') {
        const gray = 0.299 * r + 0.587 * g + 0.114 * b
        r = g = b = gray
      } else if (currentFilter.value === 'sepia') {
        const tr = 0.393 * r + 0.769 * g + 0.189 * b
        const tg = 0.349 * r + 0.686 * g + 0.168 * b
        const tb = 0.272 * r + 0.534 * g + 0.131 * b
        r = Math.min(255, tr)
        g = Math.min(255, tg)
        b = Math.min(255, tb)
      } else if (currentFilter.value === 'invert') {
        r = 255 - r
        g = 255 - g
        b = 255 - b
      } else if (currentFilter.value === 'warm') {
        r = Math.min(255, r * 1.2)
        g = Math.min(255, g * 1.1)
      } else if (currentFilter.value === 'cool') {
        b = Math.min(255, b * 1.2)
        r = r * 0.9
        g = g * 0.95
      }

      const brightness = adjustments.brightness / 100
      const contrast = (adjustments.contrast + 100) / 100
      const saturation = (adjustments.saturate + 100) / 100

      r += brightness * 255
      g += brightness * 255
      b += brightness * 255

      r = ((r / 255 - 0.5) * contrast + 0.5) * 255
      g = ((g / 255 - 0.5) * contrast + 0.5) * 255
      b = ((b / 255 - 0.5) * contrast + 0.5) * 255

      const gray = 0.299 * r + 0.587 * g + 0.114 * b
      r = gray + saturation * (r - gray)
      g = gray + saturation * (g - gray)
      b = gray + saturation * (b - gray)

      data[i] = Math.max(0, Math.min(255, r))
      data[i + 1] = Math.max(0, Math.min(255, g))
      data[i + 2] = Math.max(0, Math.min(255, b))
    }
  }

  return {
    canvas,
    canvasWrapper,
    originalImage,
    currentImage,
    thumbnailUrl,
    filters,
    currentFilter,
    adjustments,
    pixelSize,
    colorCount,
    pixelPreviewApplied,
    loadImage,
    initCanvas,
    drawImage,
    getFilterPreviewStyle,
    resetAdjustments,
    pixelateImage,
    applyFilterToData
  }
}