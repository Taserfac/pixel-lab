const mysql = require('mysql2/promise')
const config = require('../config')

const pool = mysql.createPool(config.db)

const query = async (sql, params = []) => {
  try {
    const [rows] = await pool.query(sql, params)
    return rows
  } catch (error) {
    console.error('[DB Error]', error)
    throw error
  }
}

const insert = async (sql, params = []) => {
  try {
    const [result] = await pool.query(sql, params)
    return result.insertId
  } catch (error) {
    console.error('[DB Error]', error)
    throw error
  }
}

const update = async (sql, params = []) => {
  try {
    const [result] = await pool.query(sql, params)
    return result.affectedRows
  } catch (error) {
    console.error('[DB Error]', error)
    throw error
  }
}

const remove = async (sql, params = []) => {
  try {
    const [result] = await pool.query(sql, params)
    return result.affectedRows
  } catch (error) {
    console.error('[DB Error]', error)
    throw error
  }
}

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

const testConnection = async () => {
  try {
    await pool.query('SELECT 1')
    console.log('[DB] Database connected')
    return true
  } catch (error) {
    console.error('[DB] Database connection failed:', error.message)
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
