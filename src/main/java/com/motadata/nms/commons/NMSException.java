package com.motadata.nms.commons;

import com.motadata.nms.rest.utils.ErrorCodes;

public class NMSException extends RuntimeException {
  private final int statusCode;

  public NMSException(int statusCode, String message) {
    super(message);
    this.statusCode = statusCode;
  }

  public NMSException(int statusCode, String message, Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public static NMSException notFound(String message) {
    return new NMSException(ErrorCodes.NOT_FOUND, message);
  }

  public static NMSException notFound(String message, Throwable cause) {
    return new NMSException(ErrorCodes.NOT_FOUND, message, cause);
  }

  public static NMSException internal(String message) {
    return new NMSException(ErrorCodes.INTERNAL_ERROR, message);
  }

  public static NMSException internal(String message, Throwable cause) {
    return new NMSException(ErrorCodes.INTERNAL_ERROR, message, cause);
  }

  public static NMSException badRequest(String message, IllegalArgumentException e) {
    return new NMSException(ErrorCodes.BAD_REQUEST, message);
  }

  public static NMSException badRequest(String message) {
    return new NMSException(ErrorCodes.BAD_REQUEST, message);
  }

  public static NMSException conflict(String message) {
    return new NMSException(ErrorCodes.CONFLICT, message);
  }
}

