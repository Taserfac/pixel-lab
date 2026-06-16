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
- Remaining unrelated console warning: Element Plus reports that `el-radio` `label` acting as `value` is deprecated. This warning existed outside the changed behavior.

## 2026-06-16 Drawing Selection Update

### Request

- Add a marquee selection tool to the hand drawing board.
- Let the selected area be deleted, copied, or cut.
- Change the drawing board background default to white.

### Actual Changes

- Updated `src/views/draw/Index.vue`.
- Added a `框选 (S)` tool to the left tool rail and the inspector tool selector.
- Added a visible dashed selection rectangle while dragging on the canvas.
- Added in-canvas selection actions for copy, cut, and delete.
- Added keyboard shortcuts: `S` for selection, `Ctrl+C` to copy the selected area, `Ctrl+X` to cut, `Ctrl+V` to paste, `Delete/Backspace` to delete, and `Escape` to clear the selection.
- Selection delete and cut use Fabric's `destination-out` compositing and are saved in the existing history stack.
- Selection copy and cut store an internal image clipboard; paste inserts the copied area back as a Fabric image layer.
- Changed the drawing board's default `backgroundMode` from `transparent` to `white`.

### Verification

- `npm run build` passed.
- Started the Vite dev server at `http://127.0.0.1:5173`.
- Browser interaction validation could not be completed because Playwright's bundled Chromium was not installed and no system Edge/Chrome executable was available in this environment.

## 2026-06-16 Image Category Upload And Community Filters

### Request

- Let uploaded images choose a work type/tag.
- Add community sections or tags so users can browse by category.

### Actual Changes

- Added `src/constants/imageCategories.js` with shared frontend category options.
- Updated `src/api/image.js` so image uploads can send `category` and `tags`.
- Updated `src/views/personal/Index.vue`.
- Personal-center uploads now open a work-type picker before uploading.
- Personal work cards display the saved category badge when `image.tags` exists.
- Updated `src/views/community/Index.vue`.
- Community Plaza now has category tabs and sends `category` to `/api/community/images`.
- Community cards and detail dialogs display the work category.
- Updated `src/views/workbench/Index.vue` and `src/views/draw/Index.vue`.
- Workbench saves default to the `edit` category; hand-drawn saves default to the `drawing` category.
- Updated Java backend upload and community query code to save `image.tags` and filter public images by `category`.
- Updated Node backend upload and community query code the same way for compatibility.
- Updated `pixel-lab-backend/db/sql/images.sql` so new image tables include community/tag fields.

### Verification

- `npm run build` passed.
- `node --check` passed for the modified Node backend controller/service files.
- Java backend build was not run because neither `java.exe` nor `mvn.cmd` is available in the current environment.
