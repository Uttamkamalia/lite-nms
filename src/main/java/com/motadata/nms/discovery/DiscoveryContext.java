package com.motadata.nms.discovery;


import java.util.List;

public class DiscoveryContext {
  private final List<String> targetIps;
  private final int port;
  private final int credentialProfileId;
  private final int discoveryProfileId;

  public DiscoveryContext(List<String> targetIps, int port, int credentialProfileId, int discoveryProfileId) {
    this.targetIps = targetIps;
    this.port = port;
    this.credentialProfileId = credentialProfileId;
    this.discoveryProfileId = discoveryProfileId;
  }

  public List<String> getTargetIps() {
    return targetIps;
  }

  public int getPort() {
    return port;
  }

  public int getCredentialProfileId() {
    return credentialProfileId;
  }

  public int getDiscoveryProfileId() {
    return discoveryProfileId;
  }
}
