package com.motadata.nms.discovery;

import com.motadata.nms.discovery.context.DiscoveryContext;
import com.motadata.nms.discovery.job.DiscoveryJob;
import com.motadata.nms.discovery.job.DiscoveryJobFactory;
import com.motadata.nms.discovery.job.SnmpDiscoveryJob;
import com.motadata.nms.discovery.job.SshDiscoveryJob;
import com.motadata.nms.rest.utils.ErrorCodes;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.motadata.nms.datastore.utils.ConfigKeys.DISCOVERY_BATCH_SIZE;
import static com.motadata.nms.utils.EventBusChannels.*;

public class DiscoveryVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(DiscoveryVerticle.class);
  public static final String DISCOVERY_PROFILE_ID = "discoveryProfileId";

  private int batchSize;

  @Override
  public void start(Promise<Void> startPromise) {
    batchSize = config().getInteger(DISCOVERY_BATCH_SIZE, 10);

    vertx.eventBus().consumer(DISCOVERY_TRIGGER.name(), message -> {
      Integer discoveryProfileId = (Integer) message.body();

      if (discoveryProfileId == null) {
        logger.error("Discovery Profile ID cannot be null");
        message.fail(400, "Discovery Profile ID cannot be null");
        return;
      }

      logger.info("Triggering discovery for profile ID: " + discoveryProfileId);

      JsonObject request = new JsonObject().put(DISCOVERY_PROFILE_ID, discoveryProfileId);

      vertx.eventBus().request(DISCOVERY_CONTEXT_BUILD.name(), request, contextBuildReply -> {

        if (contextBuildReply.succeeded()) {
          JsonObject contextJson = (JsonObject) contextBuildReply.result().body();
          processDiscoveryContext(contextJson, message);
        } else {
          logger.error("Failed to build discovery context: " + contextBuildReply.cause().getMessage());
          message.fail(ErrorCodes.INTERNAL_ERROR, contextBuildReply.cause().getMessage());
        }
      });
    });

    startPromise.complete();
  }

  /**
   * Process the discovery context and create jobs
   * @param contextJson The JSON representation of the discovery context
   * @param originalMessage The original message to reply to
   */
  private void processDiscoveryContext(JsonObject contextJson, Message originalMessage) {
    try {
      // Parse the discovery context
      DiscoveryContext context = DiscoveryContext.fromJson(contextJson);

      logger.info("Processing discovery context for profile ID: " + context.getDiscoveryProfileId());
      logger.info("Creating discovery jobs for " + context.getTargetIps().size() + " IPs");

      AtomicInteger totalJobs = new AtomicInteger();
      List<DiscoveryJob> jobs = DiscoveryJobFactory.createBatchedJobs(context, batchSize);
      jobs.parallelStream().forEach(job -> {
        sendJobsToEventBus(job);
        totalJobs.incrementAndGet();
      });

      logger.info("Created " + totalJobs + " discovery jobs for profile ID: " + context.getDiscoveryProfileId());

      // Reply to the original message with success
      originalMessage.reply(new JsonObject()
        .put("status", "success")
        .put("message", "Discovery started for profile ID: " + context.getDiscoveryProfileId())
        .put("jobsCreated", totalJobs));

    } catch (Exception e) {
      logger.error("Error processing discovery context", e);
      originalMessage.fail(500, "Error processing discovery context: " + e.getMessage());
    }
  }

  private void sendJobsToEventBus(DiscoveryJob job){
    if(job instanceof SnmpDiscoveryJob){
      vertx.eventBus().send(DISCOVERY_JOBS_SNMP.name(), job.toJson());
    }else if(job instanceof SshDiscoveryJob){
      vertx.eventBus().send(DISCOVERY_JOBS_SSH.name(), job.toJson());
    }
  }
}
