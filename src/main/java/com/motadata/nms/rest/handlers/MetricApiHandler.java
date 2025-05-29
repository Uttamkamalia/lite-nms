package com.motadata.nms.rest.handlers;

import com.motadata.nms.commons.RequestIdHandler;
import com.motadata.nms.commons.VertxProvider;
import com.motadata.nms.models.Metric;
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

public class MetricApiHandler {

  private static final Vertx vertx = VertxProvider.getVertx();

  public void registerRoutes(Router router) {
    router.post("/metric").handler(this::createMetric);
    router.get("/metric/:id").handler(this::getMetric);
    router.get("/metric").handler(this::getAllMetrics);
    router.get("/metric/device-type/:deviceTypeId").handler(this::getMetricsByDeviceType);
    router.put("/metric/:id").handler(this::updateMetric);
    router.delete("/metric/:id").handler(this::deleteMetric);
  }

  private void createMetric(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    Metric metric = parseAndRespond(ctx, Metric::fromJson);

    vertx.eventBus().request(METRIC_SAVE.name(), metric, getRequestIdDeliveryOpts(requestId), reply -> {
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

  private void getMetric(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer id = RestUtils.parseAndRespond(ctx, "id", Integer::parseInt, "Metric ID cannot be null");

    vertx.eventBus().request(METRIC_GET.name(), id, getRequestIdDeliveryOpts(requestId), reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .end(reply.result().body().toString());
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }

  private void getAllMetrics(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    vertx.eventBus().request(METRIC_GET_ALL.name(), "", getRequestIdDeliveryOpts(requestId), reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .end(reply.result().body().toString());
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }

  private void getMetricsByDeviceType(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer deviceTypeId = RestUtils.parseAndRespond(ctx, "deviceTypeId", Integer::parseInt, "Device Type ID cannot be null");

    vertx.eventBus().request(METRIC_GET_BY_DEVICE_TYPE.name(), deviceTypeId, getRequestIdDeliveryOpts(requestId), reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .end(reply.result().body().toString());
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }

  private void updateMetric(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer id = RestUtils.parseAndRespond(ctx, "id", Integer::parseInt, "Metric ID cannot be null");
    
    JsonObject body = ctx.body().asJsonObject();
    body.put("id", id); // Ensure ID from path is used
    
    Metric metric = Metric.fromJson(body);

    vertx.eventBus().request(METRIC_UPDATE.name(), metric, getRequestIdDeliveryOpts(requestId), reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .end(Json.encode(reply.result().body()));
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }

  private void deleteMetric(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer id = RestUtils.parseAndRespond(ctx, "id", Integer::parseInt, "Metric ID cannot be null");

    vertx.eventBus().request(METRIC_DELETE.name(), id, getRequestIdDeliveryOpts(requestId), reply -> {
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