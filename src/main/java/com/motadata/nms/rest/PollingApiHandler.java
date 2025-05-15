package com.motadata.nms.rest;


import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;

public class PollingApiHandler {
  private final Vertx vertx;

  public PollingApiHandler(Vertx vertx) {
    this.vertx = vertx;
  }

  public void registerRoutes(Router router) {
    router.post("/polling/trigger").handler(this::handleTriggerPolling);
  }

  private void handleTriggerPolling(RoutingContext ctx) {
    String requestBody = ctx.body().asString();
    JsonObject payload = new JsonObject(requestBody);

    vertx.eventBus().request("polling.trigger", payload, reply -> {
      if (reply.succeeded()) {
        ctx.response().end("Polling started");
      } else {
        ctx.response().setStatusCode(500).end("Polling failed");
      }
    });
  }
}

