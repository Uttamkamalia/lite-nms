package com.motadata.nms.commons;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.vertx.core.json.JsonObject;

public class JsonPojoMapper {

  private static final ObjectMapper mapper = new ObjectMapper()
    .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

  public static <T> T map(JsonObject jsonObject, Class<T> clazz) {
    try {
      return mapper.readValue(jsonObject.encode(), clazz);
    } catch (Exception e) {
      throw new RuntimeException("Failed to map JSON to POJO: " + e.getMessage(), e);
    }
  }

  public static JsonObject toJson(Object pojo) {
    try {
      String jsonString = mapper.writeValueAsString(pojo);
      return new JsonObject(jsonString);
    } catch (Exception e) {
      throw new RuntimeException("Failed to map POJO to JSON: " + e.getMessage(), e);
    }
  }
}
