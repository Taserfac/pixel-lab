package com.pixellab.dao;

import com.pixellab.util.SqlRows;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CommunityDao {
  private final DataSource dataSource;

  public CommunityDao(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public Map<String, Object> publicImages(int page, int pageSize, String keyword, String sortBy, Long userId) throws Exception {
    int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
    List<Object> params = new ArrayList<>();
    StringBuilder where = new StringBuilder(" WHERE i.is_public = 1 AND i.status = 1 ");
    if (keyword != null && !keyword.isBlank()) {
      where.append(" AND (i.title LIKE ? OR i.tags LIKE ? OR u.nickname LIKE ?) ");
      String like = "%" + keyword.trim() + "%";
      params.add(like);
      params.add(like);
      params.add(like);
    }

    String orderBy = "i.created_at DESC";
    if ("popular".equals(sortBy)) {
      orderBy = "i.like_count DESC, i.view_count DESC";
    } else if ("views".equals(sortBy)) {
      orderBy = "i.view_count DESC";
    }

    try (Connection conn = dataSource.getConnection()) {
      long total = count(conn, "SELECT COUNT(*) FROM image i LEFT JOIN `user` u ON i.user_id = u.id " + where, params);
      String sql = "SELECT i.*, u.nickname AS author_name, u.avatar AS author_avatar "
          + "FROM image i LEFT JOIN `user` u ON i.user_id = u.id "
          + where + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?";
      List<Object> pageParams = new ArrayList<>(params);
      pageParams.add(pageSize);
      pageParams.add(offset);
      List<Map<String, Object>> rows = query(conn, sql, pageParams);
      addUserState(conn, rows, userId);
      return pageResult(rows, total, page, pageSize);
    }
  }

  public Map<String, Object> imageDetail(long imageId, Long userId) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      Map<String, Object> image = queryOne(conn,
          "SELECT i.*, u.nickname AS author_name, u.avatar AS author_avatar "
              + "FROM image i LEFT JOIN `user` u ON i.user_id = u.id WHERE i.id = ? AND i.status = 1",
          List.of(imageId));
      if (image == null) {
        return null;
      }
      execute(conn, "UPDATE image SET view_count = view_count + 1 WHERE id = ?", List.of(imageId));
      Object viewCount = image.get("view_count");
      if (viewCount instanceof Number) {
        image.put("view_count", ((Number) viewCount).longValue() + 1);
      }
      image.put("isLiked", userId != null && exists(conn, "SELECT id FROM likes WHERE user_id = ? AND image_id = ?", List.of(userId, imageId)));
      image.put("isCollected", userId != null && exists(conn, "SELECT id FROM collections WHERE user_id = ? AND image_id = ?", List.of(userId, imageId)));
      return image;
    }
  }

  public Map<String, Object> toggleLike(long userId, long imageId) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try {
        boolean exists = exists(conn, "SELECT id FROM likes WHERE user_id = ? AND image_id = ?", List.of(userId, imageId));
        if (exists) {
          execute(conn, "DELETE FROM likes WHERE user_id = ? AND image_id = ?", List.of(userId, imageId));
          execute(conn, "UPDATE image SET like_count = GREATEST(0, like_count - 1) WHERE id = ?", List.of(imageId));
          conn.commit();
          return Map.of("liked", false);
        }
        execute(conn, "INSERT INTO likes (user_id, image_id) VALUES (?, ?)", List.of(userId, imageId));
        execute(conn, "UPDATE image SET like_count = like_count + 1 WHERE id = ?", List.of(imageId));
        conn.commit();
        return Map.of("liked", true);
      } catch (Exception ex) {
        conn.rollback();
        throw ex;
      }
    }
  }

  public Map<String, Object> toggleCollect(long userId, long imageId) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try {
        boolean exists = exists(conn, "SELECT id FROM collections WHERE user_id = ? AND image_id = ?", List.of(userId, imageId));
        if (exists) {
          execute(conn, "DELETE FROM collections WHERE user_id = ? AND image_id = ?", List.of(userId, imageId));
          execute(conn, "UPDATE image SET collect_count = GREATEST(0, collect_count - 1) WHERE id = ?", List.of(imageId));
          conn.commit();
          return Map.of("collected", false);
        }
        execute(conn, "INSERT INTO collections (user_id, image_id) VALUES (?, ?)", List.of(userId, imageId));
        execute(conn, "UPDATE image SET collect_count = collect_count + 1 WHERE id = ?", List.of(imageId));
        conn.commit();
        return Map.of("collected", true);
      } catch (Exception ex) {
        conn.rollback();
        throw ex;
      }
    }
  }

  public Map<String, Object> comments(long imageId, int page, int pageSize) throws Exception {
    int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
    try (Connection conn = dataSource.getConnection()) {
      long total = count(conn, "SELECT COUNT(*) FROM comments WHERE image_id = ? AND status = 1", List.of(imageId));
      List<Map<String, Object>> rows = query(conn,
          "SELECT c.*, u.nickname, u.avatar FROM comments c LEFT JOIN `user` u ON c.user_id = u.id "
              + "WHERE c.image_id = ? AND c.status = 1 ORDER BY c.created_at DESC LIMIT ? OFFSET ?",
          List.of(imageId, pageSize, offset));
      return pageResult(rows, total, page, pageSize);
    }
  }

  public long addComment(long userId, long imageId, String content, Long parentId) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try (PreparedStatement stmt = conn.prepareStatement(
          "INSERT INTO comments (user_id, image_id, content, parent_id) VALUES (?, ?, ?, ?)",
          Statement.RETURN_GENERATED_KEYS)) {
        stmt.setLong(1, userId);
        stmt.setLong(2, imageId);
        stmt.setString(3, content);
        if (parentId == null) {
          stmt.setNull(4, java.sql.Types.INTEGER);
        } else {
          stmt.setLong(4, parentId);
        }
        stmt.executeUpdate();
        execute(conn, "UPDATE image SET comment_count = comment_count + 1 WHERE id = ?", List.of(imageId));
        long id = 0;
        try (ResultSet rs = stmt.getGeneratedKeys()) {
          if (rs.next()) {
            id = rs.getLong(1);
          }
        }
        conn.commit();
        return id;
      } catch (Exception ex) {
        conn.rollback();
        throw ex;
      }
    }
  }

  public boolean deleteComment(long userId, long commentId) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try {
        Map<String, Object> comment = queryOne(conn, "SELECT * FROM comments WHERE id = ? AND user_id = ?", List.of(commentId, userId));
        if (comment == null) {
          conn.rollback();
          return false;
        }
        execute(conn, "DELETE FROM comments WHERE id = ?", List.of(commentId));
        execute(conn, "UPDATE image SET comment_count = GREATEST(0, comment_count - 1) WHERE id = ?", List.of(comment.get("image_id")));
        conn.commit();
        return true;
      } catch (Exception ex) {
        conn.rollback();
        throw ex;
      }
    }
  }

  public Map<String, Object> userCollections(long userId, int page, int pageSize) throws Exception {
    int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
    try (Connection conn = dataSource.getConnection()) {
      long total = count(conn, "SELECT COUNT(*) FROM collections WHERE user_id = ?", List.of(userId));
      List<Map<String, Object>> rows = query(conn,
          "SELECT i.*, u.nickname AS author_name, c.created_at AS collected_at "
              + "FROM collections c LEFT JOIN image i ON c.image_id = i.id LEFT JOIN `user` u ON i.user_id = u.id "
              + "WHERE c.user_id = ? ORDER BY c.created_at DESC LIMIT ? OFFSET ?",
          List.of(userId, pageSize, offset));
      return pageResult(rows, total, page, pageSize);
    }
  }

  public Map<String, Object> userLikes(long userId, int page, int pageSize) throws Exception {
    int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
    try (Connection conn = dataSource.getConnection()) {
      long total = count(conn, "SELECT COUNT(*) FROM likes WHERE user_id = ?", List.of(userId));
      List<Map<String, Object>> rows = query(conn,
          "SELECT i.*, u.nickname AS author_name, l.created_at AS liked_at "
              + "FROM likes l LEFT JOIN image i ON l.image_id = i.id LEFT JOIN `user` u ON i.user_id = u.id "
              + "WHERE l.user_id = ? ORDER BY l.created_at DESC LIMIT ? OFFSET ?",
          List.of(userId, pageSize, offset));
      return pageResult(rows, total, page, pageSize);
    }
  }

  public List<Map<String, Object>> activities(int limit) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      return query(conn,
          "SELECT i.id, i.title, i.original_name, i.url, i.created_at, "
              + "u.id AS author_id, u.nickname AS author_name, u.avatar AS author_avatar "
              + "FROM image i LEFT JOIN `user` u ON i.user_id = u.id "
              + "WHERE i.is_public = 1 AND i.status = 1 ORDER BY i.created_at DESC LIMIT ?",
          List.of(limit));
    }
  }

  private void addUserState(Connection conn, List<Map<String, Object>> rows, Long userId) throws Exception {
    for (Map<String, Object> row : rows) {
      long imageId = ((Number) row.get("id")).longValue();
      row.put("isLiked", userId != null && exists(conn, "SELECT id FROM likes WHERE user_id = ? AND image_id = ?", List.of(userId, imageId)));
      row.put("isCollected", userId != null && exists(conn, "SELECT id FROM collections WHERE user_id = ? AND image_id = ?", List.of(userId, imageId)));
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

  private boolean exists(Connection conn, String sql, List<Object> params) throws Exception {
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      bind(stmt, params);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next();
      }
    }
  }

  private Map<String, Object> queryOne(Connection conn, String sql, List<Object> params) throws Exception {
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      bind(stmt, params);
      try (ResultSet rs = stmt.executeQuery()) {
        return SqlRows.one(rs);
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

  private void execute(Connection conn, String sql, List<Object> params) throws Exception {
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      bind(stmt, params);
      stmt.executeUpdate();
    }
  }

  private void bind(PreparedStatement stmt, List<Object> params) throws Exception {
    for (int i = 0; i < params.size(); i++) {
      stmt.setObject(i + 1, params.get(i));
    }
  }
}
