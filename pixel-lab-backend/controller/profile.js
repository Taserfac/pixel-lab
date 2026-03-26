/**
 * 【文件路径】controller/profile.js
 * 【功能说明】用户个人资料控制器
 */

const bcrypt = require('bcryptjs')
const profileService = require('../service/profile')
const userService = require('../service/user')
const { success, error } = require('../utils/result')

/**
 * 更新用户资料
 * PATCH /api/auth/profile
 */
async function updateProfile(req, res) {
  const userId = req.user.id
  const { nickname, avatar } = req.body

  if (!nickname && !avatar) {
    return error(res, '没有要更新的内容', 400)
  }

  await profileService.updateProfile(userId, { nickname, avatar })

  // 返回更新后的用户信息
  const user = await userService.findById(userId)
  success(res, user, '更新成功')
}

/**
 * 修改密码
 * PATCH /api/auth/password
 */
async function changePassword(req, res) {
  const userId = req.user.id
  const { oldPassword, newPassword } = req.body

  if (!oldPassword || !newPassword) {
    return error(res, '请填写完整信息', 400)
  }

  if (newPassword.length < 6 || newPassword.length > 20) {
    return error(res, '新密码长度需在 6-20 位之间', 400)
  }

  // 获取用户信息
  const user = await userService.findByUsername(req.user.username)
  if (!user) {
    return error(res, '用户不存在', 404)
  }

  // 验证旧密码
  const isValid = await bcrypt.compare(oldPassword, user.password)
  if (!isValid) {
    return error(res, '原密码错误', 400)
  }

  // 加密新密码
  const hashedPassword = await bcrypt.hash(newPassword, 10)
  await profileService.changePassword(userId, hashedPassword)

  success(res, null, '密码修改成功')
}

module.exports = {
  updateProfile,
  changePassword
}