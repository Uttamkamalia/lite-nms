//package com.motadata.nms.discoveryprac;
//
//import com.motadata.nms.discovery.context.DiscoveryContext;
//import com.motadata.nms.discovery.job.DiscoveryJob;
//import com.motadata.nms.discovery.job.DiscoveryJobFactory;
//import com.motadata.nms.models.ProvisionedDevice;
//import com.motadata.nms.rest.utils.ErrorCodes;
//import io.vertx.core.AbstractVerticle;
//import io.vertx.core.Future;
//import io.vertx.core.Promise;
//import io.vertx.core.eventbus.Message;
//import io.vertx.core.json.JsonArray;
//import io.vertx.core.json.JsonObject;
//import io.vertx.core.impl.logging.Logger;;
//import io.vertx.core.impl.logging.LoggerFactory;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//import static com.motadata.nms.datastore.utils.ConfigKeys.DISCOVERY_BATCH_SIZE;
//import static com.motadata.nms.utils.EventBusChannels.DISCOVERY_CONTEXT_BUILD;
//import static com.motadata.nms.utils.EventBusChannels.DISCOVERY_TRIGGER;
//import static java.time.LocalTime.now;
//
//public class DiscoveryVerticle extends AbstractVerticle {
//  private static final Logger logger = LoggerFactory.getLogger(DiscoveryVerticle.class);
//  public static final String DISCOVERY_PROFILE_ID = "discoveryProfileId";
//
//  private final Map<Integer, com.motadata.nms.discoveryprac.DiscoveryResultTracker> trackers = new ConcurrentHashMap<>();
//  private final Map<Integer, DiscoveryContext> discoveryContextMap = new ConcurrentHashMap<>();
//  private int batchSize;
//
//  @Override
//  public void start(Promise<Void> startPromise) {
//    batchSize = config().getInteger(DISCOVERY_BATCH_SIZE, 1);
//
//    vertx.eventBus().consumer(DISCOVERY_TRIGGER.name(), message -> {
//      Integer discoveryProfileId = (Integer) message.body();
//      if (discoveryProfileId == null) {
//        logger.error("Discovery Profile ID cannot be null");
//        message.fail(400, "Discovery Profile ID cannot be null");
//        return;
//      }
//      JsonObject request = new JsonObject().put(DISCOVERY_PROFILE_ID, discoveryProfileId);
//
//      // Get Discovery Context Build , do ping-port check and batch them
//      vertx.eventBus().request(DISCOVERY_CONTEXT_BUILD.name(), request, contextBuildReply -> {
//
//        if (contextBuildReply.succeeded()) {
//          JsonObject contextJson = (JsonObject) contextBuildReply.result().body();
//          processDiscoveryContext(contextJson, message);
//        } else {
//          logger.error("Failed to build discovery context: " + contextBuildReply.cause().getMessage());
//          message.fail(ErrorCodes.INTERNAL_ERROR, contextBuildReply.cause().getMessage());
//        }
//      });
//    });
//
//
//    vertx.eventBus().consumer("discovery.batch.result", message -> {
//      JsonObject result = (JsonObject) message.body();
//      Integer discoveryProfileId = result.getInteger("discoveryProfileId");
//      String batchJobId = result.getString("batchJobId");
//      DiscoveryResultTracker tracker = trackers.get(discoveryProfileId);
//
//      if (tracker != null) {
//        if (result.containsKey("error")) {
//          tracker.addBatchFailure(batchJobId, result.getString("error"));
//        } else {
//          tracker.addBatchResult(batchJobId, result);
//          Future<List<String>> failedIps = saveProvisionedDevices(discoveryContextMap.get(discoveryProfileId), result);
//          failedIps.onSuccess(ips -> {
//            ips.forEach(ip -> tracker.addFailure(ip, "Failed to save device"));
//          });
//        }
//
//        if (tracker.allBatchesProcessed()) {
//          JsonObject finalResult = new JsonObject()
//            .put("discoveryProfileId", discoveryProfileId)
//            .put("success", new JsonArray(tracker.getSuccessfulIps()));
////            .put("failed", new JsonObject(tracker.getFailures())
////            .put("batchResults", new JsonArray(tracker.getSuccessBatchResults()));
//
//          vertx.eventBus().publish(RESULT_RESPONSE_ADDRESS + discoveryProfileId, finalResult);
//          trackers.remove(discoveryProfileId);
//          // on rest api side, wait for this message and return the result.
//        }
//      }
//    });
//    startPromise.complete();
//  }
//
//  /**
//   * Process the discovery context and create jobs
//   * @param contextJson The JSON representation of the discovery context
//   * @param originalMessage The original message to reply to
//   */
//  private void processDiscoveryContext(JsonObject contextJson, Message originalMessage) {
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
//      vertx.eventBus().request(PING_ADDRESS, ip, reply -> {
//        if (reply.succeeded()) {
//
//          boolean pingSuccess = reply.result().body().toString().equalsIgnoreCase("true");
//          if (pingSuccess) {
//
//            vertx.eventBus().request(PORT_CHECK_ADDRESS, port, portReply -> {
//
//              if (portReply.succeeded() && portReply.result().body().toString().equalsIgnoreCase("true")) {
//
//                tracker.addSuccess(ip);
//              } else {
//               tracker.addFailure(ip, "Port check failed");
//              }
//              checkAndBatch(discoveryProfileId);
//            });
//          } else {
//            tracker.addFailure(ip, "Ping failed");
//            checkAndBatch( discoveryProfileId);
//          }
//        } else {
//          tracker.addFailure(ip, "Ping error");
//          checkAndBatch(discoveryProfileId);
//        }
//      });
//    }
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
//  private void checkAndBatch(Integer discoveryId) {
//    DiscoveryResultTracker tracker = trackers.get(discoveryId);
//    if (tracker.isBatchFilled(batchSize)) {
//      List<String> batch = tracker.getSuccessfulBatch(batchSize);
//      DiscoveryJob batchJob = DiscoveryJobFactory.create(batch, discoveryContextMap.get(discoveryId));
//        vertx.eventBus().send(BATCH_PROCESS_ADDRESS, batchJob);
//      }
//    }
//}
