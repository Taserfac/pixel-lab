package com.pixellab.dao;

import com.pixellab.model.SessionUser;
import com.pixellab.util.SqlRows;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
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
    Map<String, Object> stats = new LinkedHashMap<>();
    try (Connection conn = dataSource.getConnection()) {
      stats.put("imageCount", count(conn, "SELECT COUNT(*) FROM image WHERE user_id = ? AND status = 1", userId));
      stats.put("publicImageCount", count(conn, "SELECT COUNT(*) FROM image WHERE user_id = ? AND is_public = 1 AND status = 1", userId));
      stats.put("likeCount", count(conn, "SELECT COUNT(*) FROM likes WHERE user_id = ?", userId));
      stats.put("collectionCount", count(conn, "SELECT COUNT(*) FROM collections WHERE user_id = ?", userId));
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
}
