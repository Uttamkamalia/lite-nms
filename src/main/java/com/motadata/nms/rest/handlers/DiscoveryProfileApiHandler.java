package com.motadata.nms.rest.handlers;

import com.motadata.nms.commons.VertxProvider;
import com.motadata.nms.rest.utils.ErrorHandler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import static com.motadata.nms.utils.EventBusChannels.*;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

public class DiscoveryProfileApiHandler {
  private static final Vertx vertx = VertxProvider.getVertx();

  public void registerRoutes(Router router) {
    router.post("/discovery-profile").handler(this::createDiscoveryProfile);
    router.post("/discovery-profile/trigger").handler(this::triggerDiscovery);
    router.get("/discovery-profile/:id").handler(this::getDiscoveryProfile);
    router.get("/discovery-profile").handler(this::getAllDiscoveryProfiles);
    router.delete("/discovery-profile/:id").handler(this::deleteDiscoveryProfile);
  }

  private void createDiscoveryProfile(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    vertx.eventBus()
      .request(DISCOVERY_PROFILE_SAVE.name(), body, reply -> {
        if (reply.succeeded()) {
          ctx.response()
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(201)
            .end(Json.encode(reply.result().body()));
        } else {
          ErrorHandler.respondError(ctx, reply.cause());
        }
      });
  }

  private void getDiscoveryProfile(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("id"));
    vertx.eventBus()
      .request(DISCOVERY_PROFILE_GET.name(), id, reply -> {
        if (reply.succeeded()) {
          ctx.response()
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .end(reply.result().body().toString());
        } else {
          ErrorHandler.respondError(ctx, reply.cause());
        }
      });
  }

  private void getAllDiscoveryProfiles(RoutingContext ctx) {
    vertx.eventBus()
      .request(DISCOVERY_PROFILE_GET_ALL.name(), "", reply -> {
        if (reply.succeeded()) {
          ctx.response()
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .end(reply.result().body().toString());
        } else {
          ErrorHandler.respondError(ctx, reply.cause());
        }
      });
  }

  private void deleteDiscoveryProfile(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("id"));
    vertx.eventBus()
      .request(DISCOVERY_PROFILE_DELETE.name(), id, reply -> {
        if (reply.succeeded()) {
          ctx.response()
            .setStatusCode(204)
            .end();
        } else {
          ErrorHandler.respondError(ctx, reply.cause());
        }
      });
  }

  private void triggerDiscovery(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    Integer discoveryProfileId = body.getInteger("discoveryProfileId");
    vertx.eventBus()
      .request(DISCOVERY_TRIGGER.name(), discoveryProfileId, discoverySummaryReply -> {
        if (discoverySummaryReply.succeeded()) {
          ctx.response()
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .end(discoverySummaryReply.result().body().toString());
        } else {
          ErrorHandler.respondError(ctx, discoverySummaryReply.cause());
        }
      });
  }
}
