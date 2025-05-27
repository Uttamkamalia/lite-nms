package com.motadata.nms.discovery.job;

import com.motadata.nms.models.DeviceType;
import com.motadata.nms.models.credential.CredentialProfile;
import com.motadata.nms.models.credential.SnmpCredential;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class SnmpDiscoveryJob extends DiscoveryJob {

  // SNMP connection details
  private String version;
  private String community;

  public SnmpDiscoveryJob(List<String> batch, Integer port, CredentialProfile credentialProfile, Integer discoveryProfileId) {
    super(batch, port, credentialProfile.getId(), discoveryProfileId);
    extractConnectionDetails(credentialProfile);
  }

  public void extractConnectionDetails(CredentialProfile credentialProfile) {
    if (credentialProfile != null && credentialProfile.isSnmpCredential()) {
      SnmpCredential snmpCredential = credentialProfile.getSnmpCredential();
      if (snmpCredential != null) {
        this.version = snmpCredential.getVersion();
        this.community = snmpCredential.getCommunity();
      }
    }
  }

  public SnmpDiscoveryJob(List<String> batch, Integer port, Integer credentialProfileId, Integer discoveryProfileId, String version, String community) {
    super(batch, port, credentialProfileId, discoveryProfileId);
    this.version = version;
    this.community = community;
  }


  public static SnmpDiscoveryJob fromJson(JsonObject json) {
    if (json == null) {
      throw new IllegalArgumentException("SSH discovery job JSON cannot be null");
    }

    // Extract required fields
    List<String> batch = json.getJsonArray("batch").stream().map(obj -> (String) obj).toList();
    if (batch.isEmpty()) {
      throw new IllegalArgumentException("IP address is required for SSH discovery job");
    }

    Integer port = json.getInteger("port");
    if (port == null) {
      throw new IllegalArgumentException("Port is required for SSH discovery job");
    }

    Integer discoveryProfileId = json.getInteger("discoveryProfileId");
    if (discoveryProfileId == null) {
      throw new IllegalArgumentException("Discovery profile ID is required for SSH discovery job");
    }

    Integer credentialProfileId = json.getInteger("credentialProfileId");
    if (credentialProfileId== null) {
      throw new IllegalArgumentException("Credential profile id is required for SSH discovery job");
    }

    String version = json.getString("version");
    String community = json.getString("community");

    // Create the job
    SnmpDiscoveryJob job = new SnmpDiscoveryJob(batch, port, credentialProfileId, discoveryProfileId, version, community);

    // Set command if available
    String command = json.getString("command");
    if (command != null && !command.isEmpty()) {
      job.command = command;
    }

    return job;
  }


  public JsonObject toJson() {
    JsonObject json = new JsonObject();

    json.put("type", DeviceType.Protocol.SNMP.getValue());

    // Add common fields
    json.put("batch", getBatch());
    json.put("port", getPort());
    json.put("discoveryProfileId", getDiscoveryProfileId());
    json.put("credentialProfileId", getCredentialProfileId());

    // Add snmp-specific fields
    if (version != null) {
      json.put("version", version);
    }

    if (community != null) {
      json.put("community", community);
    }

    // Add command if available
    if (command != null) {
      json.put("command", command);
    }

    return json;
  }



  @Override
  public String toString() {
    return "SnmpDiscoveryJob{" +
      "batch=" + getBatch() +
      ", port=" + getPort() +
      ", credentialProfileId=" + getCredentialProfileId() +
      ", discoveryProfileId=" + getDiscoveryProfileId() +
      ", version='" + version + '\'' +
      ", community='" + (community != null ? "***" : "null") + '\'' +
      '}';
  }

  public  JsonObject toDeviceJson(){
    JsonObject json = new JsonObject();
    json.put("protocol", DeviceType.Protocol.SNMP.getValue());

    // Add common fields
    json.put("ip", getBatch());
    json.put("port", getPort());

    JsonObject credentialJson = new JsonObject();
    // Add SSH-specific fields
    if (community != null) {
      credentialJson.put("community", community);
    }

    if (version != null) {
      credentialJson.put("version", version);
    }
    json.put("creds", credentialJson);

    return json;
  }


  public String toSerializedJson() {
    StringBuilder sb = new StringBuilder();
    JsonObject deviceJson = this.toDeviceJson();

    sb.append("{\"discoveryProfileId:\""+discoveryProfileId+",");

    sb.append("{\"devices:[\"");

    this.batch.forEach(ip -> {
      deviceJson.put("ip", ip);
      sb.append(deviceJson);
      sb.append(",");
    });
    sb.deleteCharAt(sb.length()-1);
    sb.append("]");
    sb.append("}");

    return sb.toString();
  }

  // Getters and setters for SNMP connection fields
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

  public List<String> getBatch() {
    return batch;
  }
}
