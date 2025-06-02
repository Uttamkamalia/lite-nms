package com.motadata.nms.rest.handlers;

import com.motadata.nms.commons.RequestIdHandler;
import com.motadata.nms.commons.VertxProvider;
import com.motadata.nms.models.MetricGroup;
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

public class MetricGroupApiHandler {

  private static final Vertx vertx = VertxProvider.getVertx();

  public void registerRoutes(Router router) {
    router.post("/metric-group").handler(this::createMetricGroup);
    router.get("/metric-group/:id").handler(this::getMetricGroup);
    router.get("/metric-group-details/:id").handler(this::getMetricGroupDetails);
    router.get("/metric-group-with-devices-details/:id").handler(this::getMetricGroupWithDevicesDetails);
    router.get("/metric-group").handler(this::getAllMetricGroups);
    router.get("/metric-group/device-type/:deviceTypeId").handler(this::getMetricGroupsByDeviceType);
    router.put("/metric-group/:id").handler(this::updateMetricGroup);
    router.put("/metric-group/:id/status").handler(this::updateMetricGroupStatus);
    router.delete("/metric-group/:id").handler(this::deleteMetricGroup);
    router.get("/metric-group/poll/trigger/:id").handler(this::triggerMetricGroupPolling);
  }

  private void triggerMetricGroupPolling(RoutingContext routingContext) {
    String requestId = routingContext.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer id = RestUtils.parseAndRespond(routingContext, "id", Integer::parseInt, "Metric Group ID cannot be null");

    vertx.eventBus().request(METRIC_GROUP_POLLING_TRIGGER.name(), id, getRequestIdDeliveryOpts(requestId), reply -> {
      if (reply.succeeded()) {
        routingContext.response()
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .end(reply.result().body().toString());
      } else {
        ErrorHandler.respondError(routingContext, reply.cause());
      }
    });
  }


  private void getMetricGroupDetails(RoutingContext routingContext) {
    String requestId = routingContext.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer id = RestUtils.parseAndRespond(routingContext, "id", Integer::parseInt, "Metric Group ID cannot be null");

    vertx.eventBus().request(METRIC_GROUP_GET_WITH_DETAILS.name(), id, getRequestIdDeliveryOpts(requestId), reply -> {
      if (reply.succeeded()) {
        routingContext.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .end(reply.result().body().toString());
      } else {
        ErrorHandler.respondError(routingContext, reply.cause());
      }
    });
  }

  private void getMetricGroupWithDevicesDetails(RoutingContext routingContext) {
    String requestId = routingContext.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer id = RestUtils.parseAndRespond(routingContext, "id", Integer::parseInt, "Metric Group ID cannot be null");

    vertx.eventBus().request(METRIC_GROUP_GET_WITH_DEVICES.name(), id, getRequestIdDeliveryOpts(requestId), reply -> {
      if (reply.succeeded()) {
        routingContext.response()
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .end(reply.result().body().toString());
      } else {
        ErrorHandler.respondError(routingContext, reply.cause());
      }
    });
  }

  private void createMetricGroup(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    MetricGroup metricGroup = parseAndRespond(ctx, MetricGroup::fromJson);

    vertx.eventBus().request(METRIC_GROUP_SAVE.name(), metricGroup, getRequestIdDeliveryOpts(requestId), reply -> {
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

  private void getMetricGroup(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer id = RestUtils.parseAndRespond(ctx, "id", Integer::parseInt, "Metric Group ID cannot be null");

    vertx.eventBus().request(METRIC_GROUP_GET.name(), id, getRequestIdDeliveryOpts(requestId), reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .end(reply.result().body().toString());
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }

  private void getAllMetricGroups(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    vertx.eventBus().request(METRIC_GROUP_GET_ALL.name(), "", getRequestIdDeliveryOpts(requestId), reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .end(reply.result().body().toString());
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }

  private void getMetricGroupsByDeviceType(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer deviceTypeId = RestUtils.parseAndRespond(ctx, "deviceTypeId", Integer::parseInt, "Device Type ID cannot be null");

    vertx.eventBus().request(METRIC_GROUP_GET_BY_DEVICE_TYPE.name(), deviceTypeId, getRequestIdDeliveryOpts(requestId), reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .end(reply.result().body().toString());
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }

  private void updateMetricGroup(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer id = RestUtils.parseAndRespond(ctx, "id", Integer::parseInt, "Metric Group ID cannot be null");

    JsonObject body = ctx.body().asJsonObject();
    body.put("id", id); // Ensure ID from path is used

    MetricGroup metricGroup = MetricGroup.fromJson(body);

    vertx.eventBus().request(METRIC_GROUP_UPDATE.name(), metricGroup, getRequestIdDeliveryOpts(requestId), reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .end(Json.encode(reply.result().body()));
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }

  private void updateMetricGroupStatus(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer id = RestUtils.parseAndRespond(ctx, "id", Integer::parseInt, "Metric Group ID cannot be null");

    JsonObject body = ctx.body().asJsonObject();
    String status = body.getString("status");

    if (status == null || status.isEmpty()) {
      ErrorHandler.respondError(ctx, new IllegalArgumentException("Status cannot be null or empty"));
      return;
    }

    JsonObject request = new JsonObject()
      .put("id", id)
      .put("status", status);

    vertx.eventBus().request(METRIC_GROUP_UPDATE_STATUS.name(), request, getRequestIdDeliveryOpts(requestId), reply -> {
      if (reply.succeeded()) {
        ctx.response()
          .setStatusCode(204)
          .end();
      } else {
        ErrorHandler.respondError(ctx, reply.cause());
      }
    });
  }

  private void deleteMetricGroup(RoutingContext ctx) {
    String requestId = ctx.get(RequestIdHandler.REQUEST_ID_KEY);
    Integer id = RestUtils.parseAndRespond(ctx, "id", Integer::parseInt, "Metric Group ID cannot be null");

    vertx.eventBus().request(METRIC_GROUP_DELETE.name(), id, getRequestIdDeliveryOpts(requestId), reply -> {
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
