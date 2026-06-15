import request from '@/utils/request'

export const refineDrawing = ({ image, prompt, mode, model, attachments = [] }) => {
  const formData = new FormData()
  formData.append('currentImage', image)
  formData.append('prompt', prompt)
  formData.append('mode', mode)
  formData.append('model', model)

  attachments.forEach((file) => {
    formData.append('attachments', file)
  })

  return request.post('/api/ai/refine', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 120000
  })
}
