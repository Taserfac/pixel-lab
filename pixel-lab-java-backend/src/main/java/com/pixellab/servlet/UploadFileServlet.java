package com.pixellab.servlet;

import com.pixellab.context.AppContextKeys;
import com.pixellab.util.ImageVariantUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

public class UploadFileServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String path = request.getPathInfo();
    if (path == null || path.contains("..") || path.length() <= 1) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    File uploadDir = (File) getServletContext().getAttribute(AppContextKeys.UPLOAD_DIR);
    File root = uploadDir.getCanonicalFile();
    String relativePath = path.substring(1).replace('\\', '/');
    File source = new File(root, relativePath).getCanonicalFile();
    if (!source.toPath().startsWith(root.toPath()) || !source.isFile()) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    File file = source;
    String variant = request.getParameter("variant");
    if (ImageVariantUtil.supports(variant)) {
      try {
        file = ImageVariantUtil.getOrCreateVariant(root, source, relativePath, variant);
      } catch (Exception ex) {
        getServletContext().log("[Pixel Lab] Image variant generation failed for " + relativePath, ex);
        file = source;
      }
    }

    String etag = "\"" + Long.toHexString(file.length()) + "-" + Long.toHexString(file.lastModified()) + "\"";
    response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
    response.setHeader("ETag", etag);
    response.setDateHeader("Last-Modified", file.lastModified());
    if (Objects.equals(etag, request.getHeader("If-None-Match"))) {
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }
    long ifModifiedSince = request.getDateHeader("If-Modified-Since");
    if (ifModifiedSince >= 0 && file.lastModified() / 1000 <= ifModifiedSince / 1000) {
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }

    String contentType = getServletContext().getMimeType(file.getName());
    response.setContentType(contentType == null ? "application/octet-stream" : contentType);
    response.setContentLengthLong(file.length());
    Files.copy(file.toPath(), response.getOutputStream());
  }
}
