package com.pixellab.servlet;

import com.pixellab.dao.TutorialDao;
import com.pixellab.util.RequestUtil;
import com.pixellab.util.Result;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class TutorialServlet extends BaseApiServlet {
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    TutorialDao dao = new TutorialDao(dataSource());
    try {
      if (segments.isEmpty() || (segments.size() == 1 && "videos".equals(segments.get(0)))) {
        ok(response, dao.videos(
            RequestUtil.intParam(request, "page", 1),
            RequestUtil.intParam(request, "pageSize", 24),
            request.getParameter("category"),
            request.getParameter("keyword")));
        return;
      }
      if (segments.size() == 1 && "categories".equals(segments.get(0))) {
        ok(response, dao.categories());
        return;
      }
      Result.notFound(response, "接口不存在");
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Tutorial API failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }
}
