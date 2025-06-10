package com.motadata.nms.discovery;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.ConcurrentHashMap;

public class DiscoveryPromiseTracker {
  private ConcurrentHashMap<Integer, Promise<JsonObject>> promiseMap = new ConcurrentHashMap<>();
  private static DiscoveryPromiseTracker instance;

  private DiscoveryPromiseTracker() {
  }

  public static DiscoveryPromiseTracker getInstance() {
    if (instance == null) {
      instance = new DiscoveryPromiseTracker();
    }
    return instance;
  }

  public void put(Integer key, Promise<JsonObject> value) {
    promiseMap.put(key, value);
  }

  public Promise<JsonObject> get(Integer key) {
    return promiseMap.get(key);
  }

  public void remove(Integer key) {
    promiseMap.remove(key);
  }
}
