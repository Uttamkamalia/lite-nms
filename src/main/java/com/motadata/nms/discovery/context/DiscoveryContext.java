package com.motadata.nms.discovery.context;

import com.motadata.nms.commons.NMSException;
import com.motadata.nms.models.credential.CredentialProfile;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryContext {
  public static final String DISCOVERY_PROFILE_ID = "discovery_profile_id";
  public static final String PORT = "port";
  public static final String TARGET_IPS = "target_ips";
  public static final String CREDENTIAL_PROFILE = "credential_profile";

  private final List<String> targetIps;
  private final Integer port;
  private final Integer discoveryProfileId;
  private final CredentialProfile credentialProfile;

  public DiscoveryContext(List<String> targetIps, int port, int discoveryProfileId, CredentialProfile credentialProfile) {
    this.targetIps = targetIps;
    this.port = port;
    this.discoveryProfileId = discoveryProfileId;
    this.credentialProfile = credentialProfile;
  }

  public List<String> getTargetIps() {
    return targetIps;
  }

  public Integer getPort() {
    return port;
  }

  public CredentialProfile getCredentialProfile() {
    return credentialProfile;
  }

  public Integer getDiscoveryProfileId() {
    return discoveryProfileId;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
      .put(DISCOVERY_PROFILE_ID, discoveryProfileId)
      .put(PORT, port);

    // Add target IPs as a JSON array
    JsonArray ipsArray = new JsonArray();
    for (String ip : targetIps) {
      ipsArray.add(ip);
    }
    json.put(TARGET_IPS, ipsArray);

    // Add credential profile if available
    if (credentialProfile != null) {
      json.put(CREDENTIAL_PROFILE, credentialProfile.toJson());
    }

    return json;
  }

  /**
   * Create a DiscoveryContext from a JSON object
   * @param json The JSON object to parse
   * @return A new DiscoveryContext instance
   * @throws NMSException if the JSON is invalid
   */
  public static DiscoveryContext fromJson(JsonObject json) {
    try {
      if (json == null) {
        throw NMSException.badRequest("Discovery context JSON cannot be null");
      }

      // Extract discovery profile ID
      Integer discoveryProfileId = json.getInteger(DISCOVERY_PROFILE_ID);
      if (discoveryProfileId == null) {
        throw NMSException.badRequest("Discovery profile ID is required");
      }

      // Extract port
      Integer port = json.getInteger(PORT);
      if (port == null) {
        throw NMSException.badRequest("Port is required");
      }

      // Extract target IPs
      JsonArray ipsArray = json.getJsonArray(TARGET_IPS);
      if (ipsArray == null || ipsArray.isEmpty()) {
        throw NMSException.badRequest("Target IPs are required");
      }

      List<String> targetIps = new ArrayList<>();
      for (int i = 0; i < ipsArray.size(); i++) {
        targetIps.add(ipsArray.getString(i));
      }

      // Extract credential profile
      JsonObject credentialProfileJson = json.getJsonObject(CREDENTIAL_PROFILE);
      if (credentialProfileJson == null) {
        throw NMSException.badRequest("Credential profile is required");
      }

      CredentialProfile credentialProfile = CredentialProfile.fromJson(credentialProfileJson);

      return new DiscoveryContext(targetIps, port, discoveryProfileId, credentialProfile);
    } catch (IllegalArgumentException e) {
      throw NMSException.badRequest("Invalid discovery context: " + e.getMessage(), e);
    } catch (Exception e) {
      if (e instanceof NMSException) {
        throw (NMSException) e;
      }
      throw NMSException.internal("Error parsing discovery context: " + e.getMessage(), e);
    }
  }
}
