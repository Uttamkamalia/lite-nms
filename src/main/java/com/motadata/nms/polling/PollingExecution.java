package com.motadata.nms.polling;

import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class PollingExecution {
  private static final Logger logger = LoggerFactory.getLogger(PollingExecution.class);

  private Process process;
  private JsonObject pollingJob;
  private String pluginExecutable;
  private Integer pollingJobTimeoutMs;


  public PollingExecution(JsonObject pollingJob, String pluginExecutable, Integer pollingJobTimeoutMs) {
    this.pollingJob = pollingJob;
    this.pluginExecutable = pluginExecutable;
    this.pollingJobTimeoutMs = pollingJobTimeoutMs;
  }

  public void execute(Promise<JsonObject> promise) {
    try {
      String encodedJob = pollingJob.encode();
      ProcessBuilder processBuilder = new ProcessBuilder(pluginExecutable, "POLLING" ,  encodedJob);
      processBuilder.redirectErrorStream(true);

      logger.debug("Executing polling plugin: "+  processBuilder.command());
      process = processBuilder.start();

      boolean finished = process.waitFor(pollingJobTimeoutMs, TimeUnit.MILLISECONDS);

      if (!finished) {
        logger.error("Timeout occurred while waiting for polling job:"+encodedJob);
        process.destroyForcibly(); //
      } else {
        int exitCode = process.exitValue();
        String processOutput = fetchProcessOutput(process.getInputStream());
        logger.debug("Polling plugin output: "+ processOutput);
        promise.complete();
      }

    } catch (Exception e) {
      logger.error("Failed to execute polling plugin job: "+ e.getMessage());
      promise.fail(e);
    }
  }
  private static String fetchProcessOutput(InputStream processInputStream) throws IOException {
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
