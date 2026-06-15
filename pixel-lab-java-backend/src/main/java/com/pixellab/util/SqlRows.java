package com.pixellab.util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SqlRows {
  private SqlRows() {
  }

  public static Map<String, Object> one(ResultSet rs) throws SQLException {
    if (!rs.next()) {
      return null;
    }
    return row(rs);
  }

  public static List<Map<String, Object>> many(ResultSet rs) throws SQLException {
    List<Map<String, Object>> rows = new ArrayList<>();
    while (rs.next()) {
      rows.add(row(rs));
    }
    return rows;
  }

  public static Map<String, Object> row(ResultSet rs) throws SQLException {
    ResultSetMetaData meta = rs.getMetaData();
    Map<String, Object> row = new LinkedHashMap<>();
    for (int i = 1; i <= meta.getColumnCount(); i++) {
      String key = meta.getColumnLabel(i);
      Object value = rs.getObject(i);
      if (value instanceof Timestamp) {
        value = ((Timestamp) value).toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      }
      row.put(key, value);
    }
    return row;
  }

}
