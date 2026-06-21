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

public class AlbumDao {
  private final DataSource dataSource;

  public AlbumDao(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public long createAlbum(long userId, String title, String description, Long coverImageId, boolean isPublic) throws Exception {
    String sql = "INSERT INTO albums (user_id, title, description, cover_image_id, is_public) "
        + "VALUES (?, ?, ?, ?, ?)";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setLong(1, userId);
      stmt.setString(2, title);
      if (description == null || description.isBlank()) {
        stmt.setNull(3, java.sql.Types.LONGVARCHAR);
      } else {
        stmt.setString(3, description);
      }
      if (coverImageId == null) {
        stmt.setNull(4, java.sql.Types.INTEGER);
      } else {
        stmt.setLong(4, coverImageId);
      }
      stmt.setBoolean(5, isPublic);
      stmt.executeUpdate();
      try (ResultSet rs = stmt.getGeneratedKeys()) {
        return rs.next() ? rs.getLong(1) : 0;
      }
    }
  }

  public boolean updateAlbum(long albumId, long userId, String title, String description, Long coverImageId, boolean isPublic) throws Exception {
    String sql = "UPDATE albums SET title = ?, description = ?, cover_image_id = ?, is_public = ? "
        + "WHERE id = ? AND user_id = ? AND status = 1";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, title);
      if (description == null || description.isBlank()) {
        stmt.setNull(2, java.sql.Types.LONGVARCHAR);
      } else {
        stmt.setString(2, description);
      }
      if (coverImageId == null) {
        stmt.setNull(3, java.sql.Types.INTEGER);
      } else {
        stmt.setLong(3, coverImageId);
      }
      stmt.setBoolean(4, isPublic);
      stmt.setLong(5, albumId);
      stmt.setLong(6, userId);
      return stmt.executeUpdate() > 0;
    }
  }

  public boolean deleteAlbum(long albumId, long userId) throws Exception {
    String sql = "UPDATE albums SET status = 0 WHERE id = ? AND user_id = ? AND status = 1";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, albumId);
      stmt.setLong(2, userId);
      return stmt.executeUpdate() > 0;
    }
  }

  public Map<String, Object> getAlbum(long albumId) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      Map<String, Object> album = queryOne(conn,
          "SELECT a.*, u.nickname AS author_name, u.avatar AS author_avatar, "
              + "cover.url AS cover_url, cover.thumbnail_url AS cover_thumbnail_url "
              + "FROM albums a LEFT JOIN `user` u ON a.user_id = u.id "
              + "LEFT JOIN image cover ON a.cover_image_id = cover.id "
              + "WHERE a.id = ? AND a.status = 1",
          List.of(albumId));
      if (album == null) {
        return null;
      }
      long imageCount = count(conn,
          "SELECT COUNT(*) FROM album_images WHERE album_id = ?",
          List.of(albumId));
      album.put("imageCount", imageCount);
      return album;
    }
  }

  public List<Map<String, Object>> getUserAlbums(long userId) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      return query(conn,
          "SELECT a.*, "
              + "cover.url AS cover_url, cover.thumbnail_url AS cover_thumbnail_url, "
              + "(SELECT COUNT(*) FROM album_images WHERE album_id = a.id) AS image_count "
              + "FROM albums a LEFT JOIN image cover ON a.cover_image_id = cover.id "
              + "WHERE a.user_id = ? AND a.status = 1 ORDER BY a.updated_at DESC",
          List.of(userId));
    }
  }

  public List<Map<String, Object>> getAlbumImages(long albumId) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      return query(conn,
          "SELECT ai.sort_order, ai.description AS album_description, "
          + "i.id, i.title, i.original_name, i.url, i.thumbnail_url, "
              + "i.width, i.height, i.format, i.description AS image_description "
              + "FROM album_images ai LEFT JOIN image i ON ai.image_id = i.id "
              + "WHERE ai.album_id = ? AND i.status = 1 "
              + "ORDER BY ai.sort_order ASC, ai.created_at ASC",
          List.of(albumId));
    }
  }

  public boolean addImageToAlbum(long albumId, long imageId, String description) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      // Get next sort_order
      long maxOrder = count(conn,
          "SELECT COALESCE(MAX(sort_order), 0) FROM album_images WHERE album_id = ?",
          List.of(albumId));
      String sql = "INSERT INTO album_images (album_id, image_id, sort_order, description) VALUES (?, ?, ?, ?)";
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setLong(1, albumId);
        stmt.setLong(2, imageId);
        stmt.setLong(3, maxOrder + 1);
        if (description == null || description.isBlank()) {
          stmt.setNull(4, java.sql.Types.LONGVARCHAR);
        } else {
          stmt.setString(4, description);
        }
        stmt.executeUpdate();
        return true;
      }
    }
  }

  public boolean removeImageFromAlbum(long albumId, long imageId) throws Exception {
    String sql = "DELETE FROM album_images WHERE album_id = ? AND image_id = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, albumId);
      stmt.setLong(2, imageId);
      return stmt.executeUpdate() > 0;
    }
  }

  public boolean updateAlbumImageDescription(long albumId, long imageId, String description) throws Exception {
    String sql = "UPDATE album_images SET description = ? WHERE album_id = ? AND image_id = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      if (description == null || description.isBlank()) {
        stmt.setNull(1, java.sql.Types.LONGVARCHAR);
      } else {
        stmt.setString(1, description);
      }
      stmt.setLong(2, albumId);
      stmt.setLong(3, imageId);
      return stmt.executeUpdate() > 0;
    }
  }

  public void reorderAlbumImages(long albumId, List<Long> imageIds) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try {
        String sql = "UPDATE album_images SET sort_order = ? WHERE album_id = ? AND image_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
          for (int i = 0; i < imageIds.size(); i++) {
            stmt.setInt(1, i + 1);
            stmt.setLong(2, albumId);
            stmt.setLong(3, imageIds.get(i));
            stmt.addBatch();
          }
          stmt.executeBatch();
        }
        conn.commit();
      } catch (Exception ex) {
        conn.rollback();
        throw ex;
      }
    }
  }

  // --- Helper methods (same pattern as CommunityDao) ---

  private long count(Connection conn, String sql, List<Object> params) throws Exception {
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      bind(stmt, params);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next() ? rs.getLong(1) : 0;
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

  private void bind(PreparedStatement stmt, List<Object> params) throws Exception {
    for (int i = 0; i < params.size(); i++) {
      stmt.setObject(i + 1, params.get(i));
    }
  }
}
