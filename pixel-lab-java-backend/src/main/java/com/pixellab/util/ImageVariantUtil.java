package com.pixellab.util;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

public final class ImageVariantUtil {
  public static final String CARD = "card";
  public static final String AVATAR = "avatar";

  private static final Map<String, VariantSpec> SPECS = Map.of(
      CARD, new VariantSpec(640, 640, false, 0.82f),
      AVATAR, new VariantSpec(192, 192, true, 0.86f)
  );
  private static final Object[] LOCKS = createLocks(64);

  private ImageVariantUtil() {
  }

  public static boolean supports(String variant) {
    return SPECS.containsKey(variant);
  }

  public static String withVariant(String url, String variant) {
    if (url == null || url.isBlank() || !supports(variant) || url.contains("variant=")) {
      return url;
    }
    return url + (url.contains("?") ? "&" : "?") + "variant=" + variant;
  }

  public static File getOrCreateVariant(File uploadDir, File source, String relativePath, String variant)
      throws IOException {
    VariantSpec spec = SPECS.get(variant);
    if (spec == null || source == null || !source.isFile()) {
      return source;
    }

    String normalizedPath = relativePath.replace('\\', '/');
    File target = new File(new File(uploadDir, ".variants/" + variant), normalizedPath + ".jpg");
    if (isFresh(target, source)) {
      return target;
    }

    Object lock = lockFor(target.getCanonicalPath());
    synchronized (lock) {
      if (isFresh(target, source)) {
        return target;
      }
      BufferedImage input = ImageIO.read(source);
      if (input == null) {
        return source;
      }

      BufferedImage output = spec.square
          ? squareVariant(input, spec.width, spec.height)
          : fittedVariant(input, spec.width, spec.height);
      File parent = target.getParentFile();
      if (!parent.exists() && !parent.mkdirs()) {
        throw new IOException("Cannot create image variant directory: " + parent.getAbsolutePath());
      }

      File temporary = new File(parent, target.getName() + ".tmp-" + UUID.randomUUID());
      try {
        writeJpeg(output, temporary, spec.quality);
        try {
          Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE,
              StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
          Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        target.setLastModified(source.lastModified());
      } finally {
        Files.deleteIfExists(temporary.toPath());
      }
      return target;
    }
  }

  private static Object[] createLocks(int count) {
    Object[] locks = new Object[count];
    for (int index = 0; index < count; index++) {
      locks[index] = new Object();
    }
    return locks;
  }

  private static Object lockFor(String key) {
    return LOCKS[(key.hashCode() & Integer.MAX_VALUE) % LOCKS.length];
  }

  private static boolean isFresh(File target, File source) {
    return target.isFile() && target.length() > 0 && target.lastModified() >= source.lastModified();
  }

  private static BufferedImage fittedVariant(BufferedImage input, int maxWidth, int maxHeight) {
    double scale = Math.min(1.0, Math.min(maxWidth * 1.0 / input.getWidth(), maxHeight * 1.0 / input.getHeight()));
    int width = Math.max(1, (int) Math.round(input.getWidth() * scale));
    int height = Math.max(1, (int) Math.round(input.getHeight() * scale));
    return draw(input, 0, 0, input.getWidth(), input.getHeight(), width, height);
  }

  private static BufferedImage squareVariant(BufferedImage input, int width, int height) {
    int side = Math.min(input.getWidth(), input.getHeight());
    int sourceX = (input.getWidth() - side) / 2;
    int sourceY = (input.getHeight() - side) / 2;
    return draw(input, sourceX, sourceY, side, side, width, height);
  }

  private static BufferedImage draw(BufferedImage input, int sourceX, int sourceY, int sourceWidth,
                                    int sourceHeight, int width, int height) {
    BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = output.createGraphics();
    try {
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, width, height);
      graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.drawImage(input, 0, 0, width, height, sourceX, sourceY,
          sourceX + sourceWidth, sourceY + sourceHeight, null);
    } finally {
      graphics.dispose();
    }
    return output;
  }

  private static void writeJpeg(BufferedImage image, File target, float quality) throws IOException {
    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
    ImageWriteParam params = writer.getDefaultWriteParam();
    if (params.canWriteCompressed()) {
      params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      params.setCompressionQuality(quality);
    }
    try (FileImageOutputStream output = new FileImageOutputStream(target)) {
      writer.setOutput(output);
      writer.write(null, new IIOImage(image, null, null), params);
    } finally {
      writer.dispose();
    }
  }

  private record VariantSpec(int width, int height, boolean square, float quality) {
  }
}
