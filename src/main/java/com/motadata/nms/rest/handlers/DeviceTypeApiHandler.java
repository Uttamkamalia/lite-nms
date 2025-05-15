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

public class DeviceTypeApiHandler {
  private static final Vertx vertx = VertxProvider.getVertx();

  public void registerRoutes(Router router) {
    router.post("/device-catalog").handler(this::createDeviceCatalog);

    router.get("/device-catalog/:id").handler(this::getDeviceCatalog);

    router.get("/device-catalog/").handler(this::getAllDeviceCatalog);

    router.delete("/device-catalog/:id").handler(this::deleteDeviceCatalog);
  }

  private void createDeviceCatalog(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    vertx.eventBus()
      .request(DEVICE_TYPE_SAVE.name(), body, replyId -> {
        if (replyId.succeeded()) {

          ctx.response()
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .end(Json.encode(replyId.result()));

        } else {

          ctx.response()
            .setStatusCode(500)
            .end(Json.encode(replyId.cause()));
        }

      });
  }

  private void getDeviceCatalog(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("id"));
    vertx.eventBus()
      .request(DEVICE_TYPE_GET.name(), id, replyDeviceType -> {

        if (replyDeviceType.succeeded()) {

          ctx.response()
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .end(replyDeviceType.result().body().toString());

        } else {
          ErrorHandler.respondError(ctx, replyDeviceType.cause());
        }
      });
  }

  private void getAllDeviceCatalog(RoutingContext ctx) {
    vertx.eventBus()
      .request(DEVICE_TYPE_GET_ALL.name(), "", replyAllDeviceTypes -> {

        if (replyAllDeviceTypes.succeeded()) {

          ctx.response()
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .end(replyAllDeviceTypes.result().body().toString());

        } else {
          ctx.response()
            .setStatusCode(500)
            .end(Json.encode(replyAllDeviceTypes.cause()));
        }

      });
  }

  private void deleteDeviceCatalog(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("id"));

    vertx.eventBus()
      .request(DEVICE_TYPE_DELETE.name(), id, replyDeletedId -> {
        if (replyDeletedId.succeeded()) {

          ctx.response()
            .setStatusCode(200)
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .end(Json.encode(replyDeletedId.result().body()));

        } else {

          ctx.response()
            .setStatusCode(500)
            .end(Json.encode(replyDeletedId.cause()));

        }
      });
  }
}
