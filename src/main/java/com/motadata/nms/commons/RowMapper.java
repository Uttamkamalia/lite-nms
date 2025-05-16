package com.motadata.nms.commons;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

public class RowMapper {

  public static <T> T mapRowToPojo(Row row, Class<T> clazz) {
    JsonObject json = new JsonObject();

    for (int i = 0; i < row.size(); i++) {
      String camelCaseField = toCamelCase(row.getColumnName(i));
      json.put(camelCaseField, row.getValue(i));
    }

    return json.mapTo(clazz);
  }

  public static JsonObject mapRowToJson(Row row){
    JsonObject json = new JsonObject();
    for(int i=0;i<row.size();i++){
      String column = row.getColumnName(i);
      json.put(column, row.getValue(column));
    }

    return json;
  }

  private static String toCamelCase(String snake) {
    String[] parts = snake.split("_");
    StringBuilder camelCase = new StringBuilder(parts[0]);
    for (int i = 1; i < parts.length; i++) {
      camelCase.append(parts[i].substring(0, 1).toUpperCase());
      camelCase.append(parts[i].substring(1));
    }
    return camelCase.toString();
  }
}
