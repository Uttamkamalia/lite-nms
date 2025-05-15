package com.motadata.nms.models;

import io.vertx.core.json.JsonObject;

public record DiscoveryProfile (Integer id, JsonObject targetIps, Integer port, Integer credentialsProfileId){

  public DiscoveryProfile(JsonObject targetIps, Integer port, Integer credentialsProfileId){
    this(null, targetIps, port, credentialsProfileId);
  }

  public JsonObject toJson() {
    return new JsonObject()
      .put("id", id)
      .put("targetIps", targetIps)
      .put("port", port)
      .put("credentialsProfileId", credentialsProfileId);
  }

  public static DiscoveryProfile fromJson(JsonObject json) {
    return new DiscoveryProfile(
      json.getJsonObject("targetIps"),
      json.getInteger("port"),
      json.getInteger("credentialsProfileId")
    );
  }
}

