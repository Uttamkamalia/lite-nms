package com.motadata.nms.rest.utils;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.function.Function;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import static com.motadata.nms.commons.RequestIdHandler.REQUEST_ID_FORMAT;
import static com.motadata.nms.commons.RequestIdHandler.withRequestId;

public class RestUtils {
  private static final Logger logger = LoggerFactory.getLogger(RestUtils.class.getName());

  public static  <T> T parseAndRespond(RoutingContext ctx, Function<JsonObject, T> parser){
    T pojo = null;
    try {
      JsonObject obj = ctx.body().asJsonObject();
      logger.debug(withRequestId(ctx.get("requestId"),"Parsing request body: " + obj.encodePrettily()));
      pojo = parser.apply(obj);
    } catch (Exception e) {
      ErrorHandler.respondError(ctx, e);
    }
    return pojo;
  }

  public static <T> T parseAndRespond(RoutingContext ctx, String param, Function<String, T> parser, String msg){
    T value = null;
    try {
       value = parser.apply(ctx.pathParam(param));
      logger.debug(withRequestId(ctx.get("requestId"),"Parsing request param " +param+"="+value ));
    } catch (Exception e) {
      ErrorHandler.respondError(ctx, e);
    }
    return value;
  }
}
