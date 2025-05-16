package com.motadata.nms.models.credential;

public class SnmpCredential extends Credential {
  private String version;
  private String community;

  public SnmpCredential(String version, String community) {
    this.version = version;
    this.community = community;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getCommunity() {
    return community;
  }

  public void setCommunity(String community) {
    this.community = community;
  }

  @Override
  public String toString() {
    return "SnmpCredential{" +
      "version='" + version + '\'' +
      ", community='" + community + '\'' +
      '}';
  }
}
