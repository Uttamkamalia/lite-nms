package com.motadata.nms.polling;

import com.motadata.nms.commons.NMSException;
import com.motadata.nms.commons.VertxProvider;
import com.motadata.nms.models.credential.Credential;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.motadata.nms.utils.EventBusChannels.*;

public class PollingOrchestratorVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(PollingOrchestratorVerticle.class);
  private static final int BATCH_SIZE = 10; // Number of devices per batch
  private static final String POLLING_JOBS_MAP = "polling-jobs";

  @Override
  public void start(Promise<Void> startPromise) {
    // Register event bus consumer for polling trigger
    vertx.eventBus().consumer(METRIC_GROUP_POLLING_TRIGGER.name(), this::handlePollingTrigger);

    logger.info("PollingVerticle started and listening for metric group polling trigger events.");
    startPromise.complete();
  }

  private void handlePollingTrigger(Message<Object> message) {
    Integer metricGroupId;

    try {
      if (message.body() instanceof Integer) {
        metricGroupId = (Integer) message.body();
      } else if (message.body() instanceof String) {
        metricGroupId = Integer.parseInt((String) message.body());
      } else {
        throw new IllegalArgumentException("Invalid metric group ID format");
      }
    } catch (Exception e) {
      logger.error("Failed to parse metric group ID", e);
      message.fail(400, "Invalid metric group ID: " + e.getMessage());
      return;
    }

    logger.info("Received polling trigger for metric group: " + metricGroupId);

    // Build polling context
    buildPollingContext(metricGroupId)
      .onSuccess(pollingContext -> {

        // Process the polling context
        processPollingContext(pollingContext);

        // Reply to the message
        message.reply(new JsonObject()
          .put("status", "success")
          .put("message", "Polling triggered for metric group " + metricGroupId)
          .put("deviceCount", pollingContext.getInteger("deviceCount", 0)));
      })
      .onFailure(err -> {
        logger.error("Failed to build polling context for metric group: " + metricGroupId, err);
        message.fail(500, "Failed to build polling context: " + err.getMessage());
      });
  }

  private Future<JsonObject> buildPollingContext(Integer metricGroupId) {
    String requestId = UUID.randomUUID().toString();
    DeliveryOptions options = new DeliveryOptions().addHeader("requestId", requestId);

    // Get metric group details and devices in a single call
    return vertx.eventBus().<JsonObject>request(METRIC_GROUP_GET_WITH_DEVICES.name(), metricGroupId, options)
      .map(Message::body)
      .compose(result -> {
        if (result == null) {
          return Future.failedFuture(NMSException.notFound("Metric group not found: " + metricGroupId));
        }

        // Add timestamp to the polling context
        result.put("timestamp", Instant.now().toString());

        logger.info("Built polling context for metric group " + metricGroupId +
                   " with " + result.getInteger("deviceCount", 0) + " devices");

        return Future.succeededFuture(result);
      });
  }

  private void processPollingContext(JsonObject pollingContext) {
    JsonArray devices = pollingContext.getJsonArray("devices");
    JsonObject metricGroup = pollingContext.getJsonObject("metricGroup");
    Integer metricGroupId = metricGroup.getInteger("id");
    Integer deviceTypeId = metricGroup.getJsonObject("device_type").getInteger("id");

    if (devices == null || devices.isEmpty()) {
      logger.warn("No devices found for polling metric group: " + metricGroupId);
      return;
    }

    LocalMap<String, JsonObject> pollingJobsMap = getPollingJobsMap(metricGroupId, deviceTypeId);

    // Extract plugin IDs from metrics
    JsonArray metrics = metricGroup.getJsonArray("metrics", new JsonArray());
    List<String> pluginIds = metrics.stream().map(metric ->(JsonObject)metric).map(metric -> metric.getString("plugin_id")).toList();

    // Get polling interval
    Integer pollingInterval = metricGroup.getInteger("polling_interval_seconds", 60); // Add config default
    Integer totalDevices = 0;

    // Batch devices for polling
    List<JsonObject> deviceBatch = new ArrayList<>();
    for (int i = 0; i < devices.size(); i++) {
      JsonObject parsedDevice = parseJobBasedOnProtocol(devices.getJsonObject(i));
      if(parsedDevice != null){
        deviceBatch.add(parsedDevice);
      }

      // When we reach batch size or the end of the list, create and send a batch job
      if (deviceBatch.size() >= BATCH_SIZE || i == devices.size() - 1) {
        // Create a polling job for this batch
        String jobId = UUID.randomUUID().toString();

        // Create job JSON
        JsonObject batchPollingJob = new JsonObject()
          .put("id", jobId)
          .put("metric_group_id", metricGroupId)
          .put("device_type_id", deviceTypeId)
          .put("metric_ids", new JsonArray(pluginIds))
          .put("devices", new JsonArray(deviceBatch));

        // Store the job in shared data
        pollingJobsMap.put(jobId, batchPollingJob);

        // Send the job to the batch processor
        sendPollingBatchForScheduling(jobId, metricGroupId, deviceTypeId, pollingInterval);

        // Reset for next batch
        totalDevices += deviceBatch.size();
        deviceBatch = new ArrayList<>();
      }

//      logger.info("Total devices for polling: " + totalDevices);
    }
  }

  private void sendPollingBatchForScheduling(String jobId, Integer metricGroupId, int deviceTypeId, int pollingIntervalSeconds) {
//    logger.info("Sending polling job " + jobId + " with " + deviceTypeId +
//      " devices for metric group " + metricGroupId);

    // Create batch job message
    JsonObject batchJob = new JsonObject()
      .put("job_id", jobId)
      .put("metric_group_id", metricGroupId)
      .put("device_type_id", deviceTypeId)
      .put("polling_interval_seconds", pollingIntervalSeconds);

    // Send the job ID to the batch processor
    vertx.eventBus().send(METRIC_GROUP_POLLING_SCHEDULE.name(), batchJob);
  }

  public static LocalMap<String, JsonObject> getPollingJobsMap(Integer metricGroupId, int deviceTypeId) {
    String mapName = POLLING_JOBS_MAP + "-" + metricGroupId + "-" + deviceTypeId;
    return VertxProvider.getVertx().sharedData().getLocalMap(mapName);
  }

  private JsonObject parseJobBasedOnProtocol(JsonObject device) {
    if (device == null) {
        logger.error("Device object is null");
        return null;
    }

    String protocol = device.getString("protocol");
    if (protocol == null || protocol.isEmpty()) {
        logger.error("Protocol is missing in device: " + device.encode());
        return null;
    }

    // Create a new device object with the same basic properties
    JsonObject parsedDevice = new JsonObject()
        .put("id", device.getInteger("id"))
        .put("ip", device.getString("ip"))
        .put("port", device.getInteger("port"))
        .put("protocol", protocol);

    // Get credential profile from the device
    JsonObject credentialProfile = device.getJsonObject("credential_profile");
    if (credentialProfile == null) {
        logger.warn("No credential profile found for device: " + device.getInteger("id"));
        return null;
    }

    // Get credentials from the credential profile
    JsonObject credentials = credentialProfile.getJsonObject("credential");
    if (credentials == null) {
        logger.warn("No credentials found in credential profile for device: " + device.getInteger("id"));
        return null;
    }

    // Create credential object based on protocol
    Credential credential = Credential.fromJson(credentials); //TODO error handling here

    // Add credentials to the parsed device
    parsedDevice.put("credential", credential.toJson());

    logger.debug("Parsed device with protocol " + protocol + ": " + parsedDevice.encode());
    return parsedDevice;
  }
}
