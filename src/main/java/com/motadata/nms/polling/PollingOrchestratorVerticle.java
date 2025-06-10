package com.motadata.nms.polling;

import com.motadata.nms.commons.NMSException;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.motadata.nms.commons.SharedMapUtils.getPollingJobsMap;
import static com.motadata.nms.datastore.utils.ConfigKeys.*;
import static com.motadata.nms.utils.EventBusChannels.*;

public class PollingOrchestratorVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(PollingOrchestratorVerticle.class);

  private static  int pollingBatchSize = 10; // Number of devices per batch

  @Override
  public void start(Promise<Void> startPromise) {
    pollingBatchSize = config().getJsonObject(POLLING).getInteger(POLLING_BATCH_SIZE);

    vertx.eventBus().consumer(METRIC_GROUP_POLLING_TRIGGER.name(), this::handlePollingTrigger);

    logger.info("PollingOrchestratorVerticle started and listening for metric group polling trigger events.");

    startPromise.complete();
  }

  private void handlePollingTrigger(Message<Object> message) {
    Integer metricGroupId = (Integer) message.body();

    logger.info("Received polling trigger for metric group: " + metricGroupId);

    buildPollingContext(metricGroupId)
      .onSuccess(pollingContext -> {

        processPollingContext(pollingContext);

        message.reply(new JsonObject()
          .put("status", "success")
          .put("message", "Polling triggered for metric group " + metricGroupId)
          .put("device_count", pollingContext.getInteger("deviceCount", 0)));
      })
      .onFailure(err -> {
        logger.error("Failed to build polling context for metric group: " + metricGroupId, err);
        message.fail(500, "Failed to build polling context: " + err.getMessage());
      });
  }

  private Future<JsonObject> buildPollingContext(Integer metricGroupId) {
    String requestId = UUID.randomUUID().toString();
    DeliveryOptions options = new DeliveryOptions().addHeader("requestId", requestId);

    return vertx.eventBus().<JsonObject>request(METRIC_GROUP_GET_WITH_DEVICES.name(), metricGroupId, options)
      .map(Message::body)
      .compose(result -> {
        if (result == null) {
          return Future.failedFuture(NMSException.notFound("Metric group not found: " + metricGroupId));
        }

        logger.info("Built polling context for metric group " + metricGroupId +
                   " with " + result.getInteger("device_count", 0) + " devices");

        return Future.succeededFuture(result);
      });
  }

  private void processPollingContext(JsonObject pollingContext) {
    JsonArray devices = pollingContext.getJsonArray("devices");
    JsonObject metricGroup = pollingContext.getJsonObject("metric_group");
    Integer metricGroupId = metricGroup.getInteger("id");
    Integer deviceTypeId = metricGroup.getJsonObject("device_type").getInteger("id");

    if (devices == null || devices.isEmpty()) {
      logger.warn("No devices found for polling metric group: " + metricGroupId + " .Skipping further polling");
      return;
    }

    LocalMap<String, JsonObject> pollingJobsMap = getPollingJobsMap(metricGroupId, deviceTypeId);

    JsonArray metrics = metricGroup.getJsonArray("metrics", new JsonArray());
    List<String> pluginIds = metrics
      .stream()
      .map(metric ->(JsonObject)metric)
      .map(metric -> metric.getString("plugin_id"))
      .toList();

    Integer pollingInterval = metricGroup.getInteger("polling_interval_seconds", 60); // Add config default
    Integer totalDevices = 0;

    List<JsonObject> deviceBatch = new ArrayList<>();
    for (int i = 0; i < devices.size(); i++) {
      JsonObject parsedDevice = parseJobBasedOnProtocol(devices.getJsonObject(i));
      if(parsedDevice != null){
        deviceBatch.add(parsedDevice);
      }

      if (deviceBatch.size() >= pollingBatchSize || i == devices.size() - 1) {
        String jobId = UUID.randomUUID().toString();
        JsonObject batchPollingJob = new JsonObject()
          .put("job_id", jobId)
          .put("metric_group_id", metricGroupId)
          .put("device_type_id", deviceTypeId)
          .put("metric_ids", new JsonArray(pluginIds))
          .put("devices", new JsonArray(deviceBatch));

        pollingJobsMap.put(jobId, batchPollingJob);

        sendPollingBatchForScheduling(jobId, metricGroupId, deviceTypeId, pollingInterval);

        totalDevices += deviceBatch.size();
        deviceBatch = new ArrayList<>();
      }

      logger.debug("Total devices for polling: " + totalDevices);
    }
  }

  private void sendPollingBatchForScheduling(String jobId, Integer metricGroupId, int deviceTypeId, int pollingIntervalSeconds) {
    logger.debug("Sending polling job " + jobId + " with " + deviceTypeId +
      " devices for metric group " + metricGroupId);

    JsonObject batchJob = new JsonObject()
      .put("job_id", jobId)
      .put("metric_group_id", metricGroupId)
      .put("device_type_id", deviceTypeId)
      .put("polling_interval_seconds", pollingIntervalSeconds);

    vertx.eventBus().send(METRIC_GROUP_POLLING_SCHEDULE.name(), batchJob);
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

    JsonObject parsedDevice = new JsonObject()
        .put("device_id", device.getInteger("id"))
        .put("ip", device.getString("ip"))
        .put("port", device.getInteger("port"))
        .put("protocol", protocol);

    JsonObject credentialProfile = device.getJsonObject("credential_profile");
    if (credentialProfile == null) {
        logger.warn("No credential profile found for device: " + device.getInteger("id"));
        return null;
    }

    JsonObject credentials = credentialProfile.getJsonObject("credential");
    if (credentials == null) {
        logger.warn("No credentials found in credential profile for device: " + device.getInteger("id"));
        return null;
    }

    Credential credential = Credential.fromJson(credentials); //TODO error handling here
    parsedDevice.put("credential", credential.toJson());

    logger.debug("Parsed device with protocol " + protocol + ": " + parsedDevice.encode());
    return parsedDevice;
  }
}
