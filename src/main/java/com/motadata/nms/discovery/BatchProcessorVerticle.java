package com.motadata.nms.discovery;

import com.motadata.nms.discovery.job.DiscoveryJob;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static com.motadata.nms.utils.EventBusChannels.DISCOVERY_BATCH;
import static com.motadata.nms.utils.EventBusChannels.DISCOVERY_BATCH_RESULT;


public class BatchProcessorVerticle extends AbstractVerticle {
  private static final String OUTPUT_DIR = "/home/uttam-kamalia/Documents/goprac/go-plugin/ssh-plugin/";
  private static final String GO_EXECUTABLE_PATH = "/home/uttam-kamalia/Documents/goprac/go-plugin/ssh-plugin/"; // Replace with real path
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(BatchProcessorVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) {
    vertx.eventBus().consumer(DISCOVERY_BATCH.name(), message -> {
      JsonObject batch = (JsonObject) message.body();
      DiscoveryJob batchJob = DiscoveryJob.fromJson(batch);

      String fileName = OUTPUT_DIR + batchJob.getInputFileName();
      String pluginExecutablePath = GO_EXECUTABLE_PATH +"/"+"ssh_plugin.sh";

      vertx.<JsonObject>executeBlocking(promise -> {
        try {
          Files.createDirectories(Paths.get(OUTPUT_DIR));

          // Write batch file
          Files.write(Paths.get(fileName), batchJob.toSerializedJson().getBytes());

          // Run Go executable
          Process process = new ProcessBuilder(pluginExecutablePath, fileName).start();
          int exitCode = process.waitFor();
          String output = new String(process.getInputStream().readAllBytes());
          log.info("result:"+output);

          JsonObject result = new JsonObject()
            .put("discoveryProfileId", batchJob.getDiscoveryProfileId())
            .put("batchJobId", batchJob.getId())
            .put("exitCode", exitCode)
            .put("output", output);

          promise.complete(result);
        } catch (Exception e) {
          JsonObject error = new JsonObject()
            .put("discoveryProfileId", batchJob.getDiscoveryProfileId())
            .put("batchJobId", batchJob.getId())
            .put("error", e.getMessage());
          promise.complete(error);  // complete with error JSON
        }
      }, res -> {
        // Always publish result, even if it's an error object
        vertx.eventBus().publish(DISCOVERY_BATCH_RESULT.name(), res.result());
      });
    });

    startPromise.complete();
  }
}


