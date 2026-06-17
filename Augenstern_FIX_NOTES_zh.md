# UI 修复说明

## 需求描述

此更改集解决了三个前端问题：

- 浅色模式下底部停靠栏保持深色。
- 绘图页面显示网格区域，但直接绘制不可靠，因为交互式画布层没有明确调整大小和分层。
- 个人中心的图像操作过于隐蔽，因为它们隐藏在深色悬停覆盖层中。
- Node 后端登录成功，但下一个认证请求立即显示会话已过期。

## 实际更改

### 浅色模式停靠栏

- 更新 `src/components/common/MainLayout.vue`。
- 为 `.dock-nav` 添加了浅色主题覆盖。
- 当 `data-theme='light'` 时，底部停靠栏现在使用半透明白色背景、较浅的边框和较浅的阴影。
- 现有深色模式停靠栏样式得以保留。

### 绘图画布

- 更新 `src/views/draw/Index.vue`。
- 添加 `syncFabricCanvasLayout()` 以将 Fabric 生成的包装器、上画布和下画布与所选绘图尺寸同步。
- 添加 `refreshCanvasOffset()` 以便 Fabric 在缩放后和画布平移结束后重新计算指针偏移。
- 将网格覆盖层移动到 Fabric 画布层后面，使其通过透明像素保持可见，而不会阻挡或视觉上覆盖绘图表面。
- 为 Fabric 的动态 `.canvas-container`、`.lower-canvas` 和 `.upper-canvas` 元素添加了作用域 `:deep()` CSS。

### 个人中心图像操作

- 更新 `src/views/personal/Index.vue`。
- 将仅悬停的中心覆盖层替换为每个图像卡片右上角的固定三点按钮。
- 菜单保持现有操作：查看图像、切换公开/私密可见性和删除图像。
- 添加 `handleImageCommand()` 以将下拉命令路由到现有操作处理程序。

### 登录令牌处理

- 更新 `src/store/user.js`。
- 用户存储现在从本地存储初始化 `token`，而不是总是清除它。
- 登录现在在 Node 后端返回 JWT 时保存 `data.token`。
- 清除令牌仍然会从本地存储中删除它，因此登出行为不变。
- 更新 `src/utils/request.js`。
- 当存在保存的令牌时，请求现在附加 `Authorization: Bearer <token>`。
- `withCredentials: true` 仍然到位，因此 Java 后端 cookie-session 行为仍然受支持。

### 首页社区化 UI 收尾

- 更新 `src/views/dashboard/Index.vue`。
- 将首页重排为更贴近预览图的社区首页：左侧为 Hero + 精选作品流，右侧发现栏与 Hero 顶部对齐。
- 补齐预览图中的右侧栏内容：创作中心、带计数的热门标签、推荐创作者卡片和社区动态。
- 新增作品分类标签，并在宽屏下使用 4 列精选作品网格，让首屏更像“作品信息流主入口”。
- 为 Hero 增加作品拼贴、标签贴纸和收藏按钮视觉点。
- 新增 `public/sample-images/*` 本地示例图；当社区接口没有返回作品时，首页仍会显示完整示例内容。
- 更新 `src/components/common/MainLayout.vue`。
- 增加 `作品分享社区` 品牌徽标、通知红点和头像下拉箭头，使顶部导航更贴近 UI 预览图。

### 首页可选请求静默失败

- 更新 `src/utils/request.js`、`src/api/community.js` 和 `src/api/auth.js`。
- 新增可选 `silent` 请求配置，让首页非关键展示数据请求失败时不再弹出全局错误提示。
- 首页的公开作品、统计和社区动态请求现在使用 `silent: true`；其他业务请求默认仍显示错误提示。

## 验证

- `npm ci` 在沙箱外运行后完成，因为 npm 需要访问用户缓存目录。
- `npm run build` 通过。
- 在 `http://127.0.0.1:5173` 启动了 Vite 开发服务器。
- Playwright QA 使用了下载的 Chromium，因为浏览器插件不可用，系统 Edge 可执行文件在无头模式下立即退出。
- 验证浅色模式下的 `/dashboard`：`.dock-nav` 计算的背景为 `rgba(255, 255, 255, 0.88)`。
- 验证 `/draw`：Fabric 呈现两个画布层，直接鼠标绘制改变了下画布数据，指针坐标更新没有页面错误。
- 验证 `/personal` 使用模拟图像数据：右上角三点菜单显示 `View image`、`Set private` 和 `Delete image`；选择查看打开了预览对话框。
- 针对运行在端口 `8080` 的后端验证 Node 认证：`/api/auth/login` 返回 `code=200` 和令牌，然后 `/api/auth/userinfo` 在使用 `Authorization: Bearer <token>` 调用时返回 `code=200`。
- 认证修复后重新运行 `npm run build` 并通过。
- 首页社区化收尾后重新运行 `npm run build` 并通过。
- 系统 Edge 在 headless 模式下仍会立即退出，因此安装了与 Codex runtime 中 Playwright 版本匹配的 Chromium。
- 使用模拟本地登录态抓取 `/dashboard` 的 `1905x1064` Playwright 截图。
- 对照 `UI预览.png` 检查：Hero 与右侧栏顶部对齐，右侧栏显示四个预览模块，精选作品流宽屏为 4 列，没有全局错误提示，也没有横向溢出。
- 剩余无关的控制台警告：Element Plus 报告 `el-radio` `label` 充当 `value` 已弃用。此警告存在于更改的行为之外。
