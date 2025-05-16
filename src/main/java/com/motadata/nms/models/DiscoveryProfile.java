package com.motadata.nms.models;

public class DiscoveryProfile {
  private Integer id;
  private String target;
  private Integer port;
  private Integer credentialsProfileId;

  // Default constructor
  public DiscoveryProfile() {
  }

  // Constructor without ID
  public DiscoveryProfile(String target, Integer port, Integer credentialsProfileId) {
    this(null, target, port, credentialsProfileId);
  }

  // Full constructor
  public DiscoveryProfile(Integer id, String target, Integer port, Integer credentialsProfileId) {
    this.id = id;
    this.target = target;
    this.port = port;
    this.credentialsProfileId = credentialsProfileId;
  }

  // Getters and setters
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public Integer getCredentialsProfileId() {
    return credentialsProfileId;
  }

  public void setCredentialsProfileId(Integer credentialsProfileId) {
    this.credentialsProfileId = credentialsProfileId;
  }

  @Override
  public String toString() {
    return "DiscoveryProfile{" +
      "id=" + id +
      ", target='" + target + '\'' +
      ", port=" + port +
      ", credentialsProfileId=" + credentialsProfileId +
      '}';
  }
}

