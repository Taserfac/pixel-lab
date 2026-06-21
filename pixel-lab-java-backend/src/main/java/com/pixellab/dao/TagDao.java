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

public class TagDao {
  private final DataSource dataSource;

  public TagDao(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * 按名称模糊搜索标签，按热度排序
   */
  public List<Map<String, Object>> searchByName(String keyword, int limit) throws Exception {
    int safeLimit = Math.max(1, Math.min(limit, 50));
    String sql = "SELECT id, name, usage_count FROM tag WHERE name LIKE ? ORDER BY usage_count DESC LIMIT ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, "%" + keyword + "%");
      stmt.setInt(2, safeLimit);
      try (ResultSet rs = stmt.executeQuery()) {
        return SqlRows.many(rs);
      }
    }
  }

  /**
   * 获取所有标签
   */
  public List<Map<String, Object>> findAll() throws Exception {
    String sql = "SELECT id, name, usage_count FROM tag ORDER BY usage_count DESC";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      return SqlRows.many(rs);
    }
  }

  /**
   * 根据 ID 列表批量获取标签
   */
  public List<Map<String, Object>> findByIds(List<Integer> ids) throws Exception {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    StringBuilder sql = new StringBuilder("SELECT id, name, usage_count FROM tag WHERE id IN (");
    for (int i = 0; i < ids.size(); i++) {
      if (i > 0) sql.append(",");
      sql.append("?");
    }
    sql.append(")");
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int i = 0; i < ids.size(); i++) {
        stmt.setInt(i + 1, ids.get(i));
      }
      try (ResultSet rs = stmt.executeQuery()) {
        return SqlRows.many(rs);
      }
    }
  }

  /**
   * 获取图片关联的标签
   */
  public List<Map<String, Object>> findByImageId(int imageId) throws Exception {
    String sql = "SELECT t.id, t.name, t.usage_count FROM tag t "
        + "JOIN image_tag it ON t.id = it.tag_id WHERE it.image_id = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, imageId);
      try (ResultSet rs = stmt.executeQuery()) {
        return SqlRows.many(rs);
      }
    }
  }

  /**
   * 设置图片标签（事务操作：删除旧关联 → 插入新关联 → 更新 usage_count）
   */
  public void setImageTags(int imageId, List<Integer> tagIds) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try {
        // 先获取旧标签 ID
        List<Integer> oldTagIds = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT tag_id FROM image_tag WHERE image_id = ?")) {
          stmt.setInt(1, imageId);
          try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
              oldTagIds.add(rs.getInt("tag_id"));
            }
          }
        }

        // 删除旧关联
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM image_tag WHERE image_id = ?")) {
          stmt.setInt(1, imageId);
          stmt.executeUpdate();
        }

        // 旧标签 usage_count 减 1
        if (!oldTagIds.isEmpty()) {
          StringBuilder sql = new StringBuilder("UPDATE tag SET usage_count = GREATEST(0, usage_count - 1) WHERE id IN (");
          for (int i = 0; i < oldTagIds.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
          }
          sql.append(")");
          try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < oldTagIds.size(); i++) {
              stmt.setInt(i + 1, oldTagIds.get(i));
            }
            stmt.executeUpdate();
          }
        }

        // 插入新关联
        if (tagIds != null && !tagIds.isEmpty()) {
          try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO image_tag (image_id, tag_id) VALUES (?, ?)")) {
            for (int tagId : tagIds) {
              stmt.setInt(1, imageId);
              stmt.setInt(2, tagId);
              stmt.addBatch();
            }
            stmt.executeBatch();
          }

          // 新标签 usage_count 加 1
          StringBuilder updateSql = new StringBuilder("UPDATE tag SET usage_count = usage_count + 1 WHERE id IN (");
          for (int i = 0; i < tagIds.size(); i++) {
            if (i > 0) updateSql.append(",");
            updateSql.append("?");
          }
          updateSql.append(")");
          try (PreparedStatement stmt = conn.prepareStatement(updateSql.toString())) {
            for (int i = 0; i < tagIds.size(); i++) {
              stmt.setInt(i + 1, tagIds.get(i));
            }
            stmt.executeUpdate();
          }
        }

        conn.commit();
      } catch (Exception ex) {
        conn.rollback();
        throw ex;
      }
    }
  }

  /**
   * 获取热门标签（按 usage_count 排序）
   */
  public List<Map<String, Object>> getHotTags(int limit) throws Exception {
    int safeLimit = Math.max(1, Math.min(limit, 50));
    String sql = "SELECT id, name, usage_count FROM tag WHERE usage_count > 0 ORDER BY usage_count DESC LIMIT ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, safeLimit);
      try (ResultSet rs = stmt.executeQuery()) {
        return SqlRows.many(rs);
      }
    }
  }

  /**
   * 根据图片 ID 列表批量获取标签（用于列表页）
   */
  public Map<Integer, List<Map<String, Object>>> findByImageIds(List<Integer> imageIds) throws Exception {
    if (imageIds == null || imageIds.isEmpty()) {
      return Map.of();
    }
    StringBuilder sql = new StringBuilder(
        "SELECT it.image_id, t.id, t.name, t.usage_count FROM tag t "
            + "JOIN image_tag it ON t.id = it.tag_id WHERE it.image_id IN (");
    for (int i = 0; i < imageIds.size(); i++) {
      if (i > 0) sql.append(",");
      sql.append("?");
    }
    sql.append(") ORDER BY t.usage_count DESC");
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int i = 0; i < imageIds.size(); i++) {
        stmt.setInt(i + 1, imageIds.get(i));
      }
      try (ResultSet rs = stmt.executeQuery()) {
        Map<Integer, List<Map<String, Object>>> result = new LinkedHashMap<>();
        while (rs.next()) {
          int imageId = rs.getInt("image_id");
          Map<String, Object> tag = new LinkedHashMap<>();
          tag.put("id", rs.getInt("id"));
          tag.put("name", rs.getString("name"));
          tag.put("usageCount", rs.getInt("usage_count"));
          result.computeIfAbsent(imageId, k -> new ArrayList<>()).add(tag);
        }
        return result;
      }
    }
  }
}
