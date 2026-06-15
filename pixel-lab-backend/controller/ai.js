const aiService = require('../service/ai')
const { success, error } = require('../utils/result')

const refineDrawing = async (req, res) => {
  try {
    const currentImage = req.files?.currentImage?.[0]
    const attachments = req.files?.attachments || []
    const { prompt = '', mode = 'refine', model = 'deepseek-v4-flash' } = req.body

    if (!currentImage) {
      return error(res, '请上传当前画布图片', 400)
    }

    if (!prompt.trim()) {
      return error(res, '请输入 AI 润色要求', 400)
    }

    const result = await aiService.refineDrawing({
      currentImage,
      attachments,
      prompt: prompt.trim(),
      mode,
      model
    })

    success(res, result, 'AI 润色完成')
  } catch (err) {
    console.error('[AI] Refine drawing failed:', err)
    error(res, err.message || 'AI 润色失败', err.statusCode || 500)
  }
}

module.exports = {
  refineDrawing
}
