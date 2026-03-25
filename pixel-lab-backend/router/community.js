/**
 * 【文件路径】router/community.js
 * 【功能说明】社区路由
 */

const express = require('express')
const router = express.Router()
const communityController = require('../controller/community')
const { authMiddleware } = require('../middleware/auth')

// 公开接口
// 获取公开作品列表
router.get('/images', communityController.getPublicImages)

// 获取作品详情
router.get('/images/:id', communityController.getImageDetail)

// 获取评论列表
router.get('/images/:imageId/comments', communityController.getComments)

// 需要登录的接口
// 点赞/取消点赞
router.post('/like', authMiddleware, communityController.toggleLike)

// 收藏/取消收藏
router.post('/collect', authMiddleware, communityController.toggleCollect)

// 发表评论
router.post('/comments', authMiddleware, communityController.addComment)

// 删除评论
router.delete('/comments/:id', authMiddleware, communityController.deleteComment)

// 获取用户收藏列表
router.get('/collections', authMiddleware, communityController.getUserCollections)

module.exports = router