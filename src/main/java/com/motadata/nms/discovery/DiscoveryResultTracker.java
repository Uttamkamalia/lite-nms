package com.motadata.nms.discovery;

import com.motadata.nms.commons.VertxProvider;
import com.motadata.nms.rest.utils.ErrorCodes;
import com.motadata.nms.utils.EventBusChannels;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.motadata.nms.utils.EventBusChannels.DISCOVERY_RESPONSE;


public class DiscoveryResultTracker {
  private static final Logger logger = LoggerFactory.getLogger(DiscoveryResultTracker.class);

  private final Integer totalDevices;
  private final Integer discoveryProfileId;
  private final Integer discoveryRequestTimeout;
  private final Set<String> successfulIps = ConcurrentHashMap.newKeySet();
  private final Map<String, String> failedIps = new ConcurrentHashMap<>();
  private final AtomicBoolean isResponseSent = new AtomicBoolean(false);


  public DiscoveryResultTracker(Integer discoveryProfileId, Integer totalDevices, String discoveryResponseEventBusChannel, Integer discoveryRequestTimeout) {
    this.discoveryProfileId = discoveryProfileId;
    this.totalDevices = totalDevices;
    this.discoveryRequestTimeout = discoveryRequestTimeout;

    registerDiscoveryRequestTimeout();
  }

  public void addSuccess(String ip) {
    successfulIps.add(ip);
    if (isResultComplete()) {
      sendDiscoveryResponse();
    }
  }

  public void addFailure(String ip, String reason) {
    if(ip != null && !ip.isEmpty() && reason != null && !reason.isEmpty()){
      failedIps.put(ip, reason);
      if (isResultComplete()) {
        sendDiscoveryResponse();
      }
    }
  }

  public boolean isResultComplete() {
    return successfulIps.size() + failedIps.size() == totalDevices;
  }

  private void sendDiscoveryResponse() {
    if(isResponseSent.get()){
      return;
    }

    JsonObject result = new JsonObject()
      .put("discoveryProfileId", discoveryProfileId)
      .put("success", new JsonArray(successfulIps.stream().toList()));

    JsonArray failedIpsWithReason = new JsonArray();
    failedIps
      .entrySet()
      .forEach(entry -> failedIpsWithReason.add(new JsonObject().put("ip", entry.getKey()).put("reason", entry.getValue())));

    result.put("failed", failedIpsWithReason);

    Promise<JsonObject> promise = DiscoveryPromiseTracker.getInstance().get(discoveryProfileId);
    if (promise != null) {
      promise.complete(result);
      isResponseSent.set(true);
    } else {
      logger.error("Failed to send discovery response for discovery-profile-id:" + discoveryProfileId + " as discovery-response-promise is not registered");
    }
  }

  private void registerDiscoveryRequestTimeout() {

    VertxProvider.getVertx().timer(discoveryRequestTimeout, TimeUnit.SECONDS).onComplete(id -> {
      logger.info("Discovery request timed out with "+discoveryRequestTimeout+" seconds for discovery-profile-id:" + discoveryProfileId);
      sendDiscoveryResponse();
    });
  }

  public Set<String> getSuccessfulIps() {
    return successfulIps;
  }

  public Map<String, String> getFailures() {
    return failedIps;
  }
}
