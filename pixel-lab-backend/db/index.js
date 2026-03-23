/**
 * 【文件路径】db/index.js
 * 【文件功能说明】MySQL 数据库连接封装
 * - 使用连接池管理连接
 * - Promise 封装，支持 async/await
 * - 提供通用查询方法
 */

const mysql = require('mysql2/promise')
const config = require('../config')

// 创建连接池
const pool = mysql.createPool(config.db)

/**
 * 执行 SQL 查询
 * @param {string} sql - SQL 语句
 * @param {array} params - 查询参数
 * @returns {Promise} 查询结果
 */
const query = async (sql, params = []) => {
  try {
    const [rows] = await pool.query(sql, params)
    return rows
  } catch (error) {
    console.error('[DB Error]', error)
    throw error
  }
}

/**
 * 执行插入操作，返回插入的 ID
 * @param {string} sql - SQL 语句
 * @param {array} params - 查询参数
 * @returns {Promise} 插入的 ID
 */
const insert = async (sql, params = []) => {
  try {
    const [result] = await pool.query(sql, params)
    return result.insertId
  } catch (error) {
    console.error('[DB Error]', error)
    throw error
  }
}

/**
 * 执行更新操作，返回影响的行数
 * @param {string} sql - SQL 语句
 * @param {array} params - 查询参数
 * @returns {Promise} 影响的行数
 */
const update = async (sql, params = []) => {
  try {
    const [result] = await pool.query(sql, params)
    return result.affectedRows
  } catch (error) {
    console.error('[DB Error]', error)
    throw error
  }
}

/**
 * 执行删除操作
 * @param {string} sql - SQL 语句
 * @param {array} params - 查询参数
 * @returns {Promise} 影响的行数
 */
const remove = async (sql, params = []) => {
  try {
    const [result] = await pool.query(sql, params)
    return result.affectedRows
  } catch (error) {
    console.error('[DB Error]', error)
    throw error
  }
}

/**
 * 事务封装
 * @param {Function} callback - 事务回调函数
 * @returns {Promise}
 */
const transaction = async (callback) => {
  const connection = await pool.getConnection()
  try {
    await connection.beginTransaction()
    const result = await callback(connection)
    await connection.commit()
    return result
  } catch (error) {
    await connection.rollback()
    throw error
  } finally {
    connection.release()
  }
}

/**
 * 测试数据库连接
 */
const testConnection = async () => {
  try {
    const [rows] = await pool.query('SELECT 1')
    console.log('[DB] 数据库连接成功')
    return true
  } catch (error) {
    console.error('[DB] 数据库连接失败:', error.message)
    return false
  }
}

module.exports = {
  pool,
  query,
  insert,
  update,
  remove,
  transaction,
  testConnection
}
