package com.motadata.nms.discovery.job;

import com.motadata.nms.models.credential.CredentialProfile;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.UUID;

public abstract class DiscoveryJob {
  // JSON field constants
  public static final String FIELD_PORT = "port";
  public static final String FIELD_DISCOVERY_PROFILE_ID = "discoveryProfileId";
  public static final String INPUT_FILE_EXTENSION = ".json";

  protected final String id;

  protected Integer discoveryProfileId;
  protected Integer credentialProfileId;


  protected List<String> ipBatch;
  protected Integer port;
  protected final String inputFileName;
  protected String command;


  // Full constructor
  protected DiscoveryJob(List<String> ipBatch, Integer port, Integer discoveryProfileId, Integer credentialProfileId) {
    this.ipBatch = ipBatch;
    this.port = port;
    this.discoveryProfileId = discoveryProfileId;
    this.credentialProfileId = credentialProfileId;
    this.id = UUID.randomUUID().toString();
    this.inputFileName = this.id + INPUT_FILE_EXTENSION;
  }
  public abstract void extractConnectionDetails(CredentialProfile credentialProfile);

  // Getters and setters
  public List<String> getIpBatch() {
    return ipBatch;
  }

  public void setIpBatch(List<String> ipBatch) {
    this.ipBatch = ipBatch;
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

  public Integer getCredentialProfileId() {
    return credentialProfileId;
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    return jsonObject;
  }
}
