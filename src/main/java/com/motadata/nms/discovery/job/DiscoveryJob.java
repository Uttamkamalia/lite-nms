package com.motadata.nms.discovery.job;

import com.motadata.nms.models.DeviceType;
import com.motadata.nms.models.credential.CredentialProfile;
import io.vertx.core.json.JsonArray;
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


  protected List<String> batch;
  protected Integer port;

  protected final String inputFileName;

  protected String command;


  // Full constructor
  protected DiscoveryJob(List<String> ipBatch, Integer port, Integer discoveryProfileId, Integer credentialProfileId) {
    this.batch = ipBatch;
    this.port = port;
    this.discoveryProfileId = discoveryProfileId;
    this.credentialProfileId = credentialProfileId;
    this.id = UUID.randomUUID().toString();
    this.inputFileName = this.id + INPUT_FILE_EXTENSION;
  }
  public abstract void extractConnectionDetails(CredentialProfile credentialProfile);

  // Getters and setters
  public List<String> getBatch() {
    return batch;
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

  public static DiscoveryJob fromJson(JsonObject json) {
    if(json.getString("type").equals(DeviceType.Protocol.SNMP.toString())){
      return SnmpDiscoveryJob.fromJson(json);
    } else if(json.getString("type").equals(DeviceType.Protocol.SSH.toString())){
      return SshDiscoveryJob.fromJson(json);
    }
    return null;
  }


  public String getInputFileName() {
    return inputFileName;
  }


  public String getCommand() {
    return command;
  }

  public String getId() {
    return id;
  }


  public String toSerializedJson() {
    if (this instanceof SshDiscoveryJob sshDiscoveryJob){
      return sshDiscoveryJob.toSerializedJson();
    } else if (this instanceof SnmpDiscoveryJob snmpDiscoveryJob){
      return snmpDiscoveryJob.toSerializedJson();
    }
    return null;
  }
}
