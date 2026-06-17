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

public class SocialDao {
  private final DataSource dataSource;

  public SocialDao(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  // ==================== Follow ====================

  public Map<String, Object> follow(long followerId, long followedId) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try {
        if (followerId == followedId) {
          conn.rollback();
          return Map.of("following", false, "msg", "不能关注自己");
        }
        boolean exists = exists(conn,
            "SELECT id FROM follows WHERE follower_id = ? AND followed_id = ?",
            List.of(followerId, followedId));
        if (exists) {
          conn.rollback();
          return Map.of("following", true, "msg", "已关注");
        }
        execute(conn,
            "INSERT INTO follows (follower_id, followed_id) VALUES (?, ?)",
            List.of(followerId, followedId));
        conn.commit();
        return Map.of("following", true);
      } catch (Exception ex) {
        conn.rollback();
        throw ex;
      }
    }
  }

  public Map<String, Object> unfollow(long followerId, long followedId) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try {
        execute(conn,
            "DELETE FROM follows WHERE follower_id = ? AND followed_id = ?",
            List.of(followerId, followedId));
        conn.commit();
        return Map.of("following", false);
      } catch (Exception ex) {
        conn.rollback();
        throw ex;
      }
    }
  }

  public boolean checkFollowStatus(long followerId, long followedId) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      return exists(conn,
          "SELECT id FROM follows WHERE follower_id = ? AND followed_id = ?",
          List.of(followerId, followedId));
    }
  }

  public Map<String, Object> getFollowing(long userId, int page, int pageSize) throws Exception {
    int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
    try (Connection conn = dataSource.getConnection()) {
      long total = count(conn,
          "SELECT COUNT(*) FROM follows WHERE follower_id = ?", List.of(userId));
      List<Map<String, Object>> rows = query(conn,
          "SELECT u.id, u.nickname, u.avatar, f.created_at AS followed_at "
              + "FROM follows f LEFT JOIN `user` u ON f.followed_id = u.id "
              + "WHERE f.follower_id = ? ORDER BY f.created_at DESC LIMIT ? OFFSET ?",
          List.of(userId, pageSize, offset));
      return pageResult(rows, total, page, pageSize);
    }
  }

  public Map<String, Object> getFollowers(long userId, int page, int pageSize) throws Exception {
    int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
    try (Connection conn = dataSource.getConnection()) {
      long total = count(conn,
          "SELECT COUNT(*) FROM follows WHERE followed_id = ?", List.of(userId));
      List<Map<String, Object>> rows = query(conn,
          "SELECT u.id, u.nickname, u.avatar, f.created_at AS followed_at "
              + "FROM follows f LEFT JOIN `user` u ON f.follower_id = u.id "
              + "WHERE f.followed_id = ? ORDER BY f.created_at DESC LIMIT ? OFFSET ?",
          List.of(userId, pageSize, offset));
      return pageResult(rows, total, page, pageSize);
    }
  }

  // ==================== Notifications ====================

  public List<Map<String, Object>> getNotifications(long userId, int page, int pageSize) throws Exception {
    int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
    try (Connection conn = dataSource.getConnection()) {
      return query(conn,
          "SELECT n.*, u.nickname AS sender_name, u.avatar AS sender_avatar "
              + "FROM notifications n LEFT JOIN `user` u ON n.sender_id = u.id "
              + "WHERE n.user_id = ? ORDER BY n.created_at DESC LIMIT ? OFFSET ?",
          List.of(userId, pageSize, offset));
    }
  }

  public int getUnreadCount(long userId) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      return (int) count(conn,
          "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0",
          List.of(userId));
    }
  }

  public void markNotificationRead(long userId, long notificationId) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      execute(conn,
          "UPDATE notifications SET is_read = 1 WHERE id = ? AND user_id = ?",
          List.of(notificationId, userId));
    }
  }

  public void markAllRead(long userId) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      execute(conn,
          "UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0",
          List.of(userId));
    }
  }

  public void createNotification(long userId, Long senderId, String type, String content, Long referenceId, String referenceType) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      execute(conn,
          "INSERT INTO notifications (user_id, sender_id, type, content, reference_id, reference_type) VALUES (?, ?, ?, ?, ?, ?)",
          List.of(userId, senderId, type, content, referenceId, referenceType));
    }
  }

  // ==================== Rankings ====================

  public Map<String, Object> getRankings(String rankType, int page, int pageSize) throws Exception {
    int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
    String timeFilter = "";
    if ("weekly".equals(rankType)) {
      timeFilter = " AND i.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)";
    } else if ("monthly".equals(rankType)) {
      timeFilter = " AND i.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
    }
    try (Connection conn = dataSource.getConnection()) {
      long total = count(conn,
          "SELECT COUNT(*) FROM image i WHERE i.is_public = 1 AND i.status = 1" + timeFilter,
          List.of());
      List<Map<String, Object>> rows = query(conn,
          "SELECT i.id, i.title, i.original_name, i.url, i.like_count, i.view_count, i.created_at, "
              + "u.id AS author_id, u.nickname AS author_name, u.avatar AS author_avatar "
              + "FROM image i LEFT JOIN `user` u ON i.user_id = u.id "
              + "WHERE i.is_public = 1 AND i.status = 1" + timeFilter
              + " ORDER BY i.like_count DESC, i.view_count DESC LIMIT ? OFFSET ?",
          List.of(pageSize, offset));
      return pageResult(rows, total, page, pageSize);
    }
  }

  // ==================== Similar Works ====================

  public List<Map<String, Object>> getSimilarWorks(long imageId, int limit) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      Map<String, Object> image = queryOne(conn,
          "SELECT tags FROM image WHERE id = ? AND status = 1", List.of(imageId));
      if (image == null) {
        return List.of();
      }
      String tags = (String) image.get("tags");
      if (tags == null || tags.isBlank()) {
        return List.of();
      }
      String firstTag = tags.contains(",") ? tags.substring(0, tags.indexOf(",")).trim() : tags.trim();
      if (firstTag.isEmpty()) {
        return List.of();
      }
      return query(conn,
          "SELECT i.id, i.title, i.original_name, i.url, i.like_count, "
              + "u.nickname AS author_name "
              + "FROM image i LEFT JOIN `user` u ON i.user_id = u.id "
              + "WHERE i.is_public = 1 AND i.status = 1 AND i.id != ? AND i.tags LIKE ? "
              + "ORDER BY i.like_count DESC LIMIT ?",
          List.of(imageId, "%" + firstTag + "%", limit));
    }
  }

  // ==================== Collections (folders) ====================

  public Map<String, Object> getCollections(long userId, int page, int pageSize) throws Exception {
    int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
    try (Connection conn = dataSource.getConnection()) {
      long total = count(conn,
          "SELECT COUNT(DISTINCT image_id) FROM collections WHERE user_id = ?", List.of(userId));
      List<Map<String, Object>> rows = query(conn,
          "SELECT i.*, u.nickname AS author_name, c.created_at AS collected_at "
              + "FROM collections c LEFT JOIN image i ON c.image_id = i.id "
              + "LEFT JOIN `user` u ON i.user_id = u.id "
              + "WHERE c.user_id = ? ORDER BY c.created_at DESC LIMIT ? OFFSET ?",
          List.of(userId, pageSize, offset));
      return pageResult(rows, total, page, pageSize);
    }
  }

  // ==================== Templates ====================

  public List<Map<String, Object>> getTemplates() throws Exception {
    return List.of(
        Map.of("name", "Instagram Post", "width", 1080, "height", 1080, "category", "social"),
        Map.of("name", "Instagram Story", "width", 1080, "height", 1920, "category", "social"),
        Map.of("name", "Facebook Cover", "width", 820, "height", 312, "category", "social"),
        Map.of("name", "Twitter Post", "width", 1200, "height", 675, "category", "social"),
        Map.of("name", "YouTube Thumbnail", "width", 1280, "height", 720, "category", "social"),
        Map.of("name", "Poster A4", "width", 2480, "height", 3508, "category", "print"),
        Map.of("name", "Business Card", "width", 1050, "height", 600, "category", "print"),
        Map.of("name", "Desktop Wallpaper", "width", 1920, "height", 1080, "category", "wallpaper"),
        Map.of("name", "Mobile Wallpaper", "width", 1080, "height", 1920, "category", "wallpaper")
    );
  }

  // ==================== Helpers ====================

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
