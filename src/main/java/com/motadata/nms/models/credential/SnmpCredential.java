package com.motadata.nms.models.credential;

import com.motadata.nms.models.DeviceType.Protocol;
import io.vertx.core.json.JsonObject;

public class SnmpCredential extends Credential {
  public static final String VERSION_2C = "v2c";

  private String version;
  private String community;

  public SnmpCredential() {
    super(Protocol.SNMP);
  }

  public SnmpCredential(String version, String community) {
    super(Protocol.SNMP);
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
  public JsonObject toJson() {
    JsonObject json = new JsonObject()
      .put("type", getType().getValue())
      .put("version", version)
      .put("community", community);

    return json;
  }

  public static SnmpCredential fromJson(JsonObject json) {
    if (json == null) {
      throw new IllegalArgumentException("SNMP credential JSON cannot be null");
    }

    String version = json.getString("version");
    if (version == null || version.isEmpty()) {
      throw new IllegalArgumentException("SNMP version is required");
    }

    SnmpCredential credential = new SnmpCredential();
    credential.setVersion(version);

    return credential;
  }

  @Override
  public String toString() {
    return "SnmpCredential{" +
      "version='" + version + '\'' +
      ", community='" + community + '\'' +
      '}';
  }
}
