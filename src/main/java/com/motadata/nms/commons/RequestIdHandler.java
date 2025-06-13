package com.motadata.nms.commons;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.ext.web.RoutingContext;

import java.util.UUID;

public class RequestIdHandler implements Handler<RoutingContext> {

  public static final String REQUEST_ID_KEY = "REQUEST-ID";
  public static final String REQUEST_ID_FORMAT = "[REQUEST-ID:%s] ";

  @Override
  public void handle(RoutingContext ctx) {
    String requestId = UUID.randomUUID().toString();  // or use timestamp-based ID

    ctx.put(REQUEST_ID_KEY, requestId);


    ctx.response().putHeader("X-Request-ID", requestId);

    ctx.next();
  }

  public static String withRequestId(String requestId, String msg){
    return String.format(REQUEST_ID_FORMAT, requestId) + msg;
  }

  public static DeliveryOptions getRequestIdDeliveryOpts(String requestId){
    return  new DeliveryOptions().addHeader(REQUEST_ID_KEY, requestId);
  }
}
