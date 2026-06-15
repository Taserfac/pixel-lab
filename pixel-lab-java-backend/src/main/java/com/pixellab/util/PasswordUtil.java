package com.pixellab.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {
  private PasswordUtil() {
  }

  public static String hash(String password) {
    return BCrypt.hashpw(password, BCrypt.gensalt(10));
  }

  public static boolean verify(String password, String hashed) {
    if (password == null || hashed == null || hashed.isBlank()) {
      return false;
    }
    try {
      return BCrypt.checkpw(password, hashed);
    } catch (IllegalArgumentException ignored) {
      return false;
    }
  }
}
