package com.motadata.nms.models;

import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import io.vertx.core.json.JsonObject;

public record CredentialProfile (Integer id, String name, Integer deviceTypeId, JsonObject credentials){

  public CredentialProfile(String name, Integer deviceTypeId, JsonObject credentials){
    this(null, name, deviceTypeId, credentials);
  }

  public JsonObject toJson() {
    return new JsonObject()
      .put("id", id)
      .put("name", name)
      .put("device_type", deviceTypeId)
      .put("credentials", credentials);
  }

  public static CredentialProfile fromJson(JsonObject json) {
    return new CredentialProfile(
      json.getString("name"),
      json.getInteger("device_type"),
      json.getJsonObject("credentials")
    );
  }
}

