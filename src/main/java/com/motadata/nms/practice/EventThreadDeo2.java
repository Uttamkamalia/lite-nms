package com.motadata.nms.practice;


import io.vertx.core.AbstractVerticle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class EventThreadDeo2 extends AbstractVerticle {

  private int id;
  private ConcurrentHashMap<String, List<String>> threadsToVerticleMap;

  EventThreadDeo2(int i, ConcurrentHashMap<String, List<String>> map){
    this.id = i;
    threadsToVerticleMap = map;
  }

  public void start() {

    vertx.eventBus().consumer("my-address", message -> {
      threadsToVerticleMap.computeIfAbsent(Thread.currentThread().getName(), (a) -> new ArrayList<>()).add("V["+id+" - 1] msg:" + message.body());

    });

    vertx.eventBus().consumer("my-address-execute-blocking", message -> {
      threadsToVerticleMap.computeIfAbsent(Thread.currentThread().getName(), (a) -> new ArrayList<>()).add("V["+id+" - 2] verticle doing executeBlocking:" + message.body());

      vertx.executeBlocking(promise -> {
        threadsToVerticleMap.computeIfAbsent(Thread.currentThread().getName(), (a) -> new ArrayList<>()).add("V["+id+" - 2] check which worker pool it is using:" + message.body());
        promise.complete();
      }, false, res -> {
        threadsToVerticleMap.computeIfAbsent(Thread.currentThread().getName(), (a) -> new ArrayList<>()).add("V["+id+" - 2] verticle doing executeBlocking async handler:" + message.body());
      });
    });

  }
}
