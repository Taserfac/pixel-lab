# UI Fix Notes

## Request

This change set addresses three frontend issues:

- The bottom dock stayed dark in light mode.
- The drawing page showed the grid area, but direct drawing was unreliable because the interactive canvas layer was not explicitly sized and layered.
- The image actions in the personal center were too subtle, because they were hidden inside a dark hover overlay.
- The Node backend login succeeded, but the next authenticated request immediately showed the session as expired.

## Actual Changes

### Light Mode Dock

- Updated `src/components/common/MainLayout.vue`.
- Added a light-theme override for `.dock-nav`.
- The bottom dock now uses a translucent white background, a lighter border, and a lighter shadow when `data-theme='light'`.
- The existing dark-mode dock styling is preserved.

### Drawing Canvas

- Updated `src/views/draw/Index.vue`.
- Added `syncFabricCanvasLayout()` to synchronize Fabric's generated wrapper, upper canvas, and lower canvas with the selected drawing size.
- Added `refreshCanvasOffset()` so Fabric recalculates pointer offsets after zooming and after canvas panning ends.
- Moved the grid overlay behind the Fabric canvas layers so it remains visible through transparent pixels without blocking or visually covering the drawing surface.
- Added scoped `:deep()` CSS for Fabric's dynamic `.canvas-container`, `.lower-canvas`, and `.upper-canvas` elements.

### Personal Center Image Actions

- Updated `src/views/personal/Index.vue`.
- Replaced the hover-only center overlay with a fixed three-dot button in the top-right corner of each image card.
- The menu keeps the existing actions: view image, toggle public/private visibility, and delete image.
- Added `handleImageCommand()` to route dropdown commands to the existing action handlers.

### Login Token Handling

- Updated `src/store/user.js`.
- The user store now initializes `token` from local storage instead of always clearing it.
- Login now saves `data.token` when the Node backend returns a JWT.
- Clearing the token still removes it from local storage, so logout behavior is unchanged.
- Updated `src/utils/request.js`.
- Requests now attach `Authorization: Bearer <token>` when a saved token exists.
- `withCredentials: true` remains in place, so Java backend cookie-session behavior is still supported.

### Dashboard UI/UX Completion

- Updated `src/views/dashboard/Index.vue`.
- Reworked the dashboard into a preview-matched community homepage: left column hero + featured feed, right column discovery rail aligned with the hero.
- Added the missing right-rail content from the UI preview: creation center actions, colored hot tag counters, recommended creator cards, and community activity stats.
- Added feed category tabs and a 4-column wide-screen featured grid so the first viewport reads as a works-first community feed.
- Added a hero artwork collage with label chips and a small favorite affordance.
- Added local sample works/creators backed by `public/sample-images/*` so the homepage still looks complete when the optional community APIs return no data.
- Updated `src/components/common/MainLayout.vue`.
- Added the `作品分享社区` brand badge, notification dot, and avatar caret to better match the preview header.

### Optional Dashboard Request Silence

- Updated `src/utils/request.js`, `src/api/community.js`, and `src/api/auth.js`.
- Added an optional `silent` request config so non-critical dashboard data fetches can fail without covering the first viewport with global error toasts.
- Dashboard requests for public images, stats, and activities now use `silent: true`; normal app requests still show errors by default.

### Community UI Improvements

- Updated `src/views/dashboard/Index.vue`.
- **Hero labels bound to image data**: Replaced the 3 hardcoded hero labels ("插画", "摄影", "像素艺术") with a `v-for` that extracts tags from `previewWorks`. Added `heroLabels` computed that deduplicates tags and falls back to defaults when no data exists.
- **Workbench entry in creation center**: Added a new `creationActions` entry `{ label: '工作台', icon: Picture, path: '/workbench' }` so the image editor is accessible from the homepage. Added `.creation-purple` CSS for the purple icon variant.
- **Dynamic hot tags**: Changed `popularTags` from a hardcoded array to a `computed` property that counts tag occurrences across `works`, sorts by frequency descending, and displays the top 5. Falls back to the original hardcoded defaults when no API data is available.
- **Dynamic feed tabs**: Changed `feedTabs` from a hardcoded array to a `computed` property that extracts unique tags from `works`, prepending the fixed ["推荐", "最新", "关注"] tabs.
- Updated `src/components/common/MainLayout.vue`.
- **Top bar alignment fix**: Changed `.top-bar` from CSS Grid to Flexbox layout. The search bar now uses `flex: 1; max-width: 520px; margin: 0 auto` to center itself between the brand and action buttons, fixing the left-shift alignment issue. Updated responsive breakpoints accordingly.
- Updated `src/views/community/Index.vue`.
- **Tag filter bar for community page**: Added a horizontal tag filter bar below the search bar with chips: "全部", "插画", "摄影", "设计", "UI设计", "像素艺术", "AI艺术". Clicking a chip sets `keyword` to the tag name and reloads works; clicking "全部" or the active chip clears the filter. Added `.tag-filter-bar` and `.tag-filter-chip` styles with active/hover states and responsive sizing.

### Comprehensive Feature Enhancement (17 features)

This change set adds 17 new features across the entire application:

#### New Dependencies
- Added `vue-i18n@9` for internationalization support.

#### New Components
- **`src/components/common/SkeletonCard.vue`** — Skeleton loading card component with configurable rows, avatar, and image placeholders. Uses CSS shimmer animation. Replaces spinner during initial data load on dashboard.
- **`src/components/common/ImageLightbox.vue`** — Full-screen image preview lightbox with zoom in/out (25%-400%), rotate, download, and keyboard navigation (Escape to close). Teleports to body for proper z-index layering.
- **`src/components/common/NotificationPanel.vue`** — Notification popover panel with typed notifications (like/comment/follow/system), unread count badge, and "mark all read" functionality. Mock data included.

#### New API & Store
- **`src/api/social.js`** — New social API module with endpoints: follow/unfollow users, get followers/following, notifications (list/read/mark-all/unread-count), rankings, similar works, collections, and templates. All endpoints follow existing request.js patterns.
- **`src/store/notification.js`** — Pinia notification store managing notification state, unread count, and actions (fetch, mark read, mark all read).

#### New Views
- **`src/views/ranking/Index.vue`** — Leaderboard page with weekly/monthly/all-time tabs. Shows ranked works with gold/silver/bronze medal styling, thumbnails, titles, authors, and like counts. Route: `/ranking`.

#### PostCard Tags
- **`src/components/community/PostCard.vue`** — Added tag chips below the card title. Tags are extracted from `work.tags` (max 3), displayed as green pill badges. Clicking a tag emits `tag-click` event for parent filtering.

#### Community Page Enhancements (`src/views/community/Index.vue`)
- **Infinite scroll** — Replaced "加载更多" button with IntersectionObserver. Scroll sentinel triggers auto-load when 200px from viewport bottom. Disconnects on unmount.
- **Image lightbox** — Clicking a PostCard opens the ImageLightbox instead of navigating away. Full-screen preview with zoom/rotate/download controls.
- **Share button** — Added share button in detail dialog. Uses Web Share API on mobile, clipboard fallback on desktop.
- **Similar works** — Detail dialog shows "相似作品" section with up to 4 same-tag recommendations at the bottom.
- **Comment replies** — Added reply button on each comment. Shows reply input with "取消回复" option. Reply data includes `replyTo` field. Nested reply display with indented thread styling.

#### MainLayout Enhancements (`src/components/common/MainLayout.vue`)
- **Search suggestions** — Search bar now shows dropdown with recent search history (persisted to localStorage, max 5) and hot search tags. Quick search on suggestion click.
- **Notification panel** — Replaced static bell button with el-popover NotificationPanel. Shows unread count badge (red number), typed notification icons with colors, and "全部已读" action.

#### Dashboard Skeleton (`src/views/dashboard/Index.vue`)
- Shows SkeletonCard placeholders (8 cards) during initial data load instead of empty spinner.

#### Workbench Enhancements (`src/views/workbench/`)
- **Template panel** — New `TemplatePanel.vue` component with 9 preset canvas sizes (Instagram Post/Story, Facebook Cover, Twitter Post, YouTube Thumbnail, Poster A4, Business Card, Desktop/Mobile Wallpaper) plus custom size input. Accessible via new "模板" button in ActionBar.
- **Export format dialog** — Download button now opens a format selection dialog with PNG/JPG/WebP options and quality slider (10-100%) for lossy formats. Replaces direct PNG download.
- **ActionBar update** — Added "模板" button with Grid icon between existing actions.

#### PWA Support
- **`public/manifest.json`** — PWA manifest with app name "Pixel Lab Pro", green theme (#16C784), standalone display mode, and icon references.
- **`public/sw.js`** — Service worker with cache-first strategy for static assets and network-first for API calls. Caches root and key routes.
- **`src/main.js`** — Added Service Worker registration on page load. Added `vue-i18n` plugin registration.
- **`index.html`** — Added manifest link and corrected theme-color meta tag.

#### Internationalization Framework
- **`src/i18n/index.js`** — vue-i18n instance with Chinese as default locale, legacy mode disabled for Vue 3 composition API.
- **`src/i18n/locales/zh-CN.js`** — Complete Chinese locale file with 150+ translation keys organized by domain (nav, action, dashboard, community, workbench, auth, personal, admin, settings, notification, tag, ranking, lightbox).
- **`src/i18n/locales/en-US.js`** — Complete English translation file with all corresponding keys.
- **`src/router/index.js`** — Added `/ranking` route with lazy-loaded Ranking view.

#### Social API Endpoints (backend needed)
The following API endpoints are defined but require backend implementation:
- `POST /api/social/follow`, `DELETE /api/social/follow/:userId`
- `GET /api/social/following`, `GET /api/social/followers`
- `GET /api/social/follow/status/:userId`
- `GET /api/social/notifications`, `PATCH /api/social/notifications/:id/read`, `PATCH /api/social/notifications/read-all`
- `GET /api/social/notifications/unread-count`
- `GET /api/social/rankings`
- `GET /api/community/images/:id/similar`
- `GET /api/social/collections`, `POST /api/social/collections`
- `GET /api/templates`

### i18n Internationalization Integration

- Updated `src/components/common/MainLayout.vue`, `src/views/dashboard/Index.vue`, `src/views/community/Index.vue`.
- Added `useI18n` composable and replaced 48+ hardcoded Chinese strings with `$t()` calls across the three main pages.
- **MainLayout**: Brand tag, search placeholder, upload button, notification title, dock nav labels, dropdown menu items.
- **Dashboard**: Hero title/subtitle, creation center labels (now using computed for dynamic i18n), hot tags title, recommended creators, community stats, empty state text.
- **Community**: Search placeholder, sort buttons, tag filter "全部", detail dialog (title, stats, like/collect/share buttons), comments section, empty states.
- Feed tab labels and sample data kept as-is (used for tag matching logic).

### Backend: Social & Community API Endpoints

Added new social features to the Java backend (plain Servlet + raw JDBC, matching existing architecture):

#### New Files

- **`src/main/resources/schema.sql`** — Database schema for new tables:
  - `follows` — user follow relationships (follower_id, followed_id, unique constraint, cascading foreign keys)
  - `notifications` — notification system (user_id, sender_id, type, content, reference_id, is_read, indexed)

- **`src/main/java/com/pixellab/dao/SocialDao.java`** — New DAO with methods:
  - `follow()` / `unfollow()` — transactional follow/unfollow with self-follow prevention
  - `checkFollowStatus()` — check if user A follows user B
  - `getFollowing()` / `getFollowers()` — paginated follow lists with user info
  - `getNotifications()` / `getUnreadCount()` — paginated notifications with sender info
  - `markNotificationRead()` / `markAllRead()` — mark notifications as read
  - `createNotification()` — create new notification (called from other DAOs on like/comment/follow events)
  - `getRankings()` — ranked works by like count with optional weekly/monthly time filter
  - `getSimilarWorks()` — find works with matching first tag
  - `getCollections()` — paginated user collections
  - `getTemplates()` — static list of 9 canvas templates (social media, print, wallpaper sizes)

- **`src/main/java/com/pixellab/servlet/SocialServlet.java`** — New servlet mapped to `/api/social/*`:
  - `GET /api/social/rankings` — public, supports `type` param (weekly/monthly/all)
  - `GET /api/social/templates` — public, returns template presets
  - `POST /api/social/follow` — auth required, body: `{userId}`
  - `DELETE /api/social/follow/{userId}` — auth required
  - `GET /api/social/follow/status/{userId}` — auth required
  - `GET /api/social/following` — auth required, paginated
  - `GET /api/social/followers` — auth required, paginated
  - `GET /api/social/notifications` — auth required, paginated
  - `GET /api/social/notifications/unread-count` — auth required
  - `PATCH /api/social/notifications/{id}/read` — auth required
  - `POST /api/social/notifications/read-all` — auth required
  - `GET /api/social/collections` — auth required, paginated

#### Modified Files

- **`src/main/java/com/pixellab/dao/CommunityDao.java`** — Added `similarWorks(imageId, limit)` method: finds works sharing the same first tag, ordered by like count.

- **`src/main/java/com/pixellab/servlet/CommunityServlet.java`** — Added `GET /api/community/images/{id}/similar` route (public, returns similar works).

- **`src/main/java/com/pixellab/filter/AuthFilter.java`** — Added public GET endpoints:
  - `/api/community/images/{id}/similar`
  - `/api/social/rankings`
  - `/api/social/templates`

- **`src/main/webapp/WEB-INF/web.xml`** — Registered `SocialServlet` mapped to `/api/social/*`.

### Upload → Workbench Auto-Load

- Updated `src/components/common/MainLayout.vue`.
- After successful image upload, now shows an `ElMessageBox.confirm` dialog asking "是否进入工作台编辑此图片？"
- "进入工作台" stores the image URL in localStorage (`pixel_lab_workbench_image`) and navigates to `/workbench`.
- "留在个人中心" navigates to `/personal` as before.
- Updated `src/views/workbench/Index.vue` — Added `onMounted` hook that checks `localStorage.getItem('pixel_lab_workbench_image')`. If found, automatically calls `loadImage()` to load the image into the canvas, then removes the key. This means entering the workbench from upload OR from personal center ("在工作台打开") both auto-load the image.
- Updated `src/views/personal/Index.vue` — Changed `openInWorkbench()` to use the same `pixel_lab_workbench_image` key (was `last-open-image`), ensuring consistent behavior across all entry points.

### Image Description / Diary

- Updated `src/main/resources/schema.sql` — Added `ALTER TABLE image ADD COLUMN description TEXT` for image descriptions.
- Updated `src/main/java/com/pixellab/dao/ImageDao.java` — Added `description` parameter to `create()` method and new `updateDescription(id, description)` method.
- Updated `src/main/java/com/pixellab/servlet/ImageServlet.java` — Upload handler now reads `description` from form data. Added `PATCH /api/images/{id}/description` endpoint with ownership validation.
- Updated `src/api/image.js` — Added `updateImageDescription(id, description)` API function.
- Updated `src/views/personal/Index.vue` — Each image card now shows description text (truncated 2 lines) with an "编辑说明" button. Editing opens an el-input dialog and saves via `updateImageDescription`.

### Album / Collection Feature

- Updated `src/main/resources/schema.sql` — Added `albums` table (user_id, title, description, cover_image_id, is_public, status) and `album_images` table (album_id, image_id, sort_order, per-image description) with cascading foreign keys.
- Created `src/main/java/com/pixellab/dao/AlbumDao.java` — 10 methods: createAlbum, updateAlbum, deleteAlbum (soft), getAlbum (with count), getUserAlbums, getAlbumImages, addImageToAlbum, removeImageFromAlbum, updateAlbumImageDescription, reorderAlbumImages.
- Created `src/main/java/com/pixellab/servlet/AlbumServlet.java` — Mapped to `/api/albums/*` with 9 routes (CRUD for albums, add/remove/update images, reorder).
- Updated `src/main/webapp/WEB-INF/web.xml` — Registered AlbumServlet.
- Updated `src/main/java/com/pixellab/filter/AuthFilter.java` — Added `GET /api/albums/{id}` as public endpoint.
- Created `src/api/album.js` — Frontend API module with all album endpoints.
- Updated `src/views/personal/Index.vue` — Added new "作品集" tab with:
  - Album list view (2-column grid, cover image, title, description, image count)
  - Album detail view (image list with per-image descriptions)
  - Create album dialog (title, description, cover image picker)
  - Add images dialog (select from user's images)
  - Per-image description editing within albums
  - Responsive styles for album components

## Verification

- `npm ci` completed after running outside the sandbox because npm needed access to the user cache directory.
- `npm run build` passed.
- Started the Vite dev server at `http://127.0.0.1:5173`.
- Playwright QA used downloaded Chromium because the Browser plugin was unavailable and the system Edge executable exited immediately in headless mode.
- Verified `/dashboard` in light mode: `.dock-nav` computed background was `rgba(255, 255, 255, 0.88)`.
- Verified `/draw`: Fabric rendered two canvas layers, direct mouse drawing changed the lower canvas data, and pointer coordinates updated without page errors.
- Verified `/personal` with mocked image data: the top-right three-dot menu showed `View image`, `Set private`, and `Delete image`; selecting view opened the preview dialog.
- Verified Node auth against the running backend on port `8080`: `/api/auth/login` returned `code=200` and a token, then `/api/auth/userinfo` returned `code=200` when called with `Authorization: Bearer <token>`.
- Re-ran `npm run build` after the auth fix and it passed.
- Re-ran `npm run build` after the dashboard UI completion and it passed.
- Re-ran `npm run build` after the comprehensive feature enhancement and it passed (11.85s, all 2285 modules transformed).
- Installed `vue-i18n@9` for internationalization support.
- Created 8 new component/view files, 2 new API/store files, 2 locale files, 2 PWA files.
- Installed Playwright Chromium matching the bundled Playwright runtime after system Edge exited immediately in headless mode.
- Captured a `1905x1064` Playwright screenshot of `/dashboard` with a mocked local login state.
- Compared the screenshot against `UI预览.png`: hero/right rail top alignment matched, the right rail showed four preview modules, the featured feed rendered four wide-screen columns, no global error toast appeared, and there was no horizontal overflow.
- Remaining unrelated console warning: Element Plus reports that `el-radio` `label` acting as `value` is deprecated. This warning existed outside the changed behavior.
