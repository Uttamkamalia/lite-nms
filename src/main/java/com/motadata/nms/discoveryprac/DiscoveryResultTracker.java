package com.motadata.nms.discoveryprac;

import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


class DiscoveryResultTracker {
  private final int totalDevices;
  private final AtomicInteger completedDevices = new AtomicInteger(0);

  private final List<String> successfulIps = Collections.synchronizedList(new ArrayList<>());
  private final Map<String, String> failedIps = new ConcurrentHashMap<>();

  private final Map<Integer, JsonObject> batchResults = new ConcurrentHashMap<>();
  private int totalBatches = 0;
  private final AtomicInteger completedBatches = new AtomicInteger(0);

  public DiscoveryResultTracker(int totalDevices) {
    this.totalDevices = totalDevices;
  }

  public void addSuccess(String ip) {
    successfulIps.add(ip);
    completedDevices.incrementAndGet();
  }

  public void addFailure(String ip, String reason) {
    failedIps.put(ip, reason);
    completedDevices.incrementAndGet();
  }

  public boolean isDeviceCheckComplete() {
    return completedDevices.get() >= totalDevices;
  }

  public List<String> getSuccessfulIps() {
    return successfulIps;
  }

  public Map<String, String> getFailures() {
    return failedIps;
  }

  public void setTotalBatches(int count) {
    this.totalBatches = count;
  }

  public void addBatchResult(int batchIndex, JsonObject result) {
    batchResults.put(batchIndex, result);
    completedBatches.incrementAndGet();
  }

  public void addBatchFailure(int batchIndex, String error) {
    JsonObject failure = new JsonObject()
      .put("batchIndex", batchIndex)
      .put("error", error);
    batchResults.put(batchIndex, failure);
    completedBatches.incrementAndGet();
  }

  public boolean allBatchesProcessed() {
    return completedBatches.get() >= totalBatches;
  }

  public Collection<JsonObject> getBatchResults() {
    return batchResults.values();
  }
}
