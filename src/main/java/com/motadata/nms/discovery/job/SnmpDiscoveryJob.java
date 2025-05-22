package com.motadata.nms.discovery.job;

import com.motadata.nms.models.credential.CredentialProfile;
import com.motadata.nms.models.credential.SnmpCredential;

import java.util.List;

public class SnmpDiscoveryJob extends DiscoveryJob {

  // SNMP connection details
  private String version;
  private String community;

  public SnmpDiscoveryJob(List<String> ips, Integer port, CredentialProfile credentialProfile, Integer discoveryProfileId) {
    super(ips, port, credentialProfile.getId(), discoveryProfileId);
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

  @Override
  public String toString() {
    return "SnmpDiscoveryJob{" +
      "ipBatch=" + getIpBatch() +
      ", port=" + getPort() +
      ", credentialProfileId=" + getCredentialProfileId() +
      ", discoveryProfileId=" + getDiscoveryProfileId() +
      ", version='" + version + '\'' +
      ", community='" + (community != null ? "***" : "null") + '\'' +
      '}';
  }
}
