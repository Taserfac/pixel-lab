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
}
