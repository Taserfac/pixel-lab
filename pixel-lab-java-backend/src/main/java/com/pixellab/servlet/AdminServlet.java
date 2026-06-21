package com.pixellab.servlet;

import com.pixellab.dao.AdminDao;
import com.pixellab.listener.SessionListener;
import com.pixellab.model.SessionUser;
import com.pixellab.util.RequestUtil;
import com.pixellab.util.Result;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AdminServlet extends BaseApiServlet {
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    AdminDao dao = new AdminDao(dataSource());
    try {
      if (segments.size() == 1 && "users".equals(segments.get(0))) {
        ok(response, dao.users(
            RequestUtil.intParam(request, "page", 1),
            RequestUtil.intParam(request, "pageSize", 20),
            request.getParameter("keyword"),
            request.getParameter("role"),
            request.getParameter("status"),
            request.getParameter("isDeleted"),
            request.getParameter("sortBy")));
        return;
      }
      if (segments.size() == 1 && "images".equals(segments.get(0))) {
        ok(response, dao.images(
            RequestUtil.intParam(request, "page", 1),
            RequestUtil.intParam(request, "pageSize", 20),
            request.getParameter("keyword"),
            request.getParameter("userKeyword"),
            request.getParameter("status"),
            request.getParameter("isPublic"),
            request.getParameter("isDeleted"),
            request.getParameter("sortBy")));
        return;
      }
      if (segments.size() == 1 && "albums".equals(segments.get(0))) {
        ok(response, dao.albums(
            RequestUtil.intParam(request, "page", 1),
            RequestUtil.intParam(request, "pageSize", 20),
            request.getParameter("keyword")));
        return;
      }
      if (segments.size() == 1 && "reports".equals(segments.get(0))) {
        ok(response, dao.reports(
            RequestUtil.intParam(request, "page", 1),
            RequestUtil.intParam(request, "pageSize", 20),
            request.getParameter("status")));
        return;
      }
      if (segments.size() == 1 && "stats".equals(segments.get(0))) {
        ok(response, dao.platformStats(SessionListener.getOnlineCount()));
        return;
      }
      Result.notFound(response, "接口不存在");
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Admin API failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  @Override
  protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    AdminDao dao = new AdminDao(dataSource());
    SessionUser current = currentUser(request);
    try {
      if (segments.size() == 3 && "users".equals(segments.get(0)) && "status".equals(segments.get(2))) {
        long userId = parseId(segments.get(1));
        if (userId == current.getId()) {
          Result.badRequest(response, "不能修改自己的状态");
          return;
        }
        Map<String, Object> body = body(request);
        int status = Integer.parseInt(String.valueOf(body.get("status")));
        if (status == 0) {
          // 封禁用户
          int banDays = body.containsKey("banDays") ? Integer.parseInt(String.valueOf(body.get("banDays"))) : 0;
          String banReason = RequestUtil.string(body, "banReason");
          dao.banUser(userId, banDays, banReason);
          ok(response, "已封禁", null);
        } else {
          // 解封用户
          dao.updateUserStatus(userId, status);
          ok(response, "已启用", null);
        }
        return;
      }
      if (segments.size() == 3 && "images".equals(segments.get(0)) && "status".equals(segments.get(2))) {
        long imageId = parseId(segments.get(1));
        Map<String, Object> body = body(request);
        int status = Integer.parseInt(String.valueOf(body.get("status")));
        boolean updated = dao.banImage(imageId, status);
        if (!updated) {
          Result.notFound(response, "作品不存在");
          return;
        }
        ok(response, status == 2 ? "已封禁" : "已恢复", null);
        return;
      }
      if (segments.size() == 3 && "albums".equals(segments.get(0)) && "status".equals(segments.get(2))) {
        long albumId = parseId(segments.get(1));
        Map<String, Object> body = body(request);
        int status = Integer.parseInt(String.valueOf(body.get("status")));
        boolean updated = dao.banAlbum(albumId, status);
        if (!updated) {
          Result.notFound(response, "作品集不存在");
          return;
        }
        ok(response, status == 2 ? "已封禁" : "已恢复", null);
        return;
      }
      if (segments.size() == 3 && "reports".equals(segments.get(0)) && "status".equals(segments.get(2))) {
        long reportId = parseId(segments.get(1));
        Map<String, Object> body = body(request);
        int status = Integer.parseInt(String.valueOf(body.get("status")));
        if (status != 1 && status != 2) {
          Result.badRequest(response, "举报状态无效");
          return;
        }
        boolean updated = dao.updateReportStatus(reportId, current.getId(), status);
        if (!updated) {
          Result.notFound(response, "举报不存在");
          return;
        }
        ok(response, status == 1 ? "已处理" : "已驳回", null);
        return;
      }
      if (segments.size() == 3 && "reports".equals(segments.get(0)) && "ban-image".equals(segments.get(2))) {
        long reportId = parseId(segments.get(1));
        boolean updated = dao.banReportedImage(reportId, current.getId());
        if (!updated) {
          Result.notFound(response, "举报不存在");
          return;
        }
        ok(response, "已封禁作品并处理举报", null);
        return;
      }
      if (segments.size() == 3 && "users".equals(segments.get(0)) && "role".equals(segments.get(2))) {
        long userId = parseId(segments.get(1));
        if (userId == current.getId()) {
          Result.badRequest(response, "不能修改自己的角色");
          return;
        }
        String role = RequestUtil.string(body(request), "role");
        if (!"user".equals(role) && !"admin".equals(role)) {
          Result.badRequest(response, "角色参数无效");
          return;
        }
        boolean updated = dao.updateUserRole(userId, role);
        if (!updated) {
          Result.notFound(response, "用户不存在");
          return;
        }
        ok(response, "角色已更新", null);
        return;
      }
      Result.notFound(response, "接口不存在");
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Admin patch failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  @Override
  protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    try {
      if (segments.size() == 2 && "images".equals(segments.get(0))) {
        boolean deleted = new AdminDao(dataSource()).deleteImage(parseId(segments.get(1)));
        if (!deleted) {
          Result.notFound(response, "作品不存在");
          return;
        }
        ok(response, "已删除", null);
        return;
      }
      if (segments.size() == 2 && "albums".equals(segments.get(0))) {
        boolean deleted = new AdminDao(dataSource()).deleteAlbum(parseId(segments.get(1)));
        if (!deleted) {
          Result.notFound(response, "作品集不存在");
          return;
        }
        ok(response, "已删除", null);
        return;
      }
      Result.notFound(response, "接口不存在");
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Admin delete failed", ex);
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
