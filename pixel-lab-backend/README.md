# Pixel Lab Pro 后端项目

数字图像创作与管理平台 - 后端 API 服务

## 技术栈

- Node.js + Express
- MySQL + mysql2
- JWT 身份认证
- bcryptjs 密码加密
- Joi 参数校验
- multer 文件上传

## 项目结构

```
pixel-lab-backend/
├── config/           # 全局配置
├── controller/       # 控制器层
├── db/              # 数据库
│   ├── sql/         # SQL 脚本
│   └── index.js     # 数据库连接
├── middleware/      # 中间件
│   ├── auth.js      # JWT 认证
│   ├── error-handler.js  # 错误处理
│   ├── upload.js    # 文件上传
│   └── validate.js  # 参数校验
├── router/          # 路由层
├── service/         # 服务层
├── uploads/         # 上传文件目录
├── utils/           # 工具函数
├── app.js           # 入口文件
└── package.json
```

## 启动步骤

### 1. 安装依赖

```bash
cd pixel-lab-backend
npm install
```

### 2. 配置环境变量

```bash
# 复制示例文件
cp .env.example .env

# 编辑 .env，配置数据库连接信息
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=your_password
DB_NAME=pixel_lab
JWT_SECRET=your_secret_key
```

### 3. 初始化数据库

```bash
# 在 MySQL 中执行 SQL 脚本
mysql -u root -p < db/sql/init.sql
```

### 4. 启动服务

```bash
# 开发模式（自动重启）
npm run dev

# 生产模式
npm start
```

默认启动地址：http://localhost:3000

## API 接口

### 认证接口

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | /api/auth/register | 用户注册 | 否 |
| POST | /api/auth/login | 用户登录 | 否 |
| GET | /api/auth/userinfo | 获取用户信息 | 是 |

### 请求/响应格式

**成功响应：**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": { ... }
}
```

**错误响应：**
```json
{
  "code": 400,
  "msg": "错误信息",
  "data": null
}
```

## 注意事项

1. 确保 MySQL 服务已启动
2. 首次使用请先执行数据库初始化脚本
3. JWT Secret 请使用强随机字符串
4. uploads 目录需要写入权限

## 开发团队

高校 Web 应用开发课程大作业项目
