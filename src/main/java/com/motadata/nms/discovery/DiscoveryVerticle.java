package com.motadata.nms.discovery;

import com.motadata.nms.discovery.context.DiscoveryContext;
import com.motadata.nms.discovery.job.DiscoveryJob;
import com.motadata.nms.discovery.job.DiscoveryJobFactory;
import com.motadata.nms.rest.utils.ErrorCodes;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.motadata.nms.commons.RequestIdHandler.*;
import static com.motadata.nms.datastore.utils.ConfigKeys.*;
import static com.motadata.nms.discovery.context.DiscoveryContext.DISCOVERY_PROFILE_ID;
import static com.motadata.nms.utils.EventBusChannels.*;

public class DiscoveryVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(DiscoveryVerticle.class);
  public static final String DISCOVERY_TRACKERS = "discovery-trackers";

  private final Map<Integer, DiscoveryResultTracker> trackers = new ConcurrentHashMap<>();
  private final Map<Integer, DiscoveryContext> discoveryContextMap = new ConcurrentHashMap<>();
  private int batchSize = 1;
  private int discoveryRequestTimeout = 10;

  @Override
  public void start(Promise<Void> startPromise) {
    batchSize = config().getJsonObject(DISCOVERY).getInteger(DISCOVERY_BATCH_SIZE, 1);
    discoveryRequestTimeout = config().getJsonObject(DISCOVERY).getInteger(DISCOVERY_REQUEST_TIMEOUT, 10);

    registerDiscoveryTriggerConsumer();

    startPromise.complete();
  }

  private void registerDiscoveryTriggerConsumer() {
    vertx.eventBus().<Integer>consumer(DISCOVERY_TRIGGER.name(), discoverProfileIdMsg -> {
      Integer discoveryProfileId = discoverProfileIdMsg.body();

      requestDiscoveryContextBuild(discoveryProfileId, discoverProfileIdMsg);
      discoverProfileIdMsg.reply("Discovery triggered");

//      registerDiscoverySuccessResultConsumer(discoveryProfileId);
//      registerDiscoveryFailureResultConsumer(discoveryProfileId);
    });
  }

  private void requestDiscoveryContextBuild(Integer discoveryProfileId, Message<Integer> discoverProfileIdMsg) {
    String requestId = discoverProfileIdMsg.headers().get(REQUEST_ID_KEY);
    JsonObject request = new JsonObject().put(DISCOVERY_PROFILE_ID, discoveryProfileId);

    vertx.eventBus()
      .request(DISCOVERY_CONTEXT_BUILD.name(), request, getRequestIdDeliveryOpts(requestId), contextBuildReply -> {

        if (contextBuildReply.succeeded()) {
          try {
            JsonObject contextJson = (JsonObject) contextBuildReply.result().body();
            processDiscoveryContext(contextJson);
          } catch (Exception e) {
            logger.error(withRequestId(requestId, "Failed to build discovery-context for discovery-profile-id:" + discoveryProfileId + " due to error:" + e.getMessage()));
            discoverProfileIdMsg.fail(ErrorCodes.INTERNAL_ERROR, contextBuildReply.cause().getMessage());
          }
        } else {
          logger.error(withRequestId(requestId, "Failed to build discovery-context for discovery-profile-id:" + discoveryProfileId + " due to error:" + contextBuildReply.cause().getMessage()));
          discoverProfileIdMsg.fail(ErrorCodes.INTERNAL_ERROR, contextBuildReply.cause().getMessage());
        }
      });
  }

  private void processDiscoveryContext(JsonObject contextJson) {
    DiscoveryContext context = DiscoveryContext.fromJson(contextJson);
    Integer discoveryProfileId = context.getDiscoveryProfileId();

    DiscoveryResultTracker tracker = new DiscoveryResultTracker(discoveryProfileId, context.getTargetIps().size(), DISCOVERY_RESPONSE.withId(discoveryProfileId), discoveryRequestTimeout);
    TrackerStore.getInstance().put(discoveryProfileId, tracker);
//    vertx.sharedData().getLocalMap(DISCOVERY_TRACKERS).put(discoveryProfileId, tracker); not serializable

    registerDiscoverySuccessResultConsumer(discoveryProfileId);
    registerDiscoveryFailureResultConsumer(discoveryProfileId);
    batchAndSendToBatchProcessor(context);
  }

  private void batchAndSendToBatchProcessor(DiscoveryContext context) {
    List<String> targetIps = new ArrayList<>(context.getTargetIps());
    while (!targetIps.isEmpty()) {
      List<String> batch = new ArrayList<>(targetIps.subList(0, Math.min(batchSize, targetIps.size())));
      targetIps.removeAll(batch);
      DiscoveryJob batchJob = DiscoveryJobFactory.create(batch, context);
      vertx.eventBus().send(DISCOVERY_BATCH.name(), batchJob);
    }
  }

  private void registerDiscoverySuccessResultConsumer(Integer discoveryProfileId) {
    DiscoveryResultTracker tracker = TrackerStore.getInstance().get(discoveryProfileId);
    vertx.eventBus().<String>consumer(DISCOVERY_BATCH_RESULT_SUCCESSFUL.withId(discoveryProfileId), successfulIpMsg -> {
      tracker.addSuccess(successfulIpMsg.body());
    });
  }

  private void registerDiscoveryFailureResultConsumer(Integer discoveryProfileId) {
    DiscoveryResultTracker tracker = TrackerStore.getInstance().get(discoveryProfileId);
    vertx.eventBus().<JsonObject>consumer(DISCOVERY_BATCH_RESULT_FAILED.withId(discoveryProfileId), failedIpMsg -> {
      tracker.addFailure(failedIpMsg.body().getString("ip"), failedIpMsg.body().getString("error"));
    });
  }
}
