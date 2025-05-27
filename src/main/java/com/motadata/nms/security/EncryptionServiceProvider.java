package com.motadata.nms.security;

public class EncryptionServiceProvider {
  private static EncryptionService service;

  //need to do double check locking
  public static synchronized EncryptionService getService() {
    if(service == null){
      service = new EncryptionServiceImpl();
    }
    return service;
  }
}
