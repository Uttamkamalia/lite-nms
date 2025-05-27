//package com.motadata.nms.discoveryprac;
//
//import com.motadata.nms.discovery.job.DiscoveryJob;
//import io.vertx.core.AbstractVerticle;
//import io.vertx.core.Promise;
//import io.vertx.core.json.JsonObject;
//
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.stream.Collectors;
//
//
//
//import io.vertx.core.AbstractVerticle;
//import io.vertx.core.Promise;
//import io.vertx.core.json.JsonObject;
//
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.UUID;
//
//public class BatchProcessorVerticle extends AbstractVerticle {
//  private static final String OUTPUT_DIR = "/tmp/discovery_batches/";
//  private static final String GO_EXECUTABLE_PATH = "/path/to/discovery-checker"; // Replace with real path
//  private static final String RESULT_EVENT_BUS_ADDRESS = "discovery.batch.result";
//
//  @Override
//  public void start(Promise<Void> startPromise) {
//    vertx.eventBus().consumer("discovery.batch", message -> {
//      JsonObject batch = (JsonObject) message.body();
//      DiscoveryJob batchJob = DiscoveryJob.fromJson(batch);
//
//      String fileName = OUTPUT_DIR + batchJob.getInputFileName();
//      String pluginExecutablePath = GO_EXECUTABLE_PATH +"/"+ batchJob.getCommand();
//
//      vertx.<JsonObject>executeBlocking(promise -> {
//        try {
//          Files.createDirectories(Paths.get(OUTPUT_DIR));
//
//          // Write batch file
//          Files.write(Paths.get(fileName), batch.encodePrettily().getBytes());
//
//          // Run Go executable
//          Process process = new ProcessBuilder(pluginExecutablePath, fileName).start();
//          int exitCode = process.waitFor();
//          String output = new String(process.getInputStream().readAllBytes());
//
//          JsonObject result = new JsonObject()
//            .put("discoveryProfileId", batchJob.getDiscoveryProfileId())
//            .put("batchJobId", batchJob.getId())
//            .put("exitCode", exitCode)
//            .put("output", output);
//
//          promise.complete(result);
//        } catch (Exception e) {
//          JsonObject error = new JsonObject()
//            .put("discoveryProfileId", batchJob.getDiscoveryProfileId())
//            .put("batchJobId", batchJob.getId())
//            .put("error", e.getMessage());
//          promise.complete(error);  // complete with error JSON
//        }
//      }, res -> {
//        // Always publish result, even if it's an error object
//        vertx.eventBus().publish(RESULT_EVENT_BUS_ADDRESS, res.result());
//      });
//    });
//
//    startPromise.complete();
//  }
//}
//
//
