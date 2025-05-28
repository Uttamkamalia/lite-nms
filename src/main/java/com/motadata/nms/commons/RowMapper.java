package com.motadata.nms.commons;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

import java.time.LocalDateTime;

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
      Object value = row.getValue(column);
      if(value instanceof LocalDateTime dateTime){
        json.put(column, dateTime.toString());
        continue;
      }
      json.put(column, value);
    }

    return json;
  }

  public static JsonObject mapRowToJson(Row row, Message<?> message){
    JsonObject json = new JsonObject();
    for(int i=0;i<row.size();i++){
      String column = row.getColumnName(i);
      Object value = row.getValue(column);
      if(value instanceof LocalDateTime dateTime){
        json.put(column, dateTime.toString());
        continue;
      }
      json.put(column, value);
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
