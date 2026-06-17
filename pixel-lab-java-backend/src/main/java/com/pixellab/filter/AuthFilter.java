package com.pixellab.filter;

import com.pixellab.model.SessionUser;
import com.pixellab.util.RequestUtil;
import com.pixellab.util.Result;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthFilter implements Filter {
  @Override
  public void init(FilterConfig filterConfig) {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    if (isPublicEndpoint(httpRequest)) {
      chain.doFilter(request, response);
      return;
    }

    SessionUser user = RequestUtil.currentUser(httpRequest);
    if (user == null) {
      Result.unauthorized(httpResponse, "请先登录");
      return;
    }

    request.setAttribute("currentUser", user);
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }

  private boolean isPublicEndpoint(HttpServletRequest request) {
    String method = request.getMethod();
    if ("OPTIONS".equalsIgnoreCase(method)) {
      return true;
    }

    String path = request.getRequestURI().substring(request.getContextPath().length());
    if ("/api/health".equals(path) || "/api/auth/login".equals(path) || "/api/auth/register".equals(path)) {
      return true;
    }

    if ("GET".equalsIgnoreCase(method)) {
      return path.equals("/api/community/images")
          || path.matches("^/api/community/images/\\d+$")
          || path.matches("^/api/community/images/\\d+/comments$")
          || path.matches("^/api/community/images/\\d+/similar$")
          || path.equals("/api/community/activities")
          || path.equals("/api/social/rankings")
          || path.equals("/api/social/templates")
          || path.matches("^/api/albums/\\d+$");
    }

    return false;
  }
}
