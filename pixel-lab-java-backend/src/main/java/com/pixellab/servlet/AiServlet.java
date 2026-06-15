package com.pixellab.servlet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.pixellab.config.AppConfig;
import com.pixellab.context.AppContextKeys;
import com.pixellab.util.JsonUtil;
import com.pixellab.util.Result;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@MultipartConfig(maxFileSize = 20 * 1024 * 1024, maxRequestSize = 80 * 1024 * 1024)
public class AiServlet extends BaseApiServlet {
  private static final int[][] PICO8 = {
      {0x00, 0x00, 0x00}, {0x1d, 0x2b, 0x53}, {0x7e, 0x25, 0x53}, {0x00, 0x87, 0x51},
      {0xab, 0x52, 0x36}, {0x5f, 0x57, 0x4f}, {0xc2, 0xc3, 0xc7}, {0xff, 0xf1, 0xe8},
      {0xff, 0x00, 0x4d}, {0xff, 0xa3, 0x00}, {0xff, 0xec, 0x27}, {0x00, 0xe4, 0x36},
      {0x29, 0xad, 0xff}, {0x83, 0x76, 0x9c}, {0xff, 0x77, 0xa8}, {0xff, 0xcc, 0xaa}
  };

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    List<String> segments = com.pixellab.util.RequestUtil.pathSegments(request);
    if (segments.size() != 1 || !"refine".equals(segments.get(0))) {
      Result.notFound(response, "接口不存在");
      return;
    }

    try {
      refine(request, response);
    } catch (Exception ex) {
      getServletContext().log("[Pixel Lab] AI API failed", ex);
      Result.serverError(response, ex.getMessage() == null ? "AI 处理失败" : ex.getMessage());
    }
  }

  private void refine(HttpServletRequest request, HttpServletResponse response) throws Exception {
    Part currentImage = request.getPart("currentImage");
    String prompt = safeParam(request, "prompt");
    String mode = safeParam(request, "mode");
    String model = safeParam(request, "model");
    if (currentImage == null || currentImage.getSize() == 0) {
      Result.badRequest(response, "请上传当前画布图片");
      return;
    }
    if (prompt == null || prompt.isBlank()) {
      Result.badRequest(response, "请输入 AI 创作要求");
      return;
    }
    if (mode == null || mode.isBlank()) {
      mode = "refine";
    }
    if (model == null || model.isBlank()) {
      model = "deepseek-v4-flash";
    }

    byte[] imageBytes = readAll(currentImage);
    List<String> attachmentNotes = collectAttachmentNotes(request);
    BufferedImage canvasImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
    String canvasNote = canvasImage == null
        ? "当前画布: " + currentImage.getSubmittedFileName()
        : "当前画布: " + currentImage.getSubmittedFileName() + " (" + canvasImage.getWidth() + "x" + canvasImage.getHeight() + ")";

    AppConfig config = (AppConfig) getServletContext().getAttribute(AppContextKeys.APP_CONFIG);
    String aiPrompt = "draw".equals(mode)
        ? buildDrawPrompt(prompt, canvasNote, attachmentNotes)
        : buildRefinePrompt(prompt, canvasNote, attachmentNotes);

    String notice = "";
    String text;
    if (config.get("deepseek.api.key", "").isBlank()) {
      text = fallbackText(mode, prompt);
      notice = "未配置 DEEPSEEK_API_KEY，当前返回本地模板结果；配置后会调用 DeepSeek。";
    } else {
      text = callDeepSeek(config, resolveDeepSeekModel(config, mode, model), aiPrompt);
    }

    String imageUrl = "";
    if ("draw".equals(mode) && canvasImage != null && wantsPixelArt(prompt)) {
      imageUrl = createPixelArtImage(request, canvasImage, prompt);
      if (!notice.isBlank()) {
        notice += " ";
      }
      notice += "已用 Java 本地图像处理生成像素化结果。";
    }

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("mode", mode);
    data.put("model", model);
    data.put("text", text);
    data.put("imageUrl", imageUrl);
    data.put("notice", notice);
    data.put("provider", imageUrl.isBlank() ? "deepseek" : "deepseek+java-image-transform");
    ok(response, "AI 润色完成", data);
  }

  private String callDeepSeek(AppConfig config, String model, String prompt) throws Exception {
    String baseUrl = config.get("deepseek.base.url", "https://api.deepseek.com").replaceAll("/+$", "");
    String apiKey = config.get("deepseek.api.key", "");

    Map<String, Object> message = new LinkedHashMap<>();
    message.put("role", "user");
    message.put("content", prompt);

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("model", model);
    payload.put("messages", List.of(message));
    payload.put("temperature", 0.7);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/chat/completions"))
        .timeout(Duration.ofSeconds(90))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + apiKey)
        .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.stringify(payload), StandardCharsets.UTF_8))
        .build();

    HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IllegalStateException("DeepSeek 调用失败: HTTP " + response.statusCode());
    }
    Map<String, Object> json = JsonUtil.mapper().readValue(response.body(), new TypeReference<Map<String, Object>>() {});
    List<?> choices = (List<?>) json.get("choices");
    if (choices == null || choices.isEmpty()) {
      return "";
    }
    Map<?, ?> choice = (Map<?, ?>) choices.get(0);
    Map<?, ?> msg = (Map<?, ?>) choice.get("message");
    Object content = msg == null ? null : msg.get("content");
    return content == null ? "" : String.valueOf(content);
  }

  private String resolveDeepSeekModel(AppConfig config, String mode, String selectedModel) {
    if ("draw".equals(mode)) {
      return config.get("deepseek.image.model", config.get("deepseek.chat.model", "deepseek-chat"));
    }
    return config.get("deepseek.chat.model", "deepseek-chat");
  }

  private String buildRefinePrompt(String prompt, String canvasNote, List<String> attachmentNotes) {
    return "用户正在手绘板里创作图片。请基于当前画布和可选参考文件，给出可执行的润色建议。"
        + "\n要求：保持原构图、主体比例和站位，提升完成度、线条、色彩、光影和材质。"
        + "\n" + canvasNote
        + notesText(attachmentNotes)
        + "\n用户润色要求: " + prompt
        + "\n请用中文返回：1. 整体润色方向 2. 线条/颜色/光影建议 3. 可直接复制给绘图模型的提示词。";
  }

  private String buildDrawPrompt(String prompt, String canvasNote, List<String> attachmentNotes) {
    return "用户希望基于手绘板当前画布进行二次创作。请输出能直接交给绘图模型使用的方案和提示词。"
        + "\n要求：尽量保持用户指定的构图、比例、主体位置；如果用户要求风格迁移或像素化，请明确技术步骤。"
        + "\n" + canvasNote
        + notesText(attachmentNotes)
        + "\n用户创作要求: " + prompt
        + "\n请用中文返回：1. 二次创作方向 2. 风格与光影 3. 正向提示词 4. 负面提示词。";
  }

  private String fallbackText(String mode, String prompt) {
    if ("draw".equals(mode)) {
      return "二次创作建议：保持当前画布的构图和比例，根据用户要求进行风格化处理。\n\n"
          + "正向提示词：" + prompt + ", keep original composition and proportions, clean edges, high quality rendering.\n\n"
          + "负面提示词：low quality, blurry, distorted composition, cropped subject, unwanted black background.";
    }
    return "润色建议：保持原始构图和主体比例，强化线条清晰度、主体明暗对比、边缘光和材质细节。\n\n"
        + "可复制提示词：" + prompt + ", preserve original composition, enhance line quality, color harmony, lighting, material details.";
  }

  private String createPixelArtImage(HttpServletRequest request, BufferedImage source, String prompt) throws IOException {
    int[] logical = logicalSize(prompt, source.getWidth(), source.getHeight());
    int[] output = outputSize(prompt, source.getWidth(), source.getHeight());
    BufferedImage low = new BufferedImage(logical[0], logical[1], BufferedImage.TYPE_INT_ARGB);

    for (int y = 0; y < logical[1]; y++) {
      for (int x = 0; x < logical[0]; x++) {
        int sx = Math.min(source.getWidth() - 1, x * source.getWidth() / logical[0]);
        int sy = Math.min(source.getHeight() - 1, y * source.getHeight() / logical[1]);
        int argb = source.getRGB(sx, sy);
        int alpha = (argb >>> 24) & 0xff;
        if (alpha < 16) {
          low.setRGB(x, y, 0x00000000);
        } else {
          low.setRGB(x, y, nearestPico8(argb, alpha));
        }
      }
    }
    if (prompt.toLowerCase(Locale.ROOT).contains("outline") || prompt.contains("轮廓")) {
      low = addHardOutline(low);
    }

    BufferedImage outputImage = new BufferedImage(output[0], output[1], BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < output[1]; y++) {
      for (int x = 0; x < output[0]; x++) {
        int sx = Math.min(logical[0] - 1, x * logical[0] / output[0]);
        int sy = Math.min(logical[1] - 1, y * logical[1] / output[1]);
        outputImage.setRGB(x, y, low.getRGB(sx, sy));
      }
    }

    File uploadDir = (File) getServletContext().getAttribute(AppContextKeys.UPLOAD_DIR);
    String filename = "ai_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "") + ".png";
    File target = new File(uploadDir, filename);
    ImageIO.write(outputImage, "png", target);
    return publicBaseUrl(request) + "/uploads/" + filename;
  }

  private BufferedImage addHardOutline(BufferedImage source) {
    BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < source.getHeight(); y++) {
      for (int x = 0; x < source.getWidth(); x++) {
        int argb = source.getRGB(x, y);
        int alpha = (argb >>> 24) & 0xff;
        if (alpha > 16 && touchesTransparent(source, x, y)) {
          result.setRGB(x, y, new Color(0, 0, 0, alpha).getRGB());
        } else {
          result.setRGB(x, y, argb);
        }
      }
    }
    return result;
  }

  private boolean touchesTransparent(BufferedImage image, int x, int y) {
    int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    for (int[] offset : offsets) {
      int nx = x + offset[0];
      int ny = y + offset[1];
      if (nx < 0 || ny < 0 || nx >= image.getWidth() || ny >= image.getHeight()) {
        return true;
      }
      if (((image.getRGB(nx, ny) >>> 24) & 0xff) < 16) {
        return true;
      }
    }
    return false;
  }

  private int nearestPico8(int argb, int alpha) {
    int r = (argb >> 16) & 0xff;
    int g = (argb >> 8) & 0xff;
    int b = argb & 0xff;
    int[] best = PICO8[0];
    int bestDistance = Integer.MAX_VALUE;
    for (int[] color : PICO8) {
      int dr = r - color[0];
      int dg = g - color[1];
      int db = b - color[2];
      int distance = dr * dr + dg * dg + db * db;
      if (distance < bestDistance) {
        bestDistance = distance;
        best = color;
      }
    }
    return ((alpha & 0xff) << 24) | (best[0] << 16) | (best[1] << 8) | best[2];
  }

  private int[] logicalSize(String prompt, int fallbackWidth, int fallbackHeight) {
    Matcher matcher = Pattern.compile("(\\d{2,4})\\s*[x×]\\s*(\\d{2,4})").matcher(prompt);
    if (matcher.find()) {
      int w = Integer.parseInt(matcher.group(1));
      int h = Integer.parseInt(matcher.group(2));
      if (w <= 256 && h <= 256) {
        return new int[]{Math.max(8, w), Math.max(8, h)};
      }
    }
    return new int[]{Math.max(8, fallbackWidth / 10), Math.max(8, fallbackHeight / 10)};
  }

  private int[] outputSize(String prompt, int fallbackWidth, int fallbackHeight) {
    Matcher matcher = Pattern.compile("(?:back to|upscale.*?to|输出|放大.*?到)\\s*(\\d{2,4})\\s*[x×]\\s*(\\d{2,4})", Pattern.CASE_INSENSITIVE).matcher(prompt);
    if (matcher.find()) {
      return new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
    }
    return new int[]{fallbackWidth, fallbackHeight};
  }

  private boolean wantsPixelArt(String prompt) {
    String lower = prompt.toLowerCase(Locale.ROOT);
    return lower.contains("pixel") || lower.contains("pico") || prompt.contains("像素");
  }

  private List<String> collectAttachmentNotes(HttpServletRequest request) throws Exception {
    List<String> notes = new ArrayList<>();
    for (Part part : request.getParts()) {
      if (!"attachments".equals(part.getName()) || part.getSize() == 0) {
        continue;
      }
      String type = part.getContentType() == null ? "" : part.getContentType();
      if (type.startsWith("text/") || type.equals("application/json")) {
        String text = new String(readAll(part), StandardCharsets.UTF_8);
        notes.add("参考文本 " + part.getSubmittedFileName() + ":\n" + text.substring(0, Math.min(4000, text.length())));
      } else {
        notes.add("参考文件: " + part.getSubmittedFileName() + " (" + type + ", " + part.getSize() + " bytes)");
      }
    }
    return notes;
  }

  private String notesText(List<String> notes) {
    return notes.isEmpty() ? "" : "\n参考材料:\n" + String.join("\n\n", notes);
  }

  private String safeParam(HttpServletRequest request, String name) throws IOException, ServletException {
    Part part = request.getPart(name);
    if (part != null && part.getSize() > 0) {
      return new String(readAll(part), StandardCharsets.UTF_8).trim();
    }
    String value = request.getParameter(name);
    return value == null ? null : value.trim();
  }

  private byte[] readAll(Part part) throws IOException {
    try (var in = part.getInputStream(); var out = new ByteArrayOutputStream()) {
      in.transferTo(out);
      return out.toByteArray();
    }
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
