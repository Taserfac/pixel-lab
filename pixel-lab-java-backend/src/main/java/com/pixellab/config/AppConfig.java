package com.pixellab.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public class AppConfig {
  private final Properties properties;

  private AppConfig(Properties properties) {
    this.properties = properties;
  }

  public static AppConfig load(InputStream inputStream) throws IOException {
    Properties props = new Properties();
    if (inputStream != null) {
      props.load(inputStream);
    }
    return new AppConfig(props);
  }

  public String get(String key, String defaultValue) {
    String envKey = key.toUpperCase().replace('.', '_').replace('-', '_');
    String envValue = System.getenv(envKey);
    if (envValue != null && !envValue.isBlank()) {
      return envValue.trim();
    }
    return properties.getProperty(key, defaultValue);
  }

  public int getInt(String key, int defaultValue) {
    String value = get(key, String.valueOf(defaultValue));
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ignored) {
      return defaultValue;
    }
  }

  public long getLong(String key, long defaultValue) {
    String value = get(key, String.valueOf(defaultValue));
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ignored) {
      return defaultValue;
    }
  }

  public Set<String> getCsvSet(String key) {
    String value = get(key, "");
    Set<String> result = new LinkedHashSet<>();
    if (value == null || value.isBlank()) {
      return result;
    }
    Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(item -> !item.isEmpty())
        .forEach(result::add);
    return result;
  }
}
