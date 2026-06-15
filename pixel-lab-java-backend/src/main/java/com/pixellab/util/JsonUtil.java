package com.pixellab.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class JsonUtil {
  private static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private JsonUtil() {
  }

  public static ObjectMapper mapper() {
    return MAPPER;
  }

  public static Map<String, Object> readMap(HttpServletRequest request) throws IOException {
    if (request.getInputStream() == null || request.getContentLengthLong() == 0) {
      return new LinkedHashMap<>();
    }
    return MAPPER.readValue(request.getInputStream(), new TypeReference<Map<String, Object>>() {});
  }

  public static String stringify(Object value) throws IOException {
    return MAPPER.writeValueAsString(value);
  }
}
