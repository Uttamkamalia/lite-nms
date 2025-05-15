package com.motadata.nms.discovery;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;


public class DiscoveryService {
  private final Vertx vertx;

  public DiscoveryService(Vertx vertx) {
    this.vertx = vertx;
  }

  public Future<String> discoverDevices(String discoveryPayload) {
    Promise<String> promise = Promise.promise();
    JsonObject json = new JsonObject(discoveryPayload);

    String ip = json.getString("ip");
    String protocol = json.getString("protocol");
    String snmpCommunity = json.getString("snmp_community", "public");
    String sshUser = json.getString("ssh_user");
    String sshPassword = json.getString("ssh_password");



    vertx.executeBlocking(future -> {
      try {
        if ("snmp".equalsIgnoreCase(protocol)) {
          JsonObject discoveryObj = new JsonObject();

          discoveryObj.put("ip_address", ip);
          discoveryObj.put("hostname", ip);
          discoveryObj.put("os", "routerOs");
          discoveryObj.put("device_type", "router");
          discoveryObj.put("discovery_id", 123);

          vertx.eventBus().request("db.saveDiscovery", discoveryObj);

        } else if ("ssh".equalsIgnoreCase(protocol)) {

          System.out.println("SSH Response: ");

        } else {
          throw new IllegalArgumentException("Unsupported protocol: " + protocol);
        }
        future.complete();
      } catch (Exception e) {
        future.fail(e);
      }
    }, res -> {
      if (res.succeeded()) {
        promise.complete(ip);
      } else {
        promise.fail(res.cause());
      }
    });

    return promise.future();
  }
}

