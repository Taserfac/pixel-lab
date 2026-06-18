package com.pixellab.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 缓存控制过滤器
 * 防止 index.html 和 sw.js 被浏览器缓存，确保用户始终获取最新版本
 */
public class CacheControlFilter implements Filter {

  @Override
  public void init(FilterConfig filterConfig) {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    String uri = httpRequest.getRequestURI();

    // HTML 入口页面：禁止缓存
    if (uri.endsWith("/index.html") || uri.equals("/") || uri.isEmpty()) {
      httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
      httpResponse.setHeader("Pragma", "no-cache");
      httpResponse.setDateHeader("Expires", 0);
    }
    // Service Worker：必须每次检查更新
    else if (uri.endsWith("/sw.js")) {
      httpResponse.setHeader("Cache-Control", "no-cache, must-revalidate");
    }
    // 其他静态资源（带内容哈希的 JS/CSS/图片等）：使用 Tomcat 默认缓存策略

    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }
}
