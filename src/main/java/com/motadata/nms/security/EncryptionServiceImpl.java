package com.motadata.nms.security;

public class EncryptionServiceImpl implements EncryptionService{
  private static final String ENCRYPTION_PREFIX = "encrypted";

  @Override
  public String encrypt(String password) {
    return ENCRYPTION_PREFIX+password;
  }

  @Override
  public String decrypt(String encryptedPassword) {
    return encryptedPassword.replace(ENCRYPTION_PREFIX,"");
  }

  @Override
  public boolean isEncrypted(String password) {
    return password.contains(ENCRYPTION_PREFIX);
  }
}
