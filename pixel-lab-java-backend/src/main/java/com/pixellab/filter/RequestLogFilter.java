package com.pixellab.filter;

import com.pixellab.context.AppContextKeys;
import com.pixellab.model.SessionUser;
import com.pixellab.util.RequestUtil;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class RequestLogFilter implements Filter {
  private ServletContext context;

  @Override
  public void init(FilterConfig filterConfig) {
    this.context = filterConfig.getServletContext();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    long start = System.nanoTime();
    Object listenerStart = request.getAttribute(AppContextKeys.REQUEST_START_NANOS);
    if (listenerStart instanceof Long) {
      start = (Long) listenerStart;
    }
    try {
      chain.doFilter(request, response);
    } finally {
      long ms = (System.nanoTime() - start) / 1_000_000;
      SessionUser user = RequestUtil.currentUser(httpRequest);
      String userId = user == null ? "-" : String.valueOf(user.getId());
      context.log("[Pixel Lab] " + httpRequest.getMethod() + " " + httpRequest.getRequestURI()
          + " userId=" + userId + " cost=" + ms + "ms");
    }
  }

  @Override
  public void destroy() {
  }
}
