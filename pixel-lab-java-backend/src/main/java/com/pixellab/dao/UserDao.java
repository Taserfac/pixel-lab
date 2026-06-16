package com.pixellab.dao;

import com.pixellab.model.SessionUser;
import com.pixellab.util.SqlRows;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserDao {
  private final DataSource dataSource;

  public UserDao(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public Map<String, Object> findByUsername(String username) throws Exception {
    String sql = "SELECT id, username, password, nickname, avatar, role, status, created_at, updated_at "
        + "FROM `user` WHERE username = ? AND is_deleted = 0 LIMIT 1";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, username);
      try (ResultSet rs = stmt.executeQuery()) {
        return SqlRows.one(rs);
      }
    }
  }

  public Map<String, Object> findById(long id) throws Exception {
    String sql = "SELECT id, username, nickname, avatar, role, status, created_at, updated_at "
        + "FROM `user` WHERE id = ? AND is_deleted = 0 LIMIT 1";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        return SqlRows.one(rs);
      }
    }
  }

  public boolean existsByUsername(String username) throws Exception {
    String sql = "SELECT id FROM `user` WHERE username = ? AND is_deleted = 0 LIMIT 1";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, username);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next();
      }
    }
  }

  public long create(String username, String password, String nickname) throws Exception {
    String sql = "INSERT INTO `user` (username, password, nickname, role, status) VALUES (?, ?, ?, 'user', 1)";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, username);
      stmt.setString(2, password);
      stmt.setString(3, nickname);
      stmt.executeUpdate();
      try (ResultSet rs = stmt.getGeneratedKeys()) {
        return rs.next() ? rs.getLong(1) : 0;
      }
    }
  }

  public void updateProfile(long id, String nickname, String avatar) throws Exception {
    String sql = "UPDATE `user` SET nickname = COALESCE(?, nickname), avatar = COALESCE(?, avatar) WHERE id = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, nickname);
      stmt.setString(2, avatar);
      stmt.setLong(3, id);
      stmt.executeUpdate();
    }
  }

  public void updatePassword(long id, String password) throws Exception {
    String sql = "UPDATE `user` SET password = ? WHERE id = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, password);
      stmt.setLong(2, id);
      stmt.executeUpdate();
    }
  }

  public Map<String, Object> getStats(long userId) throws Exception {
    return getStats(userId, 7);
  }

  public Map<String, Object> getStats(long userId, int days) throws Exception {
    Map<String, Object> stats = new LinkedHashMap<>();
    int safeDays = Math.max(1, Math.min(days, 90));
    try (Connection conn = dataSource.getConnection()) {
      long works = count(conn, "SELECT COUNT(*) FROM image WHERE user_id = ? AND status = 1", userId);
      long publicWorks = count(conn, "SELECT COUNT(*) FROM image WHERE user_id = ? AND is_public = 1 AND status = 1", userId);
      long receivedLikes = count(conn, "SELECT COALESCE(SUM(like_count), 0) FROM image WHERE user_id = ? AND status = 1", userId);
      long receivedCollects = count(conn, "SELECT COALESCE(SUM(collect_count), 0) FROM image WHERE user_id = ? AND status = 1", userId);
      long views = count(conn, "SELECT COALESCE(SUM(view_count), 0) FROM image WHERE user_id = ? AND status = 1", userId);
      long likedWorks = count(conn, "SELECT COUNT(*) FROM likes WHERE user_id = ?", userId);
      long collectedWorks = count(conn, "SELECT COUNT(*) FROM collections WHERE user_id = ?", userId);

      stats.put("works", works);
      stats.put("likes", receivedLikes);
      stats.put("views", views);
      stats.put("collects", receivedCollects);

      stats.put("imageCount", works);
      stats.put("publicImageCount", publicWorks);
      stats.put("receivedLikeCount", receivedLikes);
      stats.put("receivedCollectCount", receivedCollects);
      stats.put("viewCount", views);
      stats.put("likeCount", likedWorks);
      stats.put("collectionCount", collectedWorks);
      stats.put("trend", getTrend(conn, userId, safeDays));
      stats.put("hotWorks", getHotWorks(conn, userId));
    }
    return stats;
  }

  public SessionUser toSessionUser(Map<String, Object> row) {
    long id = ((Number) row.get("id")).longValue();
    String username = String.valueOf(row.get("username"));
    String nickname = row.get("nickname") == null ? username : String.valueOf(row.get("nickname"));
    String avatar = row.get("avatar") == null ? null : String.valueOf(row.get("avatar"));
    String role = row.get("role") == null ? "user" : String.valueOf(row.get("role"));
    int status = row.get("status") == null ? 1 : ((Number) row.get("status")).intValue();
    return new SessionUser(id, username, nickname, avatar, role, status);
  }

  private long count(Connection conn, String sql, long userId) throws Exception {
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, userId);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next() ? rs.getLong(1) : 0;
      }
    }
  }

  private List<Map<String, Object>> getTrend(Connection conn, long userId, int days) throws Exception {
    LocalDate startDate = LocalDate.now().minusDays(days - 1L);
    Timestamp startTime = Timestamp.valueOf(startDate.atStartOfDay());
    DateTimeFormatter keyFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
    DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("M月d日");

    Map<String, Map<String, Object>> byDate = new LinkedHashMap<>();
    for (int i = 0; i < days; i++) {
      LocalDate date = startDate.plusDays(i);
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("date", date.format(keyFormatter));
      row.put("label", date.format(labelFormatter));
      row.put("works", 0L);
      row.put("views", 0L);
      row.put("likes", 0L);
      row.put("collects", 0L);
      byDate.put(date.format(keyFormatter), row);
    }

    mergeImageTrend(conn, userId, startTime, byDate);
    mergeInteractionTrend(conn, userId, startTime, byDate,
        "SELECT DATE(l.created_at) AS day, COUNT(*) AS total "
            + "FROM likes l INNER JOIN image i ON l.image_id = i.id "
            + "WHERE i.user_id = ? AND i.status = 1 AND l.created_at >= ? "
            + "GROUP BY DATE(l.created_at)",
        "likes");
    mergeInteractionTrend(conn, userId, startTime, byDate,
        "SELECT DATE(c.created_at) AS day, COUNT(*) AS total "
            + "FROM collections c INNER JOIN image i ON c.image_id = i.id "
            + "WHERE i.user_id = ? AND i.status = 1 AND c.created_at >= ? "
            + "GROUP BY DATE(c.created_at)",
        "collects");

    return new ArrayList<>(byDate.values());
  }

  private void mergeImageTrend(Connection conn, long userId, Timestamp startTime,
                               Map<String, Map<String, Object>> byDate) throws Exception {
    String sql = "SELECT DATE(created_at) AS day, COUNT(*) AS works, COALESCE(SUM(view_count), 0) AS views "
        + "FROM image WHERE user_id = ? AND status = 1 AND created_at >= ? GROUP BY DATE(created_at)";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, userId);
      stmt.setTimestamp(2, startTime);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Map<String, Object> row = byDate.get(String.valueOf(rs.getDate("day")));
          if (row != null) {
            row.put("works", rs.getLong("works"));
            row.put("views", rs.getLong("views"));
          }
        }
      }
    }
  }

  private void mergeInteractionTrend(Connection conn, long userId, Timestamp startTime,
                                     Map<String, Map<String, Object>> byDate,
                                     String sql, String targetKey) throws Exception {
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, userId);
      stmt.setTimestamp(2, startTime);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Map<String, Object> row = byDate.get(String.valueOf(rs.getDate("day")));
          if (row != null) {
            row.put(targetKey, rs.getLong("total"));
          }
        }
      }
    }
  }

  private List<Map<String, Object>> getHotWorks(Connection conn, long userId) throws Exception {
    String sql = "SELECT id, title, original_name, url, thumbnail_url, view_count, like_count, collect_count, created_at "
        + "FROM image WHERE user_id = ? AND status = 1 "
        + "ORDER BY view_count DESC, like_count DESC, collect_count DESC, created_at DESC LIMIT 5";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, userId);
      try (ResultSet rs = stmt.executeQuery()) {
        return SqlRows.many(rs);
      }
    }
  }
}
