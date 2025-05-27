package com.motadata.nms.discovery.job;

import com.motadata.nms.models.DeviceType;
import com.motadata.nms.models.credential.CredentialProfile;
import com.motadata.nms.models.credential.SshCredential;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SshDiscoveryJob extends DiscoveryJob {

  private String username;
  private String password;


  public SshDiscoveryJob(List<String> batch, Integer port, CredentialProfile credentialProfile, Integer discoveryProfileId) {
    super(batch, port, credentialProfile.getId(), discoveryProfileId);

    // Validate that the credential profile contains SSH credentials
    if (!credentialProfile.isSshCredential()) {
      throw new IllegalArgumentException("CredentialProfile must contain SSH credentials for SshDiscoveryJob");
    }

    // Extract SSH connection details from credential profile
    extractConnectionDetails(credentialProfile);
  }

  public SshDiscoveryJob(List<String> batch, Integer port, Integer credentialProfileId, Integer discoveryProfileId, String username, String password){
    super(batch, port, credentialProfileId, discoveryProfileId);
    this.username = username;
    this.password = password;
  }

  /**
   * Extract SSH connection details from the credential profile
   */
  public void extractConnectionDetails(CredentialProfile credentialProfile) {
    if (credentialProfile != null && credentialProfile.isSshCredential()) {
      SshCredential sshCredential = credentialProfile.getSshCredential();
      if (sshCredential != null) {
        this.username = sshCredential.getUsername();
        this.password = sshCredential.getPassword();
      }

      this.command = sshCredential.getType().getPluginName();
    }
  }

  // Getters and setters for SSH connection fields
  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }


  @Override
  public String toString() {
    return "SshDiscoveryJob{" +
      "ip=" + getBatch() +
      ", port=" + getPort() +
      ", credentialProfileId=" + getCredentialProfileId() +
      ", discoveryProfileId=" + getDiscoveryProfileId() +
      ", username='" + username + '\'' +
      ", hasPassword=" + (password != null && !password.isEmpty()) +
      '}';
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }


  public static SshDiscoveryJob fromJson(JsonObject json) {
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

    Integer credentialProfileId = json.getInteger("credentialProfile");
    if (credentialProfileId == null) {
      throw new IllegalArgumentException("Credential profile is required for SSH discovery job");
    }

    // Set SSH-specific fields
    String username = json.getString("username");
    String password = json.getString("password");

    // Create the job
    SshDiscoveryJob job = new SshDiscoveryJob(batch, port, credentialProfileId, discoveryProfileId, username, password);
    // Set command if available
    String command = json.getString("command");
    if (command != null && !command.isEmpty()) {
      job.command = command;
    }

    return job;
  }


  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("protocol", DeviceType.Protocol.SSH.getValue());

    // Add common fields
    json.put("ip", getBatch());
    json.put("port", getPort());
    json.put("discoveryProfileId", getDiscoveryProfileId());
    json.put("credentialProfileId", getCredentialProfileId());

    JsonObject credentialJson = new JsonObject();
    // Add SSH-specific fields
    if (username != null) {
      credentialJson.put("username", username);
    }

    if (password != null) {
      credentialJson.put("password", password);
    }
    json.put("creds", credentialJson);

    // Add command if available
    if (command != null) {
      json.put("command", command);
    }

    return json;
  }

  public  JsonObject toDeviceJson(){
    JsonObject json = new JsonObject();
    json.put("protocol", DeviceType.Protocol.SSH.getValue());

    // Add common fields
    json.put("ip", getBatch());
    json.put("port", getPort());

    JsonObject credentialJson = new JsonObject();
    // Add SSH-specific fields
    if (username != null) {
      credentialJson.put("username", username);
    }

    if (password != null) {
      credentialJson.put("password", password);
    }
    json.put("creds", credentialJson);

    return json;
  }



  public String toSerializedJson() {
    StringBuilder sb = new StringBuilder();
    JsonObject deviceJson = this.toDeviceJson();

    sb.append("{\"DiscoveryProfileId\":"+discoveryProfileId+",");

    sb.append("\"devices\":[");

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

}
