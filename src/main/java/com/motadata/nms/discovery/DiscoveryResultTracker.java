package com.motadata.nms.discoveryprac;

import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class DiscoveryResultTracker {
  private final int totalDevices;
  private final AtomicInteger completedDevices = new AtomicInteger(0);

  private final List<String> successfulIps = Collections.synchronizedList(new ArrayList<>());
  private final Map<String, String> failedIps = new ConcurrentHashMap<>();

  private final Map<String, JsonObject> batchResults = new ConcurrentHashMap<>();
  private final Map<String, JsonObject> failedBatchResults = new ConcurrentHashMap<>();
  private final AtomicInteger totalBatches = new AtomicInteger(0);
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

  public boolean isBatchFilled(int batchSize) {
    return successfulIps.size() >= batchSize;
  }

  public List<String> getSuccessfulBatch(int batchSize){
     List<String> batch =  this.successfulIps.subList(0, batchSize);
     this.successfulIps.removeAll(batch);
     this.totalBatches.getAndIncrement();
     return batch;
  }

  public List<String> getSuccessfulIps() {
    return successfulIps;
  }

  public Map<String, String> getFailures() {
    return failedIps;
  }


  public Integer addBatch(){
    return this.totalBatches.incrementAndGet();
  }

  public void addBatchResult(String batchJobId, JsonObject result) {
    batchResults.put(batchJobId, result);
    completedBatches.incrementAndGet();
  }

  public void addBatchFailure(String batchJobId, String error) {
    JsonObject failure = new JsonObject()
      .put("error", error);
    failedBatchResults.put(batchJobId, failure);
    completedBatches.incrementAndGet();
  }

  public boolean allBatchesProcessed() {
    return completedBatches.get() >= totalBatches.get();
  }

  public Collection<JsonObject> getSuccessBatchResults() {
    return batchResults.values();
  }

  public Collection<JsonObject> getFailedBatchResults() {
    return failedBatchResults.values();
  }
}
