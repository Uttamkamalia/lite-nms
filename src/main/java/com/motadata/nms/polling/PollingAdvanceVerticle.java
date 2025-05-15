package com.motadata.nms.polling;



import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class PollingAdvanceVerticle extends AbstractVerticle {

  private static final int BATCH_SIZE = 100; // Number of devices per batch

  @Override
  public void start() {
    vertx.setPeriodic(1000, id -> { // Polling interval every 1 second
      vertx.eventBus().request("db.getDiscoveredDevices", "", reply -> {
        if (reply.succeeded()) {
          JsonArray devices = (JsonArray) reply.result().body();

          // Break devices into batches
          for (int i = 0; i < devices.size(); i += BATCH_SIZE) {
            JsonArray batch = new JsonArray();
            for (int j = i; j < i + BATCH_SIZE && j < devices.size(); j++) {
              batch.add(devices.getJsonObject(j));
            }

            // Send batch to the worker queue
            JsonObject job = new JsonObject().put("devices", batch);
            vertx.eventBus().send("polling.job.queue", job);
          }
        } else {
          System.err.println("Polling fetch failed: " + reply.cause().getMessage());
        }
      });
    });
  }
}

