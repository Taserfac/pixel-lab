package com.pixellab.servlet;

import com.pixellab.dao.TagDao;
import com.pixellab.util.RequestUtil;
import com.pixellab.util.Result;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class TagServlet extends BaseApiServlet {

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    TagDao tagDao = new TagDao(dataSource());
    try {
      // GET /api/tags?keyword=xxx&limit=20 — 搜索标签（输入联想）
      if (segments.isEmpty()) {
        String keyword = request.getParameter("keyword");
        int limit = RequestUtil.intParam(request, "limit", 20);
        if (keyword == null || keyword.isBlank()) {
          // 无关键词时返回热门标签
          ok(response, tagDao.getHotTags(limit));
        } else {
          ok(response, tagDao.searchByName(keyword.trim(), limit));
        }
        return;
      }
      // GET /api/tags/all — 获取所有标签
      if (segments.size() == 1 && "all".equals(segments.get(0))) {
        ok(response, tagDao.findAll());
        return;
      }
      Result.notFound(response, "接口不存在");
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Tag API failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }
}
