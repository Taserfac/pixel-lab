package com.pixellab.servlet;

import com.pixellab.context.AppContextKeys;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class UploadFileServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String path = request.getPathInfo();
    if (path == null || path.contains("..")) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    File uploadDir = (File) getServletContext().getAttribute(AppContextKeys.UPLOAD_DIR);
    File file = new File(uploadDir, path.substring(1));
    if (!file.exists() || !file.isFile()) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    String contentType = getServletContext().getMimeType(file.getName());
    response.setContentType(contentType == null ? "application/octet-stream" : contentType);
    response.setContentLengthLong(file.length());
    Files.copy(file.toPath(), response.getOutputStream());
  }
}
