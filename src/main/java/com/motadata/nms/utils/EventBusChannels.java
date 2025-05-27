package com.motadata.nms.utils;


public enum EventBusChannels {

  // Device Type Channels
  DEVICE_TYPE_SAVE("device.type.save"),
  DEVICE_TYPE_GET("device.type.get"),
  DEVICE_TYPE_GET_ALL("device.type.get.all"),
  DEVICE_TYPE_DELETE("device.type.delete"),

  // Credential Profile Channels
  CREDENTIAL_PROFILE_SAVE("credential.profile.save"),
  CREDENTIAL_PROFILE_GET("credential.profile.get"),
  CREDENTIAL_PROFILE_GET_ALL("credential.profile.get.all"),
  CREDENTIAL_PROFILE_UPDATE("credential.profile.update"),
  CREDENTIAL_PROFILE_DELETE("credential.profile.delete"),

  // Discovery Profile Channels
  DISCOVERY_PROFILE_SAVE("discovery.profile.save"),
  DISCOVERY_PROFILE_GET("discovery.profile.get"),
  DISCOVERY_PROFILE_GET_ALL("discovery.profile.get.all"),
  DISCOVERY_PROFILE_DELETE("discovery.profile.delete"),
  DISCOVERY_TRIGGER("discovery.trigger"),
  DISCOVERY_CONTEXT_BUILD("discovery.context.build"),
  DISCOVERY_JOBS_SNMP("discovery.jobs.snmp"),
  DISCOVERY_JOBS_SSH("discovery.jobs.ssh"),
  DISCOVERY_JOBS_START("discovery.jobs.start"),
  DISCOVERY_BATCH("discovery.batch"),
  DISCOVERY_BATCH_RESULT("discovery.batch.result"),
  DISCOVERY_RESULT("discovery.result."),

  // Encryption Channels
  ENCRYPT_PASSWORD("encryption.password"),
  DECRYPT_PASSWORD("encryption.password.decrypt"),
  IS_ENCRYPTED("encryption.password.is.encrypted");

    private static String SEPERATOR = ".";
    private String name;

    EventBusChannels(String name){
      this.name = name;
    }


  public String withClient(String client){
      return client + SEPERATOR + name;
    }

    public String withClientAndCorrelationId(String client, String correlationId){
      return client + SEPERATOR + name + SEPERATOR + correlationId;
    }

    public String withId(Integer id){
      return name + SEPERATOR + id;
    }
}
