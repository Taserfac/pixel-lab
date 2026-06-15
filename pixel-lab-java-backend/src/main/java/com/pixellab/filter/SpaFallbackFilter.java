package com.pixellab.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class SpaFallbackFilter implements Filter {
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String method = httpRequest.getMethod();

    if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
      chain.doFilter(request, response);
      return;
    }

    String path = pathWithoutContext(httpRequest);
    if (path == null || path.isEmpty() || "/".equals(path)
        || path.startsWith("/api/")
        || "/api/health".equals(path)
        || path.startsWith("/uploads/")
        || path.startsWith("/assets/")
        || path.startsWith("/WEB-INF/")) {
      chain.doFilter(request, response);
      return;
    }

    String lastSegment = path.substring(path.lastIndexOf('/') + 1);
    if (lastSegment.contains(".")) {
      chain.doFilter(request, response);
      return;
    }

    request.getRequestDispatcher("/index.html").forward(request, response);
  }

  private String pathWithoutContext(HttpServletRequest request) {
    String uri = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
      return uri.substring(contextPath.length());
    }
    return uri;
  }
}
