package com.pixellab.servlet;

import com.pixellab.config.AppConfig;
import com.pixellab.context.AppContextKeys;
import com.pixellab.dao.UserDao;
import com.pixellab.model.SessionUser;
import com.pixellab.util.PasswordUtil;
import com.pixellab.util.RequestUtil;
import com.pixellab.util.Result;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@MultipartConfig
public class AuthServlet extends BaseApiServlet {
  private static final Set<String> ALLOWED_AVATAR_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    String action = segments.isEmpty() ? "" : segments.get(0);
    try {
      if ("login".equals(action)) {
        login(request, response);
      } else if ("register".equals(action)) {
        register(request, response);
      } else if ("logout".equals(action)) {
        logout(request, response);
      } else if ("avatar".equals(action)) {
        uploadAvatar(request, response);
      } else {
        Result.notFound(response, "接口不存在");
      }
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Auth API failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    String action = segments.isEmpty() ? "" : segments.get(0);
    try {
      if ("userinfo".equals(action)) {
        userinfo(request, response);
      } else if ("stats".equals(action)) {
        stats(request, response);
      } else {
        Result.notFound(response, "接口不存在");
      }
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Auth API failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  @Override
  protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    String action = segments.isEmpty() ? "" : segments.get(0);
    try {
      if ("profile".equals(action)) {
        updateProfile(request, response);
      } else if ("password".equals(action)) {
        changePassword(request, response);
      } else {
        Result.notFound(response, "接口不存在");
      }
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Auth API failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  private void register(HttpServletRequest request, HttpServletResponse response) throws Exception {
    Map<String, Object> body = body(request);
    String username = RequestUtil.string(body, "username");
    String password = RequestUtil.string(body, "password");
    String nickname = RequestUtil.string(body, "nickname");
    if (username == null || username.length() < 3 || password == null || password.length() < 6) {
      Result.badRequest(response, "用户名或密码格式不正确");
      return;
    }

    UserDao userDao = new UserDao(dataSource());
    if (userDao.existsByUsername(username)) {
      Result.write(response, HttpServletResponse.SC_CONFLICT, 409, "用户名已存在", null);
      return;
    }

    long id = userDao.create(username, PasswordUtil.hash(password), nickname == null || nickname.isBlank() ? username : nickname);
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("id", id);
    Result.write(response, HttpServletResponse.SC_CREATED, 201, "注册成功", data);
  }

  private void login(HttpServletRequest request, HttpServletResponse response) throws Exception {
    Map<String, Object> body = body(request);
    String username = RequestUtil.string(body, "username");
    String password = RequestUtil.string(body, "password");
    if (username == null || password == null) {
      Result.badRequest(response, "请输入用户名和密码");
      return;
    }

    UserDao userDao = new UserDao(dataSource());
    Map<String, Object> row = userDao.findByUsername(username);
    if (row == null || !PasswordUtil.verify(password, String.valueOf(row.get("password")))) {
      Result.unauthorized(response, "用户名或密码错误，请重新输入");
      return;
    }
    int status = row.get("status") == null ? 1 : ((Number) row.get("status")).intValue();
    if (status != 1) {
      Result.forbidden(response, "账号已被禁用");
      return;
    }

    SessionUser user = userDao.toSessionUser(row);
    HttpSession session = request.getSession(true);
    AppConfig config = (AppConfig) getServletContext().getAttribute(AppContextKeys.APP_CONFIG);
    session.setMaxInactiveInterval(config.getInt("session.timeout.minutes", 10080) * 60);
    session.setAttribute(AppContextKeys.LOGIN_USER, user);
    session.setAttribute("userId", user.getId());
    session.setAttribute("role", user.getRole());
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("user", user.toSafeMap());
    ok(response, "登录成功", data);
  }

  private void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    ok(response, "退出成功", null);
  }

  private void userinfo(HttpServletRequest request, HttpServletResponse response) throws Exception {
    SessionUser current = currentUser(request);
    if (current == null) {
      Result.unauthorized(response, "请先登录");
      return;
    }
    UserDao userDao = new UserDao(dataSource());
    Map<String, Object> user = userDao.findById(current.getId());
    if (user == null) {
      Result.notFound(response, "用户不存在");
      return;
    }
    ok(response, user);
  }

  private void stats(HttpServletRequest request, HttpServletResponse response) throws Exception {
    SessionUser current = currentUser(request);
    int days = RequestUtil.intParam(request, "days", 7);
    ok(response, new UserDao(dataSource()).getStats(current.getId(), days));
  }

  private void updateProfile(HttpServletRequest request, HttpServletResponse response) throws Exception {
    SessionUser current = currentUser(request);
    Map<String, Object> body = body(request);
    String nickname = RequestUtil.string(body, "nickname");
    String avatar = RequestUtil.string(body, "avatar");
    if ((nickname == null || nickname.isBlank()) && (avatar == null || avatar.isBlank())) {
      Result.badRequest(response, "没有可更新的资料");
      return;
    }

    UserDao userDao = new UserDao(dataSource());
    userDao.updateProfile(current.getId(), nickname == null || nickname.isBlank() ? null : nickname, avatar == null || avatar.isBlank() ? null : avatar);
    Map<String, Object> user = userDao.findById(current.getId());
    request.getSession(false).setAttribute(AppContextKeys.LOGIN_USER, userDao.toSessionUser(user));
    ok(response, "更新成功", user);
  }

  private void uploadAvatar(HttpServletRequest request, HttpServletResponse response) throws Exception {
    SessionUser current = currentUser(request);
    if (current == null) {
      Result.unauthorized(response, "请先登录");
      return;
    }

    Part part = request.getPart("avatar");
    if (part == null || part.getSize() == 0) {
      Result.badRequest(response, "请选择要上传的头像");
      return;
    }

    long maxSize = 2L * 1024 * 1024;
    if (part.getSize() > maxSize) {
      Result.badRequest(response, "头像大小不能超过 2MB");
      return;
    }

    String originalName = getFileName(part);
    String ext = extension(originalName);
    if (!ALLOWED_AVATAR_EXT.contains(ext)) {
      Result.badRequest(response, "仅支持 jpg、png、gif、webp 图片");
      return;
    }

    File uploadDir = (File) getServletContext().getAttribute(AppContextKeys.UPLOAD_DIR);
    File avatarDir = new File(uploadDir, "avatars");
    if (!avatarDir.exists() && !avatarDir.mkdirs()) {
      throw new IllegalStateException("Cannot create avatar directory: " + avatarDir.getAbsolutePath());
    }

    String filename = "avatar_" + current.getId() + "_" + System.currentTimeMillis() + "_"
        + UUID.randomUUID().toString().replace("-", "") + "." + ext;
    File target = new File(avatarDir, filename);
    try (var inputStream = part.getInputStream()) {
      Files.copy(inputStream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    BufferedImage image = ImageIO.read(target);
    if (image == null) {
      Files.deleteIfExists(target.toPath());
      Result.badRequest(response, "请上传有效的图片文件");
      return;
    }

    String avatarUrl = publicBaseUrl(request) + "/uploads/avatars/" + filename;
    UserDao userDao = new UserDao(dataSource());
    userDao.updateProfile(current.getId(), null, avatarUrl);
    Map<String, Object> user = userDao.findById(current.getId());
    request.getSession(false).setAttribute(AppContextKeys.LOGIN_USER, userDao.toSessionUser(user));

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("url", avatarUrl);
    data.put("user", user);
    ok(response, "头像更新成功", data);
  }

  private void changePassword(HttpServletRequest request, HttpServletResponse response) throws Exception {
    SessionUser current = currentUser(request);
    Map<String, Object> body = body(request);
    String oldPassword = RequestUtil.string(body, "oldPassword");
    String newPassword = RequestUtil.string(body, "newPassword");
    if (oldPassword == null || newPassword == null || newPassword.length() < 6) {
      Result.badRequest(response, "密码格式不正确");
      return;
    }

    UserDao userDao = new UserDao(dataSource());
    Map<String, Object> row = userDao.findByUsername(current.getUsername());
    if (row == null || !PasswordUtil.verify(oldPassword, String.valueOf(row.get("password")))) {
      Result.unauthorized(response, "原密码错误");
      return;
    }
    userDao.updatePassword(current.getId(), PasswordUtil.hash(newPassword));
    ok(response, "密码修改成功", null);
  }

  private String getFileName(Part part) {
    String header = part.getHeader("content-disposition");
    if (header == null) {
      return "avatar";
    }
    for (String item : header.split(";")) {
      String trimmed = item.trim();
      if (trimmed.startsWith("filename=")) {
        return trimmed.substring(trimmed.indexOf('=') + 1).trim().replace("\"", "");
      }
    }
    return "avatar";
  }

  private String extension(String filename) {
    int index = filename == null ? -1 : filename.lastIndexOf('.');
    return index < 0 ? "" : filename.substring(index + 1).toLowerCase(Locale.ROOT);
  }

  private String publicBaseUrl(HttpServletRequest request) {
    String proto = request.getHeader("X-Forwarded-Proto");
    String host = request.getHeader("X-Forwarded-Host");
    if (proto == null || proto.isBlank()) {
      proto = request.getScheme();
    }
    if (host == null || host.isBlank()) {
      host = request.getHeader("Host");
    }
    return proto + "://" + host;
  }
}
