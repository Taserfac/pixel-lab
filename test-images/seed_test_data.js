const fs = require('fs')
const path = require('path')
const crypto = require('crypto')
const mysql = require('../pixel-lab-backend/node_modules/mysql2/promise')
const bcrypt = require('../pixel-lab-backend/node_modules/bcryptjs')
const sharp = require('../pixel-lab-backend/node_modules/sharp')

const projectRoot = path.resolve(__dirname, '..')
const envPath = path.join(projectRoot, 'pixel-lab-backend', '.env')
const seedPassword = 'Test123456'
const publicBaseUrl = 'http://localhost:8080'

const authors = {
  'Simon Bailly': { username: 'simon_bailly', tags: ['插画', '艺术欣赏'] },
  Tiub: { username: 'tiub', tags: ['插画', '治愈系', '风景'] },
  Yiett: { username: 'yiett', tags: ['插画', '概念艺术'] },
  '赤羽ブギウギ（akabane_1999）': { username: 'akabane_1999', tags: ['插画', '动漫', '概念艺术'] },
  Andrew: { username: 'andrew', tags: ['摄影', '风景摄影'] },
  Fnvlcy: { username: 'fnvlcy', tags: ['摄影', '人像摄影'] }
}

const commentTexts = [
  '这组色彩和氛围太舒服了。',
  '构图很有张力，细节也很耐看。',
  '光影处理得真好，先收藏了。',
  '很喜欢这个视角和画面节奏。',
  '这张作品给了我不少灵感。'
]

function readEnv(file) {
  const values = {}
  for (const rawLine of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#') || !line.includes('=')) continue
    const separator = line.indexOf('=')
    values[line.slice(0, separator).trim()] = line.slice(separator + 1).trim().replace(/^(['"])(.*)\1$/, '$2')
  }
  return values
}

function stableNumber(value) {
  return crypto.createHash('sha256').update(value).digest().readUInt32BE(0)
}

function cleanTitle(filename, index, total) {
  const withoutExtension = path.parse(filename).name
  const base = withoutExtension
    .replace(/_\d+_[^_]+_来自小红书网页版(?: \(\d+\))?$/, '')
    .replace(/_来自小红书网页版(?: \(\d+\))?$/, '')
    .trim()
  const suffix = total > 1 ? ` · ${index + 1}` : ''
  return `${base}${suffix}`.slice(0, 100)
}

function discoverImages() {
  const records = []
  for (const [nickname, config] of Object.entries(authors)) {
    const label = config.tags[0]
    const authorDir = path.join(__dirname, label, nickname)
    if (!fs.existsSync(authorDir)) {
      throw new Error(`找不到作者目录：${authorDir}`)
    }
    const files = fs.readdirSync(authorDir)
      .filter(file => /\.(?:jpe?g|png|webp)$/i.test(file))
      .sort((a, b) => a.localeCompare(b, 'zh-CN', { numeric: true }))
    files.forEach((file, index) => {
      records.push({
        nickname,
        username: config.username,
        tags: config.tags,
        sourcePath: path.join(authorDir, file),
        originalName: file,
        title: cleanTitle(file, index, files.length),
        index,
        total: files.length
      })
    })
  }
  return records
}

async function main() {
  if (!fs.existsSync(envPath)) throw new Error(`找不到数据库配置：${envPath}`)
  const env = readEnv(envPath)
  const uploadSetting = env.UPLOAD_PATH || 'uploads'
  const tomcatHome = process.env.CATALINA_HOME || path.join(projectRoot, '.tools', 'apache-tomcat-9.0.93')
  const uploadRoot = path.isAbsolute(uploadSetting) ? uploadSetting : path.join(tomcatHome, uploadSetting)
  const seedUploadDir = path.join(uploadRoot, 'test-seed')
  fs.mkdirSync(seedUploadDir, { recursive: true })

  const images = discoverImages()
  const connection = await mysql.createConnection({
    host: env.DB_HOST || 'localhost',
    port: Number(env.DB_PORT || 3306),
    user: env.DB_USER || 'root',
    password: env.DB_PASSWORD || '',
    database: env.DB_NAME || 'pixel_lab',
    charset: 'utf8mb4'
  })

  const passwordHash = await bcrypt.hash(seedPassword, 10)
  const userIds = new Map()
  const imageIds = new Map()
  const authorImages = new Map()

  try {
    const [commentStatusColumns] = await connection.execute("SHOW COLUMNS FROM comments LIKE 'status'")
    if (commentStatusColumns.length === 0) {
      await connection.execute('ALTER TABLE comments ADD COLUMN status TINYINT NOT NULL DEFAULT 1 AFTER parent_id')
    }
    await connection.beginTransaction()

    for (const [nickname, config] of Object.entries(authors)) {
      await connection.execute(
        `INSERT INTO user (username, password, nickname, role, status, is_deleted)
         VALUES (?, ?, ?, 'user', 1, 0)
         ON DUPLICATE KEY UPDATE password = VALUES(password), nickname = VALUES(nickname), status = 1, is_deleted = 0`,
        [config.username, passwordHash, nickname]
      )
      const [[user]] = await connection.execute('SELECT id FROM user WHERE username = ? LIMIT 1', [config.username])
      userIds.set(config.username, user.id)
      authorImages.set(config.username, [])
    }

    for (const record of images) {
      const extension = path.extname(record.originalName).toLowerCase() || '.jpg'
      const filename = `seed_${record.username}_${String(record.index + 1).padStart(2, '0')}${extension}`
      const destination = path.join(seedUploadDir, filename)
      fs.copyFileSync(record.sourcePath, destination)
      const metadata = await sharp(record.sourcePath).metadata()
      const stats = fs.statSync(record.sourcePath)
      const url = `${publicBaseUrl}/uploads/test-seed/${filename}`
      const hash = stableNumber(`${record.username}/${record.originalName}`)
      const viewCount = 480 + (hash % 11500)
      const likeCount = 45 + ((hash >>> 5) % 1600)
      const collectCount = 12 + ((hash >>> 11) % 420)
      const ageHours = 12 + ((hash >>> 17) % (24 * 28))
      const description = `${record.nickname} 的${record.tags[0]}测试作品，用于社区内容与交互功能验证。`

      const [[existing]] = await connection.execute('SELECT id FROM image WHERE url = ? LIMIT 1', [url])
      let imageId
      if (existing) {
        imageId = existing.id
        await connection.execute(
          `UPDATE image SET user_id = ?, filename = ?, original_name = ?, thumbnail_url = ?, width = ?, height = ?,
           size = ?, format = ?, is_public = 1, status = 1, title = ?, description = ?, tags = ?,
           view_count = ?, like_count = ?, collect_count = ?, created_at = DATE_SUB(NOW(), INTERVAL ? HOUR)
           WHERE id = ?`,
          [userIds.get(record.username), filename, record.originalName, url, metadata.width || null, metadata.height || null,
            stats.size, (metadata.format || extension.slice(1)).slice(0, 10), record.title, description,
            record.tags.join(','), viewCount, likeCount, collectCount, ageHours, imageId]
        )
      } else {
        const [inserted] = await connection.execute(
          `INSERT INTO image
           (user_id, filename, original_name, url, thumbnail_url, width, height, size, format, is_public, status,
            title, description, tags, view_count, like_count, comment_count, collect_count, created_at)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, ?, ?, ?, ?, ?, 0, ?, DATE_SUB(NOW(), INTERVAL ? HOUR))`,
          [userIds.get(record.username), filename, record.originalName, url, url, metadata.width || null, metadata.height || null,
            stats.size, (metadata.format || extension.slice(1)).slice(0, 10), record.title, description,
            record.tags.join(','), viewCount, likeCount, collectCount, ageHours]
        )
        imageId = inserted.insertId
      }
      imageIds.set(url, imageId)
      authorImages.get(record.username).push({ id: imageId, url })
    }

    for (const [username, authoredImages] of authorImages.entries()) {
      await connection.execute('UPDATE user SET avatar = ? WHERE id = ?', [authoredImages[0].url, userIds.get(username)])
    }

    const seedUserIds = [...userIds.values()]
    const placeholders = seedUserIds.map(() => '?').join(',')
    await connection.execute(`DELETE FROM follows WHERE follower_id IN (${placeholders}) AND followed_id IN (${placeholders})`, [...seedUserIds, ...seedUserIds])
    const usernames = [...userIds.keys()]
    for (let index = 0; index < usernames.length; index++) {
      for (let offset = 1; offset <= 2; offset++) {
        await connection.execute(
          'INSERT IGNORE INTO follows (follower_id, followed_id, created_at) VALUES (?, ?, DATE_SUB(NOW(), INTERVAL ? DAY))',
          [userIds.get(usernames[index]), userIds.get(usernames[(index + offset) % usernames.length]), index + offset]
        )
      }
    }

    for (const record of images) {
      const extension = path.extname(record.originalName).toLowerCase() || '.jpg'
      const filename = `seed_${record.username}_${String(record.index + 1).padStart(2, '0')}${extension}`
      const url = `${publicBaseUrl}/uploads/test-seed/${filename}`
      const imageId = imageIds.get(url)
      const ownerId = userIds.get(record.username)
      const hash = stableNumber(url)
      const candidates = seedUserIds.filter(id => id !== ownerId)
      const likeUsers = candidates.slice(0, 2 + (hash % 4))
      const collectUsers = candidates.slice().reverse().slice(0, 1 + ((hash >>> 4) % 3))
      const commentUsers = candidates.slice(0, 1 + ((hash >>> 8) % 3))

      await connection.execute(`DELETE FROM likes WHERE image_id = ? AND user_id IN (${placeholders})`, [imageId, ...seedUserIds])
      await connection.execute(`DELETE FROM collections WHERE image_id = ? AND user_id IN (${placeholders})`, [imageId, ...seedUserIds])
      await connection.execute(`DELETE FROM comments WHERE image_id = ? AND user_id IN (${placeholders})`, [imageId, ...seedUserIds])

      for (const [index, userId] of likeUsers.entries()) {
        await connection.execute(
          'INSERT IGNORE INTO likes (user_id, image_id, created_at) VALUES (?, ?, DATE_SUB(NOW(), INTERVAL ? HOUR))',
          [userId, imageId, index + 1]
        )
      }
      for (const [index, userId] of collectUsers.entries()) {
        await connection.execute(
          'INSERT IGNORE INTO collections (user_id, image_id, created_at) VALUES (?, ?, DATE_SUB(NOW(), INTERVAL ? HOUR))',
          [userId, imageId, index + 2]
        )
      }
      for (const [index, userId] of commentUsers.entries()) {
        await connection.execute(
          `INSERT INTO comments (user_id, image_id, content, status, created_at)
           VALUES (?, ?, ?, 1, DATE_SUB(NOW(), INTERVAL ? HOUR))`,
          [userId, imageId, commentTexts[(hash + index) % commentTexts.length], index + 1]
        )
      }
      const [[commentTotal]] = await connection.execute('SELECT COUNT(*) AS total FROM comments WHERE image_id = ? AND status = 1', [imageId])
      await connection.execute('UPDATE image SET comment_count = ? WHERE id = ?', [commentTotal.total, imageId])
    }

    await connection.execute(`DELETE FROM notifications WHERE user_id IN (${placeholders}) AND reference_type = 'test-seed'`, seedUserIds)
    for (let index = 0; index < usernames.length; index++) {
      const receiverName = usernames[index]
      const senderName = usernames[(index + 1) % usernames.length]
      const receiverId = userIds.get(receiverName)
      const senderId = userIds.get(senderName)
      const referenceImage = authorImages.get(receiverName)[0]
      const senderNickname = Object.entries(authors).find(([, value]) => value.username === senderName)[0]
      await connection.execute(
        `INSERT INTO notifications (user_id, sender_id, type, content, reference_id, reference_type, is_read, created_at)
         VALUES (?, ?, 'like', ?, ?, 'test-seed', 0, DATE_SUB(NOW(), INTERVAL 8 MINUTE)),
                (?, ?, 'comment', ?, ?, 'test-seed', 0, DATE_SUB(NOW(), INTERVAL 35 MINUTE))`,
        [receiverId, senderId, `${senderNickname} 赞了你的作品`, referenceImage.id,
          receiverId, senderId, `${senderNickname} 评论了你的作品`, referenceImage.id]
      )
    }

    await connection.commit()
    console.log(`已导入 ${userIds.size} 个作者账号、${images.length} 张作品。`)
    console.log(`图片目录：${seedUploadDir}`)
    console.log(`统一测试密码：${seedPassword}`)
  } catch (error) {
    await connection.rollback()
    throw error
  } finally {
    await connection.end()
  }
}

main().catch(error => {
  console.error(error)
  process.exitCode = 1
})
