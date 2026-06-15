package com.pixellab.util;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Result {
  private Result() {
  }

  public static void ok(HttpServletResponse response, Object data) throws IOException {
    write(response, HttpServletResponse.SC_OK, 200, "success", data);
  }

  public static void ok(HttpServletResponse response, String msg, Object data) throws IOException {
    write(response, HttpServletResponse.SC_OK, 200, msg, data);
  }

  public static void badRequest(HttpServletResponse response, String msg) throws IOException {
    write(response, HttpServletResponse.SC_BAD_REQUEST, 400, msg, null);
  }

  public static void unauthorized(HttpServletResponse response, String msg) throws IOException {
    write(response, HttpServletResponse.SC_UNAUTHORIZED, 401, msg, null);
  }

  public static void forbidden(HttpServletResponse response, String msg) throws IOException {
    write(response, HttpServletResponse.SC_FORBIDDEN, 403, msg, null);
  }

  public static void notFound(HttpServletResponse response, String msg) throws IOException {
    write(response, HttpServletResponse.SC_NOT_FOUND, 404, msg, null);
  }

  public static void serverError(HttpServletResponse response, String msg) throws IOException {
    write(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 500, msg, null);
  }

  public static void unavailable(HttpServletResponse response, String msg) throws IOException {
    write(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, 503, msg, null);
  }

  public static void write(HttpServletResponse response, int httpStatus, int code, String msg, Object data) throws IOException {
    response.setStatus(httpStatus);
    response.setCharacterEncoding("UTF-8");
    response.setContentType("application/json;charset=UTF-8");

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("code", code);
    body.put("msg", msg);
    body.put("data", data);
    response.getWriter().write(JsonUtil.stringify(body));
  }
}
