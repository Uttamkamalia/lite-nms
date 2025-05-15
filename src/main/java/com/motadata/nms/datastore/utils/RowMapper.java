package com.motadata.nms.datastore.utils;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

public class RowMapper {
  public static <T> T mapRow(Row row, Class<T> clazz) {
    JsonObject json = new JsonObject();
    for (int i = 0; i < row.size(); i++) {
      String column = row.getColumnName(i);
      json.put(column, row.getValue(column));
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
}
