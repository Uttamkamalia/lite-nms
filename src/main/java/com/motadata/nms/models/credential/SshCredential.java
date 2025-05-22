package com.motadata.nms.models.credential;

import com.motadata.nms.models.DeviceType;
import com.motadata.nms.security.EncryptionServiceProvider;
import io.vertx.core.json.JsonObject;

public class SshCredential extends Credential{
  private String username;
  private String password;

  public SshCredential(){
    super(DeviceType.Protocol.SSH);
  }

  public SshCredential(String username, String password) {
    super(DeviceType.Protocol.SSH);
    this.username = username;
    this.password = EncryptionServiceProvider.getService().encrypt(password);
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

  @Override
  public JsonObject toJson() {
    return new JsonObject()
      .put("type", getType().getValue())
      .put("username", username)
      .put("password", password);
  }
}
