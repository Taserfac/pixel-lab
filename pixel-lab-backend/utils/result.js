/**
 * 【文件路径】utils/result.js
 * 【文件功能说明】统一接口返回格式封装
 * - 成功返回
 * - 失败返回
 * - 统一格式：{ code, msg, data }
 */

/**
 * 成功响应
 * @param {object} res - Express response 对象
 * @param {*} data - 返回数据
 * @param {string} msg - 提示信息
 * @param {number} code - 状态码
 */
const success = (res, data = null, msg = '操作成功', code = 200) => {
  res.json({
    code,
    msg,
    data
  })
}

/**
 * 失败响应
 * @param {object} res - Express response 对象
 * @param {string} msg - 错误信息
 * @param {number} code - 错误码
 * @param {*} data - 额外数据
 */
const error = (res, msg = '操作失败', code = 400, data = null) => {
  res.status(code >= 400 && code < 600 ? code : 400).json({
    code,
    msg,
    data
  })
}

module.exports = {
  success,
  error
}
