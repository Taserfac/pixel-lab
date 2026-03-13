/**
 * 【文件路径】src/utils/validate.js
 * 【文件功能说明】表单校验规则
 * - 手机号校验
 * - 邮箱校验
 * - 密码强度校验
 * - 其他常用校验规则
 */

/**
 * 手机号校验
 * @param {string} phone - 手机号
 * @returns {boolean}
 */
export const isPhone = (phone) => {
  const reg = /^1[3-9]\d{9}$/
  return reg.test(phone)
}

/**
 * 邮箱校验
 * @param {string} email - 邮箱
 * @returns {boolean}
 */
export const isEmail = (email) => {
  const reg = /^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/
  return reg.test(email)
}

/**
 * 密码强度校验
 * @param {string} password - 密码
 * @returns {object} { valid: boolean, level: number, msg: string }
 */
export const checkPasswordStrength = (password) => {
  if (!password || password.length < 6) {
    return { valid: false, level: 0, msg: '密码长度至少6位' }
  }
  
  let level = 0
  
  // 包含数字
  if (/\d/.test(password)) level++
  // 包含小写字母
  if (/[a-z]/.test(password)) level++
  // 包含大写字母
  if (/[A-Z]/.test(password)) level++
  // 包含特殊字符
  if (/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) level++
  
  const msgs = ['密码强度太弱', '密码强度较弱', '密码强度中等', '密码强度较强', '密码强度很强']
  
  return {
    valid: level >= 2,
    level,
    msg: msgs[level]
  }
}

/**
 * 用户名校验（字母、数字、下划线，4-20位）
 * @param {string} username - 用户名
 * @returns {boolean}
 */
export const isUsername = (username) => {
  const reg = /^[a-zA-Z0-9_]{4,20}$/
  return reg.test(username)
}

/**
 * URL 校验
 * @param {string} url - URL
 * @returns {boolean}
 */
export const isUrl = (url) => {
  const reg = /^(https?:\/\/)?([\da-z.-]+)\.([a-z.]{2,6})([/\w .-]*)*\/?$/
  return reg.test(url)
}

/**
 * 身份证号校验（简化版）
 * @param {string} idCard - 身份证号
 * @returns {boolean}
 */
export const isIdCard = (idCard) => {
  const reg = /^\d{17}[\dXx]|\d{15}$/
  return reg.test(idCard)
}

/**
 * 表单校验规则（用于 Element Plus）
 */
export const formRules = {
  // 用户名
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '长度在 4 到 20 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母、数字、下划线', trigger: 'blur' }
  ],
  
  // 密码
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  
  // 确认密码
  confirmPassword: (passwordField = 'password') => [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordField) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  
  // 邮箱
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  
  // 手机号
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  
  // 必填
  required: (message = '此项为必填项') => [
    { required: true, message, trigger: 'blur' }
  ]
}
