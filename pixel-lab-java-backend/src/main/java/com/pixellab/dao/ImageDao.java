package com.pixellab.dao;

import com.pixellab.util.SqlRows;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

public class ImageDao {
  private final DataSource dataSource;

  public ImageDao(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public long create(long userId, String filename, String originalName, String url, String thumbnailUrl,
                     Integer width, Integer height, long size, String format, String description) throws Exception {
    String sql = "INSERT INTO image (user_id, filename, original_name, url, thumbnail_url, width, height, size, format, description) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setLong(1, userId);
      stmt.setString(2, filename);
      stmt.setString(3, originalName);
      stmt.setString(4, url);
      stmt.setString(5, thumbnailUrl);
      if (width == null) {
        stmt.setNull(6, java.sql.Types.INTEGER);
      } else {
        stmt.setInt(6, width);
      }
      if (height == null) {
        stmt.setNull(7, java.sql.Types.INTEGER);
      } else {
        stmt.setInt(7, height);
      }
      stmt.setLong(8, size);
      stmt.setString(9, format);
      if (description == null || description.isBlank()) {
        stmt.setNull(10, java.sql.Types.LONGVARCHAR);
      } else {
        stmt.setString(10, description);
      }
      stmt.executeUpdate();
      try (ResultSet rs = stmt.getGeneratedKeys()) {
        return rs.next() ? rs.getLong(1) : 0;
      }
    }
  }

  public Map<String, Object> findById(long id) throws Exception {
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement("SELECT * FROM image WHERE id = ? AND status = 1")) {
      stmt.setLong(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        return SqlRows.one(rs);
      }
    }
  }

  public List<Map<String, Object>> findByUserId(long userId, int page, int pageSize) throws Exception {
    int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
    String sql = "SELECT * FROM image WHERE user_id = ? AND status = 1 ORDER BY created_at DESC LIMIT ? OFFSET ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, userId);
      stmt.setInt(2, pageSize);
      stmt.setInt(3, offset);
      try (ResultSet rs = stmt.executeQuery()) {
        return SqlRows.many(rs);
      }
    }
  }

  public long countByUserId(long userId) throws Exception {
    String sql = "SELECT COUNT(*) FROM image WHERE user_id = ? AND status = 1";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, userId);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next() ? rs.getLong(1) : 0;
      }
    }
  }

  public void softDelete(long id) throws Exception {
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement("UPDATE image SET status = 0 WHERE id = ?")) {
      stmt.setLong(1, id);
      stmt.executeUpdate();
    }
  }

  public void updateVisibility(long id, boolean isPublic) throws Exception {
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement("UPDATE image SET is_public = ? WHERE id = ?")) {
      stmt.setInt(1, isPublic ? 1 : 0);
      stmt.setLong(2, id);
      stmt.executeUpdate();
    }
  }

  public void updateMetadata(long id, String title, String description) throws Exception {
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement("UPDATE image SET title = ?, description = ? WHERE id = ?")) {
      stmt.setString(1, title == null || title.isBlank() ? null : title.trim());
      stmt.setString(2, description == null || description.isBlank() ? null : description.trim());
      stmt.setLong(3, id);
      stmt.executeUpdate();
    }
  }

  public void updateDescription(long id, String description) throws Exception {
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement("UPDATE image SET description = ? WHERE id = ?")) {
      if (description == null || description.isBlank()) {
        stmt.setNull(1, java.sql.Types.LONGVARCHAR);
      } else {
        stmt.setString(1, description);
      }
      stmt.setLong(2, id);
      stmt.executeUpdate();
    }
  }
}
