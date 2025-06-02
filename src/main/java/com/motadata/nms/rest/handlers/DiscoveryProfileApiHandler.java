package com.motadata.nms.rest.handlers;

import com.motadata.nms.commons.RequestIdHandler;
import com.motadata.nms.commons.VertxProvider;
import com.motadata.nms.models.DiscoveryProfile;
import com.motadata.nms.rest.utils.ErrorHandler;
import com.motadata.nms.rest.utils.RestUtils;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import static com.motadata.nms.commons.RequestIdHandler.getRequestIdDeliveryOpts;
import static com.motadata.nms.rest.utils.RestUtils.parseAndRespond;
import static com.motadata.nms.utils.EventBusChannels.*;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

public class DiscoveryProfileApiHandler {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DiscoveryProfileApiHandler.class);
  Logger logger = LoggerFactory.getLogger(DiscoveryProfileApiHandler.class);
  private static final Vertx vertx = VertxProvider.getVertx();

  public void registerRoutes(Router router) {
    router.post("/discovery-profile").handler(this::createDiscoveryProfile);
    router.post("/discovery-profile/trigger").handler(this::triggerDiscovery);
    router.get("/discovery-profile/:id").handler(this::getDiscoveryProfile);
    router.get("/discovery-profile").handler(this::getAllDiscoveryProfiles);
    router.delete("/discovery-profile/:id").handler(this::deleteDiscoveryProfile);
  }

  private void createDiscoveryProfile(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    DiscoveryProfile discoveryProfile = parseAndRespond(ctx, DiscoveryProfile::fromJson);

    vertx.eventBus()
      .request(DISCOVERY_PROFILE_SAVE.name(), discoveryProfile, getRequestIdDeliveryOpts(requestId), reply -> {
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
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer id = RestUtils.parseAndRespond(ctx, "id", Integer::parseInt, "Discovery-Profile ID cannot be null");

    vertx.eventBus()
      .request(DISCOVERY_PROFILE_GET.name(), id, getRequestIdDeliveryOpts(requestId), reply -> {
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
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);

    vertx.eventBus()
      .request(DISCOVERY_PROFILE_GET_ALL.name(), "", getRequestIdDeliveryOpts(requestId), reply -> {
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
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer id = RestUtils.parseAndRespond(ctx, "id", Integer::parseInt, "Discovery-Profile ID cannot be null");

    vertx.eventBus()
      .request(DISCOVERY_PROFILE_DELETE.name(), id, getRequestIdDeliveryOpts(requestId), reply -> {
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
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);

    JsonObject body = ctx.body().asJsonObject();
    Integer discoveryProfileId = body.getInteger("discovery_profile_id");
    if (discoveryProfileId == null) {
      ErrorHandler.respondError(ctx, new IllegalArgumentException("Discovery-Profile ID cannot be null"));
      return;
    }

    vertx.eventBus()
      .request(DISCOVERY_TRIGGER.name(), discoveryProfileId, getRequestIdDeliveryOpts(requestId), discoverySummaryReply -> {

        if (discoverySummaryReply.succeeded()) {
          logger.info("Discovery trigger with discovery-profile-id:"+discoveryProfileId);
          registerDiscoveryResponseConsumer(ctx, discoveryProfileId);
        } else {
          ErrorHandler.respondError(ctx, discoverySummaryReply.cause());
        }
      });
  }

  private void registerDiscoveryResponseConsumer(RoutingContext ctx, Integer discoveryProfileId) {
    vertx.eventBus().<JsonObject>consumer(DISCOVERY_RESPONSE.withId(discoveryProfileId) , discoveryResultMsg -> {
      JsonObject result = discoveryResultMsg.body();
//      log.debug("Final result: " + result.encodePrettily());

      ctx.response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .end(result.encodePrettily());
    });
  }
}
