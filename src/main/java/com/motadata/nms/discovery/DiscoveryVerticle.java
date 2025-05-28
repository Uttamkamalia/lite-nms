package com.motadata.nms.discovery;

import com.motadata.nms.discovery.context.DiscoveryContext;
import com.motadata.nms.discovery.job.DiscoveryJob;
import com.motadata.nms.discovery.job.DiscoveryJobFactory;
import com.motadata.nms.discoveryprac.DiscoveryResultTracker;
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
import static com.motadata.nms.datastore.utils.ConfigKeys.DISCOVERY;
import static com.motadata.nms.datastore.utils.ConfigKeys.DISCOVERY_BATCH_SIZE;
import static com.motadata.nms.discovery.context.DiscoveryContext.DISCOVERY_PROFILE_ID;
import static com.motadata.nms.utils.EventBusChannels.*;

public class DiscoveryVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(DiscoveryVerticle.class);

  private final Map<Integer, DiscoveryResultTracker> trackers = new ConcurrentHashMap<>();
  private final Map<Integer, DiscoveryContext> discoveryContextMap = new ConcurrentHashMap<>();
  private int batchSize;

  @Override
  public void start(Promise<Void> startPromise) {
    batchSize = config().getJsonObject(DISCOVERY).getInteger(DISCOVERY_BATCH_SIZE, 1);

    registerDiscoveryTriggerConsumer();
    registerDiscoveryBatchResultConsumer();

    startPromise.complete();
  }

  private void registerDiscoveryTriggerConsumer() {
    vertx.eventBus().<Integer>consumer(DISCOVERY_TRIGGER.name(), discoverProfileIdMsg -> {
      Integer discoveryProfileId = discoverProfileIdMsg.body();

      requestDiscoveryContextBuild(discoveryProfileId, discoverProfileIdMsg);
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

  private void registerDiscoveryBatchResultConsumer() {
    vertx.eventBus().consumer(DISCOVERY_BATCH_RESULT.name(), message -> {
      JsonObject result = (JsonObject) (message.body());
      Integer discoveryProfileId = result.getInteger("discoveryProfileId");
      String batchJobId = result.getString("batchJobId");
      DiscoveryResultTracker tracker = trackers.get(discoveryProfileId);

      if (tracker != null) {
        if (result.containsKey("error")) {
          tracker.addBatchFailure(batchJobId, result.getString("error"));
        } else {
          tracker.addBatchResult(batchJobId, result);
        }
      }
      if (tracker.allBatchesProcessed()) {
        JsonObject finalResult = new JsonObject()
          .put("discoveryProfileId", discoveryProfileId);
//           .put("success", new JsonArray((List) tracker.getSuccessBatchResults()));
//           .put("batchResults", new JsonArray(tracker.getSuccessBatchResults()));

        vertx.eventBus().publish("discovery.result." + discoveryProfileId, finalResult);
        trackers.remove(discoveryProfileId);
      }
    });
  }

  private void processDiscoveryContext(JsonObject contextJson) {
    DiscoveryContext context = DiscoveryContext.fromJson(contextJson);
    discoveryContextMap.put(context.getDiscoveryProfileId(), context);
    batchAndSendToBatchProcessor(context);
  }

  private void batchAndSendToBatchProcessor(DiscoveryContext context) {
    DiscoveryResultTracker tracker = new DiscoveryResultTracker(context.getTargetIps().size());
    trackers.put(context.getDiscoveryProfileId(), tracker);

    List<String> targetIps = new ArrayList<>(context.getTargetIps());
    while (!targetIps.isEmpty()) {
      List<String> batch = new ArrayList<>(targetIps.subList(0, Math.min(batchSize, targetIps.size())));
      targetIps.removeAll(batch);
      DiscoveryJob batchJob = DiscoveryJobFactory.create(batch, context);
      vertx.eventBus().send(DISCOVERY_BATCH.name(), batchJob);

      logger.info("Batch added with total:" + tracker.addBatch());
    }
  }
}
