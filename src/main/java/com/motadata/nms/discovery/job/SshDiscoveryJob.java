package com.motadata.nms.discovery.job;

import com.motadata.nms.commons.NMSException;
import com.motadata.nms.models.DeviceType.Protocol;
import com.motadata.nms.models.credential.CredentialProfile;
import com.motadata.nms.models.credential.SshCredential;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class SshDiscoveryJob extends DiscoveryJob {

  private String username;
  private String password;


  public SshDiscoveryJob(List<String> ips, Integer port, CredentialProfile credentialProfile, Integer discoveryProfileId) {
    super(ips, port, credentialProfile.getId(), discoveryProfileId);

    // Validate that the credential profile contains SSH credentials
    if (credentialProfile != null && !credentialProfile.isSshCredential()) {
      throw new IllegalArgumentException("CredentialProfile must contain SSH credentials for SshDiscoveryJob");
    }

    // Extract SSH connection details from credential profile
    extractConnectionDetails(credentialProfile);
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
  public JsonObject toJson() {
    JsonObject json = super.toJson();

    // Add SSH connection fields
    if (username != null) {
      json.put("username", username);
    }

    if (password != null) {
      json.put("password", password);
    }
    return json;
  }


  @Override
  public String toString() {
    return "SshDiscoveryJob{" +
      "ips=" + getIpBatch() +
      ", port=" + getPort() +
      ", credentialProfileId=" + getCredentialProfileId() +
      ", discoveryProfileId=" + getDiscoveryProfileId() +
      ", username='" + username + '\'' +
      ", hasPassword=" + (password != null && !password.isEmpty()) +
      '}';
  }

}
