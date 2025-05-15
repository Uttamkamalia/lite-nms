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

public class CredentialProfileApiHandler {

  private static final Vertx vertx = VertxProvider.getVertx();

  public void registerRoutes(Router router) {
    router.post("/credential-profile").handler(this::createCredentialProfile);
    router.get("/credential-profile/:id").handler(this::getCredentialProfile);
    router.get("/credential-profile").handler(this::getAllCredentialProfiles);
    router.put("/credential-profile/:id").handler(this::updateCredentialProfile);
    router.delete("/credential-profile/:id").handler(this::deleteCredentialProfile);
  }

  private void createCredentialProfile(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    vertx.eventBus().request(CREDENTIAL_PROFILE_SAVE.name(), body, reply -> {
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

  private void getCredentialProfile(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("id"));

    vertx.eventBus().request(CREDENTIAL_PROFILE_GET.name(), id, reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .end(reply.result().body().toString());
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }

  private void getAllCredentialProfiles(RoutingContext ctx) {
    vertx.eventBus().request(CREDENTIAL_PROFILE_GET_ALL.name(), "", reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .end(reply.result().body().toString());
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }

  private void updateCredentialProfile(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    vertx.eventBus().request(CREDENTIAL_PROFILE_UPDATE.name(), body, reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .end(Json.encode(reply.result().body()));
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }

  private void deleteCredentialProfile(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("id"));

    vertx.eventBus().request(CREDENTIAL_PROFILE_DELETE.name(), id, reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .end(Json.encode(reply.result().body()));
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }
}
