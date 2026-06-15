package com.pixellab.servlet;

import com.pixellab.model.SessionUser;
import com.pixellab.util.JsonUtil;
import com.pixellab.util.RequestUtil;
import com.pixellab.util.Result;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class BaseApiServlet extends HttpServlet {
  @Override
  protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    if ("PATCH".equalsIgnoreCase(request.getMethod())) {
      doPatch(request, response);
      return;
    }
    super.service(request, response);
  }

  protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
  }

  protected DataSource dataSource() {
    return RequestUtil.dataSource(getServletContext());
  }

  protected SessionUser currentUser(HttpServletRequest request) {
    return RequestUtil.currentUser(request);
  }

  protected Map<String, Object> body(HttpServletRequest request) throws IOException {
    return JsonUtil.readMap(request);
  }

  protected void ok(HttpServletResponse response, Object data) throws IOException {
    Result.ok(response, data);
  }

  protected void ok(HttpServletResponse response, String msg, Object data) throws IOException {
    Result.ok(response, msg, data);
  }

  protected long idAt(HttpServletRequest request, int index) {
    List<String> segments = RequestUtil.pathSegments(request);
    if (index < 0 || index >= segments.size()) {
      return -1;
    }
    try {
      return Long.parseLong(segments.get(index));
    } catch (NumberFormatException ignored) {
      return -1;
    }
  }
}
