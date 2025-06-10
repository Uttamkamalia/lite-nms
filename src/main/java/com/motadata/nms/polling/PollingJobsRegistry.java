package com.motadata.nms.polling;

import io.vertx.core.json.JsonObject;

import java.util.concurrent.ConcurrentHashMap;

public class PollingJobsRegistry {

  private ConcurrentHashMap <String, JsonObject> map = new ConcurrentHashMap<>();
}
