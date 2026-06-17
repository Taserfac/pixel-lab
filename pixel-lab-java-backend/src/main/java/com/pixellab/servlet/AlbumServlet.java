package com.pixellab.servlet;

import com.pixellab.dao.AlbumDao;
import com.pixellab.model.SessionUser;
import com.pixellab.util.RequestUtil;
import com.pixellab.util.Result;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AlbumServlet extends BaseApiServlet {

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    AlbumDao dao = new AlbumDao(dataSource());
    try {
      // GET /api/albums — list user's albums
      if (segments.isEmpty()) {
        SessionUser user = currentUser(request);
        ok(response, dao.getUserAlbums(user.getId()));
        return;
      }
      // GET /api/albums/{id} — get album detail with images
      if (segments.size() == 1) {
        long albumId = parseId(segments.get(0));
        if (albumId <= 0) {
          Result.notFound(response, "作品集不存在");
          return;
        }
        Map<String, Object> album = dao.getAlbum(albumId);
        if (album == null) {
          Result.notFound(response, "作品集不存在");
          return;
        }
        List<Map<String, Object>> images = dao.getAlbumImages(albumId);
        album.put("images", images);
        ok(response, album);
        return;
      }
      // GET /api/albums/{id}/images — get images in album
      if (segments.size() == 2 && "images".equals(segments.get(1))) {
        long albumId = parseId(segments.get(0));
        if (albumId <= 0) {
          Result.notFound(response, "作品集不存在");
          return;
        }
        ok(response, dao.getAlbumImages(albumId));
        return;
      }
      Result.notFound(response, "接口不存在");
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Album API failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    AlbumDao dao = new AlbumDao(dataSource());
    SessionUser user = currentUser(request);
    try {
      // POST /api/albums — create album
      if (segments.isEmpty()) {
        Map<String, Object> body = body(request);
        String title = RequestUtil.string(body, "title");
        if (title == null || title.isBlank()) {
          Result.badRequest(response, "标题不能为空");
          return;
        }
        if (title.length() > 100) {
          Result.badRequest(response, "标题不能超过100字");
          return;
        }
        String description = RequestUtil.string(body, "description");
        Long coverImageId = body.get("coverImageId") == null ? null :
            Long.parseLong(String.valueOf(body.get("coverImageId")));
        long albumId = dao.createAlbum(user.getId(), title, description, coverImageId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", albumId);
        Result.write(response, HttpServletResponse.SC_CREATED, 201, "创建成功", data);
        return;
      }
      // POST /api/albums/{id}/images — add image to album
      if (segments.size() == 2 && "images".equals(segments.get(1))) {
        long albumId = parseId(segments.get(0));
        if (albumId <= 0) {
          Result.notFound(response, "作品集不存在");
          return;
        }
        Map<String, Object> album = dao.getAlbum(albumId);
        if (album == null || ((Number) album.get("user_id")).longValue() != user.getId()) {
          Result.forbidden(response, "无权操作此作品集");
          return;
        }
        Map<String, Object> body = body(request);
        Object rawImageId = body.get("imageId");
        if (rawImageId == null) {
          Result.badRequest(response, "缺少图片ID");
          return;
        }
        long imageId = Long.parseLong(String.valueOf(rawImageId));
        String description = RequestUtil.string(body, "description");
        dao.addImageToAlbum(albumId, imageId, description);
        ok(response, "添加成功", null);
        return;
      }
      Result.notFound(response, "接口不存在");
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Album API failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  @Override
  protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    AlbumDao dao = new AlbumDao(dataSource());
    SessionUser user = currentUser(request);
    try {
      // DELETE /api/albums/{id} — delete album
      if (segments.size() == 1) {
        long albumId = parseId(segments.get(0));
        if (albumId <= 0) {
          Result.notFound(response, "作品集不存在");
          return;
        }
        boolean deleted = dao.deleteAlbum(albumId, user.getId());
        if (!deleted) {
          Result.notFound(response, "作品集不存在或无权删除");
          return;
        }
        ok(response, "删除成功", null);
        return;
      }
      // DELETE /api/albums/{id}/images/{imageId} — remove image from album
      if (segments.size() == 3 && "images".equals(segments.get(1))) {
        long albumId = parseId(segments.get(0));
        long imageId = parseId(segments.get(2));
        if (albumId <= 0 || imageId <= 0) {
          Result.notFound(response, "参数错误");
          return;
        }
        Map<String, Object> album = dao.getAlbum(albumId);
        if (album == null || ((Number) album.get("user_id")).longValue() != user.getId()) {
          Result.forbidden(response, "无权操作此作品集");
          return;
        }
        boolean removed = dao.removeImageFromAlbum(albumId, imageId);
        if (!removed) {
          Result.notFound(response, "图片不在作品集中");
          return;
        }
        ok(response, "移除成功", null);
        return;
      }
      Result.notFound(response, "接口不存在");
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Album API failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  @Override
  protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    AlbumDao dao = new AlbumDao(dataSource());
    SessionUser user = currentUser(request);
    try {
      // PATCH /api/albums/{id} — update album
      if (segments.size() == 1) {
        long albumId = parseId(segments.get(0));
        if (albumId <= 0) {
          Result.notFound(response, "作品集不存在");
          return;
        }
        Map<String, Object> body = body(request);
        String title = RequestUtil.string(body, "title");
        if (title == null || title.isBlank()) {
          Result.badRequest(response, "标题不能为空");
          return;
        }
        if (title.length() > 100) {
          Result.badRequest(response, "标题不能超过100字");
          return;
        }
        String description = RequestUtil.string(body, "description");
        Long coverImageId = body.get("coverImageId") == null ? null :
            Long.parseLong(String.valueOf(body.get("coverImageId")));
        boolean updated = dao.updateAlbum(albumId, user.getId(), title, description, coverImageId);
        if (!updated) {
          Result.notFound(response, "作品集不存在或无权修改");
          return;
        }
        ok(response, "更新成功", null);
        return;
      }
      // PATCH /api/albums/{id}/images/{imageId} — update per-image description
      if (segments.size() == 3 && "images".equals(segments.get(1))) {
        long albumId = parseId(segments.get(0));
        long imageId = parseId(segments.get(2));
        if (albumId <= 0 || imageId <= 0) {
          Result.notFound(response, "参数错误");
          return;
        }
        Map<String, Object> album = dao.getAlbum(albumId);
        if (album == null || ((Number) album.get("user_id")).longValue() != user.getId()) {
          Result.forbidden(response, "无权操作此作品集");
          return;
        }
        Map<String, Object> body = body(request);
        String description = RequestUtil.string(body, "description");
        boolean updated = dao.updateAlbumImageDescription(albumId, imageId, description);
        if (!updated) {
          Result.notFound(response, "图片不在作品集中");
          return;
        }
        ok(response, "更新成功", null);
        return;
      }
      Result.notFound(response, "接口不存在");
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Album API failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  // PUT is handled by service() override in BaseApiServlet, but since BaseApiServlet
  // doesn't override doPut, we need to handle it via service() or override doPut.
  // BaseApiServlet.service() only overrides PATCH. doPut falls through to HttpServlet.doPut
  // which returns 405. We need to override service() to handle PUT, or simply use
  // the existing doPatch for reorder. But the spec says PUT /api/albums/{id}/reorder.
  // HttpServlet supports doPut, so we just override it.

  @Override
  protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    AlbumDao dao = new AlbumDao(dataSource());
    SessionUser user = currentUser(request);
    try {
      // PUT /api/albums/{id}/reorder — reorder images
      if (segments.size() == 2 && "reorder".equals(segments.get(1))) {
        long albumId = parseId(segments.get(0));
        if (albumId <= 0) {
          Result.notFound(response, "作品集不存在");
          return;
        }
        Map<String, Object> album = dao.getAlbum(albumId);
        if (album == null || ((Number) album.get("user_id")).longValue() != user.getId()) {
          Result.forbidden(response, "无权操作此作品集");
          return;
        }
        Map<String, Object> body = body(request);
        Object rawIds = body.get("imageIds");
        if (!(rawIds instanceof List)) {
          Result.badRequest(response, "缺少imageIds参数");
          return;
        }
        List<Long> imageIds = new ArrayList<>();
        for (Object item : (List<?>) rawIds) {
          imageIds.add(Long.parseLong(String.valueOf(item)));
        }
        dao.reorderAlbumImages(albumId, imageIds);
        ok(response, "排序更新成功", null);
        return;
      }
      Result.notFound(response, "接口不存在");
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Album API failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  private long parseId(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ignored) {
      return -1;
    }
  }
}
