package com.motadata.nms.models;

import io.vertx.core.json.JsonObject;

public record ProvisionedDevice(
  Integer id,
  String ip,
  Integer port,
  Integer discoveryProfileId,
  Integer credentialsProfileId,
  String hostname,
  String os,
  String deviceType,
  String status,
  String discoveredAt
) {

  // Constructor without id (e.g. for inserts)
  public ProvisionedDevice(
    String ip,
    Integer port,
    Integer discoveryProfileId,
    Integer credentialsProfileId,
    String hostname,
    String os,
    String deviceType,
    String status,
    String discoveredAt
  ) {
    this(null, ip, port, discoveryProfileId, credentialsProfileId, hostname, os, deviceType, status, discoveredAt);
  }

  public JsonObject toJson() {
    return new JsonObject()
      .put("id", id)
      .put("ip", ip)
      .put("port", port)
      .put("discoveryProfileId", discoveryProfileId)
      .put("credentialsProfileId", credentialsProfileId)
      .put("hostname", hostname)
      .put("os", os)
      .put("deviceType", deviceType)
      .put("status", status)
      .put("discoveredAt", discoveredAt);
  }

  public static ProvisionedDevice fromJson(JsonObject json) {
    return new ProvisionedDevice(
      json.getInteger("id"),
      json.getString("ip"),
      json.getInteger("port"),
      json.getInteger("discoveryProfileId"),
      json.getInteger("credentialsProfileId"),
      json.getString("hostname"),
      json.getString("os"),
      json.getString("deviceType"),
      json.getString("status"),
      json.getString("discoveredAt")
    );
  }
}


