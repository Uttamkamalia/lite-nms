package com.motadata.nms.models.credential;


public class CredentialProfile {
  private Integer id;
  private String name;
  private Integer deviceTypeId;
  private Credential credential;

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

  @Override
  public String toString() {
    return "CredentialProfile{" +
      "id=" + id +
      ", name='" + name + '\'' +
      ", deviceTypeId=" + deviceTypeId +
      ", credential=" + credential +
      '}';
  }
}

