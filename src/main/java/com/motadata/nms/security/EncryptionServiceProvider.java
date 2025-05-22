package com.motadata.nms.security;

public class EncryptionServiceProvider {
  EncryptionService service;

  //need to do double check locking
  public static synchronized EncryptionService getService() {
    if(service == null){
      service = new EncryptionServiceImpl();
    }
    return service;
  }
}
