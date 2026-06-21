package com.pixellab.listener;

import com.pixellab.config.AppConfig;
import com.pixellab.context.AppContextKeys;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class AppContextListener implements ServletContextListener {
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    ServletContext context = sce.getServletContext();
    try {
      String configLocation = context.getInitParameter("appConfigLocation");
      InputStream inputStream = context.getResourceAsStream(configLocation);
      AppConfig config = AppConfig.load(inputStream);
      context.setAttribute(AppContextKeys.APP_CONFIG, config);

      HikariConfig hikari = new HikariConfig();
      String jdbcUrl = "jdbc:mysql://" + config.get("db.host", "localhost") + ":"
          + config.get("db.port", "3306") + "/" + config.get("db.name", "pixel_lab")
          + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
      hikari.setJdbcUrl(jdbcUrl);
      hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
      hikari.setUsername(config.get("db.user", "root"));
      hikari.setPassword(config.get("db.password", ""));
      hikari.setMaximumPoolSize(config.getInt("db.pool.size", 10));
      hikari.setPoolName("PixelLabHikariPool");
      hikari.addDataSourceProperty("cachePrepStmts", "true");
      hikari.addDataSourceProperty("prepStmtCacheSize", "250");
      hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

      HikariDataSource dataSource = new HikariDataSource(hikari);
      context.setAttribute(AppContextKeys.DATA_SOURCE, dataSource);
      ensureMediaVariantUrls(dataSource);
      ensureReportingSchema(dataSource);
      ensureTutorialSchema(dataSource);
      seedTutorialVideos(context, dataSource);

      File uploadDir = resolveUploadDir(config.get("upload.path", "uploads"));
      if (!uploadDir.exists() && !uploadDir.mkdirs()) {
        throw new IllegalStateException("Cannot create upload directory: " + uploadDir.getAbsolutePath());
      }
      context.setAttribute(AppContextKeys.UPLOAD_DIR, uploadDir);

      context.log("[Pixel Lab] Application started. DB pool initialized. Upload dir: " + uploadDir.getAbsolutePath());
    } catch (Exception ex) {
      context.log("[Pixel Lab] Application startup failed", ex);
      throw new IllegalStateException("Pixel Lab backend startup failed", ex);
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    ServletContext context = sce.getServletContext();
    Object dataSource = context.getAttribute(AppContextKeys.DATA_SOURCE);
    if (dataSource instanceof HikariDataSource) {
      ((HikariDataSource) dataSource).close();
      context.log("[Pixel Lab] DB pool closed.");
    }
  }

  private File resolveUploadDir(String uploadPath) {
    File dir = new File(uploadPath);
    if (dir.isAbsolute()) {
      return dir;
    }
    String base = System.getProperty("catalina.base", System.getProperty("user.dir"));
    return new File(base, uploadPath);
  }

  private void ensureReportingSchema(DataSource dataSource) throws Exception {
    String sql = "CREATE TABLE IF NOT EXISTS `reports` ("
        + "`id` INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,"
        + "`image_id` INT UNSIGNED NOT NULL,"
        + "`reporter_id` INT UNSIGNED NOT NULL,"
        + "`reason` VARCHAR(80) NOT NULL,"
        + "`detail` VARCHAR(500) DEFAULT NULL,"
        + "`status` TINYINT NOT NULL DEFAULT 0,"
        + "`handler_id` INT UNSIGNED DEFAULT NULL,"
        + "`handled_at` TIMESTAMP NULL DEFAULT NULL,"
        + "`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
        + "UNIQUE KEY `uk_reporter_image_open` (`reporter_id`, `image_id`, `status`),"
        + "INDEX `idx_image_id` (`image_id`),"
        + "INDEX `idx_status` (`status`),"
        + "INDEX `idx_created_at` (`created_at`),"
        + "CONSTRAINT `fk_reports_image` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE,"
        + "CONSTRAINT `fk_reports_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,"
        + "CONSTRAINT `fk_reports_handler` FOREIGN KEY (`handler_id`) REFERENCES `user` (`id`) ON DELETE SET NULL"
        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    try (Connection conn = dataSource.getConnection();
         Statement stmt = conn.createStatement()) {
      stmt.executeUpdate(sql);
    }
  }

  private void ensureMediaVariantUrls(DataSource dataSource) throws Exception {
    String updateThumbnails = "UPDATE image SET thumbnail_url = CONCAT(url, "
        + "CASE WHEN LOCATE('?', url) > 0 THEN '&' ELSE '?' END, 'variant=card') "
        + "WHERE url LIKE '%/uploads/%' "
        + "AND (thumbnail_url IS NULL OR thumbnail_url = '' OR thumbnail_url = url)";
    String updateAvatars = "UPDATE `user` SET avatar = CONCAT(avatar, "
        + "CASE WHEN LOCATE('?', avatar) > 0 THEN '&' ELSE '?' END, 'variant=avatar') "
        + "WHERE avatar LIKE '%/uploads/%' AND LOCATE('variant=', avatar) = 0";
    try (Connection conn = dataSource.getConnection();
         Statement stmt = conn.createStatement()) {
      stmt.executeUpdate(updateThumbnails);
      stmt.executeUpdate(updateAvatars);
    }
  }

  private void ensureTutorialSchema(DataSource dataSource) throws Exception {
    String sql = "CREATE TABLE IF NOT EXISTS `tutorial_videos` ("
        + "`id` INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,"
        + "`content_type` ENUM('video', 'article') NOT NULL DEFAULT 'video',"
        + "`category` VARCHAR(40) NOT NULL,"
        + "`title` VARCHAR(200) NOT NULL,"
        + "`description` VARCHAR(500) DEFAULT NULL,"
        + "`cover_url` VARCHAR(500) DEFAULT NULL,"
        + "`source_url` VARCHAR(500) NOT NULL,"
        + "`embed_url` VARCHAR(500) DEFAULT NULL,"
        + "`source_name` VARCHAR(40) DEFAULT NULL,"
        + "`author_name` VARCHAR(100) DEFAULT NULL,"
        + "`duration` VARCHAR(30) DEFAULT NULL,"
        + "`view_count` INT UNSIGNED NOT NULL DEFAULT 0,"
        + "`published_at` VARCHAR(40) DEFAULT NULL,"
        + "`sort_order` INT NOT NULL DEFAULT 100,"
        + "`status` TINYINT NOT NULL DEFAULT 1,"
        + "`crawled_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
        + "UNIQUE KEY `uk_tutorial_source_url` (`source_url`),"
        + "INDEX `idx_category` (`category`),"
        + "INDEX `idx_status` (`status`),"
        + "INDEX `idx_sort_order` (`sort_order`),"
        + "INDEX `idx_view_count` (`view_count`)"
        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    try (Connection conn = dataSource.getConnection();
         Statement stmt = conn.createStatement()) {
      stmt.executeUpdate(sql);
      addColumnIfMissing(stmt, "ALTER TABLE `tutorial_videos` ADD COLUMN `content_type` ENUM('video', 'article') NOT NULL DEFAULT 'video' AFTER `id`");
    }
  }

  private void addColumnIfMissing(Statement stmt, String sql) throws Exception {
    try {
      stmt.executeUpdate(sql);
    } catch (SQLException ex) {
      if (!"42S21".equals(ex.getSQLState()) && ex.getErrorCode() != 1060) {
        throw ex;
      }
    }
  }

  private void seedTutorialVideos(ServletContext context, DataSource dataSource) throws Exception {
    try (InputStream input = context.getResourceAsStream("/WEB-INF/classes/tutorial_videos_seed.sql")) {
      if (input == null) {
        return;
      }
      String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8)
          .lines()
          .filter(line -> !line.trim().startsWith("--"))
          .reduce("", (left, right) -> left + "\n" + right);
      try (Connection conn = dataSource.getConnection();
           Statement stmt = conn.createStatement()) {
        for (String statement : sql.split(";\\s*(\\r?\\n|$)")) {
          String trimmed = statement.trim();
          if (trimmed.isEmpty() || trimmed.startsWith("--")) {
            continue;
          }
          stmt.executeUpdate(trimmed);
        }
      }
      context.log("[Pixel Lab] Tutorial videos seeded from tutorial_videos_seed.sql");
    }
  }
}
