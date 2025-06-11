package com.motadata.nms.discovery;

import com.motadata.nms.commons.VertxProvider;
import com.motadata.nms.datastore.utils.ConfigKeys;
import com.motadata.nms.discovery.context.DiscoveryContext;
import com.motadata.nms.discovery.job.DiscoveryBatchExecution;
import com.motadata.nms.discovery.job.DiscoveryJob;
import com.motadata.nms.discovery.job.DiscoveryJobFactory;
import com.motadata.nms.rest.utils.ErrorCodes;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static com.motadata.nms.commons.RequestIdHandler.*;
import static com.motadata.nms.datastore.utils.ConfigKeys.*;
import static com.motadata.nms.discovery.context.DiscoveryContext.DISCOVERY_PROFILE_ID;
import static com.motadata.nms.utils.EventBusChannels.*;

public class DiscoveryVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(DiscoveryVerticle.class);

  private int batchSize = 1;
  private int discoveryRequestTimeout = 10;
  private String pluginIODir;
  private String pluginExecutableDir;
  private long discoveryBatchTimeout;

  @Override
  public void start(Promise<Void> startPromise) {
    batchSize = config().getJsonObject(DISCOVERY).getInteger(DISCOVERY_BATCH_SIZE, 1);
    discoveryRequestTimeout = config().getJsonObject(DISCOVERY).getInteger(DISCOVERY_REQUEST_TIMEOUT, 10);
    pluginIODir = config().getJsonObject(ConfigKeys.DISCOVERY).getString(DISCOVERY_PLUGIN_IO_DIR);
    pluginExecutableDir = config().getJsonObject(ConfigKeys.DISCOVERY).getString(DISCOVERY_PLUGIN_EXECUTABLE_DIR);
    discoveryBatchTimeout = config().getJsonObject(ConfigKeys.DISCOVERY).getInteger(DISCOVERY_BATCH_TIMEOUT_MS, 30000);

    registerExceptionHandler();

    registerDiscoveryTriggerConsumer();

    startPromise.complete();
  }

  private void registerDiscoveryTriggerConsumer() {
    vertx.eventBus().<Integer>consumer(DISCOVERY_TRIGGER.name(), discoverProfileIdMsg -> {
      Integer discoveryProfileId = discoverProfileIdMsg.body();
      if(discoveryProfileId != null){
        requestDiscoveryContextBuild(discoveryProfileId, discoverProfileIdMsg);
        discoverProfileIdMsg.reply("Discovery triggered");
      }
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
            discoverProfileIdMsg.fail(ErrorCodes.INTERNAL_ERROR, e.getMessage());
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

    DiscoveryResultTracker tracker = new DiscoveryResultTracker(discoveryProfileId, context.getTargetIps().size(), discoveryRequestTimeout);
    DiscoveryResultTrackerRegistry.getInstance().put(discoveryProfileId, tracker);

    batchAndSendForExecution(context);
  }

  private void batchAndSendForExecution(DiscoveryContext context) {
    List<String> targetIps = new ArrayList<>(context.getTargetIps());
    while (!targetIps.isEmpty()) {
      List<String> batch = new ArrayList<>(targetIps.subList(0, Math.min(batchSize, targetIps.size())));
      targetIps.removeAll(batch);
      DiscoveryJob batchJob = DiscoveryJobFactory.create(batch, context);
      submitDiscoveryBatchForExecution(batchJob);
    }
  }

  private void submitDiscoveryBatchForExecution(DiscoveryJob batchJob) {
    DiscoveryBatchExecution batchExecution = new DiscoveryBatchExecution(batchJob, pluginIODir, pluginExecutableDir, discoveryBatchTimeout);

    VertxProvider.getVertx()
      .fileSystem()
      .writeFile(batchExecution.getInputFile(), Buffer.buffer(batchJob.toSerializedJson()))
      .onFailure(cause -> {
        logger.error("Failed to write input file:"+ batchExecution.getInputFile(), cause);
      })
      .onSuccess(v -> {
        vertx.executeBlocking(batchExecution::runDiscoveryBatch, false, res -> {
          if (res.failed()) {
            logger.error("Failed to execute discovery plugin for discovery-profile-id: " + batchJob.getDiscoveryProfileId() + " and batch-job-id: " + batchJob.getId() + " with error: ", res.cause());
          }
        });
      });
  }

  private void registerExceptionHandler() {
    vertx.getOrCreateContext().exceptionHandler(cause -> {
      logger.error("Discovery Verticle context exception handler: " + cause.getMessage(), cause);
    });
  }
}
