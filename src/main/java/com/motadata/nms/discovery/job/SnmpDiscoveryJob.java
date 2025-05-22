package com.motadata.nms.discovery.job;

import com.motadata.nms.models.credential.CredentialProfile;
import com.motadata.nms.models.credential.SnmpCredential;

import java.util.List;

public class SnmpDiscoveryJob extends DiscoveryJob {

  // SNMP connection details
  private String version;
  private String community;
  private String securityName;

  public SnmpDiscoveryJob(List<String> ips, Integer port, CredentialProfile credentialProfile, Integer discoveryProfileId) {
    super(ips, port, credentialProfile.getId(), discoveryProfileId);
    extractSnmpDetails(credentialProfile);
  }

  private void extractSnmpDetails(CredentialProfile credentialProfile) {
    if (credentialProfile != null && credentialProfile.isSnmpCredential()) {
      SnmpCredential snmpCredential = credentialProfile.getSnmpCredential();
      if (snmpCredential != null) {
        this.version = snmpCredential.getVersion();
        this.community = snmpCredential.getCommunity();
        this.securityName = snmpCredential.getSecurityName();
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

  public String getSecurityName() {
    return securityName;
  }

  public void setSecurityName(String securityName) {
    this.securityName = securityName;
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
      ", securityName='" + securityName + '\'' +
      '}';
  }
}
