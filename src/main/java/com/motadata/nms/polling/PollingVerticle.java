package com.motadata.nms.polling;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.impl.logging.Logger;;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.core.eventbus.Message;

public class PollingVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(PollingVerticle.class);

  @Override
  public void start() {
    vertx.eventBus().consumer("polling.trigger", this::handlePollingRequest);
    logger.info("PollingVerticle started and listening for polling trigger events.");
  }

  private void handlePollingRequest(Message<JsonObject> message) {
    JsonObject request = message.body();
    String deviceId = request.getString("deviceId");
    String metricName = request.getString("metricName");
    int interval = request.getInteger("interval", 60); // Default 60 seconds interval

    logger.info("Starting polling for device " + deviceId + " with metric " + metricName + " every " + interval + " seconds.");

    vertx.setPeriodic(interval * 1000L, id -> {
      pollDeviceMetrics(deviceId, metricName);
    });
    message.reply("Polling started");
  }

  private void pollDeviceMetrics(String deviceId, String metricName) {
    try {
      // Simulate fetching metrics from the device, e.g., using SNMP/SSH or another protocol
      JsonObject metric = new JsonObject()
        .put("deviceId", deviceId)
        .put("metricName", metricName)
        .put("metricValue", Math.random() * 100) // Simulated metric
        .put("timestamp", System.currentTimeMillis());

      vertx.eventBus().request("db.saveMetric", metric, dbRes -> {
        if (dbRes.failed()) {
          logger.error("Failed to store metric: " + dbRes.cause().getMessage());
        } else {
          logger.info("Metric stored: " + metric.toString());
        }
      });
    } catch (Exception e) {
      logger.error("Error during metric polling for device " + deviceId, e);
    }
  }
}
