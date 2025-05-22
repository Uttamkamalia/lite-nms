package com.motadata.nms.discovery;

import com.motadata.nms.commons.VertxProvider;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static com.motadata.nms.utils.EventBusChannels.DISCOVERY_TRIGGER;

public class DiscoveryService {
  private static final Logger logger = LoggerFactory.getLogger(DiscoveryService.class);
  private final Vertx vertx;

  public DiscoveryService() {
    this.vertx = VertxProvider.getVertx();
  }

  public DiscoveryService(Vertx vertx) {
    this.vertx = vertx;
  }

  /**
   * Trigger the discovery process for a discovery profile
   * @param discoveryProfileId The ID of the discovery profile to use
   * @return A Future containing the result of the discovery process
   */
  public Future<JsonObject> discoverDevices(Integer discoveryProfileId) {
    Promise<JsonObject> promise = Promise.promise();

    if (discoveryProfileId == null) {
      return Future.failedFuture("Discovery Profile ID cannot be null");
    }

    logger.info("Initiating discovery for profile ID: " + discoveryProfileId);

    // Send the discovery trigger message to the DiscoveryVerticle
    vertx.eventBus().request(DISCOVERY_TRIGGER.name(), discoveryProfileId, reply -> {
      if (reply.succeeded()) {
        logger.info("Discovery successfully initiated for profile ID: " + discoveryProfileId);
        promise.complete((JsonObject) reply.result().body());
      } else {
        logger.error("Failed to initiate discovery: " + reply.cause().getMessage());
        promise.fail(reply.cause());
      }
    });

    return promise.future();
  }
}
