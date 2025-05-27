package com.motadata.nms.discovery;

import com.motadata.nms.discovery.context.DiscoveryContext;
import com.motadata.nms.discovery.job.DiscoveryJob;
import com.motadata.nms.discovery.job.DiscoveryJobFactory;

import com.motadata.nms.discoveryprac.DiscoveryResultTracker;
import com.motadata.nms.models.ProvisionedDevice;
import com.motadata.nms.rest.utils.ErrorCodes;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.motadata.nms.commons.RequestIdHandler.*;
import static com.motadata.nms.datastore.utils.ConfigKeys.DISCOVERY;
import static com.motadata.nms.datastore.utils.ConfigKeys.DISCOVERY_BATCH_SIZE;

import static com.motadata.nms.utils.EventBusChannels.*;
import static java.time.LocalTime.now;

public class DiscoveryVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(DiscoveryVerticle.class);
  public static final String DISCOVERY_PROFILE_ID = "discoveryProfileId";


  private final Map<Integer, DiscoveryResultTracker> trackers = new ConcurrentHashMap<>();
  private final Map<Integer, DiscoveryContext> discoveryContextMap = new ConcurrentHashMap<>();
  private int batchSize;

  @Override
  public void start(Promise<Void> startPromise) {
    batchSize = config().getJsonObject(DISCOVERY).getInteger(DISCOVERY_BATCH_SIZE, 1);

    vertx.eventBus().consumer(DISCOVERY_TRIGGER.name(), discoverProfileIdMsg -> {
      String requestId = discoverProfileIdMsg.headers().get(REQUEST_ID_KEY);
        Integer discoveryProfileId = (Integer) discoverProfileIdMsg.body();
        JsonObject request = new JsonObject().put(DISCOVERY_PROFILE_ID, discoveryProfileId);

        vertx.eventBus()
          .request(DISCOVERY_CONTEXT_BUILD.name(), request, getRequestIdDeliveryOpts(requestId), contextBuildReply -> {

          if (contextBuildReply.succeeded()) {
            JsonObject contextJson = (JsonObject) contextBuildReply.result().body();
            processDiscoveryContext(contextJson, discoverProfileIdMsg);
          } else {
            logger.error(withRequestId(requestId,"Failed to build discovery-context for discovery-profile-id:"+discoveryProfileId+" due to error:" + contextBuildReply.cause().getMessage()));
            discoverProfileIdMsg.fail(ErrorCodes.INTERNAL_ERROR, contextBuildReply.cause().getMessage());
          }
        });

      });

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
        if(tracker.allBatchesProcessed()){
          JsonObject finalResult = new JsonObject()
            .put("discoveryProfileId", discoveryProfileId)
            .put("success", new JsonArray((List) tracker.getSuccessBatchResults()));
//            .put("batchResults", new JsonArray(tracker.getSuccessBatchResults()));

          vertx.eventBus().publish("discovery.result." + discoveryProfileId, finalResult);
          trackers.remove(discoveryProfileId);
      }});
    startPromise.complete();
    }

    private void processDiscoveryContext(JsonObject contextJson, Message originalMessage) {
    DiscoveryContext context = DiscoveryContext.fromJson(contextJson);
    discoveryContextMap.put(context.getDiscoveryProfileId(), context);
    batchAndSendToBatchProcessor(context);
  }

    private void batchAndSendToBatchProcessor(DiscoveryContext context){
    DiscoveryResultTracker tracker = new DiscoveryResultTracker(context.getTargetIps().size());
    trackers.put(context.getDiscoveryProfileId(), tracker);

    List<String> targetIps = new ArrayList<>(context.getTargetIps());
    while(!targetIps.isEmpty()){
      List<String> batch = targetIps.subList(0, Math.min(batchSize, targetIps.size()));
      targetIps.removeAll(batch);
      DiscoveryJob batchJob = DiscoveryJobFactory.create(batch, context);
      vertx.eventBus().send(DISCOVERY_BATCH.name(), batchJob);

     logger.info("Batch added with total:"+ tracker.addBatch());
    }
  }
}


//
//vertx.eventBus().consumer("discovery.batch.result-advance", message -> {
//JsonObject result = (JsonObject) message.body();
//Integer discoveryProfileId = result.getInteger("discoveryProfileId");
//String batchJobId = result.getString("batchJobId");
//DiscoveryResultTracker tracker = trackers.get(discoveryProfileId);
//
//      if (tracker != null) {
//  if (result.containsKey("error")) {
//  tracker.addBatchFailure(batchJobId, result.getString("error"));
//  } else {
//  tracker.addBatchResult(batchJobId, result);
//Future<List<String>> failedIps = saveProvisionedDevices(discoveryContextMap.get(discoveryProfileId), result);
//          failedIps.onSuccess(ips -> {
//  ips.forEach(ip -> tracker.addFailure(ip, "Failed to save device"));
//  });
//  }
//
//  if (tracker.allBatchesProcessed()) {
//JsonObject finalResult = new JsonObject()
//  .put("discoveryProfileId", discoveryProfileId)
//  .put("success", new JsonArray(tracker.getSuccessfulIps()));
////            .put("failed", new JsonObject(tracker.getFailures())
////            .put("batchResults", new JsonArray(tracker.getSuccessBatchResults()));
//
//          vertx.eventBus().publish(RESULT_RESPONSE_ADDRESS + discoveryProfileId, finalResult);
//          trackers.remove(discoveryProfileId);
//// on rest api side, wait for this message and return the result.
//        }
//          }
//          });

//

//
//
//  /**
//   * with ping and port check
//   * @param context
//   * @param result
//   * @return
//   */
//
//    // result => {results:[{deviceIp:"192.168.1.1", "output":"os-name"}]}
//    private Future<List<String>> saveProvisionedDevices(DiscoveryContext context, JsonObject result){
//    List<String> failedIps = new ArrayList<>();
//      result.getJsonArray("results").forEach(obj -> {
//        JsonObject device = (JsonObject) obj;
//        String ip = device.getString("deviceIp");
//        String output = device.getString("output");
//
//        // Save the device to the database
//        ProvisionedDevice provisionedDevice = new ProvisionedDevice(
//          ip,
//          context.getPort(),
//          context.getDiscoveryProfileId(),
//          context.getCredentialProfile().getId(),
//          "hostname",
//          "os",
//          context.getCredentialProfile().getCredential().getType().getValue(),
//          "PROVISIONED",
//          now().toString());
//        vertx.eventBus().request("db.saveProvisionedDevice", provisionedDevice.toJson(), dbRes -> {
//          if (dbRes.failed()) {
//            logger.error("Failed to store device: " + dbRes.cause().getMessage());
//            failedIps.add(provisionedDevice.getIpAddress());
//          } else {
//            logger.info("Device stored: " + provisionedDevice);
//            //accumulate success
//          }
//        });
//      });
//
//      return Future.succeededFuture(new ArrayList<>());
//    }
//
//
//
//
//
//
//
//  /**
//   * Process the discovery context and create jobs
//   * @param contextJson The JSON representation of the discovery context
//   * @param originalMessage The original message to reply to
//   */
//  private void processDiscoveryContextWithPingAndIpExecution(JsonObject contextJson, Message originalMessage) {
//    try {
//      // Parse the discovery context
//      DiscoveryContext context = DiscoveryContext.fromJson(contextJson);
//      discoveryContextMap.put(context.getDiscoveryProfileId(), context);
//
//      DiscoveryResultTracker tracker = new DiscoveryResultTracker(context.getTargetIps().size());
//      trackers.put(context.getDiscoveryProfileId(), tracker);
//
//      context.getTargetIps()
//        .stream().forEach(ip -> processPingAndPortCheck(context.getDiscoveryProfileId(),  ip, context.getPort()));
//
//    } catch (Exception e) {
//      logger.error("Error processing discovery context", e);
//      originalMessage.fail(500, "Error processing discovery context: " + e.getMessage());
//    }
//  }
//
//  private void processPingAndPortCheck(Integer discoveryProfileId, String ip, Integer port){
//
//    DiscoveryResultTracker tracker = trackers.get(discoveryProfileId);
//
//    vertx.eventBus().request(PING_ADDRESS, ip, reply -> {
//      if (reply.succeeded()) {
//
//        boolean pingSuccess = reply.result().body().toString().equalsIgnoreCase("true");
//        if (pingSuccess) {
//
//          vertx.eventBus().request(PORT_CHECK_ADDRESS, port, portReply -> {
//
//            if (portReply.succeeded() && portReply.result().body().toString().equalsIgnoreCase("true")) {
//
//              tracker.addSuccess(ip);
//            } else {
//              tracker.addFailure(ip, "Port check failed");
//            }
//            checkAndBatch(discoveryProfileId);
//          });
//        } else {
//          tracker.addFailure(ip, "Ping failed");
//          checkAndBatch( discoveryProfileId);
//        }
//      } else {
//        tracker.addFailure(ip, "Ping error");
//        checkAndBatch(discoveryProfileId);
//      }
//    });
//  }
//
//
//  private void checkAndBatch(Integer discoveryId) {
//    DiscoveryResultTracker tracker = trackers.get(discoveryId);
//    if (tracker.isBatchFilled(batchSize)) {
//      List<String> batch = tracker.getSuccessfulBatch(batchSize);
//      DiscoveryJob batchJob = DiscoveryJobFactory.create(batch, discoveryContextMap.get(discoveryId));
//      vertx.eventBus().send(BATCH_PROCESS_ADDRESS, batchJob);
//    }
//  }
//}
