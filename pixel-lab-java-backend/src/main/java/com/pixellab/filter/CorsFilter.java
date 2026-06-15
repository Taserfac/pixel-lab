package com.pixellab.filter;

import com.pixellab.config.AppConfig;
import com.pixellab.context.AppContextKeys;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

public class CorsFilter implements Filter {
  private Set<String> allowedOrigins;

  @Override
  public void init(FilterConfig filterConfig) {
    AppConfig config = (AppConfig) filterConfig.getServletContext().getAttribute(AppContextKeys.APP_CONFIG);
    this.allowedOrigins = config.getCsvSet("cors.allowed.origins");
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    String origin = httpRequest.getHeader("Origin");

    if (origin != null && (allowedOrigins.contains(origin) || allowedOrigins.contains("*"))) {
      httpResponse.setHeader("Access-Control-Allow-Origin", origin);
      httpResponse.setHeader("Vary", "Origin");
      httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
      httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
      httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
      httpResponse.setHeader("Access-Control-Max-Age", "86400");
    }

    if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
      httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
      return;
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }
}
