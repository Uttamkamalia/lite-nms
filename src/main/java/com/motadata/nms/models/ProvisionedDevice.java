package com.motadata.nms.models;

public class ProvisionedDevice {
  private Integer id;
  private String ip;
  private Integer port;
  private Integer discoveryProfileId;
  private Integer credentialsProfileId;
  private String hostname;
  private String os;
  private String deviceType;
  private String status;
  private String discoveredAt;

  // Default constructor
  public ProvisionedDevice() {
  }

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

  // Full constructor
  public ProvisionedDevice(
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
    this.id = id;
    this.ip = ip;
    this.port = port;
    this.discoveryProfileId = discoveryProfileId;
    this.credentialsProfileId = credentialsProfileId;
    this.hostname = hostname;
    this.os = os;
    this.deviceType = deviceType;
    this.status = status;
    this.discoveredAt = discoveredAt;
  }

  // Getters and setters
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
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

  public Integer getCredentialsProfileId() {
    return credentialsProfileId;
  }

  public void setCredentialsProfileId(Integer credentialsProfileId) {
    this.credentialsProfileId = credentialsProfileId;
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

  public String getDeviceType() {
    return deviceType;
  }

  public void setDeviceType(String deviceType) {
    this.deviceType = deviceType;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getDiscoveredAt() {
    return discoveredAt;
  }

  public void setDiscoveredAt(String discoveredAt) {
    this.discoveredAt = discoveredAt;
  }

  @Override
  public String toString() {
    return "ProvisionedDevice{" +
      "id=" + id +
      ", ip='" + ip + '\'' +
      ", port=" + port +
      ", discoveryProfileId=" + discoveryProfileId +
      ", credentialsProfileId=" + credentialsProfileId +
      ", hostname='" + hostname + '\'' +
      ", os='" + os + '\'' +
      ", deviceType='" + deviceType + '\'' +
      ", status='" + status + '\'' +
      ", discoveredAt='" + discoveredAt + '\'' +
      '}';
  }
}


