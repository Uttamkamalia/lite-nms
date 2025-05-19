package com.motadata.nms.models.credential;

import io.vertx.core.json.JsonObject;

public class SshCredential extends Credential{
  private String username;
  private String password;

  public SshCredential(String username, String password) {
    this.username = username;
    this.password = password;
  }

  public static SshCredential fromJson(JsonObject json) {
    if (json == null) {
      throw new IllegalArgumentException("SSH credential JSON cannot be null");
    }

    String username = json.getString("username");
    if (username == null || username.isEmpty()) {
      throw new IllegalArgumentException("SSH username is required");
    }

    SshCredential credential = new SshCredential();
    credential.setUsername(username);

    String password = json.getString("password");
    String privateKey = json.getString("privateKey");

    // At least one authentication method must be provided
    if ((password == null || password.isEmpty()) && (privateKey == null || privateKey.isEmpty())) {
      throw new IllegalArgumentException("SSH requires either password or private key authentication");
    }

    credential.setPassword(password);
    credential.setPrivateKey(privateKey);
    credential.setPort(json.getInteger("port", DEFAULT_PORT));

    return credential;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public String toString() {
    return super.toString();
  }
}
