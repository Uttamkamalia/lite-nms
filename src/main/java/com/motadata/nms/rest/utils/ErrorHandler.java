package com.motadata.nms.rest.utils;

import com.motadata.nms.commons.NMSException;

import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.impl.logging.Logger;;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import java.sql.SQLException;

import static com.motadata.nms.commons.RequestIdHandler.*;

public class ErrorHandler {
  private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

  public static void respondError(RoutingContext ctx, Throwable err) {
    NMSException appEx = (err instanceof ReplyException replyErr)
      ? new NMSException(ErrorCodes.INTERNAL_ERROR, replyErr.getMessage())
      : mapToNMSException(err);

    logger.error(withRequestId(ctx.get(REQUEST_ID_KEY), "Failed to handle request with error:" + appEx.getStatusCode() + " - " + appEx.getMessage()), err);

    ctx.response()
      .setStatusCode(appEx.getStatusCode())
      .putHeader("Content-Type", "application/json")
      .end(new JsonObject()
        .put("error", appEx.getMessage())
        .encodePrettily());
  }

  private static NMSException mapToNMSException(Throwable err) {
    // Customize known exception mappings here
    if (err instanceof NMSException appEx) {
      return appEx;
    }

    if (err instanceof IllegalArgumentException illegalArgumentException) {
      return NMSException.badRequest(illegalArgumentException.getMessage(), illegalArgumentException);
    }

    if (err instanceof SQLException sqlEx) {
      // Example: map unique constraint violation (Postgres code 23505)
      if ("23505".equals(sqlEx.getSQLState())) {
        return NMSException.conflict("Duplicate entry: " + sqlEx.getMessage());
      }
      return NMSException.internal("SQL Error: " + sqlEx.getMessage());
    }

    return NMSException.internal("Internal server error: " + err.getMessage());
  }
}
