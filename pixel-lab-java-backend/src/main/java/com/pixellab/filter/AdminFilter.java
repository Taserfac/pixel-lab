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

public class AdminFilter implements Filter {
  @Override
  public void init(FilterConfig filterConfig) {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    SessionUser user = RequestUtil.currentUser((HttpServletRequest) request);
    if (user == null) {
      Result.unauthorized((HttpServletResponse) response, "请先登录");
      return;
    }
    if (!user.isAdmin()) {
      Result.forbidden((HttpServletResponse) response, "需要管理员权限");
      return;
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }
}
