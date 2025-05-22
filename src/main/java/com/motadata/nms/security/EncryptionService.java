package com.motadata.nms.security;

public interface EncryptionService {
  String encrypt(String password);
  String decrypt(String encryptedPassword);
  boolean isEncrypted(String password);
}
