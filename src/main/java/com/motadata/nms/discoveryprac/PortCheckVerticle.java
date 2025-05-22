package com.motadata.nms.discoveryprac;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;



class PortCheckVerticle extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) {
    vertx.eventBus().consumer("discovery.portcheck", message -> {
      JsonObject input = (JsonObject) message.body();
      String ip = input.getString("ip");
      int port = input.getInteger("port");

      vertx.<Boolean>executeBlocking(promise -> {
        boolean isOpen = simulatePortCheck(ip, port);
        promise.complete(isOpen);
      }, res -> message.reply(String.valueOf(res.result())));
    });
    startPromise.complete();
  }

  private boolean simulatePortCheck(String ip, int port) {
    // Real implementation: try socket connect
    return true; // Assume open for demo
  }
}
