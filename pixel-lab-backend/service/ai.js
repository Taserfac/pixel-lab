const { randomUUID } = require('crypto')
const sharp = require('sharp')

const TEXT_TYPES = new Set([
  'text/plain',
  'text/markdown',
  'application/json'
])

const MODEL_CONFIGS = {
  'deepseek-v4-flash': {
    apiModel: 'deepseek-v4-flash',
    mode: 'refine',
    label: 'DeepSeek V4 Flash'
  },
  'deepseek-v4-pro': {
    apiModel: 'deepseek-v4-pro',
    mode: 'refine',
    label: 'DeepSeek V4 Pro'
  },
  'deepseek-v4-flash-draw': {
    apiModel: 'deepseek-v4-flash',
    mode: 'draw',
    label: 'DeepSeek V4 Flash Draw'
  },
  'deepseek-v4-pro-draw': {
    apiModel: 'deepseek-v4-pro',
    mode: 'draw',
    label: 'DeepSeek V4 Pro Draw'
  }
}

const DEFAULT_MODEL_BY_MODE = {
  refine: 'deepseek-v4-flash',
  draw: 'deepseek-v4-flash-draw'
}

const getDeepSeekChatUrl = () => {
  const baseUrl = (process.env.DEEPSEEK_BASE_URL || 'https://api.deepseek.com').replace(/\/+$/, '')
  return `${baseUrl}/chat/completions`
}

const getDeepSeekImageUrl = () => {
  return (process.env.DEEPSEEK_IMAGE_API_URL || '').trim()
}

const getImageProvider = () => {
  return (process.env.DEEPSEEK_IMAGE_PROVIDER || 'comfyui').trim().toLowerCase()
}

const PICO8_PALETTE = [
  [0x00, 0x00, 0x00],
  [0x1d, 0x2b, 0x53],
  [0x7e, 0x25, 0x53],
  [0x00, 0x87, 0x51],
  [0xab, 0x52, 0x36],
  [0x5f, 0x57, 0x4f],
  [0xc2, 0xc3, 0xc7],
  [0xff, 0xf1, 0xe8],
  [0xff, 0x00, 0x4d],
  [0xff, 0xa3, 0x00],
  [0xff, 0xec, 0x27],
  [0x00, 0xe4, 0x36],
  [0x29, 0xad, 0xff],
  [0x83, 0x76, 0x9c],
  [0xff, 0x77, 0xa8],
  [0xff, 0xcc, 0xaa]
]

const resolveModelConfig = ({ mode, model }) => {
  const selectedMode = mode === 'draw' ? 'draw' : 'refine'
  const modelConfig = MODEL_CONFIGS[model]

  if (modelConfig && modelConfig.mode === selectedMode) {
    return {
      id: model,
      ...modelConfig
    }
  }

  const fallbackModel = DEFAULT_MODEL_BY_MODE[selectedMode]
  return {
    id: fallbackModel,
    ...MODEL_CONFIGS[fallbackModel]
  }
}

const readImageMetadata = async (file) => {
  if (!file || !file.mimetype?.startsWith('image/')) {
    return null
  }

  try {
    return await sharp(file.buffer).metadata()
  } catch (error) {
    return null
  }
}

const describeImageFile = async (file, label) => {
  if (!file || !file.mimetype?.startsWith('image/')) {
    return null
  }

  const metadata = await readImageMetadata(file)
  const size = metadata?.width && metadata?.height
    ? `${metadata.width}x${metadata.height}`
    : 'unknown size'

  return `${label}: ${file.originalname || 'image'} (${file.mimetype}, ${size})`
}

const imageFileToPayload = async (file) => {
  if (!file || !file.mimetype?.startsWith('image/')) {
    return null
  }

  const metadata = await readImageMetadata(file)
  return {
    name: file.originalname || 'image.png',
    mimeType: file.mimetype || 'image/png',
    width: metadata?.width || null,
    height: metadata?.height || null,
    base64: file.buffer.toString('base64'),
    dataUrl: `data:${file.mimetype || 'image/png'};base64,${file.buffer.toString('base64')}`
  }
}

const collectContextNotes = async ({ currentImage, attachments }) => {
  const textNotes = []
  const imageNotes = []
  const fileNotes = []

  const canvasNote = await describeImageFile(currentImage, '当前画布')
  if (canvasNote) {
    imageNotes.push(canvasNote)
  }

  for (const file of attachments) {
    if (TEXT_TYPES.has(file.mimetype)) {
      const text = file.buffer.toString('utf8').slice(0, 8000)
      textNotes.push(`File: ${file.originalname}\n${text}`)
      continue
    }

    if (file.mimetype?.startsWith('image/')) {
      const imageNote = await describeImageFile(file, '参考图片')
      if (imageNote) {
        imageNotes.push(imageNote)
      }
      continue
    }

    fileNotes.push(`${file.originalname} (${file.mimetype})`)
  }

  return { textNotes, imageNotes, fileNotes }
}

const appendContextNotes = (parts, { textNotes, imageNotes, fileNotes }) => {
  if (imageNotes.length > 0) {
    parts.push(`图像素材:\n${imageNotes.join('\n')}`)
  }

  if (textNotes.length > 0) {
    parts.push(`参考文本:\n${textNotes.join('\n\n')}`)
  }

  if (fileNotes.length > 0) {
    parts.push(`其他参考文件: ${fileNotes.join(', ')}`)
  }
}

const buildRefinePrompt = ({ prompt, contextNotes }) => {
  const parts = [
    '用户正在手绘板里创作图片，需要你根据用户要求给出可执行的润色建议。',
    '工作目标：保持原始构图、主体站位和大比例不变，提升画面完成度、线条、颜色、光影和材质表现。',
    '如果用户上传了参考图片或文件，请围绕这些素材做关联建议；不要要求用户重新描述已经提供的素材。',
    `用户润色要求: ${prompt}`
  ]

  appendContextNotes(parts, contextNotes)

  parts.push([
    '请用中文返回，结构如下:',
    '1. 整体润色方向',
    '2. 线条/颜色/光影建议',
    '3. 可直接复制的图片润色提示词'
  ].join('\n'))

  return parts.join('\n\n')
}

const buildDrawPrompt = ({ prompt, contextNotes, canGenerateImage }) => {
  const parts = [
    '用户想基于手绘板当前画布和可选参考素材进行二次创作。',
    canGenerateImage
      ? '工作目标：输出完整二次创作方案，并给出适合图片生成服务直接使用的正向提示词和负面提示词。'
      : '工作目标：输出完整二次创作方案，并给出可复制给绘图模型使用的正向提示词和负面提示词。',
    '如果用户上传了参考图片或文件，请围绕这些素材做关联创作；不要要求用户重新描述已经提供的素材。',
    `用户创作要求: ${prompt}`
  ]

  appendContextNotes(parts, contextNotes)

  parts.push([
    '请用中文返回，结构如下:',
    '1. 二次创作方向',
    '2. 构图与主体',
    '3. 风格、颜色、光影',
    '4. 可直接复制的绘图提示词',
    '5. 负面提示词'
  ].join('\n'))

  return parts.join('\n\n')
}

const extractPromptParts = (text) => {
  const source = text.trim()
  const codeBlocks = [...source.matchAll(/```(?:[a-zA-Z0-9_-]+)?\s*([\s\S]*?)```/g)]
    .map((match) => match[1].trim())
    .filter(Boolean)

  const negativeMatch = source.match(/(?:负面提示词|negative prompt)[:：]?\s*(?:```(?:[a-zA-Z0-9_-]+)?\s*([\s\S]*?)```|([\s\S]*?))(?=\n\s*(?:-{3,}|#{1,6}\s|\d+\.|$))/i)
  const negative = (negativeMatch?.[1] || negativeMatch?.[2] || '').trim()

  const positive = codeBlocks.find((block) => block !== negative) ||
    source.match(/(?:可直接复制的(?:绘图|图片润色)?提示词|正向提示词)[:：]?\s*([\s\S]*?)(?=\n\s*(?:\d+\.|#{1,6}\s|负面提示词|negative prompt|$))/i)?.[1]?.trim() ||
    source

  return {
    positive: positive.slice(0, 5000),
    negative: negative.slice(0, 2000)
  }
}

const normalizeImageUrl = (value) => {
  if (!value) return ''

  if (typeof value === 'string') {
    const trimmedValue = value.trim()
    if (!trimmedValue) return ''
    if (/^(https?:|data:image\/)/i.test(trimmedValue)) return trimmedValue
    if (/^[A-Za-z0-9+/=\s]+$/.test(trimmedValue) && trimmedValue.length > 200) {
      return `data:image/png;base64,${trimmedValue.replace(/\s/g, '')}`
    }
    return ''
  }

  if (typeof value === 'object') {
    return normalizeImageUrl(
      value.imageUrl ||
      value.image_url ||
      value.url ||
      value.b64_json ||
      value.base64 ||
      value.data
    )
  }

  return ''
}

const pickGeneratedImageUrl = (payload) => {
  return normalizeImageUrl(payload?.imageUrl) ||
    normalizeImageUrl(payload?.image_url) ||
    normalizeImageUrl(payload?.url) ||
    normalizeImageUrl(payload?.output) ||
    normalizeImageUrl(payload?.image) ||
    normalizeImageUrl(payload?.data?.[0]) ||
    normalizeImageUrl(payload?.images?.[0]) ||
    normalizeImageUrl(payload?.artifacts?.[0])
}

const isPixelArtRequest = (prompt = '') => {
  return /pixel\s*art|pico-?8|nearest\s*neighbor|16-?color|downscale\s+to/i.test(prompt)
}

const parseSizeFromPrompt = ({ prompt, pattern, fallback }) => {
  const match = prompt.match(pattern)
  if (!match) return fallback

  const width = Number(match[1])
  const height = Number(match[2])

  if (!Number.isFinite(width) || !Number.isFinite(height) || width < 1 || height < 1) {
    return fallback
  }

  return { width, height }
}

const nearestPaletteColor = (r, g, b) => {
  let bestColor = PICO8_PALETTE[0]
  let bestDistance = Infinity

  for (const color of PICO8_PALETTE) {
    const dr = r - color[0]
    const dg = g - color[1]
    const db = b - color[2]
    const distance = dr * dr + dg * dg + db * db

    if (distance < bestDistance) {
      bestDistance = distance
      bestColor = color
    }
  }

  return bestColor
}

const createPixelArtConversion = async ({ currentImage, prompt }) => {
  const metadata = await readImageMetadata(currentImage)
  const originalWidth = metadata?.width || 800
  const originalHeight = metadata?.height || 600
  const targetSize = parseSizeFromPrompt({
    prompt,
    pattern: /upscale\s+back\s+to\s+(\d+)\s*[x×]\s*(\d+)/i,
    fallback: { width: originalWidth, height: originalHeight }
  })
  const lowSize = parseSizeFromPrompt({
    prompt,
    pattern: /downscale\s+to\s+(\d+)\s*[x×]\s*(\d+)/i,
    fallback: {
      width: Math.max(1, Math.round(targetSize.width / 10)),
      height: Math.max(1, Math.round(targetSize.height / 10))
    }
  })

  const { data } = await sharp(currentImage.buffer)
    .ensureAlpha()
    .resize(lowSize.width, lowSize.height, {
      fit: 'fill',
      kernel: 'nearest'
    })
    .raw()
    .toBuffer({ resolveWithObject: true })

  const lowPixelCount = lowSize.width * lowSize.height
  const lowRgb = Buffer.alloc(lowPixelCount * 3)
  const objectMask = new Uint8Array(lowPixelCount)

  for (let pixelIndex = 0; pixelIndex < lowPixelCount; pixelIndex += 1) {
    const sourceIndex = pixelIndex * 4
    const targetIndex = pixelIndex * 3
    const alpha = data[sourceIndex + 3] / 255
    const r = Math.round(data[sourceIndex] * alpha + 255 * (1 - alpha))
    const g = Math.round(data[sourceIndex + 1] * alpha + 255 * (1 - alpha))
    const b = Math.round(data[sourceIndex + 2] * alpha + 255 * (1 - alpha))
    const [pr, pg, pb] = nearestPaletteColor(r, g, b)

    lowRgb[targetIndex] = pr
    lowRgb[targetIndex + 1] = pg
    lowRgb[targetIndex + 2] = pb

    const distanceFromPaper = Math.abs(r - 255) + Math.abs(g - 255) + Math.abs(b - 255)
    if (alpha > 0.08 && distanceFromPaper > 60) {
      objectMask[pixelIndex] = 1
    }
  }

  const outlinedRgb = Buffer.from(lowRgb)
  const outlineOffsets = [
    [-1, 0],
    [1, 0],
    [0, -1],
    [0, 1]
  ]

  for (let y = 0; y < lowSize.height; y += 1) {
    for (let x = 0; x < lowSize.width; x += 1) {
      const pixelIndex = y * lowSize.width + x
      if (!objectMask[pixelIndex]) continue

      for (const [dx, dy] of outlineOffsets) {
        const nx = x + dx
        const ny = y + dy
        if (nx < 0 || ny < 0 || nx >= lowSize.width || ny >= lowSize.height) continue

        const neighborIndex = ny * lowSize.width + nx
        if (objectMask[neighborIndex]) continue

        const targetIndex = neighborIndex * 3
        outlinedRgb[targetIndex] = 0
        outlinedRgb[targetIndex + 1] = 0
        outlinedRgb[targetIndex + 2] = 0
      }
    }
  }

  const outputBuffer = await sharp(outlinedRgb, {
    raw: {
      width: lowSize.width,
      height: lowSize.height,
      channels: 3
    }
  })
    .resize(targetSize.width, targetSize.height, {
      fit: 'fill',
      kernel: 'nearest'
    })
    .png()
    .toBuffer()

  return {
    provider: 'local-pixel-art',
    model: 'pixel-art-converter',
    apiModel: 'sharp-nearest-palette',
    mode: 'draw',
    imageUrl: `data:image/png;base64,${outputBuffer.toString('base64')}`,
    imageServiceConfigured: true,
    notice: `已使用本地像素画转换：${lowSize.width}x${lowSize.height} -> ${targetSize.width}x${targetSize.height}，Pico-8 16 色近似调色板。`,
    text: '已完成像素画转换。该请求使用确定性图像处理完成，保留原始构图，避免扩散模型重绘导致黑块或变形。'
  }
}

const joinUrl = (baseUrl, pathname) => {
  return `${baseUrl.replace(/\/+$/, '')}/${pathname.replace(/^\/+/, '')}`
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

const roundToMultiple = (value, multiple) => {
  return Math.max(multiple, Math.round(value / multiple) * multiple)
}

const getComfyImageSize = async (currentImage) => {
  const metadata = await readImageMetadata(currentImage)
  const sourceWidth = metadata?.width || 512
  const sourceHeight = metadata?.height || 512
  const maxSize = Number(process.env.COMFYUI_MAX_SIZE || 768)
  const scale = Math.min(1, maxSize / Math.max(sourceWidth, sourceHeight))

  return {
    width: Number(process.env.COMFYUI_WIDTH) || roundToMultiple(sourceWidth * scale, 64),
    height: Number(process.env.COMFYUI_HEIGHT) || roundToMultiple(sourceHeight * scale, 64)
  }
}

const uploadComfyImage = async ({ baseUrl, file }) => {
  const flattenedBuffer = await sharp(file.buffer)
    .flatten({ background: '#ffffff' })
    .png()
    .toBuffer()
  const formData = new FormData()
  formData.append('image', new Blob([flattenedBuffer], { type: 'image/png' }), file.originalname || 'canvas.png')
  formData.append('type', 'input')
  formData.append('overwrite', 'true')

  const response = await fetch(joinUrl(baseUrl, '/upload/image'), {
    method: 'POST',
    body: formData
  })

  const payload = await response.json().catch(() => null)

  if (!response.ok) {
    const err = new Error(payload?.error || payload?.message || `ComfyUI 上传图片失败 (${response.status})`)
    err.statusCode = response.status
    throw err
  }

  return payload?.name || file.originalname || 'canvas.png'
}

const buildComfyWorkflow = ({ sourceImageName, promptParts, width, height }) => {
  const checkpointName = process.env.COMFYUI_CHECKPOINT || process.env.DEEPSEEK_IMAGE_MODEL || 'sd_turbo.safetensors'
  const steps = Number(process.env.COMFYUI_STEPS || 4)
  const cfg = Number(process.env.COMFYUI_CFG || 1)
  const denoise = Number(process.env.COMFYUI_DENOISE || 0.68)

  return {
    1: {
      class_type: 'LoadImage',
      inputs: {
        image: sourceImageName
      }
    },
    2: {
      class_type: 'CheckpointLoaderSimple',
      inputs: {
        ckpt_name: checkpointName
      }
    },
    3: {
      class_type: 'CLIPTextEncode',
      inputs: {
        clip: ['2', 1],
        text: promptParts.positive
      }
    },
    4: {
      class_type: 'CLIPTextEncode',
      inputs: {
        clip: ['2', 1],
        text: promptParts.negative || 'low quality, blurry, distorted, bad anatomy, messy lines'
      }
    },
    5: {
      class_type: 'ImageScale',
      inputs: {
        image: ['1', 0],
        upscale_method: 'bilinear',
        width,
        height,
        crop: 'disabled'
      }
    },
    6: {
      class_type: 'VAEEncode',
      inputs: {
        pixels: ['5', 0],
        vae: ['2', 2]
      }
    },
    7: {
      class_type: 'KSampler',
      inputs: {
        model: ['2', 0],
        positive: ['3', 0],
        negative: ['4', 0],
        latent_image: ['6', 0],
        seed: Math.floor(Math.random() * Number.MAX_SAFE_INTEGER),
        steps,
        cfg,
        sampler_name: process.env.COMFYUI_SAMPLER || 'euler',
        scheduler: process.env.COMFYUI_SCHEDULER || 'normal',
        denoise
      }
    },
    8: {
      class_type: 'VAEDecode',
      inputs: {
        samples: ['7', 0],
        vae: ['2', 2]
      }
    },
    9: {
      class_type: 'SaveImage',
      inputs: {
        images: ['8', 0],
        filename_prefix: 'pixel_lab'
      }
    }
  }
}

const fetchComfyImageDataUrl = async ({ baseUrl, image }) => {
  const params = new URLSearchParams({
    filename: image.filename,
    subfolder: image.subfolder || '',
    type: image.type || 'output'
  })

  const response = await fetch(`${joinUrl(baseUrl, '/view')}?${params.toString()}`)

  if (!response.ok) {
    const err = new Error(`ComfyUI 获取生成图片失败 (${response.status})`)
    err.statusCode = response.status
    throw err
  }

  const arrayBuffer = await response.arrayBuffer()
  const base64 = Buffer.from(arrayBuffer).toString('base64')
  const contentType = response.headers.get('content-type') || 'image/png'
  return `data:${contentType};base64,${base64}`
}

const waitForComfyOutput = async ({ baseUrl, promptId }) => {
  const timeoutMs = Number(process.env.COMFYUI_TIMEOUT_MS || 180000)
  const startedAt = Date.now()

  while (Date.now() - startedAt < timeoutMs) {
    const response = await fetch(joinUrl(baseUrl, `/history/${promptId}`))
    const payload = await response.json().catch(() => null)
    const historyItem = payload?.[promptId]

    if (historyItem?.status?.status_str === 'error') {
      const err = new Error('ComfyUI 生成失败，请打开 ComfyUI 查看节点错误。')
      err.statusCode = 502
      throw err
    }

    const image = historyItem?.outputs?.['9']?.images?.[0]
    if (image) {
      return fetchComfyImageDataUrl({ baseUrl, image })
    }

    await sleep(1000)
  }

  const err = new Error('ComfyUI 生成超时，请确认模型已加载且显存足够。')
  err.statusCode = 504
  throw err
}

const callComfyImageGenerator = async ({ text, currentImage }) => {
  const baseUrl = getDeepSeekImageUrl()
  const promptParts = extractPromptParts(text)
  const sourceImageName = await uploadComfyImage({ baseUrl, file: currentImage })
  const { width, height } = await getComfyImageSize(currentImage)
  const prompt = buildComfyWorkflow({
    sourceImageName,
    promptParts,
    width,
    height
  })
  const clientId = randomUUID()

  const response = await fetch(joinUrl(baseUrl, '/prompt'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      client_id: clientId,
      prompt
    })
  })

  const payload = await response.json().catch(() => null)

  if (!response.ok || !payload?.prompt_id) {
    const err = new Error(payload?.error?.message || payload?.error || `ComfyUI 提交工作流失败 (${response.status})`)
    err.statusCode = response.status || 502
    throw err
  }

  return {
    configured: true,
    imageUrl: await waitForComfyOutput({
      baseUrl,
      promptId: payload.prompt_id
    })
  }
}

const callImageGenerator = async ({ text, currentImage, attachments, modelConfig }) => {
  const imageUrl = getDeepSeekImageUrl()
  if (!imageUrl) {
    return {
      configured: false,
      imageUrl: ''
    }
  }

  if (getImageProvider() === 'comfyui') {
    return callComfyImageGenerator({
      text,
      currentImage,
      attachments,
      modelConfig
    })
  }

  const sourceImage = await imageFileToPayload(currentImage)
  const referenceImages = []

  for (const file of attachments.filter((item) => item.mimetype?.startsWith('image/')).slice(0, 4)) {
    const imagePayload = await imageFileToPayload(file)
    if (imagePayload) {
      referenceImages.push(imagePayload)
    }
  }

  const promptParts = extractPromptParts(text)
  const headers = {
    'Content-Type': 'application/json'
  }

  if (process.env.DEEPSEEK_IMAGE_API_KEY) {
    headers.Authorization = `Bearer ${process.env.DEEPSEEK_IMAGE_API_KEY}`
  }

  const response = await fetch(imageUrl, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      provider: 'deepseek-compatible-image',
      model: process.env.DEEPSEEK_IMAGE_MODEL || modelConfig.label,
      prompt: promptParts.positive,
      negative_prompt: promptParts.negative,
      source_image: sourceImage,
      image: sourceImage,
      reference_images: referenceImages,
      width: sourceImage?.width || 1024,
      height: sourceImage?.height || 1024
    })
  })

  const payload = await response.json().catch(() => null)

  if (!response.ok) {
    const err = new Error(payload?.error?.message || payload?.msg || `图片生成服务请求失败 (${response.status})`)
    err.statusCode = response.status
    throw err
  }

  const generatedImageUrl = pickGeneratedImageUrl(payload)
  if (!generatedImageUrl) {
    const err = new Error('图片生成服务没有返回可用图片，请检查 DEEPSEEK_IMAGE_API_URL 的响应格式。')
    err.statusCode = 502
    throw err
  }

  return {
    configured: true,
    imageUrl: generatedImageUrl
  }
}

const normalizeDeepSeekError = ({ status, payload }) => {
  const message = payload?.error?.message || `DeepSeek request failed with status ${status}`

  if (status === 401) {
    return {
      statusCode: 401,
      message: 'DeepSeek API Key 无效或已过期，请检查后端 .env 里的 DEEPSEEK_API_KEY。'
    }
  }

  if (status === 429) {
    return {
      statusCode: 429,
      message: 'DeepSeek 请求过于频繁，请稍后再试。'
    }
  }

  return {
    statusCode: status,
    message
  }
}

const callDeepSeekChat = async ({ prompt, modelConfig }) => {
  const response = await fetch(getDeepSeekChatUrl(), {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${process.env.DEEPSEEK_API_KEY}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      model: modelConfig.apiModel,
      messages: [
        {
          role: 'system',
          content: modelConfig.mode === 'draw'
            ? '你是专业数字绘画提示词设计师。你输出二次创作方案、正向提示词和负面提示词，帮助用户把草图或参考素材转化为可执行的绘图提示。'
            : '你是专业数字绘画美术指导。你输出清晰可执行的图片润色建议和提示词，帮助用户提升手绘板作品。'
        },
        {
          role: 'user',
          content: prompt
        }
      ],
      temperature: modelConfig.mode === 'draw' ? 0.85 : 0.7,
      max_tokens: modelConfig.mode === 'draw' ? 1500 : 1200
    })
  })

  const payload = await response.json().catch(() => null)

  if (!response.ok) {
    const normalizedError = normalizeDeepSeekError({
      status: response.status,
      payload
    })
    const err = new Error(normalizedError.message)
    err.statusCode = normalizedError.statusCode
    throw err
  }

  return payload?.choices?.[0]?.message?.content || ''
}

const refineDrawing = async ({ currentImage, attachments = [], prompt, mode, model }) => {
  const modelConfig = resolveModelConfig({ mode, model })

  if (modelConfig.mode === 'draw' && isPixelArtRequest(prompt)) {
    return createPixelArtConversion({
      currentImage,
      prompt
    })
  }

  if (!process.env.DEEPSEEK_API_KEY) {
    const err = new Error('未配置 DEEPSEEK_API_KEY，无法调用 DeepSeek。')
    err.statusCode = 503
    throw err
  }

  const canGenerateImage = modelConfig.mode === 'draw' && Boolean(getDeepSeekImageUrl())
  const contextNotes = await collectContextNotes({ currentImage, attachments })
  const fullPrompt = modelConfig.mode === 'draw'
    ? buildDrawPrompt({ prompt, contextNotes, canGenerateImage })
    : buildRefinePrompt({ prompt, contextNotes })

  const text = await callDeepSeekChat({
    prompt: fullPrompt,
    modelConfig
  })

  let imageResult = {
    configured: false,
    imageUrl: ''
  }

  if (modelConfig.mode === 'draw') {
    imageResult = await callImageGenerator({
      text,
      currentImage,
      attachments,
      modelConfig
    })
  }

  return {
    provider: 'deepseek',
    model: modelConfig.id,
    apiModel: modelConfig.apiModel,
    mode: modelConfig.mode,
    imageUrl: imageResult.imageUrl,
    imageServiceConfigured: imageResult.configured,
    notice: modelConfig.mode === 'draw' && !imageResult.configured
      ? '当前未配置 DeepSeek/Janus 图片生成服务，只返回二次创作提示词。配置 DEEPSEEK_IMAGE_API_URL 后可返回图片。'
      : '',
    text
  }
}

module.exports = {
  refineDrawing
}
