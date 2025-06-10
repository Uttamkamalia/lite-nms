package com.motadata.nms.discovery.job;

import com.motadata.nms.commons.VertxProvider;
import com.motadata.nms.discovery.DiscoveryResultTracker;
import com.motadata.nms.discovery.DiscoveryResultTrackerRegistry;
import com.motadata.nms.models.ProvisionedDevice;
import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static com.motadata.nms.utils.EventBusChannels.*;

public class DiscoveryBatchExecution {
  private static final Logger log = LoggerFactory.getLogger(DiscoveryBatchExecution.class);
  private static final String PLUGIN_EXECUTION_MODE = "DISCOVERY";
  private static final String PLUGIN_OUTPUT_JSON_FIELD_SUCCESSFUL_DEVICES = "successful";
  private static final String PLUGIN_OUTPUT_JSON_FIELD_FAILED_DEVICES = "failed";

  private DiscoveryJob job;
  private final String pluginExecutableDir;

  private final String inputFile;
  private final String resultFile;

  private final long discoveryBatchTimeout;
  private final DiscoveryResultTracker resultTracker;

  public DiscoveryBatchExecution(DiscoveryJob job, String pluginIODir, String pluginExecutableDir, long discoveryBatchTimeout) {
    this.job = job;
    this.pluginExecutableDir = pluginExecutableDir;
    this.inputFile = pluginIODir + job.getInputFileName();
    this.resultFile = pluginIODir + "discovery-result-" + job.getId() + ".json";
    this.discoveryBatchTimeout = discoveryBatchTimeout;
    this.resultTracker = DiscoveryResultTrackerRegistry.getInstance().get(job.getDiscoveryProfileId());
  }

  public void runDiscoveryBatch(Promise<Object> promise) {
    try {
      String pluginExecutable = pluginExecutableDir + job.getCommand();

      Process process = new ProcessBuilder(pluginExecutable, PLUGIN_EXECUTION_MODE, inputFile, resultFile).start();
      boolean finished = process.waitFor(discoveryBatchTimeout, TimeUnit.MILLISECONDS);

      if (!finished) {
        handleDiscoveryTimeout(process);
      } else {
        processDiscoveryBatchResult(process, job);
      }
      handleCleanup();
      promise.complete();
    } catch (Exception e) {
      promise.fail(e);
    }
  }

  private void handleDiscoveryTimeout(Process process){
    log.error("Timeout occurred while waiting for discovery-profile-id:"+ job.getDiscoveryProfileId()+ " and batch-job-id:"+ job.getId());
    process.destroyForcibly();

    job.getBatch().forEach(ip -> {
      resultTracker.addFailure(ip, "Discovery batch timed out after " + discoveryBatchTimeout + " ms");
    });
  }

  private void processDiscoveryBatchResult(Process process, DiscoveryJob job){
    if(process.exitValue() == 0){
      String resultContent = VertxProvider.getVertx().fileSystem().readFileBlocking(resultFile).toString();
      JsonObject outputJson = new JsonObject(resultContent);
      JsonArray successfulDevices = outputJson.getJsonArray(PLUGIN_OUTPUT_JSON_FIELD_SUCCESSFUL_DEVICES, new JsonArray());
      JsonArray failedDevices = outputJson.getJsonArray(PLUGIN_OUTPUT_JSON_FIELD_FAILED_DEVICES, new JsonArray());

      processSuccessfulDiscoveryBatchResult(successfulDevices, job);

      if(failedDevices != null){
        failedDevices.stream().map(obj -> (JsonObject)obj).forEach(failedDevice -> {
          resultTracker.addFailure(failedDevice.getString("ip"), failedDevice.getString("error"));
        });
      }
    } else {
      try (BufferedReader reader = process.errorReader()) {
        StringBuilder errorContentBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
         errorContentBuilder.append(line);
        }
        String errorContent = errorContentBuilder.toString();
        log.error("Plugin execution failed with exit code: " + process.exitValue() + " and error: " + errorContent);
        job.getBatch().forEach(ip -> {
          resultTracker.addFailure(ip, "Plugin execution failed with exit code: "+ process.exitValue()+ " and error: " +errorContent );
        });

      } catch (IOException e) {
        log.error("Failed to read plugin error stream for discovery-profile-id:" + job.getDiscoveryProfileId() + " and batch-job-id:" + job.getId(), e);
      }
    }
  }

  private void processSuccessfulDiscoveryBatchResult(JsonArray successfulDevices, DiscoveryJob job){
    if(successfulDevices != null && !successfulDevices.isEmpty()){
      successfulDevices.forEach(obj -> {
        JsonObject device = (JsonObject) obj;
        String ip = device.getString("ip");
        Integer port = device.getInteger("port");
        String protocol = device.getString("protocol");
        String pluginResult = device.getJsonObject("metrics").getString("uname");

        // process plugin result
        String hostname = pluginResult;
        String os = pluginResult;

        ProvisionedDevice provisionedDevice = new ProvisionedDevice();
        provisionedDevice.setIpAddress(ip);
        provisionedDevice.setPort(port);
        provisionedDevice.setProtocol(protocol);
        provisionedDevice.setDeviceTypeId(job.getDeviceTypeId());
        provisionedDevice.setDiscoveryProfileId(job.getDiscoveryProfileId());
        provisionedDevice.setCredentialProfileId(job.getCredentialProfileId());
        provisionedDevice.setMetadata(new JsonObject());
        provisionedDevice.setHostname(hostname);
        provisionedDevice.setOs(os);
        provisionedDevice.setOs(os);
        provisionedDevice.setStatus("PROVISIONED");

        // Save the device to the database
        log.info("Trying to save provisioned device: " + provisionedDevice);
        saveProvisionedDevice(provisionedDevice);
      });
    }
  }

  private void saveProvisionedDevice(ProvisionedDevice provisionedDevice){

    VertxProvider.getVertx().eventBus().request(PROVISIONED_DEVICE_SAVE.name(), provisionedDevice, dbRes -> {
      if (dbRes.failed()) {
        // TODO Retry
        String errMsg = "Failed to store provisioned-device after discovery with error: " + dbRes.cause().getMessage();
        log.error(errMsg);
        resultTracker.addFailure(provisionedDevice.getIpAddress(), errMsg);
      } else {
        log.info("Device stored: " + provisionedDevice);
        resultTracker.addSuccess(provisionedDevice.getIpAddress());
        // TODO trigger up/down metric polling
      }
    });
  }

  public void handleCleanup(){
    try {
      Files.deleteIfExists(Paths.get(inputFile));
      Files.deleteIfExists(Paths.get(resultFile));
    } catch (IOException e) {
      log.warn("Failed to delete input file:" + inputFile, e);
    }
  }

  public String getInputFile() {
    return inputFile;
  }
}

