package com.pixellab.servlet;

import com.pixellab.config.AppConfig;
import com.pixellab.context.AppContextKeys;
import com.pixellab.dao.ImageDao;
import com.pixellab.model.SessionUser;
import com.pixellab.util.RequestUtil;
import com.pixellab.util.Result;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
public class ImageServlet extends BaseApiServlet {
  private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    try {
      if (segments.size() == 1 && "upload".equals(segments.get(0))) {
        upload(request, response);
        return;
      }
      if (segments.size() == 2 && "visibility".equals(segments.get(1))) {
        updateVisibility(request, response, parseId(segments.get(0)));
        return;
      }
      Result.notFound(response, "接口不存在");
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Image API failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    try {
      SessionUser user = currentUser(request);
      int page = RequestUtil.intParam(request, "page", 1);
      int pageSize = RequestUtil.intParam(request, "pageSize", 20);
      ImageDao imageDao = new ImageDao(dataSource());
      List<Map<String, Object>> list = imageDao.findByUserId(user.getId(), page, pageSize);
      long total = imageDao.countByUserId(user.getId());

      Map<String, Object> pagination = new LinkedHashMap<>();
      pagination.put("page", page);
      pagination.put("pageSize", pageSize);
      pagination.put("total", total);
      pagination.put("totalPages", (int) Math.ceil(total * 1.0 / pageSize));

      Map<String, Object> data = new LinkedHashMap<>();
      data.put("list", list);
      data.put("pagination", pagination);
      ok(response, data);
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Image list failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  @Override
  protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    long id = segments.isEmpty() ? -1 : parseId(segments.get(0));
    try {
      if (id <= 0) {
        Result.notFound(response, "图片不存在");
        return;
      }
      SessionUser user = currentUser(request);
      ImageDao imageDao = new ImageDao(dataSource());
      Map<String, Object> image = imageDao.findById(id);
      if (image == null) {
        Result.notFound(response, "图片不存在");
        return;
      }
      long ownerId = ((Number) image.get("user_id")).longValue();
      if (ownerId != user.getId()) {
        Result.forbidden(response, "无权删除此图片");
        return;
      }
      imageDao.softDelete(id);
      ok(response, "删除成功", null);
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Image delete failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  @Override
  protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = RequestUtil.pathSegments(request);
    try {
      if (segments.size() == 2 && "metadata".equals(segments.get(1))) {
        long id = parseId(segments.get(0));
        if (id <= 0) {
          Result.notFound(response, "图片不存在");
          return;
        }
        SessionUser user = currentUser(request);
        ImageDao imageDao = new ImageDao(dataSource());
        Map<String, Object> image = imageDao.findById(id);
        if (image == null) {
          Result.notFound(response, "图片不存在");
          return;
        }
        if (((Number) image.get("user_id")).longValue() != user.getId()) {
          Result.forbidden(response, "无权修改该作品");
          return;
        }

        Map<String, Object> body = body(request);
        String title = RequestUtil.string(body, "title");
        String description = RequestUtil.string(body, "description");
        if (title == null || title.isBlank()) {
          Result.badRequest(response, "作品标题不能为空");
          return;
        }
        if (title.length() > 100) {
          Result.badRequest(response, "作品标题不能超过 100 个字符");
          return;
        }
        if (description != null && description.length() > 1000) {
          Result.badRequest(response, "作品说明不能超过 1000 个字符");
          return;
        }

        Object rawTags = body.get("tags");
        List<?> tagValues = rawTags instanceof List<?> ? (List<?>) rawTags : List.of();
        if (tagValues.size() > 5) {
          Result.badRequest(response, "最多添加 5 个标签");
          return;
        }
        List<String> tags = new java.util.ArrayList<>();
        for (Object value : tagValues) {
          String tag = String.valueOf(value).trim().replaceFirst("^#", "");
          if (tag.isBlank()) {
            continue;
          }
          if (tag.length() > 20) {
            Result.badRequest(response, "单个标签不能超过 20 个字符");
            return;
          }
          if (!tags.contains(tag)) {
            tags.add(tag);
          }
        }

        imageDao.updateMetadata(id, title, String.join(",", tags), description);
        ok(response, "作品信息更新成功", null);
        return;
      }
      if (segments.size() == 2 && "description".equals(segments.get(1))) {
        long id = parseId(segments.get(0));
        if (id <= 0) {
          Result.notFound(response, "图片不存在");
          return;
        }
        SessionUser user = currentUser(request);
        ImageDao imageDao = new ImageDao(dataSource());
        Map<String, Object> image = imageDao.findById(id);
        if (image == null) {
          Result.notFound(response, "图片不存在");
          return;
        }
        if (((Number) image.get("user_id")).longValue() != user.getId()) {
          Result.forbidden(response, "无权修改");
          return;
        }
        Map<String, Object> body = body(request);
        String description = RequestUtil.string(body, "description");
        imageDao.updateDescription(id, description);
        ok(response, "更新成功", null);
        return;
      }
      Result.notFound(response, "接口不存在");
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] Image PATCH failed", ex);
      Result.serverError(response, "服务器错误");
    }
  }

  private void upload(HttpServletRequest request, HttpServletResponse response) throws Exception {
    SessionUser user = currentUser(request);
    Part part = request.getPart("image");
    if (part == null || part.getSize() == 0) {
      Result.badRequest(response, "请选择要上传的图片");
      return;
    }

    AppConfig config = (AppConfig) getServletContext().getAttribute(AppContextKeys.APP_CONFIG);
    long maxSize = config.getLong("upload.max.size", 20 * 1024 * 1024);
    if (part.getSize() > maxSize) {
      Result.badRequest(response, "图片大小不能超过 " + (maxSize / 1024 / 1024) + "MB");
      return;
    }

    String originalName = getFileName(part);
    String ext = extension(originalName);
    if (!ALLOWED_EXT.contains(ext)) {
      Result.badRequest(response, "仅支持 jpg、png、gif、webp 图片");
      return;
    }

    String filename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
    File uploadDir = (File) getServletContext().getAttribute(AppContextKeys.UPLOAD_DIR);
    File target = new File(uploadDir, filename);
    try (var inputStream = part.getInputStream()) {
      Files.copy(inputStream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    Integer width = null;
    Integer height = null;
    try {
      BufferedImage image = ImageIO.read(target);
      if (image != null) {
        width = image.getWidth();
        height = image.getHeight();
      }
    } catch (Exception ignored) {
    }

    String url = publicBaseUrl(request) + "/uploads/" + filename;
    String description = request.getParameter("description");
    long imageId = new ImageDao(dataSource()).create(user.getId(), filename, originalName, url, width, height, target.length(), ext, description);

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("id", imageId);
    data.put("url", url);
    data.put("originalName", originalName);
    data.put("size", target.length());
    data.put("width", width);
    data.put("height", height);
    Result.write(response, HttpServletResponse.SC_CREATED, 0, "上传成功", data);
  }

  private void updateVisibility(HttpServletRequest request, HttpServletResponse response, long id) throws Exception {
    if (id <= 0) {
      Result.notFound(response, "图片不存在");
      return;
    }
    SessionUser user = currentUser(request);
    ImageDao imageDao = new ImageDao(dataSource());
    Map<String, Object> image = imageDao.findById(id);
    if (image == null) {
      Result.notFound(response, "图片不存在");
      return;
    }
    if (((Number) image.get("user_id")).longValue() != user.getId()) {
      Result.forbidden(response, "无权修改");
      return;
    }
    Map<String, Object> body = body(request);
    imageDao.updateVisibility(id, RequestUtil.boolValue(body.get("isPublic"), false));
    ok(response, "更新成功", null);
  }

  private long parseId(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ignored) {
      return -1;
    }
  }

  private String getFileName(Part part) {
    String header = part.getHeader("content-disposition");
    if (header == null) {
      return "image";
    }
    for (String item : header.split(";")) {
      String trimmed = item.trim();
      if (trimmed.startsWith("filename=")) {
        return trimmed.substring(trimmed.indexOf('=') + 1).trim().replace("\"", "");
      }
    }
    return "image";
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
