# Pixel Lab Pro 前端项目

数字图像创作与管理平台 - 前端部分

## 技术栈

- Vue 3 + Composition API
- Vite 构建工具
- Element Plus UI 组件库
- Vue Router 路由管理
- Pinia 状态管理
- Axios HTTP 请求
- ECharts 图表库

## 项目结构

```
pixel-lab-frontend/
├── public/                 # 静态公共资源
├── src/
│   ├── api/               # 接口请求封装
│   ├── assets/            # 静态资源（样式、图片、图标）
│   ├── components/        # 组件
│   │   ├── common/        # 通用基础组件
│   │   └── business/      # 业务组件
│   ├── router/            # 路由配置
│   ├── store/             # Pinia 状态管理
│   ├── utils/             # 工具函数
│   ├── views/             # 页面组件
│   │   ├── auth/          # 登录注册
│   │   ├── workbench/     # 图像处理工作台
│   │   ├── draw/          # 手绘板
│   │   ├── community/     # 社区广场
│   │   ├── personal/      # 个人中心
│   │   ├── dashboard/     # 数据统计
│   │   └── admin/         # 后台管理
│   ├── App.vue            # 根组件
│   └── main.js            # 入口文件
├── .env.development       # 开发环境配置
├── .env.production        # 生产环境配置
├── .env.example           # 环境变量示例
├── vite.config.js         # Vite 配置
└── package.json           # 项目依赖
```

## 启动步骤

### 1. 安装依赖

```bash
cd pixel-lab-frontend
npm install
```

### 2. 配置环境变量

```bash
# 复制示例文件
cp .env.example .env.development

# 编辑 .env.development，修改后端 API 地址
VITE_API_BASE_URL=http://localhost:3000
```

### 3. 启动开发服务器

```bash
npm run dev
```

默认启动地址：http://localhost:5173

### 4. 构建生产版本

```bash
npm run build
```

## 功能特性

- ✅ Vue 3 + Vite 现代化开发
- ✅ Element Plus 组件库 + 中文语言包
- ✅ Vue Router 路由管理 + 路由守卫
- ✅ Pinia 状态管理 + localStorage 持久化
- ✅ Axios 请求封装 + 拦截器
- ✅ 亮/暗双主题系统
- ✅ 登录/注册完整闭环
- ✅ 响应式布局
- ✅ ESLint 代码规范

## 注意事项

1. 确保后端服务已启动，默认监听端口 3000
2. 开发时前端端口 5173，后端端口 3000，已通过 Vite 代理解决跨域
3. 首次使用请先注册账号

## 开发团队

高校 Web 应用开发课程大作业项目
