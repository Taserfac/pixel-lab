package com.pixellab.servlet;

import com.pixellab.dao.CommunityDao;
import com.pixellab.model.SessionUser;
import com.pixellab.util.RequestUtil;
import com.pixellab.util.Result;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CommunityServlet extends BaseApiServlet {
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    CommunityDao dao = new CommunityDao(dataSource());
    try {
      if (segments.size() == 1 && "images".equals(segments.get(0))) {
        SessionUser user = currentUser(request);
        Long userId = user == null ? null : user.getId();
        ok(response, dao.publicImages(
            RequestUtil.intParam(request, "page", 1),
            RequestUtil.intParam(request, "pageSize", 20),
            request.getParameter("keyword"),
            request.getParameter("sortBy"),
            userId));
        return;
      }
      if (segments.size() == 2 && "images".equals(segments.get(0))) {
        SessionUser user = currentUser(request);
        Long userId = user == null ? null : user.getId();
        Map<String, Object> detail = dao.imageDetail(parseId(segments.get(1)), userId);
        if (detail == null) {
          Result.notFound(response, "作品不存在");
          return;
        }
        ok(response, detail);
        return;
      }
      if (segments.size() == 3 && "images".equals(segments.get(0)) && "comments".equals(segments.get(2))) {
        ok(response, dao.comments(
            parseId(segments.get(1)),
            RequestUtil.intParam(request, "page", 1),
            RequestUtil.intParam(request, "pageSize", 20)));
        return;
      }
      if (segments.size() == 1 && "collections".equals(segments.get(0))) {
        SessionUser user = currentUser(request);
        ok(response, dao.userCollections(user.getId(), RequestUtil.intParam(request, "page", 1), RequestUtil.intParam(request, "pageSize", 20)));
        return;
      }
      if (segments.size() == 1 && "likes".equals(segments.get(0))) {
        SessionUser user = currentUser(request);
        ok(response, dao.userLikes(user.getId(), RequestUtil.intParam(request, "page", 1), RequestUtil.intParam(request, "pageSize", 20)));
        return;
      }
      if (segments.size() == 1 && "activities".equals(segments.get(0))) {
        ok(response, dao.activities(RequestUtil.intParam(request, "limit", 10)));
        return;
      }
      if (segments.size() == 2 && "users".equals(segments.get(0))) {
        SessionUser user = currentUser(request);
        Long userId = user == null ? null : user.getId();
        Map<String, Object> profile = dao.publicProfile(
            parseId(segments.get(1)),
            userId,
            RequestUtil.intParam(request, "page", 1),
            RequestUtil.intParam(request, "pageSize", 24));
        if (profile == null) {
          Result.notFound(response, "用户不存在");
          return;
        }
        ok(response, profile);
        return;
      }
      // GET /api/community/images/{id}/similar
      if (segments.size() == 3 && "images".equals(segments.get(0)) && "similar".equals(segments.get(2))) {
        long imageId = parseId(segments.get(1));
        int limit = RequestUtil.intParam(request, "limit", 4);
        List<Map<String, Object>> similar = dao.similarWorks(imageId, limit);
        ok(response, Map.of("list", similar));
        return;
      }
      Result.notFound(response, "接口不存在");
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Community API failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    CommunityDao dao = new CommunityDao(dataSource());
    SessionUser user = currentUser(request);
    try {
      if (segments.size() == 1 && "like".equals(segments.get(0))) {
        long imageId = readImageId(request);
        if (imageId <= 0) {
          Result.badRequest(response, "缺少作品ID");
          return;
        }
        ok(response, dao.toggleLike(user.getId(), imageId));
        return;
      }
      if (segments.size() == 1 && "collect".equals(segments.get(0))) {
        long imageId = readImageId(request);
        if (imageId <= 0) {
          Result.badRequest(response, "缺少作品ID");
          return;
        }
        ok(response, dao.toggleCollect(user.getId(), imageId));
        return;
      }
      if (segments.size() == 1 && "comments".equals(segments.get(0))) {
        Map<String, Object> body = body(request);
        Object rawImageId = body.get("imageId");
        String content = RequestUtil.string(body, "content");
        if (rawImageId == null || content == null || content.isBlank()) {
          Result.badRequest(response, "缺少必要参数");
          return;
        }
        if (content.length() > 500) {
          Result.badRequest(response, "评论内容不能超过500字");
          return;
        }
        Long parentId = body.get("parentId") == null ? null : Long.parseLong(String.valueOf(body.get("parentId")));
        long commentId = dao.addComment(user.getId(), Long.parseLong(String.valueOf(rawImageId)), content, parentId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", commentId);
        ok(response, data);
        return;
      }
      Result.notFound(response, "接口不存在");
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Community API failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  @Override
  protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    try {
      if (segments.size() == 2 && "comments".equals(segments.get(0))) {
        boolean deleted = new CommunityDao(dataSource()).deleteComment(currentUser(request).getId(), parseId(segments.get(1)));
        if (!deleted) {
          Result.notFound(response, "评论不存在或无权删除");
          return;
        }
        ok(response, null);
        return;
      }
      Result.notFound(response, "接口不存在");
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Community delete failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  private long readImageId(HttpServletRequest request) throws IOException {
    Object value = body(request).get("imageId");
    if (value == null) {
      return -1;
    }
    return Long.parseLong(String.valueOf(value));
  }

  private long parseId(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ignored) {
      return -1;
    }
  }
}
