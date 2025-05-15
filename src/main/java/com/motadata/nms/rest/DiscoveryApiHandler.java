package com.motadata.nms.rest;


import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DiscoveryApiHandler {
  private static final Logger logger = LoggerFactory.getLogger(DiscoveryApiHandler.class);
  private final Vertx vertx;

  public DiscoveryApiHandler(Vertx vertx) {
    this.vertx = vertx;
  }

  public void registerRoutes(Router router) {
    router.post("/discovery/trigger").handler(this::handleTriggerDiscovery);
    router.get("/discovery/devices").handler(this::handleGetDiscoveredDevices);
  }

  private void handleTriggerDiscovery(RoutingContext ctx) {
    String requestBody = ctx.body().asString();
    vertx.eventBus().request("discovery.trigger", requestBody, reply -> {
      if (reply.succeeded()) {
        logger.info("Discovery triggered successfully");
        ctx.response().end("Discovery triggered");
      } else {
        logger.error("Discovery failed: " + reply.cause().getMessage());
        ctx.response().setStatusCode(500).end("Discovery failed");
      }
    });
  }

  private void handleGetDiscoveredDevices(RoutingContext ctx) {
    vertx.eventBus().request("db.getDiscoveredDevices", "", reply -> {
      if (reply.succeeded()) {
        logger.info("Fetched discovered devices successfully.");
        ctx.response()
          .putHeader("Content-Type", "application/json")
          .end(reply.result().body().toString());
      } else {
        logger.error("Failed to fetch discovered devices: " + reply.cause().getMessage());
        ctx.response().setStatusCode(500).end("Failed to fetch devices");
      }
    });
  }
}

