package com.motadata.nms.discovery;

import io.vertx.core.json.JsonObject;

public record DiscoveryJob(String ip, Integer port, Integer credentialsProfileId, Integer discoveryProfileId) {

  public JsonObject toJson() {
    return new JsonObject()
      .put("ip", ip)
      .put("port", port)
      .put("credentialsProfileId", credentialsProfileId)
      .put("discoveryProfileId", discoveryProfileId);
  }

  public static DiscoveryJob fromJson(JsonObject json) {
    return new DiscoveryJob(
      json.getString("ip"),
      json.getInteger("port"),
      json.getInteger("credentialsProfileId"),
      json.getInteger("discoveryProfileId")
    );
  }
}
