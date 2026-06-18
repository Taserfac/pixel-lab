# Pixel Lab 修改记录

---

## 2026-06-18

### 全站 UI 改版与功能重构

- **登录优先验证**：应用启动时立即检查登录状态，过期会话直接跳转登录页，而非进入首页后才提示登录超时。修改 `user.js` Store 初始化逻辑和 `main.js` 路由守卫。
- **登录/注册页固定浅色模式**：登录和注册页面始终使用浅色主题，不受网站日夜模式切换影响。新增 `AuthLayout.vue` 组件，使用 `loginbg.png` 作为左侧装饰图，右侧为登录/注册表单的左右分栏布局。
- **功能命名更新**：「手绘板」更名为「创意画布」，「工作台」更名为「图像工坊」，同步更新 i18n 语言包（zh-CN / en-US）。
- **底部导航栏改版**：导航项改为「首页、社区、上传、创作、个人中心」五项。上传保持绿色突出按钮样式；新增「创作」按钮，点击后弹出选择弹层，可进入创意画布或图像工坊。
- **顶部导航栏精简**：移除顶部导航的上传按钮，上传功能仅保留在底部导航栏。
- **通知已读状态持久化**：通知改为接入后端 API，已读状态在重新登录后不再显示红点提醒。修改 `notification.js` Store 和 `social.js` API 模块。
- **首页布局改版**：删除右侧「创作中心」模块，左侧 Hero 横幅扩展为横贯全宽并移除其中的上传和浏览社区按钮。用户滚动时右侧热门标签、推荐创作者、社区动态保持固定吸附，仅横幅和瀑布流内容滑动。移除外部字体依赖。

### 个人主页与帖子详情

- **新增个人主页** `/user/:id`：公开展示用户作品、获赞数、浏览量、粉丝及关注列表，支持关注/取关操作。
- **新增帖子详情页** `/post/:id`：展示作品大图、作者信息、点赞、收藏、评论功能。首页和社区的图片点击后进入帖子详情页。
- **社区接口扩展**：`CommunityDao.java` 新增帖子详情查询、用户作品列表、用户信息查询等方法；`CommunityServlet.java` 新增对应路由；`AuthFilter.java` 白名单新增公开端点。
- **PostCard 组件增强**：作者头像和昵称支持点击跳转个人主页。

### 测试数据灌入

- **新增种子脚本** `test-images/seed_test_data.js`：创建 6 个测试账号（simon_bailly、tiub、yiett、akabane_1999、andrew、fnvlcy），导入 25 张作品，模拟 181,929 浏览、18,944 点赞、4,343 收藏、82 条点赞关系、49 条收藏、48 条评论、12 条关注、12 条通知。脚本可重复执行不会重复创建，测试密码统一为 `Test123456`。
- **图片文件归档**：`test-images/` 下新增「插画」和「摄影」分类目录，对应图片已复制至 Tomcat 上传目录。

### Tomcat 编码问题诊断

- **问题现象**：首页加载后停在骨架屏，控制台报 ECharts 脚本语法错误。
- **根因定位**：`CharacterEncodingFilter.java` 对所有 `/*` 响应强制 UTF-8 编码转换，导致已为 UTF-8 的前端构建文件被按 Windows 默认编码读取后再次转换，破坏脚本内容。
- **建议修复**：将过滤器路径限制为 `/api/*`，清除旧 Service Worker 缓存。

### 修改文件清单

| 文件 | 变更说明 |
|------|----------|
| `src/main.js` | 路由守卫增加登录状态预检 |
| `src/router/index.js` | 新增 `/user/:id`、`/post/:id` 路由 |
| `src/store/user.js` | 初始化时验证 token 有效性 |
| `src/store/notification.js` | 接入后端通知 API，持久化已读状态 |
| `src/api/auth.js` | 新增登录状态检查接口 |
| `src/api/social.js` | 通知接口改接后端 |
| `src/api/community.js` | 新增帖子详情、用户信息接口 |
| `src/components/common/MainLayout.vue` | 底部导航改版、创作弹层、移除顶部上传、通知已读逻辑 |
| `src/components/auth/AuthLayout.vue` | 新增：登录/注册页固定浅色布局 + loginbg.png |
| `src/components/community/PostCard.vue` | 作者信息可点击跳转 |
| `src/views/auth/Login.vue` | 改用 AuthLayout，固定浅色模式 |
| `src/views/auth/Register.vue` | 改用 AuthLayout，固定浅色模式 |
| `src/views/dashboard/Index.vue` | 横幅全宽、删除创作中心、右侧内容吸附 |
| `src/views/community/Index.vue` | 图片点击进入帖子详情 |
| `src/views/draw/Index.vue` | 命名更新为「创意画布」 |
| `src/views/personal/Index.vue` | 命名同步更新 |
| `src/views/post/Index.vue` | 新增：帖子详情页 |
| `src/views/user/Index.vue` | 新增：个人主页 |
| `src/i18n/locales/zh-CN.js` | 命名和导航文案更新 |
| `src/i18n/locales/en-US.js` | 命名和导航文案更新 |
| `src/assets/css/index.css` | 移除外部字体引用 |
| `CommunityDao.java` | 新增帖子详情、用户查询方法 |
| `CommunityServlet.java` | 新增帖子/用户路由 |
| `AuthFilter.java` | 白名单新增公开端点 |
| `web.xml` | 编码过滤器路径调整 |

### 启动脚本优化

- **Maven 增量构建**：`start-java-backend.ps1` 中 `mvn clean package` 改为 `mvn package`，新增 `-Clean` 参数支持全量构建，启动速度显著提升。
- **PowerShell 命名规范修复**：`Normalize-ProcessPath` → `Set-ProcessPathCase`、`Build-War` → `New-WarPackage`、`Deploy-War` → `Install-WarToTomcat`，消除 PSScriptAnalyzer 警告。

### 项目文档

- **新建 CLAUDE.md**：包含项目架构、技术栈、启动命令、API 代理、代码约定等信息。加入 `.gitignore` 仅保留本地使用。
- **修改记录规范**：CLAUDE.md 中新增 `Change Log Rule`，要求每次修改后必须记录到修改文件。
- **同步中文修改记录**：将英文修改记录同步为中文版，补齐所有缺失章节。
- **Git 清理**：将 `Augenstern_FIX_NOTES_zh.md` 从 git 追踪中移除（`git rm --cached`），仅保留本地维护中文版。

### 个人中心

- **工作台编辑入口**：图片卡片三点菜单新增"在工作台中编辑"选项，点击后自动加载图片到工作台并跳转。

### 管理员后台封禁功能

- **用户封禁**：将"禁用"改为"封禁"，支持选择封禁天数（3天/7天/30天/永久）+ 封禁原因，状态列显示封禁到期时间。
- **用户解封**：封禁状态的用户显示"解封"按钮，恢复为正常状态。
- **作品封禁**：作品管理操作列新增"封禁"按钮（status=2），已封禁作品显示"解封"按钮。
- **作品状态筛选**：状态筛选下拉增加"已封禁"选项。
- **作品集管理**：新增"作品集管理"tab，支持搜索、分页、封禁/解封、删除操作。
- **登录封禁检查**：被封禁用户登录时返回封禁到期时间和原因。
- **数据库变更**：`user` 表新增 `ban_days`、`ban_reason`、`ban_end_at` 字段。

---

## 早期变更（已提交）

> 以下为项目早期提交中的变更记录，已合并到主分支。

### 前端 UI 修复

- **浅色模式停靠栏**：为 `.dock-nav` 添加浅色主题覆盖，底部停靠栏在浅色模式下使用半透明白色背景。
- **绘图画布修复**：添加 `syncFabricCanvasLayout()` 和 `refreshCanvasOffset()` 同步 Fabric 画布尺寸和指针偏移。
- **个人中心图像操作**：将悬停覆盖层替换为固定三点按钮菜单。
- **登录令牌处理**：用户 Store 从 localStorage 初始化 token，请求自动附加 Bearer token。

### 首页社区化

- **首页 UI 重排**：左侧 Hero + 精选作品流，右侧发现栏，4 列宽屏网格，作品拼贴和标签贴纸。
- **可选请求静默**：非关键数据请求失败不再弹出全局错误提示。
- **社区 UI 改进**：Hero 标签动态绑定、工作台入口、动态热门标签、动态 Feed 标签页、顶部栏对齐修复、社区页标签筛选栏。

### 全面功能增强（17 项）

- **新组件**：SkeletonCard 骨架屏、ImageLightbox 全屏灯箱、NotificationPanel 通知面板。
- **新 API**：社交 API 模块（关注/通知/排行榜/收藏/模板）、Pinia 通知 Store。
- **新页面**：排行榜页面 `/ranking`。
- **社区增强**：无限滚动、图片灯箱、分享按钮、相似作品、评论回复。
- **MainLayout 增强**：搜索建议、通知面板。
- **工作台增强**：模板面板（9 种预设尺寸）、导出格式弹窗。
- **PWA 支持**：manifest.json、Service Worker、SW 注册。
- **国际化**：vue-i18n 框架，中英文语言包，48+ 字符串替换为 `$t()` 调用。

### 后端社交与社区 API

- **新表**：`follows`（关注关系）、`notifications`（通知系统）。
- **新 DAO**：`SocialDao.java`（关注/通知/排行榜/收藏/模板）。
- **新 Servlet**：`SocialServlet.java`（12 个路由）。
- **修改**：`CommunityDao` 新增相似作品、`AuthFilter` 新增公开端点。

### 上传与工作台联动

- 上传成功后弹出对话框询问是否进入工作台编辑。
- 工作台和统一使用 `pixel_lab_workbench_image` localStorage 键，所有入口行为一致。

### 图片说明与相册

- **图片说明**：`image` 表新增 `description` 列，上传时可填写说明，个人中心可编辑。
- **相册功能**：`albums` 和 `album_images` 表，相册 CRUD、图片管理、排序，个人中心新增"作品集"标签页。
