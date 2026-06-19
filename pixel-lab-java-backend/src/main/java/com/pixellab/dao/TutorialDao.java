package com.pixellab.dao;

import com.pixellab.util.SqlRows;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TutorialDao {
  private static final List<String> ART_CATEGORIES = List.of(
      "造型基础",
      "素描观察",
      "色彩修养",
      "构图审美",
      "光影关系",
      "透视空间",
      "人体结构",
      "动态速写",
      "材质肌理",
      "大师临摹",
      "艺术史鉴赏",
      "视觉叙事");

  private final DataSource dataSource;

  public TutorialDao(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public List<Map<String, Object>> categories() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      return query(conn,
          "SELECT category, COUNT(*) AS count FROM tutorial_videos "
              + "WHERE status = 1 AND content_type = 'video' AND category IN ("
              + placeholders(ART_CATEGORIES.size()) + ") "
              + "GROUP BY category ORDER BY MIN(sort_order), category",
          new ArrayList<>(ART_CATEGORIES));
    }
  }

  public Map<String, Object> videos(int page, int pageSize, String category, String keyword) throws Exception {
    int safePage = Math.max(page, 1);
    int safePageSize = Math.max(1, Math.min(pageSize, 48));
    int offset = (safePage - 1) * safePageSize;
    StringBuilder where = new StringBuilder(
        " WHERE status = 1 AND content_type = 'video' AND category IN ("
            + placeholders(ART_CATEGORIES.size()) + ") ");
    List<Object> params = new ArrayList<>(ART_CATEGORIES);

    if (category != null && !category.isBlank() && !"全部".equals(category)) {
      where.append(" AND category = ? ");
      params.add(category.trim());
    }
    if (keyword != null && !keyword.isBlank()) {
      where.append(" AND (title LIKE ? OR author_name LIKE ? OR description LIKE ?) ");
      String like = "%" + keyword.trim() + "%";
      params.add(like);
      params.add(like);
      params.add(like);
    }

    try (Connection conn = dataSource.getConnection()) {
      long total = count(conn, "SELECT COUNT(*) FROM tutorial_videos " + where, params);
      List<Object> pageParams = new ArrayList<>(params);
      pageParams.add(safePageSize);
      pageParams.add(offset);
      List<Map<String, Object>> rows = query(conn,
          "SELECT id, content_type, category, title, description, cover_url, source_url, embed_url, source_name, "
              + "author_name, duration, view_count, published_at, crawled_at "
              + "FROM tutorial_videos " + where
              + " ORDER BY sort_order ASC, view_count DESC, crawled_at DESC LIMIT ? OFFSET ?",
          pageParams);
      return pageResult(rows, total, safePage, safePageSize);
    }
  }

  private Map<String, Object> pageResult(List<Map<String, Object>> rows, long total, int page, int pageSize) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("list", rows);
    result.put("total", total);
    result.put("page", page);
    result.put("pageSize", pageSize);
    result.put("totalPages", (int) Math.ceil(total * 1.0 / pageSize));
    return result;
  }

  private long count(Connection conn, String sql, List<Object> params) throws Exception {
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      bind(stmt, params);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next() ? rs.getLong(1) : 0;
      }
    }
  }

  private List<Map<String, Object>> query(Connection conn, String sql, List<Object> params) throws Exception {
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      bind(stmt, params);
      try (ResultSet rs = stmt.executeQuery()) {
        return SqlRows.many(rs);
      }
    }
  }

  private void bind(PreparedStatement stmt, List<Object> params) throws Exception {
    for (int i = 0; i < params.size(); i++) {
      stmt.setObject(i + 1, params.get(i));
    }
  }

  private String placeholders(int count) {
    return String.join(", ", java.util.Collections.nCopies(count, "?"));
  }
}
