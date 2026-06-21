package com.pixellab.dao;

import com.pixellab.util.SqlRows;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommunityDao {
  private static final List<String> SYSTEM_TAGS = List.of(
      "摄影", "插画", "AI艺术", "设计", "旅行", "像素艺术", "城市", "生活");
  private static final int MIN_TRENDING_USAGE = 2;
  private static final int MIN_TRENDING_LIKES = 3;

  private final DataSource dataSource;

  public CommunityDao(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public Map<String, Object> publicImages(int page, int pageSize, String keyword, String sortBy, Long userId) throws Exception {
    int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
    List<Object> params = new ArrayList<>();
    StringBuilder where = new StringBuilder(" WHERE i.is_public = 1 AND i.status = 1 ");
    if (keyword != null && !keyword.isBlank()) {
      where.append(" AND (i.title LIKE ? OR i.original_name LIKE ? OR i.filename LIKE ? OR i.tags LIKE ?) ");
      String like = "%" + keyword.trim() + "%";
      params.add(like);
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

  public List<Map<String, Object>> publicUsers(String keyword, int limit) throws Exception {
    if (keyword == null || keyword.isBlank()) {
      return List.of();
    }
    int safeLimit = Math.max(1, Math.min(limit, 20));
    String trimmedKeyword = keyword.trim();
    String like = "%" + trimmedKeyword + "%";
    try (Connection conn = dataSource.getConnection()) {
      return query(conn,
          "SELECT u.id, u.username, u.nickname, u.avatar, COUNT(i.id) AS work_count "
              + "FROM `user` u LEFT JOIN image i ON i.user_id = u.id AND i.is_public = 1 AND i.status = 1 "
              + "WHERE u.status = 1 AND u.is_deleted = 0 AND (u.username LIKE ? OR u.nickname LIKE ?) "
              + "GROUP BY u.id, u.username, u.nickname, u.avatar "
              + "ORDER BY CASE WHEN u.username = ? OR u.nickname = ? THEN 0 ELSE 1 END, u.nickname, u.username LIMIT ?",
          List.of(like, like, trimmedKeyword, trimmedKeyword, safeLimit));
    }
  }

  public Map<String, Object> publicTags(int limit) throws Exception {
    int safeLimit = Math.max(1, Math.min(limit, 50));
    Map<String, TagStats> statsByTag = new LinkedHashMap<>();

    try (Connection conn = dataSource.getConnection()) {
      List<Map<String, Object>> rows = query(conn,
          "SELECT tags, like_count FROM image "
              + "WHERE is_public = 1 AND status = 1 AND tags IS NOT NULL AND tags != ''",
          List.of());

      for (Map<String, Object> row : rows) {
        String rawTags = String.valueOf(row.get("tags"));
        long likes = number(row.get("like_count"));
        Set<String> uniquePostTags = new HashSet<>();
        for (String rawTag : rawTags.split("[,，]")) {
          String tagName = rawTag.trim().replaceFirst("^#+", "");
          if (tagName.isEmpty() || !uniquePostTags.add(tagName)) continue;
          statsByTag.computeIfAbsent(tagName, ignored -> new TagStats()).addPost(likes);
        }
      }
    }

    List<Map<String, Object>> trendingTags = statsByTag.entrySet().stream()
        .filter(entry -> entry.getValue().usageCount >= MIN_TRENDING_USAGE
            || entry.getValue().likeCount >= MIN_TRENDING_LIKES)
        .sorted(Comparator
            .<Map.Entry<String, TagStats>>comparingDouble(entry -> entry.getValue().score()).reversed()
            .thenComparing(Map.Entry::getKey))
        .limit(safeLimit)
        .map(entry -> {
          Map<String, Object> tag = new LinkedHashMap<>();
          tag.put("name", entry.getKey());
          tag.put("usageCount", entry.getValue().usageCount);
          tag.put("score", entry.getValue().score());
          return tag;
        })
        .toList();

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("systemTags", SYSTEM_TAGS);
    result.put("trendingTags", trendingTags);
    return result;
  }

  public Map<String, Object> imageDetail(long imageId, Long userId) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      Map<String, Object> image = queryOne(conn,
          "SELECT i.*, u.nickname AS author_name, u.avatar AS author_avatar "
              + "FROM image i LEFT JOIN `user` u ON i.user_id = u.id "
              + "WHERE i.id = ? AND i.status = 1 AND (i.is_public = 1 OR i.user_id = ?)",
          List.of(imageId, userId == null ? -1L : userId));
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
        Map<String, Object> image = imageOwnerInfo(conn, imageId);
        notifyImageOwner(conn, image, userId, "like",
            actorName(conn, userId) + "点赞了你的作品《" + imageTitle(image) + "》", imageId);
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
        Map<String, Object> image = imageOwnerInfo(conn, imageId);
        notifyImageOwner(conn, image, userId, "collect",
            actorName(conn, userId) + "收藏了你的作品《" + imageTitle(image) + "》", imageId);
        conn.commit();
        return Map.of("collected", true);
      } catch (Exception ex) {
        conn.rollback();
        throw ex;
      }
    }
  }

  public long reportImage(long userId, long imageId, String reason, String detail) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      Map<String, Object> image = queryOne(conn,
          "SELECT id FROM image WHERE id = ? AND status = 1 AND is_public = 1",
          List.of(imageId));
      if (image == null) {
        return -1;
      }
      if (exists(conn,
          "SELECT id FROM reports WHERE reporter_id = ? AND image_id = ? AND status = 0",
          List.of(userId, imageId))) {
        return -2;
      }
      try (PreparedStatement stmt = conn.prepareStatement(
          "INSERT INTO reports (image_id, reporter_id, reason, detail) VALUES (?, ?, ?, ?)",
          Statement.RETURN_GENERATED_KEYS)) {
        stmt.setLong(1, imageId);
        stmt.setLong(2, userId);
        stmt.setString(3, reason);
        if (detail == null || detail.isBlank()) {
          stmt.setNull(4, java.sql.Types.VARCHAR);
        } else {
          stmt.setString(4, detail);
        }
        stmt.executeUpdate();
        try (ResultSet rs = stmt.getGeneratedKeys()) {
          return rs.next() ? rs.getLong(1) : 0;
        }
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
      try {
        Map<String, Object> parent = null;
        if (parentId != null) {
          parent = queryOne(conn,
              "SELECT c.*, u.nickname FROM comments c LEFT JOIN `user` u ON c.user_id = u.id "
                  + "WHERE c.id = ? AND c.image_id = ? AND c.status = 1",
              List.of(parentId, imageId));
          if (parent == null) {
            parentId = null;
          }
        }

        long id = 0;
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
          try (ResultSet rs = stmt.getGeneratedKeys()) {
            if (rs.next()) {
              id = rs.getLong(1);
            }
          }
        }

        execute(conn, "UPDATE image SET comment_count = comment_count + 1 WHERE id = ?", List.of(imageId));
        Map<String, Object> image = imageOwnerInfo(conn, imageId);
        String actor = actorName(conn, userId);
        String text = snippet(content);

        if (parent != null) {
          long targetUserId = number(parent.get("user_id"));
          createNotificationIfNeeded(conn, targetUserId, userId, "reply",
              actor + "回复了你的评论：" + text, imageId, "image");
          long imageOwnerId = number(image == null ? null : image.get("user_id"));
          if (imageOwnerId != targetUserId) {
            notifyImageOwner(conn, image, userId, "comment",
                actor + "评论了你的作品《" + imageTitle(image) + "》：" + text, imageId);
          }
        } else {
          notifyImageOwner(conn, image, userId, "comment",
              actor + "评论了你的作品《" + imageTitle(image) + "》：" + text, imageId);
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
        Map<String, Object> comment = queryOne(conn,
            "SELECT c.* FROM comments c JOIN image i ON c.image_id = i.id "
                + "WHERE c.id = ? AND (c.user_id = ? OR i.user_id = ?)",
            List.of(commentId, userId, userId));
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
      long total = count(conn,
          "SELECT COUNT(*) FROM collections c JOIN image i ON c.image_id = i.id WHERE c.user_id = ? AND i.status = 1",
          List.of(userId));
      List<Map<String, Object>> rows = query(conn,
          "SELECT i.*, u.nickname AS author_name, c.created_at AS collected_at "
              + "FROM collections c LEFT JOIN image i ON c.image_id = i.id LEFT JOIN `user` u ON i.user_id = u.id "
              + "WHERE c.user_id = ? AND i.status = 1 ORDER BY c.created_at DESC LIMIT ? OFFSET ?",
          List.of(userId, pageSize, offset));
      return pageResult(rows, total, page, pageSize);
    }
  }

  public Map<String, Object> userLikes(long userId, int page, int pageSize) throws Exception {
    int offset = (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
    try (Connection conn = dataSource.getConnection()) {
      long total = count(conn,
          "SELECT COUNT(*) FROM likes l JOIN image i ON l.image_id = i.id WHERE l.user_id = ? AND i.status = 1",
          List.of(userId));
      List<Map<String, Object>> rows = query(conn,
          "SELECT i.*, u.nickname AS author_name, l.created_at AS liked_at "
              + "FROM likes l LEFT JOIN image i ON l.image_id = i.id LEFT JOIN `user` u ON i.user_id = u.id "
              + "WHERE l.user_id = ? AND i.status = 1 ORDER BY l.created_at DESC LIMIT ? OFFSET ?",
          List.of(userId, pageSize, offset));
      return pageResult(rows, total, page, pageSize);
    }
  }

  public List<Map<String, Object>> similarWorks(long imageId, int limit) throws Exception {
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

  public List<Map<String, Object>> followingCreators(long userId) throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      return query(conn,
          "SELECT u.id, u.username, u.nickname, u.avatar, "
              + "COUNT(i.id) AS work_count, COALESCE(SUM(i.like_count), 0) AS like_count, "
              + "COALESCE(SUM(i.collect_count), 0) AS collect_count, COALESCE(SUM(i.comment_count), 0) AS comment_count, "
              + "(SELECT cover.url FROM image cover WHERE cover.user_id = u.id AND cover.is_public = 1 AND cover.status = 1 "
              + "ORDER BY cover.like_count DESC, cover.created_at DESC LIMIT 1) AS representative_url "
              + "FROM follows f JOIN `user` u ON f.followed_id = u.id "
              + "LEFT JOIN image i ON i.user_id = u.id AND i.is_public = 1 AND i.status = 1 "
              + "WHERE f.follower_id = ? AND u.status = 1 AND u.is_deleted = 0 "
              + "GROUP BY u.id, u.username, u.nickname, u.avatar, f.created_at ORDER BY f.created_at DESC",
          List.of(userId));
    }
  }

  public Map<String, Object> publicProfile(long profileUserId, Long viewerId, int page, int pageSize) throws Exception {
    int safePage = Math.max(page, 1);
    int safePageSize = Math.max(1, Math.min(pageSize, 50));
    int offset = (safePage - 1) * safePageSize;
    try (Connection conn = dataSource.getConnection()) {
      Map<String, Object> user = queryOne(conn,
          "SELECT id, username, nickname, avatar, created_at FROM `user` "
              + "WHERE id = ? AND status = 1 AND is_deleted = 0",
          List.of(profileUserId));
      if (user == null) {
        return null;
      }

      long workCount = count(conn,
          "SELECT COUNT(*) FROM image WHERE user_id = ? AND is_public = 1 AND status = 1",
          List.of(profileUserId));
      long likeCount = count(conn,
          "SELECT COALESCE(SUM(like_count), 0) FROM image WHERE user_id = ? AND is_public = 1 AND status = 1",
          List.of(profileUserId));
      long viewCount = count(conn,
          "SELECT COALESCE(SUM(view_count), 0) FROM image WHERE user_id = ? AND is_public = 1 AND status = 1",
          List.of(profileUserId));
      long followerCount = count(conn,
          "SELECT COUNT(*) FROM follows WHERE followed_id = ?", List.of(profileUserId));
      long followingCount = count(conn,
          "SELECT COUNT(*) FROM follows WHERE follower_id = ?", List.of(profileUserId));

      List<Map<String, Object>> works = query(conn,
          "SELECT i.*, u.nickname AS author_name, u.avatar AS author_avatar "
              + "FROM image i LEFT JOIN `user` u ON i.user_id = u.id "
              + "WHERE i.user_id = ? AND i.is_public = 1 AND i.status = 1 "
              + "ORDER BY i.created_at DESC LIMIT ? OFFSET ?",
          List.of(profileUserId, safePageSize, offset));
      addUserState(conn, works, viewerId);

      List<Map<String, Object>> albums = query(conn,
          "SELECT a.id, a.title, a.description, a.cover_image_id, a.created_at, a.updated_at, "
              + "cover.url AS cover_url, cover.thumbnail_url AS cover_thumbnail_url, "
              + "(SELECT COUNT(*) FROM album_images ai JOIN image album_image ON ai.image_id = album_image.id "
              + "WHERE ai.album_id = a.id AND album_image.status = 1) AS image_count "
              + "FROM albums a LEFT JOIN image cover ON a.cover_image_id = cover.id "
              + "WHERE a.user_id = ? AND a.is_public = 1 AND a.status = 1 "
              + "ORDER BY a.updated_at DESC",
          List.of(profileUserId));

      Map<String, Object> stats = new LinkedHashMap<>();
      stats.put("works", workCount);
      stats.put("likes", likeCount);
      stats.put("views", viewCount);
      stats.put("followers", followerCount);
      stats.put("following", followingCount);

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("user", user);
      result.put("stats", stats);
      result.put("isFollowing", viewerId != null && viewerId != profileUserId
          && exists(conn, "SELECT id FROM follows WHERE follower_id = ? AND followed_id = ?",
              List.of(viewerId, profileUserId)));
      result.put("works", pageResult(works, workCount, safePage, safePageSize));
      result.put("albums", albums);
      return result;
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

  private Map<String, Object> imageOwnerInfo(Connection conn, long imageId) throws Exception {
    return queryOne(conn,
        "SELECT id, user_id, title, original_name FROM image WHERE id = ? AND status = 1",
        List.of(imageId));
  }

  private String imageTitle(Map<String, Object> image) {
    if (image == null) return "未命名作品";
    Object title = image.get("title");
    if (title != null && !String.valueOf(title).isBlank()) return String.valueOf(title);
    Object originalName = image.get("original_name");
    if (originalName != null && !String.valueOf(originalName).isBlank()) return String.valueOf(originalName);
    return "未命名作品";
  }

  private String actorName(Connection conn, long userId) throws Exception {
    Map<String, Object> user = queryOne(conn, "SELECT nickname, username FROM `user` WHERE id = ?", List.of(userId));
    if (user == null) return "有人";
    Object nickname = user.get("nickname");
    if (nickname != null && !String.valueOf(nickname).isBlank()) return String.valueOf(nickname);
    Object username = user.get("username");
    if (username != null && !String.valueOf(username).isBlank()) return String.valueOf(username);
    return "有人";
  }

  private String snippet(String content) {
    String text = content == null ? "" : content.trim().replaceAll("\\s+", " ");
    if (text.length() <= 80) return text;
    return text.substring(0, 80) + "...";
  }

  private long number(Object value) {
    if (value instanceof Number) return ((Number) value).longValue();
    if (value == null) return 0;
    return Long.parseLong(String.valueOf(value));
  }

  private void notifyImageOwner(Connection conn, Map<String, Object> image, long senderId, String type, String content, long imageId) throws Exception {
    if (image == null) return;
    createNotificationIfNeeded(conn, number(image.get("user_id")), senderId, type, content, imageId, "image");
  }

  private void createNotificationIfNeeded(Connection conn, long userId, long senderId, String type, String content, long referenceId, String referenceType) throws Exception {
    if (userId <= 0 || userId == senderId) return;
    try (PreparedStatement stmt = conn.prepareStatement(
        "INSERT INTO notifications (user_id, sender_id, type, content, reference_id, reference_type) VALUES (?, ?, ?, ?, ?, ?)")) {
      stmt.setLong(1, userId);
      stmt.setLong(2, senderId);
      stmt.setString(3, type);
      stmt.setString(4, content);
      stmt.setLong(5, referenceId);
      stmt.setString(6, referenceType);
      stmt.executeUpdate();
    }
  }

  private void bind(PreparedStatement stmt, List<Object> params) throws Exception {
    for (int i = 0; i < params.size(); i++) {
      stmt.setObject(i + 1, params.get(i));
    }
  }

  private static final class TagStats {
    private long usageCount;
    private long likeCount;

    private void addPost(long likes) {
      usageCount++;
      likeCount += likes;
    }

    private double score() {
      return usageCount * 1.5 + likeCount * 2;
    }
  }
}
