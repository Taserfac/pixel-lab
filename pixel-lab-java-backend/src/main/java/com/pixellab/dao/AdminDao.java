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

public class AdminDao {
  private final DataSource dataSource;

  public AdminDao(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public Map<String, Object> users(int page, int pageSize, String keyword) throws Exception {
    int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
    StringBuilder where = new StringBuilder(" WHERE is_deleted = 0 ");
    List<Object> params = new ArrayList<>();
    if (keyword != null && !keyword.isBlank()) {
      where.append(" AND (username LIKE ? OR nickname LIKE ?) ");
      String like = "%" + keyword.trim() + "%";
      params.add(like);
      params.add(like);
    }
    try (Connection conn = dataSource.getConnection()) {
      long total = count(conn, "SELECT COUNT(*) FROM `user` " + where, params);
      List<Object> pageParams = new ArrayList<>(params);
      pageParams.add(pageSize);
      pageParams.add(offset);
      List<Map<String, Object>> rows = query(conn,
          "SELECT id, username, nickname, avatar, role, status, ban_days, ban_reason, ban_end_at, created_at FROM `user` "
              + where + " ORDER BY created_at DESC LIMIT ? OFFSET ?",
          pageParams);
      return pageResult(rows, total, page, pageSize);
    }
  }

  public boolean updateUserStatus(long userId, int status) throws Exception {
    return update("UPDATE `user` SET status = ?, ban_days = NULL, ban_reason = NULL, ban_end_at = NULL WHERE id = ? AND is_deleted = 0", List.of(status, userId)) > 0;
  }

  public boolean banUser(long userId, int banDays, String banReason) throws Exception {
    String sql = "UPDATE `user` SET status = 0, ban_days = ?, ban_reason = ?, ban_end_at = ";
    if (banDays <= 0) {
      sql += "NULL WHERE id = ? AND is_deleted = 0";
      return update(sql, List.of(0, banReason, userId)) > 0;
    } else {
      sql += "DATE_ADD(NOW(), INTERVAL ? DAY) WHERE id = ? AND is_deleted = 0";
      return update(sql, List.of(banDays, banReason, banDays, userId)) > 0;
    }
  }

  public boolean updateUserRole(long userId, String role) throws Exception {
    return update("UPDATE `user` SET role = ? WHERE id = ? AND is_deleted = 0", List.of(role, userId)) > 0;
  }

  public Map<String, Object> images(int page, int pageSize, String keyword, String status) throws Exception {
    int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
    StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
    List<Object> params = new ArrayList<>();
    if (keyword != null && !keyword.isBlank()) {
      where.append(" AND (i.title LIKE ? OR i.original_name LIKE ? OR u.nickname LIKE ?) ");
      String like = "%" + keyword.trim() + "%";
      params.add(like);
      params.add(like);
      params.add(like);
    }
    if (status != null && !status.isBlank()) {
      where.append(" AND i.status = ? ");
      params.add(Integer.parseInt(status));
    }
    try (Connection conn = dataSource.getConnection()) {
      long total = count(conn, "SELECT COUNT(*) FROM image i LEFT JOIN `user` u ON i.user_id = u.id " + where, params);
      List<Object> pageParams = new ArrayList<>(params);
      pageParams.add(pageSize);
      pageParams.add(offset);
      List<Map<String, Object>> rows = query(conn,
          "SELECT i.*, u.username, u.nickname AS author_name FROM image i LEFT JOIN `user` u ON i.user_id = u.id "
              + where + " ORDER BY i.created_at DESC LIMIT ? OFFSET ?",
          pageParams);
      return pageResult(rows, total, page, pageSize);
    }
  }

  public boolean deleteImage(long imageId) throws Exception {
    return update("UPDATE image SET status = 0 WHERE id = ?", List.of(imageId)) > 0;
  }

  public boolean banImage(long imageId, int status) throws Exception {
    return update("UPDATE image SET status = ? WHERE id = ?", List.of(status, imageId)) > 0;
  }

  public Map<String, Object> albums(int page, int pageSize, String keyword) throws Exception {
    int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
    StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
    List<Object> params = new ArrayList<>();
    if (keyword != null && !keyword.isBlank()) {
      where.append(" AND (a.title LIKE ? OR u.nickname LIKE ?) ");
      String like = "%" + keyword.trim() + "%";
      params.add(like);
      params.add(like);
    }
    try (Connection conn = dataSource.getConnection()) {
      long total = count(conn, "SELECT COUNT(*) FROM albums a LEFT JOIN `user` u ON a.user_id = u.id " + where, params);
      List<Object> pageParams = new ArrayList<>(params);
      pageParams.add(pageSize);
      pageParams.add(offset);
      List<Map<String, Object>> rows = query(conn,
          "SELECT a.*, u.username, u.nickname AS author_name, "
              + "(SELECT COUNT(*) FROM album_images ai WHERE ai.album_id = a.id) AS image_count "
              + "FROM albums a LEFT JOIN `user` u ON a.user_id = u.id "
              + where + " ORDER BY a.created_at DESC LIMIT ? OFFSET ?",
          pageParams);
      return pageResult(rows, total, page, pageSize);
    }
  }

  public boolean deleteAlbum(long albumId) throws Exception {
    return update("UPDATE albums SET status = 0 WHERE id = ?", List.of(albumId)) > 0;
  }

  public boolean banAlbum(long albumId, int status) throws Exception {
    return update("UPDATE albums SET status = ? WHERE id = ?", List.of(status, albumId)) > 0;
  }

  public Map<String, Object> platformStats(int onlineCount) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      Map<String, Object> users = new LinkedHashMap<>();
      users.put("total", scalar(conn, "SELECT COUNT(*) FROM `user` WHERE is_deleted = 0"));
      users.put("today", scalar(conn, "SELECT COUNT(*) FROM `user` WHERE is_deleted = 0 AND DATE(created_at) = CURDATE()"));

      Map<String, Object> images = new LinkedHashMap<>();
      images.put("total", scalar(conn, "SELECT COUNT(*) FROM image WHERE status = 1"));
      images.put("today", scalar(conn, "SELECT COUNT(*) FROM image WHERE status = 1 AND DATE(created_at) = CURDATE()"));
      images.put("public", scalar(conn, "SELECT COUNT(*) FROM image WHERE status = 1 AND is_public = 1"));

      Map<String, Object> interactions = new LinkedHashMap<>();
      interactions.put("views", scalar(conn, "SELECT COALESCE(SUM(view_count), 0) FROM image WHERE status = 1"));
      interactions.put("likes", scalar(conn, "SELECT COUNT(*) FROM likes"));
      interactions.put("collects", scalar(conn, "SELECT COUNT(*) FROM collections"));
      interactions.put("comments", scalar(conn, "SELECT COUNT(*) FROM comments"));

      Map<String, Object> sessions = new LinkedHashMap<>();
      sessions.put("online", onlineCount);

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("users", users);
      result.put("images", images);
      result.put("interactions", interactions);
      result.put("sessions", sessions);
      return result;
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

  private long scalar(Connection conn, String sql) throws Exception {
    try (PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      return rs.next() ? rs.getLong(1) : 0;
    }
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

  private int update(String sql, List<Object> params) throws Exception {
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      bind(stmt, params);
      return stmt.executeUpdate();
    }
  }

  private void bind(PreparedStatement stmt, List<Object> params) throws Exception {
    for (int i = 0; i < params.size(); i++) {
      stmt.setObject(i + 1, params.get(i));
    }
  }
}
