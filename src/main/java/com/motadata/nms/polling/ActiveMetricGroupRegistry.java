package com.motadata.nms.polling;

import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

public class ActiveMetricGroupRegistry {

  private static final Logger logger = LoggerFactory.getLogger(ActiveMetricGroupRegistry.class);

  //key : device-type-id + metric-group-id
  private ConcurrentHashMap <String, Collection<JsonObject>> batches = new ConcurrentHashMap<>();
  private static ActiveMetricGroupRegistry instance;
  private static int size = 0;

  private ActiveMetricGroupRegistry() {}

  public static synchronized ActiveMetricGroupRegistry getInstance() {
    if (instance == null) {
      instance = new ActiveMetricGroupRegistry();
    }
    return instance;
  }

  public void put(String key, JsonObject value) {
    batches.computeIfAbsent(key, (a) -> new ConcurrentLinkedQueue<>()).add(value);
  }

  public void put(Integer deviceTypeId, Integer metricGroupId, JsonObject value) {
    String key = getKey(deviceTypeId, metricGroupId);
    logger.debug("yo! Adding polling job for " + key + " : " + Json.encodePrettily(value));
    if(size++ < 10) {
      put(key, value);
    }

  }

  public Collection<JsonObject> get(String key) {
    Collection<JsonObject> result = batches.get(key);
    logger.debug("yo! Getting polling jobs for " + key + " : " + Json.encodePrettily(result));
    return result;
  }

  public void remove(String key) {
    batches.remove(key);
  }

  static String getKey(Integer deviceTypeId, Integer metricGroupId) {
    return deviceTypeId + "-" + metricGroupId;
  }
}
