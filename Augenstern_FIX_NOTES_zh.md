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

### 工作台修复与改进

- **修改时间**: 2026-06-18 21:30
- **修改文件**: `pixel-lab-frontend/src/views/workbench/Index.vue`
- **问题描述**:
  1. 裁剪框与图片不对准：裁剪时 canvas 存在 viewportTransform（缩放/平移），导致裁剪坐标与导出像素不匹配。
  2. 从个人中心导入的图片没有铺满屏幕：`loadImageUrl` 中 `Math.min(..., 1)` 限制了小图不会放大。
  3. 添加文字后右键删除功能失效：切换工具模式时文字对象被设为 `evented: false`，无法接收右键事件。
- **修改内容**:
  1. **裁剪坐标修正**：`mergeToTopForCrop` 和 `applyCrop` 中在导出前重置 viewportTransform 为单位矩阵，导出后恢复，确保裁剪坐标与导出像素 1:1 对应。同步修复了 `canvasDataUrl` 导出函数的 viewport 问题。
  2. **图片铺满画布**：移除 `Math.min(..., 1)` 中的 `1` 上限，小图也会按比例放大以填满画布区域。
  3. **右键菜单常驻可用**：`setTextSelectable` 中将 `evented` 始终设为 `true`，仅切换 `selectable`/`hasControls`/`editable`，保证文字对象在任何工具模式下都能响应右键菜单。

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

### 工作台修复（第二批）

- **修改时间**: 2026-06-18 22:15
- **修改文件**: `pixel-lab-frontend/src/views/workbench/Index.vue`, `pixel-lab-frontend/src/views/personal/Index.vue`
- **问题描述**:
  1. 裁剪模式下切换到其他工具后图片消失：`mergeToTopForCrop` 清空了底层 canvas，取消时未恢复底图。
  2. 裁剪框依旧没有对准图片：离屏 canvas 使用 CSS 尺寸而非实际像素尺寸，导致坐标空间不匹配。
  3. 文字在其他工具模式下仍可被选中：上一轮修复中 `evented` 始终为 true 导致。
  4. 右键删除文字交互不直观：应改为文字面板中的删除按钮。
  5. 橡皮擦只能擦除画笔线条：`destination-out` 合成模式只影响路径像素，无法删除 Fabric 对象。
  6. 个人中心缺少修改图片说明的入口。
  7. 个人中心缺少修改图片标签的功能。
- **修改内容**:
  1. **裁剪取消恢复底图**：`cancelTool` 中检测裁剪取消，将 `baseImage` 重新添加到底层 canvas。
  2. **裁剪坐标修正**：`mergeToTopForCrop` 中离屏 canvas 改用 `bgCanvasRef.value.width/height`（实际像素尺寸），确保导出数据与 canvas 坐标 1:1 对应。
  3. **文字不可选中**：`setTextSelectable` 恢复 `evented` 与 `selectable` 联动，非文字工具模式下文字对象完全不可交互。
  4. **删除按钮移入文字面板**：移除右键菜单及其事件监听，文字面板新增"删除选中文字"按钮（基于 `getActiveObject` 判断选中状态）。
  5. **橡皮擦支持删除对象**：新增 `eraserCheckObject` 事件处理，在橡皮擦模式下通过 `findTarget` 检测擦过位置的对象并移除（排除底图和裁剪框）。
  6. **个人中心编辑信息**：作品卡片下拉菜单新增"编辑信息"选项，弹窗支持修改标题、说明和标签，通过 `PATCH /api/images/:id/metadata` 保存。

### Pixel Lab 全站体验重构、独立管理后台与登录回归修复

- **修改时间**: 2026-06-18 16:58
- **问题描述**:
  - 用户端页面缺少统一的视觉语言和基础组件，作品流、发布、个人中心与编辑器之间的体验割裂。
  - 管理员复用了用户端顶栏和底部导航，不符合后台管理场景。
  - 开发环境 API 固定指向 `localhost:8080`，当前端通过 `127.0.0.1` 访问时 Session Cookie 会因跨站限制丢失，表现为登录成功后个人中心立即跳回登录页。
  - 开发环境 Service Worker 缓存旧模块，可能导致路由白屏；作品评论兼容旧数据库时返回 500；排行榜参数格式错误；后台趋势 SQL 不兼容 `ONLY_FULL_GROUP_BY`。

- **前端修改文件**:
  - `.env.development`
  - `public/sw.js`
  - `src/main.js`
  - `src/router/index.js`
  - `src/utils/request.js`
  - `src/api/image.js`
  - `src/components/common/MainLayout.vue`
  - `src/components/admin/AdminLayout.vue`（新建）
  - `src/components/community/PostCard.vue`
  - `src/components/ui/UiModal.vue`、`UiTag.vue`、`UploadBox.vue`（新建）
  - `src/views/explore/Index.vue`、`discover/Index.vue`、`publish/Index.vue`、`post/Index.vue`（新建）
  - `src/views/personal/Index.vue`
  - `src/views/workbench/Index.vue`
  - `src/views/admin/Index.vue`
  - `src/views/auth/Login.vue`
  - `src/views/ranking/Index.vue`

- **后端修改文件**:
  - `src/main/java/com/pixellab/util/Result.java`
  - `src/main/java/com/pixellab/dao/ImageDao.java`
  - `src/main/java/com/pixellab/dao/AdminDao.java`
  - `src/main/java/com/pixellab/dao/CommunityDao.java`
  - `src/main/java/com/pixellab/servlet/ImageServlet.java`
  - `src/main/java/com/pixellab/servlet/AuthServlet.java`
  - `src/main/java/com/pixellab/servlet/AlbumServlet.java`

- **修改内容**:
  - 重建用户端应用壳：统一搜索、通知、主题、账户入口和浮动底栏；底部导航全部改为“探索 / 发现 / 发布 / 创作 / 我的”。
  - 新增 Explore、Discover、Publish、作品详情页面，并重写 Profile；加入真实 API 数据、加载态、空状态、筛选、草稿和作品管理操作。
  - 重写 Studio 为 Fabric.js 三栏编辑器，支持选取、画笔、橡皮、图层、滤镜、亮度/对比度/饱和度、撤销/重做、快捷键、保存与导出。
  - 将 `/dashboard` 从用户布局中彻底拆出，新增专用后台侧栏、顶栏和移动端抽屉；数据概览、用户管理和作品管理由查询参数切换。
  - 开发 API 改为同源 `/api` 代理，修复 `localhost` 与 `127.0.0.1` 混用导致的 Session 丢失；登录后按安全的 `redirect` 参数返回原页面。
  - 开发环境主动注销旧 Service Worker 并清理 Pixel Lab 缓存；生产缓存升级为 v2，页面导航网络优先，避免旧 `index.html` 和登录状态被缓存污染。
  - 后端成功业务码统一为 `0`，前端同时兼容旧版 2xx 业务码与 `message/msg` 字段。
  - 新增作品元数据更新接口；Dashboard 返回用户增长、作品增长和标签分布的真实数据。
  - 评论查询兼容没有 `status` 字段的早期数据库；后台趋势查询改为子查询聚合，兼容 MySQL `ONLY_FULL_GROUP_BY`。
  - 修复排行榜请求参数和后端字段映射；个人中心各内容请求增加失败兜底，避免单个接口故障导致页面不可用。

- **新视觉概念与 QA 产物**:
  - `pixel-lab-frontend/docs/design/explore-concept.png`
  - `pixel-lab-frontend/docs/design/studio-concept.png`
  - `pixel-lab-frontend/docs/qa/` 中保存了本轮桌面与移动端回归截图。

- **验证结果**:
  - 使用仓库内 `.tools/apache-maven-3.9.9/bin/mvn.cmd` 执行 `clean package`，33 个 Java 源文件重新编译，WAR 构建成功。
  - `npm run build` 成功，Vite 转换 2270 个模块。
  - 对本轮前端文件执行 ESLint：`0 errors`；保留项目既有的模板换行类 warning，未进行大范围自动格式化。
  - Browser 插件在当前环境不可用，因此按调试技能要求使用项目 Playwright 运行时回归；验证桌面和移动端、登录、Explore、Discover、Publish、Studio、作品详情、Profile、Settings、Stats、Ranking 与独立 Dashboard。
  - 真实浏览器验证 `test1` 可登录并进入 `/profile`，控制台无错误；管理员 Dashboard 中 `.bottom-nav` 数量为 0、`.admin-sidebar` 数量为 1。
  - 最终部署 WAR 后直连验证：管理员登录、个人作品、后台统计、评论、排行榜和 `test1` 登录均返回 `code=0`；后台增长数据正常返回。

### 工作台图片编辑器重构

- **中文文本统一**：`MainLayout.vue` 中用户菜单 "Profile" → "个人资料"、"Dashboard" → "控制台"；`workbench/Index.vue` 中空状态提示和导出按钮 "Profile" → "个人中心"。
- **图片编辑模式**：工作台从绘图画布编辑器重构为以图片编辑为核心的工具（类似美图秀秀），删除空画布、图层、选区等绘画相关功能。
- **工具栏**：左侧工具栏改为 6 个图片编辑工具——裁剪(C)、调色(A)、滤镜(F)、文字(T)、涂鸦(D)、马赛克(M)。
- **裁剪**：支持自由裁剪和 6 种预设比例（1:1 / 4:3 / 3:2 / 16:9 / 9:16），可拖拽调整裁剪框，应用后离屏裁剪写回图片。
- **调色**：亮度、对比度、饱和度、色温 4 项调整，滑块实时预览（`@input`），保存/取消确认。
- **工具保存/取消机制**：进入工具时自动保存 canvas 快照，每个工具面板底部显示"保存"和"取消"按钮。切换工具或按 Esc 时未保存的修改自动回滚到进入工具前的状态。
- **滤镜**：10 种预设滤镜（原图/清晰/暖色/冷色/黑白/复古/胶片/日系/美食/油画），缩略图网格展示。
- **文字**：可调整字号、颜色、字体（Arial/黑体/宋体/楷体/微软雅黑），添加后可拖拽编辑。
- **涂鸦笔**：集成画笔和橡皮两种模式，画笔支持颜色和大小调节，橡皮支持大小调节（使用 `destination-out` 合成实时擦除），切换画笔/橡皮模式时自动同步画笔参数。
- **马赛克**：可调整马赛克块大小，鼠标涂抹区域实时生成彩色方块模拟马赛克效果。修复了马赛克绘制时图片跟随移动的问题（`preventDefault` + `stopPropagation` 阻止事件冒泡）。

### 编辑器双层 Canvas 架构重构

- **问题描述**：橡皮擦使用 `destination-out` 合成会穿透所有下层对象包括底图；取消按钮有时不生效（`setTool` 未 `await cancelTool`）；裁剪框与图片不对齐；图片无法铺满编辑区。
- **修改文件**: `pixel-lab-frontend/src/views/workbench/Index.vue`
- **修改内容**:
  - **双层 Canvas 架构**：底层 `bgCanvas` 放固定底图（不可交互），上层 `fabricCanvas` 放涂鸦/文字/马赛克/裁剪框。橡皮擦的 `destination-out` 只作用于上层，不会擦除底图。
  - **工具切换异步修复**：`setTool` 改为 `async`，切换工具时 `await cancelTool()` 确保快照恢复完成后再保存新快照。
  - **图片铺满**：底图通过 `Math.min(bg.width / img.width, bg.height / img.height, 1)` 铺满底层 canvas，裁剪框基于底图实际位置和尺寸计算。
  - **导出合并**：导出时将两层 canvas 合并为一张图片。
  - **右键删除文字**：上层 canvas 右键点击文字对象弹出"删除文字"菜单。
  - **文字工具隔离**：只有文字工具激活时文字对象可选中，切换到其他工具自动锁定。

### 部署缓存问题修复

- **问题描述**：重构代码后通过启动脚本部署，浏览器仍显示旧版本。根本原因是 Service Worker 缓存版本号未更新、`index.html` 和 `sw.js` 缺少缓存控制头、Tomcat work 目录残留编译缓存。
- **修改文件**:
  - `pixel-lab-frontend/public/sw.js`
  - `pixel-lab-java-backend/src/main/java/com/pixellab/filter/CacheControlFilter.java`（新建）
  - `pixel-lab-java-backend/src/main/webapp/WEB-INF/web.xml`
  - `start-java-backend.ps1`
- **修改内容**:
  - `sw.js`：`CACHE_NAME` 从 `pixel-lab-v2` 升级为 `pixel-lab-v3`，activate 时自动清除旧缓存；install 事件中 `skipWaiting()` 提前确保新版 SW 立即接管。
  - 新建 `CacheControlFilter`：对 `index.html` / `/` 添加 `no-cache, no-store, must-revalidate` 头；对 `sw.js` 添加 `no-cache, must-revalidate` 头；带内容哈希的静态资源不受影响。
  - `web.xml`：注册 `CacheControlFilter`，映射 `/*`，位于 `SpaFallbackFilter` 之前。
  - `start-java-backend.ps1`：`Install-WarToTomcat` 函数中新增清理 `work\Catalina` 目录逻辑，避免 Tomcat 编译缓存导致旧版本残留。

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
