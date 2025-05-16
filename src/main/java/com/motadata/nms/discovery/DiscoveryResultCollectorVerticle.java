package com.motadata.nms.discovery;

import com.motadata.nms.models.ProvisionedDevice;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryResultCollectorVerticle extends AbstractVerticle {

  private static final String DISCOVERY_RESULT_CHANNEL = "discovery.result.channel";
  private final List<ProvisionedDevice> successful = new ArrayList<>();
  private final List<JsonObject> failed = new ArrayList<>();

  @Override
  public void start(Promise<Void> startPromise) {
    vertx.eventBus().consumer(DISCOVERY_RESULT_CHANNEL, msg -> {
      JsonObject result = (JsonObject) msg.body();

      if ("FAILED".equals(result.getString("status"))) {
        failed.add(result);
      } else {
        ProvisionedDevice device = ProvisionedDevice.fromJson(result);
        successful.add(device);
        saveToDb(device); // async
      }
    });

    startPromise.complete();
  }

  private void saveToDb(ProvisionedDevice device) {
    // Call DAO for storing to database
  }
}
