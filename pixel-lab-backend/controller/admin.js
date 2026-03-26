/**
 * 【文件路径】controller/admin.js
 * 【功能说明】后台管理控制器
 */

const adminService = require('../service/admin')
const { success, error } = require('../utils/result')

/**
 * 获取用户列表
 * GET /api/admin/users
 */
async function getUsers(req, res) {
  const { page = 1, pageSize = 20, keyword = '' } = req.query

  const result = await adminService.getUsers({
    page: parseInt(page),
    pageSize: parseInt(pageSize),
    keyword
  })

  success(res, result)
}

/**
 * 更新用户状态
 * PATCH /api/admin/users/:id/status
 */
async function updateUserStatus(req, res) {
  const { id } = req.params
  const { status } = req.body

  if (status === undefined) {
    return error(res, '缺少状态参数', 400)
  }

  // 不能禁用自己
  if (parseInt(id) === req.user.id) {
    return error(res, '不能修改自己的状态', 400)
  }

  const result = await adminService.updateUserStatus(parseInt(id), parseInt(status))
  
  if (!result) {
    return error(res, '用户不存在', 404)
  }

  success(res, null, status === 1 ? '已启用' : '已禁用')
}

/**
 * 更新用户角色
 * PATCH /api/admin/users/:id/role
 */
async function updateUserRole(req, res) {
  const { id } = req.params
  const { role } = req.body

  if (!role || !['user', 'admin'].includes(role)) {
    return error(res, '角色参数无效', 400)
  }

  // 不能修改自己的角色
  if (parseInt(id) === req.user.id) {
    return error(res, '不能修改自己的角色', 400)
  }

  const result = await adminService.updateUserRole(parseInt(id), role)
  
  if (!result) {
    return error(res, '用户不存在', 404)
  }

  success(res, null, '角色已更新')
}

/**
 * 获取作品列表
 * GET /api/admin/images
 */
async function getImages(req, res) {
  const { page = 1, pageSize = 20, keyword = '', status = '' } = req.query

  const result = await adminService.getImages({
    page: parseInt(page),
    pageSize: parseInt(pageSize),
    keyword,
    status
  })

  success(res, result)
}

/**
 * 删除作品
 * DELETE /api/admin/images/:id
 */
async function deleteImage(req, res) {
  const { id } = req.params

  const result = await adminService.deleteImage(parseInt(id))
  
  if (!result) {
    return error(res, '作品不存在', 404)
  }

  success(res, null, '已删除')
}

/**
 * 获取平台统计
 * GET /api/admin/stats
 */
async function getStats(req, res) {
  const stats = await adminService.getPlatformStats()
  success(res, stats)
}

module.exports = {
  getUsers,
  updateUserStatus,
  updateUserRole,
  getImages,
  deleteImage,
  getStats
}