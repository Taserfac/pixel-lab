package com.pixellab.servlet;

import com.pixellab.dao.SocialDao;
import com.pixellab.model.SessionUser;
import com.pixellab.util.RequestUtil;
import com.pixellab.util.Result;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SocialServlet extends BaseApiServlet {

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    SocialDao dao = new SocialDao(dataSource());
    try {
      // GET /api/social/follow/status/{userId}
      if (segments.size() == 3 && "follow".equals(segments.get(0)) && "status".equals(segments.get(1))) {
        SessionUser user = currentUser(request);
        if (user == null) {
          Result.unauthorized(response, "请先登录");
          return;
        }
        long targetId = parseId(segments.get(2));
        boolean following = dao.checkFollowStatus(user.getId(), targetId);
        ok(response, Map.of("following", following));
        return;
      }

      // GET /api/social/following
      if (segments.size() == 1 && "following".equals(segments.get(0))) {
        SessionUser user = currentUser(request);
        if (user == null) {
          Result.unauthorized(response, "请先登录");
          return;
        }
        ok(response, dao.getFollowing(
            user.getId(),
            RequestUtil.intParam(request, "page", 1),
            RequestUtil.intParam(request, "pageSize", 20)));
        return;
      }

      // GET /api/social/followers
      if (segments.size() == 1 && "followers".equals(segments.get(0))) {
        SessionUser user = currentUser(request);
        if (user == null) {
          Result.unauthorized(response, "请先登录");
          return;
        }
        ok(response, dao.getFollowers(
            user.getId(),
            RequestUtil.intParam(request, "page", 1),
            RequestUtil.intParam(request, "pageSize", 20)));
        return;
      }

      // GET /api/social/notifications
      if (segments.size() == 1 && "notifications".equals(segments.get(0))) {
        SessionUser user = currentUser(request);
        if (user == null) {
          Result.unauthorized(response, "请先登录");
          return;
        }
        ok(response, dao.getNotifications(
            user.getId(),
            RequestUtil.intParam(request, "page", 1),
            RequestUtil.intParam(request, "pageSize", 20)));
        return;
      }

      // GET /api/social/notifications/unread-count
      if (segments.size() == 2 && "notifications".equals(segments.get(0)) && "unread-count".equals(segments.get(1))) {
        SessionUser user = currentUser(request);
        if (user == null) {
          Result.unauthorized(response, "请先登录");
          return;
        }
        ok(response, Map.of("count", dao.getUnreadCount(user.getId())));
        return;
      }

      // GET /api/social/rankings
      if (segments.size() == 1 && "rankings".equals(segments.get(0))) {
        String type = request.getParameter("type");
        if (type == null || type.isBlank()) {
          type = "weekly";
        }
        ok(response, dao.getRankings(
            type,
            RequestUtil.intParam(request, "page", 1),
            RequestUtil.intParam(request, "pageSize", 20)));
        return;
      }

      // GET /api/social/collections
      if (segments.size() == 1 && "collections".equals(segments.get(0))) {
        SessionUser user = currentUser(request);
        if (user == null) {
          Result.unauthorized(response, "请先登录");
          return;
        }
        ok(response, dao.getCollections(
            user.getId(),
            RequestUtil.intParam(request, "page", 1),
            RequestUtil.intParam(request, "pageSize", 20)));
        return;
      }

      // GET /api/social/templates
      if (segments.size() == 1 && "templates".equals(segments.get(0))) {
        ok(response, dao.getTemplates());
        return;
      }

      Result.notFound(response, "接口不存在");
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Social API failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    SocialDao dao = new SocialDao(dataSource());
    SessionUser user = currentUser(request);
    try {
      // POST /api/social/follow
      if (segments.size() == 1 && "follow".equals(segments.get(0))) {
        if (user == null) {
          Result.unauthorized(response, "请先登录");
          return;
        }
        Map<String, Object> body = body(request);
        Object rawUserId = body.get("userId");
        if (rawUserId == null) {
          Result.badRequest(response, "缺少用户ID");
          return;
        }
        long followedId = Long.parseLong(String.valueOf(rawUserId));
        ok(response, dao.follow(user.getId(), followedId));
        return;
      }

      // POST /api/social/notifications/read-all
      if (segments.size() == 2 && "notifications".equals(segments.get(0)) && "read-all".equals(segments.get(1))) {
        if (user == null) {
          Result.unauthorized(response, "请先登录");
          return;
        }
        dao.markAllRead(user.getId());
        ok(response, null);
        return;
      }

      Result.notFound(response, "接口不存在");
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Social API failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  @Override
  protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    SocialDao dao = new SocialDao(dataSource());
    SessionUser user = currentUser(request);
    try {
      // DELETE /api/social/follow/{userId}
      if (segments.size() == 2 && "follow".equals(segments.get(0))) {
        if (user == null) {
          Result.unauthorized(response, "请先登录");
          return;
        }
        long followedId = parseId(segments.get(1));
        ok(response, dao.unfollow(user.getId(), followedId));
        return;
      }

      Result.notFound(response, "接口不存在");
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Social API failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  @Override
  protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    SocialDao dao = new SocialDao(dataSource());
    SessionUser user = currentUser(request);
    try {
      // PATCH /api/social/notifications/{id}/read
      if (segments.size() == 2 && "notifications".equals(segments.get(0)) && "read".equals(segments.get(1))) {
        if (user == null) {
          Result.unauthorized(response, "请先登录");
          return;
        }
        // This won't match because segments are [notifications, {id}, read]
      }

      // PATCH /api/social/notifications/{id}/read (segments: [notifications, {id}, read])
      if (segments.size() == 3 && "notifications".equals(segments.get(0)) && "read".equals(segments.get(2))) {
        if (user == null) {
          Result.unauthorized(response, "请先登录");
          return;
        }
        long notifId = parseId(segments.get(1));
        dao.markNotificationRead(user.getId(), notifId);
        ok(response, null);
        return;
      }

      Result.notFound(response, "接口不存在");
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Social API failed", ex);
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
