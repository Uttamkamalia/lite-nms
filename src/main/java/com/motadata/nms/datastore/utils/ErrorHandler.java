package com.motadata.nms.datastore.utils;

import com.motadata.nms.commons.NMSException;
import com.motadata.nms.rest.utils.ErrorCodes;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;;

import static com.motadata.nms.commons.RequestIdHandler.REQUEST_ID_KEY;
import static com.motadata.nms.commons.RequestIdHandler.withRequestId;

public class ErrorHandler {

  // Handle DAO or service errors in EventBus consumers
  public static void replyFailure(Message<?> msg, Logger logger, Throwable err) {
    String requestId = msg.headers().get(REQUEST_ID_KEY);
    if (err instanceof NMSException appEx) {
      logger.error(withRequestId(requestId, "Error in DAO layer: "+ appEx.getMessage()));
      msg.fail(appEx.getStatusCode(), withRequestId(requestId,appEx.getMessage()));
    } else {
      logger.error(withRequestId(requestId, "Error in DAO layer: "+ err.getMessage()));
      msg.fail(ErrorCodes.INTERNAL_ERROR, withRequestId(msg.headers().get(REQUEST_ID_KEY),"Internal server error: " + err.getMessage()));
    }
  }

}
