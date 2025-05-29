package com.motadata.nms.discovery;

import com.motadata.nms.commons.VertxProvider;
import com.motadata.nms.rest.utils.ErrorCodes;
import com.motadata.nms.utils.EventBusChannels;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static com.motadata.nms.utils.EventBusChannels.DISCOVERY_RESPONSE;


public class DiscoveryResultTracker {
  private static final Logger logger = LoggerFactory.getLogger(DiscoveryResultTracker.class);

  private final Integer totalDevices;
  private final Integer discoveryProfileId;
  private final String discoveryResponseEventBusChannel;
  private final Integer discoveryRequestTimeout;
  private final List<String> successfulIps = new CopyOnWriteArrayList<>();
  private final Map<String, String> failedIps = new ConcurrentHashMap<>();


  public DiscoveryResultTracker(Integer discoveryProfileId, Integer totalDevices, String discoveryResponseEventBusChannel, Integer discoveryRequestTimeout) {
    this.discoveryProfileId = discoveryProfileId;
    this.totalDevices = totalDevices;
    this.discoveryResponseEventBusChannel = discoveryResponseEventBusChannel;
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
    failedIps.put(ip, reason);
    if (isResultComplete()) {
      sendDiscoveryResponse();
    }
  }

  public boolean isResultComplete() {
    return successfulIps.size() + failedIps.size() == totalDevices;
  }

  private void sendDiscoveryResponse() {
    JsonObject result = new JsonObject()
      .put("discoveryProfileId", discoveryProfileId)
      .put("success", new JsonArray(successfulIps));

    JsonArray failedIpsWithReason = new JsonArray();
    failedIps
      .entrySet()
      .forEach(entry -> failedIpsWithReason.add(new JsonObject().put("ip", entry.getKey()).put("reason", entry.getValue())));

    result.put("failed", failedIpsWithReason);

    VertxProvider.getVertx().eventBus().send(discoveryResponseEventBusChannel, result);
  }

  private void registerDiscoveryRequestTimeout() {

    VertxProvider.getVertx().timer(discoveryRequestTimeout).onComplete(id -> {
      logger.info("Discovery request timed out with "+discoveryRequestTimeout+"ms for discovery-profile-id:" + discoveryProfileId);
      sendDiscoveryResponse();
    });
  }

  public List<String> getSuccessfulIps() {
    return successfulIps;
  }

  public Map<String, String> getFailures() {
    return failedIps;
  }
}
