package com.motadata.nms.discovery;

import com.motadata.nms.datastore.utils.ConfigKeys;
import com.motadata.nms.discovery.job.DiscoveryJob;
import com.motadata.nms.models.ProvisionedDevice;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static com.motadata.nms.datastore.utils.ConfigKeys.*;
import static com.motadata.nms.utils.EventBusChannels.*;

public class DiscoveryBatchExecutorVerticle extends AbstractVerticle {
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(DiscoveryBatchExecutorVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) {
    String pluginIODir = config().getJsonObject(ConfigKeys.DISCOVERY).getString(DISCOVERY_PLUGIN_IO_DIR);
    String pluginExecutable = config().getJsonObject(ConfigKeys.DISCOVERY).getString(DISCOVERY_PLUGIN_EXECUTABLE);

    vertx.eventBus().consumer(DISCOVERY_BATCH.name(), message -> {
      DiscoveryJob batchJob = (DiscoveryJob) message.body();

      String inputFile = pluginIODir + batchJob.getInputFileName();
      String resultFile = pluginIODir + "discovery-result-"+batchJob.getId()+".json";

      JsonObject result;
      Process process = null;

      try {
        Files.createDirectories(Paths.get(pluginIODir));

        Files.write(Paths.get(inputFile), batchJob.toSerializedJson().getBytes());

        process = new ProcessBuilder(pluginExecutable, "DISCOVERY", inputFile,resultFile).start();
        long discoveryBatchTimeout = config()
          .getJsonObject(ConfigKeys.DISCOVERY)
          .getInteger(DISCOVERY_BATCH_TIMEOUT_MS, 30000);

        boolean finished = process.waitFor(discoveryBatchTimeout, TimeUnit.MILLISECONDS);

        if (!finished) {
          log.error("Timeout occurred while waiting for discovery batch (profileId={}, batchId={})",
            batchJob.getDiscoveryProfileId(), batchJob.getId());

          process.destroyForcibly(); // Kill the hung process

          result = new JsonObject()
            .put("discoveryProfileId", batchJob.getDiscoveryProfileId())
            .put("batchJobId", batchJob.getId())
            .put("error", "Discovery batch timed out after " + discoveryBatchTimeout + " ms")
            .put("failedIps", batchJob.getBatch());
          processFailedDiscoveryBatchResult(result);

        } else {
          int exitCode = process.exitValue();
          String resultContent = Files.readString(Paths.get(resultFile));
          JsonObject outputJson = new JsonObject(resultContent);


          result = new JsonObject()
            .put("discoveryProfileId", batchJob.getDiscoveryProfileId())
            .put("batchJobId", batchJob.getId())
            .put("exitCode", exitCode)
            .put("successfulDevices", outputJson.getJsonArray("successful"))
            .put("failedDevices", outputJson.getJsonArray("failed"));
          log.info("Discovery batch result: {} {}", batchJob.toSerializedJson(),resultContent);

          processDiscoveryBatchResult(result, batchJob);
        }

      } catch (Exception e) {
        log.error("Failed to execute Go plugin or read result", e);
        result = new JsonObject()
          .put("discoveryProfileId", batchJob.getDiscoveryProfileId())
          .put("batchJobId", batchJob.getId())
          .put("error", e.getMessage())
          .put("failedIps", batchJob.getBatch());
        processFailedDiscoveryBatchResult(result);
      } finally {
        try {
          Files.deleteIfExists(Paths.get(inputFile));
          Files.deleteIfExists(Paths.get(resultFile));
        } catch (IOException e) {
          log.warn("Failed to delete input file: {}", inputFile, e);
        }
      }
    });

    startPromise.complete();
  }

  private void processDiscoveryBatchResult(JsonObject result, DiscoveryJob job){
    if(result.getInteger("exitCode") == 0){
      processSuccessfulDiscoveryBatchResult(result, job);

      processFailedDevicesAfterSuccessfulPluginExecution(result);
    } else {
      processFailedDiscoveryBatchCommandResult(job);
    }
  }

  private void processFailedDiscoveryBatchCommandResult(DiscoveryJob job){
      Integer discoveryProfileId = job.getDiscoveryProfileId();
      job.getBatch().forEach(ip -> {
        JsonObject failedDeviceJson = new JsonObject();
        failedDeviceJson.put("ip",ip );
        failedDeviceJson.put("error", "Plugin execution failed");
        failedDeviceJson.put("discovery_profile_id", discoveryProfileId);
        vertx.eventBus().publish(DISCOVERY_BATCH_RESULT_FAILED.withId(discoveryProfileId), failedDeviceJson);
      });
  }

  private void processFailedDevicesAfterSuccessfulPluginExecution(JsonObject result){
    if(result.getJsonArray("failedDevices")==null){
      return;
    }
    Integer discoveryProfileId = result.getInteger("discoveryProfileId");

    JsonArray failedDevices = result.getJsonArray("failedDevices");
    if(failedDevices!=null){
      failedDevices.stream().map(obj -> (JsonObject)obj).forEach(failedDevice -> {
        JsonObject failedDeviceJson = new JsonObject();
        failedDeviceJson.put("ip", failedDevice.getString("ip") );
        failedDeviceJson.put("error", failedDevice.getString("error"));
        failedDeviceJson.put("discovery_profile_id", discoveryProfileId);
        vertx.eventBus().publish(DISCOVERY_BATCH_RESULT_FAILED.withId(discoveryProfileId), failedDeviceJson);
      });
   }
  }

  private void processFailedDiscoveryBatchResult(JsonObject result) {
    Integer discoveryProfileId = result.getInteger("discoveryProfileId");
    String reason = result.getString("error");
    result.getJsonArray("failedIps").stream().forEach(ip -> {
      JsonObject failedIpResult = new JsonObject()
        .put("discovery_profile_id",discoveryProfileId )
        .put("ip", ip)
        .put("error",reason);
      vertx.eventBus().publish(DISCOVERY_BATCH_RESULT_FAILED.withId(discoveryProfileId), failedIpResult);
    });
  }

  private void processSuccessfulDiscoveryBatchResult(JsonObject result, DiscoveryJob job){
    if(result.getJsonArray("successfulDevices")==null){
      return;
    }

    result.getJsonArray("successfulDevices").forEach(obj -> {
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
      provisionedDevice.setDeviceTypeId(job.getDeviceTypeId()); //TODO
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

  private void saveProvisionedDevice(ProvisionedDevice provisionedDevice){

    vertx.eventBus().request(PROVISIONED_DEVICE_SAVE.name(), provisionedDevice, dbRes -> {
      if (dbRes.failed()) {
        String errMsg = "Failed to store provisioned-device after discovery with error: " + dbRes.cause().getMessage();
        log.error(errMsg);
        JsonObject failedIpResult = new JsonObject()
          .put("discovery_profile_id", provisionedDevice.getDiscoveryProfileId())
          .put("ip", provisionedDevice.getId())
          .put("error",errMsg);
        vertx.eventBus().publish(DISCOVERY_BATCH_RESULT_FAILED.withId(provisionedDevice.getDiscoveryProfileId()), failedIpResult);
      } else {
        log.info("Device stored: " + provisionedDevice);

        vertx.eventBus().send(DISCOVERY_BATCH_RESULT_SUCCESSFUL.withId(provisionedDevice.getDiscoveryProfileId()), provisionedDevice.getIpAddress()); //TODO error handling for above statement
      }
    });
  }
}

//{"discovery_profile_id": 1,"discovery_batch_job_id": "0a23ae49-bf55-4d0a-aae6-4fa6ccb52595","metric_ids":["cpu_usage", "memory_usage"],"devices": [{"protocol": "SSH","ip": "127.10.0.5","port": 2222,"credential": {"username": "username","password": "password"}}]}
