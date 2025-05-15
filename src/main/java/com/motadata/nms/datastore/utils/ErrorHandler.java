package com.motadata.nms.datastore.utils;

import com.motadata.nms.commons.NMSException;
import com.motadata.nms.rest.utils.ErrorCodes;
import io.vertx.core.eventbus.Message;

public class ErrorHandler {

  // Handle DAO or service errors in EventBus consumers
  public static void replyFailure(Message<?> msg, Throwable err) {
    if (err instanceof NMSException appEx) {
      msg.fail(appEx.getStatusCode(), appEx.getMessage());
    } else {
      msg.fail(ErrorCodes.INTERNAL_ERROR, "Internal server error: " + err.getMessage());
    }
  }

}
