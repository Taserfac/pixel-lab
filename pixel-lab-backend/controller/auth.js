/**
 * 【文件路径】controller/auth.js
 * 【文件功能说明】用户认证控制器
 * - 注册、登录、获取用户信息
 */

const bcrypt = require('bcryptjs')
const userService = require('../service/user')
const { generateToken } = require('../utils/jwt')
const { success, error } = require('../utils/result')

/**
 * 用户注册
 * POST /api/auth/register
 * @param {string} username - 用户名
 * @param {string} password - 密码
 */
const register = async (req, res) => {
  const { username, password } = req.body

  // 检查用户名是否已存在
  const existingUser = await userService.findByUsername(username)
  if (existingUser) {
    return error(res, '用户名已存在', 409)
  }

  // 加密密码
  const hashedPassword = await bcrypt.hash(password, 10)

  // 创建用户
  const userId = await userService.create({
    username,
    password: hashedPassword
  })

  success(res, { id: userId }, '注册成功', 201)
}

/**
 * 用户登录
 * POST /api/auth/login
 * @param {string} username - 用户名
 * @param {string} password - 密码
 */
const login = async (req, res) => {
  const { username, password } = req.body

  // 查找用户
  const user = await userService.findByUsername(username)
  if (!user) {
    return error(res, '用户名或密码错误', 401)
  }

  // 检查用户状态
  if (user.status !== 1) {
    return error(res, '账号已被禁用', 403)
  }

  // 验证密码
  const isPasswordValid = await bcrypt.compare(password, user.password)
  if (!isPasswordValid) {
    return error(res, '用户名或密码错误', 401)
  }

  // 生成 JWT Token
  const token = generateToken({
    id: user.id,
    username: user.username,
    role: user.role
  })

  // 返回用户信息和 token
  success(res, {
    token,
    user: {
      id: user.id,
      username: user.username,
      nickname: user.nickname,
      avatar: user.avatar,
      role: user.role
    }
  }, '登录成功')
}

/**
 * 获取当前用户信息
 * GET /api/auth/userinfo
 * 需要登录
 */
const getUserInfo = async (req, res) => {
  const userId = req.user.id
  const user = await userService.findById(userId)

  if (!user) {
    return error(res, '用户不存在', 404)
  }

  success(res, user)
}

module.exports = {
  register,
  login,
  getUserInfo
}
