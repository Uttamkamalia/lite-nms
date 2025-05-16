package com.motadata.nms.discovery;

import com.motadata.nms.models.ProvisionedDevice;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

public class DiscoveryJobWorkerVerticle extends AbstractVerticle {

  private static final String DISCOVERY_JOB_CHANNEL = "discovery.job.channel";
  private static final String DISCOVERY_RESULT_CHANNEL = "discovery.result.channel";
  private static final int TIMEOUT = 3000; // ms

  @Override
  public void start(Promise<Void> startPromise) {
    vertx.eventBus().consumer(DISCOVERY_JOB_CHANNEL, msg -> {
      DiscoveryJob job = DiscoveryJob.fromJson((JsonObject) msg.body());

      performDiscovery(job)
        .onSuccess(result -> vertx.eventBus().send(DISCOVERY_RESULT_CHANNEL, result.toJson()))
        .onFailure(err -> {
          JsonObject fail = new JsonObject()
            .put("ip", job.ip())
            .put("status", "FAILED")
            .put("reason", err.getMessage());
          vertx.eventBus().send(DISCOVERY_RESULT_CHANNEL, fail);
        });
    });

    startPromise.complete();
  }

  private Future<ProvisionedDevice> performDiscovery(DiscoveryJob job) {
    Promise<ProvisionedDevice> promise = Promise.promise();

    vertx.setTimer(TIMEOUT, t -> promise.fail("Timeout"));

    // Simulated example: replace with real implementations
    ping(job.ip()).compose(pingSuccess ->
        checkPort(job.ip(), job.port())
      ).compose(portOpen -> {
        if (job.port() == 161) {
          return snmpCheck(job); // Replace with real SNMP logic
        } else if (job.port() == 22) {
          return sshCheck(job); // Replace with real SSH logic
        } else {
          return Future.failedFuture("Unsupported port");
        }
      }).onSuccess(promise::complete)
      .onFailure(promise::fail);

    return promise.future();
  }

  private Future<Void> ping(String ip) {
    // Dummy ping logic, replace with real
    return Future.succeededFuture();
  }

  private Future<Void> checkPort(String ip, int port) {
    // Dummy port check
    return Future.succeededFuture();
  }

  private Future<ProvisionedDevice> snmpCheck(DiscoveryJob job) {
    // Dummy SNMP response
    return Future.succeededFuture(new ProvisionedDevice(job.ip(), job.port(), job.discoveryProfileId(), job.credentialsProfileId(), "snmp-host", "Linux", "SNMP", "SUCCESS", Instant.now().toString()));
  }

  private Future<ProvisionedDevice> sshCheck(DiscoveryJob job) {
    return Future.succeededFuture(new ProvisionedDevice(job.ip(), job.port(), job.discoveryProfileId(), job.credentialsProfileId(), "ssh-host", "Linux", "SSH", "SUCCESS", Instant.now().toString()));
  }
}
