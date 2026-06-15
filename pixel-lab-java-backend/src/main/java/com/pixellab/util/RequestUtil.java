package com.pixellab.util;

import com.pixellab.context.AppContextKeys;
import com.pixellab.model.SessionUser;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RequestUtil {
  private RequestUtil() {
  }

  public static DataSource dataSource(ServletContext context) {
    return (DataSource) context.getAttribute(AppContextKeys.DATA_SOURCE);
  }

  public static SessionUser currentUser(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null) {
      return null;
    }
    Object value = session.getAttribute(AppContextKeys.LOGIN_USER);
    return value instanceof SessionUser ? (SessionUser) value : null;
  }

  public static String pathInfo(HttpServletRequest request) {
    String path = request.getPathInfo();
    return path == null || path.isBlank() ? "/" : path;
  }

  public static List<String> pathSegments(HttpServletRequest request) {
    String path = pathInfo(request);
    List<String> parts = new ArrayList<>();
    for (String item : path.split("/")) {
      if (!item.isBlank()) {
        parts.add(item);
      }
    }
    return parts;
  }

  public static String string(Map<String, Object> body, String key) {
    Object value = body.get(key);
    return value == null ? null : String.valueOf(value).trim();
  }

  public static int intParam(HttpServletRequest request, String key, int defaultValue) {
    String value = request.getParameter(key);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ignored) {
      return defaultValue;
    }
  }

  public static long longParam(HttpServletRequest request, String key, long defaultValue) {
    String value = request.getParameter(key);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ignored) {
      return defaultValue;
    }
  }

  public static boolean boolValue(Object value, boolean defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    if (value instanceof Number) {
      return ((Number) value).intValue() != 0;
    }
    String text = String.valueOf(value).trim();
    if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
      return true;
    }
    if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
      return false;
    }
    return defaultValue;
  }
}
