//package com.motadata.nms.discoveryprac;
//
//import io.vertx.core.AbstractVerticle;
//import io.vertx.core.Promise;
//import io.vertx.core.json.JsonArray;
//import io.vertx.core.json.JsonObject;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.stream.Collectors;
//
//public class DiscoveryOrchestratorVerticle extends AbstractVerticle {
//
//  public static final String PING_ADDRESS = "discovery.ping";
//  public static final String PORT_CHECK_ADDRESS = "discovery.portcheck";
//  public static final String BATCH_PROCESS_ADDRESS = "discovery.batch";
//  public static final String RESULT_RESPONSE_ADDRESS = "discovery.result.";
//
//  private final Map<String, DiscoveryResultTracker> trackers = new ConcurrentHashMap<>();
//
//  @Override
//  public void start(Promise<Void> startPromise) {
//    vertx.eventBus().consumer("discovery.start", message -> {
//      JsonObject payload = (JsonObject) message.body();
//      DiscoveryContext context = payload.mapTo(DiscoveryContext.class);
//      String discoveryId = UUID.randomUUID().toString();
//
//      DiscoveryResultTracker tracker = new DiscoveryResultTracker(context.getIps().size());
//      trackers.put(discoveryId, tracker);
//
//      for (String ip : context.getIps()) {
//        JsonObject ipTask = new JsonObject()
//          .put("ip", ip)
//          .put("discoveryId", discoveryId)
//          .put("username", context.getUsername())
//          .put("password", context.getPassword())
//          .put("port", context.getPort());
//
//        vertx.eventBus().request(PING_ADDRESS, ipTask, reply -> {
//          if (reply.succeeded()) {
//            boolean pingSuccess = reply.result().body().toString().equalsIgnoreCase("true");
//            if (pingSuccess) {
//              vertx.eventBus().request(PORT_CHECK_ADDRESS, ipTask, portReply -> {
//                if (portReply.succeeded() && portReply.result().body().toString().equalsIgnoreCase("true")) {
//                  tracker.addSuccess(ip);
//                } else {
//                  tracker.addFailure(ip, "Port check failed");
//                }
//                checkAndBatch(tracker, discoveryId, context);
//              });
//            } else {
//              tracker.addFailure(ip, "Ping failed");
//              checkAndBatch(tracker, discoveryId, context);
//            }
//          } else {
//            tracker.addFailure(ip, "Ping error");
//            checkAndBatch(tracker, discoveryId, context);
//          }
//        });
//      }
//
//      message.reply(new JsonObject().put("discovery_id", discoveryId));
//    });
//
//    vertx.eventBus().consumer("discovery.batch.result", message -> {
//      JsonObject result = (JsonObject) message.body();
//      Integer discoveryProfileId = result.getInteger("discoveryProfileId");
//      String batchJobId = result.getString("batchJobId");
//
//      DiscoveryResultTracker tracker = trackers.get(discoveryProfileId);
//
//      if (tracker != null) {
//        if (result.containsKey("error")) {
//          tracker.addBatchFailure(batchJobId, result.getString("error"));
//        } else {
//          tracker.addBatchResult(batchIndex, result);
//        }
//
//        if (tracker.allBatchesProcessed()) {
//          JsonObject finalResult = new JsonObject()
//            .put("discoveryProfileId", discoveryId)
//            .put("success", new JsonArray(tracker.getSuccessfulIps()))
//            .put("failed", new JsonObject(tracker.getFailures()))
//            .put("batchResults", new JsonArray(tracker.getBatchResults()));
//
//          vertx.eventBus().publish(RESULT_RESPONSE_ADDRESS + discoveryId, finalResult);
//          trackers.remove(discoveryId);
//          System.out.println("Discovery completed. Final result: " + finalResult.encodePrettily());
//          System.out.println("Total batches: " + tracker.getTotalBatches());
//          System.out.println("Total IPs: " + tracker.getTotalIps());
//          System.out.println("Total batches failed: " + tracker.getBatchFailures().size());
//          System.out.println("Total IPs failed: " + tracker.getFailures().size());
//          System.out.println("Total IPs successful: " + tracker.getSuccessfulIps().size());
//          // on rest api side, wait for this message and return the result.
//        }
//      }
//    });
//
//    startPromise.complete();
//  }
//
//  private void checkAndBatch(DiscoveryResultTracker tracker, String discoveryId, DiscoveryContext context) {
//    if (tracker.isDeviceCheckComplete()) {
//      List<String> successfulIps = tracker.getSuccessfulIps();
//      List<List<String>> batches = batchList(successfulIps, 10);
//
//      tracker.setTotalBatches(batches.size());
//
//      for (int i = 0; i < batches.size(); i++) {
//        List<String> batch = batches.get(i);
//        JsonObject batchFile = new JsonObject()
//          .put("discoveryId", discoveryId)
//          .put("batchIndex", i)
//          .put("ips", new JsonArray(batch))
//          .put("username", context.getUsername())
//          .put("password", context.getPassword());
//
//        vertx.eventBus().send(BATCH_PROCESS_ADDRESS, batchFile);
//      }
//    }
//  }
//
//  private List<List<String>> batchList(List<String> inputList, int batchSize) {
//    List<List<String>> batches = new ArrayList<>();
//    for (int i = 0; i < inputList.size(); i += batchSize) {
//      batches.add(inputList.subList(i, Math.min(i + batchSize, inputList.size())));
//    }
//    return batches;
//  }
//
//  public Map<String, DiscoveryResultTracker> getTrackers() {
//    return trackers;
//  }
//}
