package com.motadata.nms.models;

import io.vertx.core.json.JsonObject;

public class ProvisionedDevice {
  private Integer id;
  private String ipAddress;
  private Integer port;
  private Integer discoveryProfileId;
  private Integer credentialProfileId;
  private String hostname;
  private String os;
  private String protocol;
  private String status;
  private String discoveryTime;

  public ProvisionedDevice() {
  }

  public ProvisionedDevice(String ipAddress, Integer port, Integer discoveryProfileId,
                          Integer credentialProfileId, String hostname, String os,
                          String protocol, String status, String discoveryTime) {
    this.ipAddress = ipAddress;
    this.port = port;
    this.discoveryProfileId = discoveryProfileId;
    this.credentialProfileId = credentialProfileId;
    this.hostname = hostname;
    this.os = os;
    this.protocol = protocol;
    this.status = status;
    this.discoveryTime = discoveryTime;
  }

  public static ProvisionedDevice fromJson(JsonObject json) {
    ProvisionedDevice device = new ProvisionedDevice();
    device.setId(json.getInteger("id"));
    device.setIpAddress(json.getString("ip_address"));
    device.setPort(json.getInteger("port"));
    device.setDiscoveryProfileId(json.getInteger("discovery_profile_id"));
    device.setCredentialProfileId(json.getInteger("credential_profile_id"));
    device.setHostname(json.getString("hostname"));
    device.setOs(json.getString("os"));
    device.setProtocol(json.getString("protocol"));
    device.setStatus(json.getString("status"));
    device.setDiscoveryTime(json.getString("discovery_time"));
    return device;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
      .put("ip_address", ipAddress)
      .put("port", port)
      .put("discovery_profile_id", discoveryProfileId)
      .put("credential_profile_id", credentialProfileId)
      .put("hostname", hostname)
      .put("os", os)
      .put("protocol", protocol)
      .put("status", status)
      .put("discovery_time", discoveryTime);

    if (id != null) {
      json.put("id", id);
    }

    return json;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public Integer getDiscoveryProfileId() {
    return discoveryProfileId;
  }

  public void setDiscoveryProfileId(Integer discoveryProfileId) {
    this.discoveryProfileId = discoveryProfileId;
  }

  public Integer getCredentialProfileId() {
    return credentialProfileId;
  }

  public void setCredentialProfileId(Integer credentialProfileId) {
    this.credentialProfileId = credentialProfileId;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public String getOs() {
    return os;
  }

  public void setOs(String os) {
    this.os = os;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getDiscoveryTime() {
    return discoveryTime;
  }

  public void setDiscoveryTime(String discoveryTime) {
    this.discoveryTime = discoveryTime;
  }

  @Override
  public String toString() {
    return "ProvisionedDevice{" +
      "id=" + id +
      ", ipAddress='" + ipAddress + '\'' +
      ", port=" + port +
      ", discoveryProfileId=" + discoveryProfileId +
      ", credentialProfileId=" + credentialProfileId +
      ", hostname='" + hostname + '\'' +
      ", os='" + os + '\'' +
      ", protocol='" + protocol + '\'' +
      ", status='" + status + '\'' +
      ", discoveryTime='" + discoveryTime + '\'' +
      '}';
  }
}


