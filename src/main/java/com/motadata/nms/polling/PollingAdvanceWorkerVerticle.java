package com.motadata.nms.polling;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class PollingAdvanceWorkerVerticle extends AbstractVerticle {

  @Override
  public void start() {
    vertx.eventBus().consumer("polling.job.queue", message -> {
      JsonObject job = (JsonObject) message.body();
      JsonArray devices = job.getJsonArray("devices");

      // Process each device in the batch
      for (int i = 0; i < devices.size(); i++) {
        JsonObject device = devices.getJsonObject(i);
        String ip = device.getString("ip_address");
        String protocol = device.getString("device_type");

        System.out.println("Polling job received for: " + ip + " via " + protocol);

        // Simulate polling based on protocol
        if ("SNMP".equalsIgnoreCase(protocol)) {
          simulateSnmpPoll(ip);
        } else if ("SSH".equalsIgnoreCase(protocol)) {
          simulateSshPoll(ip);
        } else {
          System.err.println("Unknown protocol: " + protocol);
        }
      }

      message.reply("Polling completed for batch");
    });
  }

  private void simulateSnmpPoll(String ip) {
    System.out.println("Polling SNMP metrics for " + ip);
    // Simulate SNMP call
  }

  private void simulateSshPoll(String ip) {
    System.out.println("Polling SSH metrics for " + ip);
    // Simulate SSH call
  }
}

