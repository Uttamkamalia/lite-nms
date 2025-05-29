package com.motadata.nms.rest.handlers;

import com.motadata.nms.commons.RequestIdHandler;
import com.motadata.nms.commons.VertxProvider;
import com.motadata.nms.models.ProvisionedDevice;
import com.motadata.nms.rest.utils.ErrorHandler;
import com.motadata.nms.rest.utils.RestUtils;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import static com.motadata.nms.commons.RequestIdHandler.getRequestIdDeliveryOpts;
import static com.motadata.nms.rest.utils.RestUtils.parseAndRespond;
import static com.motadata.nms.utils.EventBusChannels.*;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

public class ProvisionedDeviceApiHandler {

  private static final Vertx vertx = VertxProvider.getVertx();

  public void registerRoutes(Router router) {
    router.post("/provisioned-device").handler(this::createProvisionedDevice);
    router.get("/provisioned-device/:id").handler(this::getProvisionedDevice);
    router.get("/provisioned-device").handler(this::getAllProvisionedDevices);
    router.get("/provisioned-device/ip/:ip").handler(this::getProvisionedDeviceByIp);
    router.get("/provisioned-device/discovery-profile/:discoveryProfileId").handler(this::getProvisionedDevicesByDiscoveryProfile);
    router.put("/provisioned-device/:id").handler(this::updateProvisionedDevice);
    router.put("/provisioned-device/:id/status").handler(this::updateProvisionedDeviceStatus);
    router.delete("/provisioned-device/:id").handler(this::deleteProvisionedDevice);
  }

  private void createProvisionedDevice(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    ProvisionedDevice provisionedDevice = parseAndRespond(ctx, ProvisionedDevice::fromJson);

    vertx.eventBus().request(PROVISIONED_DEVICE_SAVE.name(), provisionedDevice, getRequestIdDeliveryOpts(requestId), reply -> {
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

  private void getProvisionedDevice(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer id = RestUtils.parseAndRespond(ctx, "id", Integer::parseInt, "Provisioned Device ID cannot be null");

    vertx.eventBus().request(PROVISIONED_DEVICE_GET.name(), id, getRequestIdDeliveryOpts(requestId), reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .end(reply.result().body().toString());
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }

  private void getAllProvisionedDevices(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    vertx.eventBus().request(PROVISIONED_DEVICE_GET_ALL.name(), "", getRequestIdDeliveryOpts(requestId), reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .end(reply.result().body().toString());
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }

  private void getProvisionedDeviceByIp(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    String ip = ctx.pathParam("ip");
    
    if (ip == null || ip.isEmpty()) {
      ErrorHandler.respondError(ctx, new IllegalArgumentException("IP address cannot be null or empty"));
      return;
    }

    vertx.eventBus().request(PROVISIONED_DEVICE_GET_BY_IP.name(), ip, getRequestIdDeliveryOpts(requestId), reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .end(reply.result().body().toString());
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }

  private void getProvisionedDevicesByDiscoveryProfile(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer discoveryProfileId = RestUtils.parseAndRespond(ctx, "discoveryProfileId", Integer::parseInt, 
                                                          "Discovery Profile ID cannot be null");

    vertx.eventBus().request(PROVISIONED_DEVICE_GET_BY_DISCOVERY_PROFILE.name(), discoveryProfileId, 
                            getRequestIdDeliveryOpts(requestId), reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .end(reply.result().body().toString());
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }

  private void updateProvisionedDevice(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer id = RestUtils.parseAndRespond(ctx, "id", Integer::parseInt, "Provisioned Device ID cannot be null");
    
    JsonObject body = ctx.body().asJsonObject();
    body.put("id", id); // Ensure ID from path is used
    
    ProvisionedDevice provisionedDevice = ProvisionedDevice.fromJson(body);

    vertx.eventBus().request(PROVISIONED_DEVICE_UPDATE.name(), provisionedDevice, getRequestIdDeliveryOpts(requestId), reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .end(Json.encode(reply.result().body()));
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }

  private void updateProvisionedDeviceStatus(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer id = RestUtils.parseAndRespond(ctx, "id", Integer::parseInt, "Provisioned Device ID cannot be null");
    
    JsonObject body = ctx.body().asJsonObject();
    String status = body.getString("status");
    
    if (status == null || status.isEmpty()) {
      ErrorHandler.respondError(ctx, new IllegalArgumentException("Status cannot be null or empty"));
      return;
    }
    
    JsonObject request = new JsonObject()
      .put("id", id)
      .put("status", status);

    vertx.eventBus().request(PROVISIONED_DEVICE_UPDATE_STATUS.name(), request, getRequestIdDeliveryOpts(requestId), reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .setStatusCode(204)
          .end();
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }

  private void deleteProvisionedDevice(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer id = RestUtils.parseAndRespond(ctx, "id", Integer::parseInt, "Provisioned Device ID cannot be null");

    vertx.eventBus().request(PROVISIONED_DEVICE_DELETE.name(), id, getRequestIdDeliveryOpts(requestId), reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .setStatusCode(204)
          .end();
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }
}