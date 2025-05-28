package com.motadata.nms.rest.utils;


public class ErrorCodes {
  public static final int INTERNAL_ERROR = 500;
  public static final int NOT_FOUND = 404;
  public static final int BAD_REQUEST = 400;
  public static final int CONFLICT = 409;
  public static final int UNAUTHORIZED = 401;
  public static final int FORBIDDEN = 403;

  public static final String DAO_ERROR = "[DAO_ERROR] ";
  public static final String REST_HANDLER_ERROR = "[REST_HANDLER_ERROR] ";
}
