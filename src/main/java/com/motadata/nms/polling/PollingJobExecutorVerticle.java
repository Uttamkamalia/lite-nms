package com.motadata.nms.polling;

import com.motadata.nms.datastore.utils.ConfigKeys;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static com.motadata.nms.datastore.utils.ConfigKeys.*;
import static com.motadata.nms.utils.EventBusChannels.*;

public class PollingJobExecutorVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(PollingJobExecutorVerticle.class);

    private Integer pollingJobTimeoutMs;
    private String pluginExecutable;

    @Override
    public void start(Promise<Void> startPromise) {
      pollingJobTimeoutMs = config().getJsonObject(ConfigKeys.POLLING).getInteger(POLLING_JOB_TIMEOUT_MS, 3000);
      pluginExecutable = config().getJsonObject(ConfigKeys.POLLING).getString(POLLING_PLUGIN_EXECUTABLE);

      registerProcessExecutor();

      logger.info("PollingJobExecutorVerticle started");
      startPromise.complete();
    }

    private void registerProcessExecutor() {
      vertx.eventBus().consumer(METRIC_POLLER_EXECUTE.name(), message -> {
        JsonObject pollingJob = (JsonObject) message.body();

        String jobId = pollingJob.getString("id");
        logger.debug("Executing polling job: {}", jobId);

        Process process = null;
        try {
          String encodedJob = pollingJob.encode();
          ProcessBuilder processBuilder = new ProcessBuilder(pluginExecutable, "POLLING" ,  encodedJob);
          processBuilder.redirectErrorStream(true);

          logger.debug("Executing polling plugin: {}", String.join(" ", processBuilder.command()));
          process = processBuilder.start();

          boolean finished = process.waitFor(pollingJobTimeoutMs, TimeUnit.MILLISECONDS);

          if (!finished) {
            logger.error("Timeout occurred while waiting for polling job (jobId={})", jobId);
            process.destroyForcibly(); //
          } else {
            int exitCode = process.exitValue();
            String processOutput = fetchProcessOutput(process.getInputStream());
            logger.debug("Polling plugin output: {}", processOutput);
          }

        } catch (Exception e) {
          logger.error("Failed to parse plugin output as JSON: {}", e.getMessage());
        }
      });
    }

    private String fetchProcessOutput(InputStream processInputStream) throws IOException {
      StringBuilder output = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(processInputStream))) {
        String line;
        while ((line = reader.readLine()) != null) {
          output.append(line).append("\n");
        }
      }
      return output.toString();
    }
}
