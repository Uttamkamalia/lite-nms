package com.motadata.nms.models.credential;

import com.motadata.nms.commons.NMSException;
import com.motadata.nms.models.DeviceType.Protocol;
import io.vertx.core.json.JsonObject;

public abstract class Credential {
  private Protocol type;

  public Credential(Protocol type) {
    this.type = type;
  }

  public Protocol getType() {
    return type;
  }


  public void setType(Protocol type) {
    this.type = type;
  }

  public abstract JsonObject toJson();

  @Override
  public String toString() {
    return "Credential{type='" + type + "'}";
  }

  /**
   * Create a credential from JSON based on the credential type
   * @param json The JSON object containing credential data
   * @return The appropriate credential object
   * @throws NMSException if the JSON is invalid or missing required fields
   */
  public static Credential fromJson(JsonObject json) {
    if (json == null) {
      throw new IllegalArgumentException("Credential JSON cannot be null");
    }

    String typeStr = json.getString("type");
    if (typeStr == null || typeStr.isEmpty()) {
      throw new IllegalArgumentException("Credential type is required");
    }

    Protocol type = Protocol.fromString(typeStr);
    if (type == Protocol.UNKNOWN) {
      throw new IllegalArgumentException("Unknown credential type: " + typeStr);
    }

    switch (type) {
      case SNMP:
        return SnmpCredential.fromJson(json);
      case SSH:
        return SshCredential.fromJson(json);
      default:
        throw new IllegalArgumentException("Unsupported credential type: " + type);
    }
  }
}
