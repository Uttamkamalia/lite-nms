package com.motadata.nms.models;

import io.vertx.core.json.JsonObject;

public record DeviceType (Integer id, String type, JsonObject metadata){

  public DeviceType ( String type, JsonObject metadata) {
    this(null , type, metadata);
  }

  public JsonObject toJson() {
    return new JsonObject()
      .put("id", id)
      .put("type", type)
      .put("metadata", metadata);
  }

  public static DeviceType fromJson(JsonObject json) {
    return new DeviceType(
      json.getString("type"),
      json.getJsonObject("metadata")
    );
  }
}

