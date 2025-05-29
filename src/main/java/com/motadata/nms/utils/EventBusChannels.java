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
  DISCOVERY_BATCH_RESULT("discovery.batch.result.successful"),
  DISCOVERY_BATCH_RESULT_SUCCESSFUL("discovery.batch.result.successful"),
  DISCOVERY_BATCH_RESULT_FAILED("discovery.batch.result.failed"),
  DISCOVERY_RESPONSE("discovery.response"),

  // Encryption Channels
  ENCRYPT_PASSWORD("encryption.password"),
  DECRYPT_PASSWORD("encryption.password.decrypt"),
  IS_ENCRYPTED("encryption.password.is.encrypted"),

  // Metric Channels
  METRIC_SAVE("metric.save"),
  METRIC_GET("metric.get"),
  METRIC_GET_ALL("metric.get.all"),
  METRIC_GET_BY_DEVICE_TYPE("metric.get.by.device.type"),
  METRIC_UPDATE("metric.update"),
  METRIC_DELETE("metric.delete"),

  // MetricGroup Channels
  METRIC_GROUP_SAVE("metric.group.save"),
  METRIC_GROUP_GET("metric.group.get"),
  METRIC_GROUP_GET_ALL("metric.group.get.all"),
  METRIC_GROUP_GET_BY_DEVICE_TYPE("metric.group.get.by.device.type"),
  METRIC_GROUP_UPDATE("metric.group.update"),
  METRIC_GROUP_UPDATE_STATUS("metric.group.update.status"),
  METRIC_GROUP_DELETE("metric.group.delete"),

  // New channels for ProvisionedDevice
  PROVISIONED_DEVICE_SAVE("provisioned.device.save"),
  PROVISIONED_DEVICE_GET("provisioned.device.get"),
  PROVISIONED_DEVICE_GET_ALL("provisioned.device.get.all"),
  PROVISIONED_DEVICE_GET_BY_IP("provisioned.device.get.by.ip"),
  PROVISIONED_DEVICE_GET_BY_DISCOVERY_PROFILE("provisioned.device.get.by.discovery.profile"),
  PROVISIONED_DEVICE_UPDATE("provisioned.device.update"),
  PROVISIONED_DEVICE_UPDATE_STATUS("provisioned.device.update.status"),
  PROVISIONED_DEVICE_DELETE("provisioned.device.delete");

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
