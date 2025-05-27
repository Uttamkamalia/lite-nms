package com.motadata.nms.rest.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motadata.nms.commons.RequestIdHandler;
import com.motadata.nms.commons.VertxProvider;
import com.motadata.nms.models.DeviceType;
import com.motadata.nms.rest.utils.ErrorCodes;
import com.motadata.nms.rest.utils.ErrorHandler;
import com.motadata.nms.rest.utils.RestUtils;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.function.Function;

import static com.motadata.nms.commons.RequestIdHandler.getRequestIdDeliveryOpts;
import static com.motadata.nms.rest.utils.RestUtils.parseAndRespond;
import static com.motadata.nms.utils.EventBusChannels.*;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

public class DeviceTypeApiHandler {
  private static final Vertx vertx = VertxProvider.getVertx();
  Logger logger = LoggerFactory.getLogger(DeviceTypeApiHandler.class);

  public void registerRoutes(Router router) {
    router.post("/device-catalog").handler(this::createDeviceCatalog);

    router.get("/device-catalog/:id").handler(this::getDeviceCatalog);

    router.get("/device-catalog/").handler(this::getAllDeviceCatalog);

    router.delete("/device-catalog/:id").handler(this::deleteDeviceCatalog);
  }

  private void createDeviceCatalog(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    DeviceType deviceType = parseAndRespond(ctx, DeviceType::fromJson);

    vertx.eventBus()
      .request(DEVICE_TYPE_SAVE.name(), deviceType, getRequestIdDeliveryOpts(requestId), replyId -> {
        if (replyId.succeeded()) {

          ctx.response()
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .end(Json.encode(replyId.result()));

        } else {
          ErrorHandler.respondError(ctx, replyId.cause());
        }
      });
  }

  private void getDeviceCatalog(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer id = RestUtils.parseAndRespond(ctx, "id", Integer::parseInt, "Device-type ID cannot be null");

    vertx.eventBus()
      .request(DEVICE_TYPE_GET.name(), id, getRequestIdDeliveryOpts(requestId), replyDeviceType -> {

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
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    vertx.eventBus()
      .request(DEVICE_TYPE_GET_ALL.name(), "", getRequestIdDeliveryOpts(requestId),replyAllDeviceTypes -> {

        if (replyAllDeviceTypes.succeeded()) {

          ctx.response()
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .end(replyAllDeviceTypes.result().body().toString());

        } else {
          ErrorHandler.respondError(ctx, replyAllDeviceTypes.cause());
        }

      });
  }

  private void deleteDeviceCatalog(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer id = RestUtils.parseAndRespond(ctx, "id", Integer::parseInt, "Device-type ID cannot be null");

    vertx.eventBus()
      .request(DEVICE_TYPE_DELETE.name(), id, getRequestIdDeliveryOpts(requestId),replyDeletedId -> {
        if (replyDeletedId.succeeded()) {

          ctx.response()
            .setStatusCode(200)
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .end(Json.encode(replyDeletedId.result().body()));

        } else {
          ErrorHandler.respondError(ctx, replyDeletedId.cause());
        }
      });
  }
}
