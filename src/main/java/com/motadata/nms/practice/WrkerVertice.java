package com.motadata.nms.practice;

import io.vertx.core.AbstractVerticle;

public class WrkerVertice extends AbstractVerticle {
  @Override
  public void start() {
    System.out.println("Worker Verticle started");

    vertx.eventBus().consumer("my-worker-address", message -> {
      System.out.println("Worker Verticle :::: Thread:"+Thread.currentThread().getName()+" ::: msg:" + message.body());
      message.reply("Hello from worker verticle");
    });
  }
  @Override
  public void stop() {
    System.out.println("Worker Verticle stopped");
  }
}
