package com.motadata.nms.models.credential;

import com.motadata.nms.models.DeviceType.Protocol;
import io.vertx.core.json.JsonObject;

public class SnmpCredential extends Credential {
  private String version;
  private String community;
  private String securityName;
  private String authProtocol;
  private String authPassword;
  private String privProtocol;
  private String privPassword;

  public SnmpCredential() {
    super(Protocol.SNMP);
  }

  public SnmpCredential(String version, String community) {
    super(Protocol.SNMP);
    this.version = version;
    this.community = community;
  }

  public SnmpCredential(String version, String securityName, String authProtocol,
                        String authPassword, String privProtocol, String privPassword) {
    super(Protocol.SNMP);
    this.version = version;
    this.securityName = securityName;
    this.authProtocol = authProtocol;
    this.authPassword = authPassword;
    this.privProtocol = privProtocol;
    this.privPassword = privPassword;
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

  public String getSecurityName() {
    return securityName;
  }

  public void setSecurityName(String securityName) {
    this.securityName = securityName;
  }

  public String getAuthProtocol() {
    return authProtocol;
  }

  public void setAuthProtocol(String authProtocol) {
    this.authProtocol = authProtocol;
  }

  public String getAuthPassword() {
    return authPassword;
  }

  public void setAuthPassword(String authPassword) {
    this.authPassword = authPassword;
  }

  public String getPrivProtocol() {
    return privProtocol;
  }

  public void setPrivProtocol(String privProtocol) {
    this.privProtocol = privProtocol;
  }

  public String getPrivPassword() {
    return privPassword;
  }

  public void setPrivPassword(String privPassword) {
    this.privPassword = privPassword;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject()
      .put("type", getType().getValue())
      .put("version", version);

    if ("v1".equals(version) || "v2c".equals(version)) {
      json.put("community", community);
    } else if ("v3".equals(version)) {
      json.put("securityName", securityName)
        .put("authProtocol", authProtocol)
        .put("authPassword", authPassword)
        .put("privProtocol", privProtocol)
        .put("privPassword", privPassword);
    }

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

    if (VERSION_1.equals(version) || VERSION_2C.equals(version)) {
      String community = json.getString("community");
      if (community == null || community.isEmpty()) {
        throw new IllegalArgumentException("SNMP community string is required for v1/v2c");
      }
      credential.setCommunity(community);
    } else if (VERSION_3.equals(version)) {
      String securityName = json.getString("securityName");
      if (securityName == null || securityName.isEmpty()) {
        throw new IllegalArgumentException("SNMP security name is required for v3");
      }
      credential.setSecurityName(securityName);

      String authProtocol = json.getString("authProtocol");
      String authPassword = json.getString("authPassword");

      // If auth protocol is specified, password must be provided
      if (authProtocol != null && !authProtocol.isEmpty()) {
        if (authPassword == null || authPassword.isEmpty()) {
          throw new IllegalArgumentException("SNMP auth password is required when auth protocol is specified");
        }
        credential.setAuthProtocol(authProtocol);
        credential.setAuthPassword(authPassword);

        // If privacy is specified, both protocol and password are required
        String privProtocol = json.getString("privProtocol");
        String privPassword = json.getString("privPassword");

        if (privProtocol != null && !privProtocol.isEmpty()) {
          if (privPassword == null || privPassword.isEmpty()) {
            throw new IllegalArgumentException("SNMP privacy password is required when privacy protocol is specified");
          }
          credential.setPrivProtocol(privProtocol);
          credential.setPrivPassword(privPassword);
        }
      }
    } else {
      throw new IllegalArgumentException("Unsupported SNMP version: " + version);
    }

    return credential;
  }

  @Override
  public String toString() {
    return "SnmpCredential{" +
      "version='" + version + '\'' +
      ", community='" + community + '\'' +
      ", securityName='" + securityName + '\'' +
      ", authProtocol='" + authProtocol + '\'' +
      ", authPassword='***'" +
      ", privProtocol='" + privProtocol + '\'' +
      ", privPassword='***'" +
      '}';
  }
}
