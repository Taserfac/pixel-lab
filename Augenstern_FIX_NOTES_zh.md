# Pixel Lab 修改记录

---

## 2026-06-18

### 启动脚本优化

- **Maven 增量构建**：`start-java-backend.ps1` 中 `mvn clean package` 改为 `mvn package`，新增 `-Clean` 参数支持全量构建，启动速度显著提升。
- **PowerShell 命名规范修复**：`Normalize-ProcessPath` → `Set-ProcessPathCase`、`Build-War` → `New-WarPackage`、`Deploy-War` → `Install-WarToTomcat`，消除 PSScriptAnalyzer 警告。

### 项目文档

- **新建 CLAUDE.md**：包含项目架构、技术栈、启动命令、API 代理、代码约定等信息。加入 `.gitignore` 仅保留本地使用。
- **修改记录规范**：CLAUDE.md 中新增 `Change Log Rule`，要求每次修改后必须记录到修改文件。
- **同步中文修改记录**：将英文修改记录同步为中文版，补齐所有缺失章节。
- **Git 清理**：将 `Augenstern_FIX_NOTES_zh.md` 从 git 追踪中移除（`git rm --cached`），仅保留本地维护中文版。

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
