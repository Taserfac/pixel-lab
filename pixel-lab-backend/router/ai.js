const express = require('express')
const multer = require('multer')
const aiController = require('../controller/ai')
const { authMiddleware } = require('../middleware/auth')

const router = express.Router()

const upload = multer({
  storage: multer.memoryStorage(),
  limits: {
    fileSize: 20 * 1024 * 1024,
    files: 8
  }
})

router.post(
  '/refine',
  authMiddleware,
  upload.fields([
    { name: 'currentImage', maxCount: 1 },
    { name: 'attachments', maxCount: 7 }
  ]),
  aiController.refineDrawing
)

module.exports = router
