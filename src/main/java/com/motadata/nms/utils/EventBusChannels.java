package com.motadata.nms.utils;


import java.util.List;


public enum EventBusChannels {

  // device-type actions
  DEVICE_TYPE_SAVE("device.type.save"),
  DEVICE_TYPE_GET("device.type.get"),
  DEVICE_TYPE_GET_ALL("device.type.get.all"),
  DEVICE_TYPE_DELETE("device.type.delete"),

  // Credential Profile Events
  CREDENTIAL_PROFILE_SAVE("credential.profile.save"),
  CREDENTIAL_PROFILE_GET("credential.profile.get"),
  CREDENTIAL_PROFILE_GET_ALL("credential.profile.get.all"),
  CREDENTIAL_PROFILE_UPDATE("credential.profile.update"),
  CREDENTIAL_PROFILE_DELETE("credential.profile.delete"),



    DISCOVERY_REQUEST("discovery.request"),     // polled by DiscoveryService to validate snmp connection and trigger discovery-job event
    DISCOVERY_JOB("discovery.jobs"),            // polled by Collector , snmp connect and save event
    DISCOVERED_DEVICE("discovery.provisioned"),        // polled by DiscoveryDaoVerticle to save and send discovery-response
    DISCOVERY_RESPONSE("discovery.response"),  //  DiscoveryDaoVerticle sends response which is received by Router

    METRIC_REQUEST("metric.config.save"), // polled by MetricService to verify and save metric-info

    METRIC_POLL_START("metric.polling.start"),    // verifies metric-id from db and calls Polling service and creates jobs
    METRIC_POLL_JOBS("metric.polling.jobs"),    // polled by collector, snmp-get
    METRIC_POLL_START_VERIFIED("metric.polling.record"), // polled by DbService to record metric

    METRIC_POLL_STOP("metric.polling.stop");   // maintain a shared-map for all the ongoing metric-config-id




    private static String SEPERATOR = ".";
    private String name;
    private Class sendMsgType;
    private Class receiveMsgType;

    EventBusChannels(String name){
      this.name = name;
    }
    EventBusChannels(String name, Class sendMsgType, Class receiveMsgType){
    this.name = name;
    this.sendMsgType = sendMsgType;
    this.receiveMsgType = receiveMsgType;
  }


  public String withClient(String client){
      return client + SEPERATOR + name;
    }

    public String withClientAndCorrelationId(String client, String correlationId){
      return client + SEPERATOR + name + SEPERATOR + correlationId;
    }
}
