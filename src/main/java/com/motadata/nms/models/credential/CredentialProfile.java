package com.motadata.nms.models.credential;

import io.vertx.core.json.JsonObject;

public class CredentialProfile {
  private Integer id;
  private String name;
  private Integer deviceTypeId;
  private Credential credential;

  public CredentialProfile() {
    // Default constructor
  }

  public CredentialProfile(String name, Integer deviceTypeId, Credential credential) {
    this(null, name, deviceTypeId, credential);
  }

  public CredentialProfile(Integer id, String name, Integer deviceTypeId, Credential credential) {
    this.id = id;
    this.name = name;
    this.deviceTypeId = deviceTypeId;
    this.credential = credential;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getDeviceTypeId() {
    return deviceTypeId;
  }

  public void setDeviceTypeId(Integer deviceTypeId) {
    this.deviceTypeId = deviceTypeId;
  }

  public Credential getCredential() {
    return credential;
  }

  public void setCredential(Credential credential) {
    this.credential = credential;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
      .put("id", id)
      .put("name", name)
      .put("device_type", deviceTypeId);

    if (credential != null) {
      json.put("credentials", credential.toJson());
    }

    return json;
  }

  public static CredentialProfile fromJson(JsonObject json) {
    if (json == null) return null;

    Integer id = json.getInteger("id");
    String name = json.getString("name");
    Integer deviceTypeId = json.getInteger("device_type");
    JsonObject credentialsJson = json.getJsonObject("credentials");
    Credential credential = Credential.fromJson(credentialsJson);
    return new CredentialProfile(id, name, deviceTypeId, credential);
  }

  @Override
  public String toString() {
    return "CredentialProfile{" +
      "id=" + id +
      ", name='" + name + '\'' +
      ", deviceTypeId=" + deviceTypeId +
      ", credential=" + credential +
      '}';
  }

  // Helper methods to check credential type
  public boolean isSnmpCredential() {
    return credential instanceof SnmpCredential;
  }

  public boolean isSshCredential() {
    return credential instanceof SshCredential;
  }

  // Helper methods to get specific credential types
  public SnmpCredential getSnmpCredential() {
    return isSnmpCredential() ? (SnmpCredential) credential : null;
  }

  public SshCredential getSshCredential() {
    return isSshCredential() ? (SshCredential) credential : null;
  }
}

