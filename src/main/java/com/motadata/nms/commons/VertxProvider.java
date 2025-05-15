package com.motadata.nms.commons;

import io.vertx.core.Vertx;

public class VertxProvider {

  private static Vertx vertxInstance;

  // Prevent instantiation
  private VertxProvider() {}

  // Initialize once during application bootstrap
  public static synchronized void initialize(Vertx vertx) {
    if (vertxInstance == null) {
      vertxInstance = vertx;
    }
  }

  // Accessor for shared Vertx instance
  public static Vertx getVertx() {
    if (vertxInstance == null) {
      throw new IllegalStateException("Vertx instance not initialized. Call VertxProvider.initialize() first.");
    }
    return vertxInstance;
  }
}
