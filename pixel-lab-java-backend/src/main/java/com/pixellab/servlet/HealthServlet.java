package com.pixellab.servlet;

import com.pixellab.config.AppConfig;
import com.pixellab.context.AppContextKeys;
import com.pixellab.listener.SessionListener;
import com.pixellab.util.Result;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

public class HealthServlet extends BaseApiServlet {
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("status", "ok");
    data.put("onlineUsers", SessionListener.getOnlineCount());

    AppConfig config = (AppConfig) getServletContext().getAttribute(AppContextKeys.APP_CONFIG);
    data.put("deepseekConfigured", !config.get("deepseek.api.key", "").isBlank());

    try (Connection ignored = ((DataSource) getServletContext().getAttribute(AppContextKeys.DATA_SOURCE)).getConnection()) {
      data.put("database", "ok");
      Result.ok(response, data);
    } catch (Exception ex) {
      data.put("database", "error");
      Result.write(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, 503, "数据库连接失败", data);
    }
  }
}
