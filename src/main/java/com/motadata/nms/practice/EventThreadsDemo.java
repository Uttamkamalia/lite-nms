package com.motadata.nms.practice;

import io.vertx.core.AbstractVerticle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class EventThreadsDemo extends AbstractVerticle {

  private int id;
  private ConcurrentHashMap<String, List<String>> threadsToVerticleMap;

  EventThreadsDemo(int i, ConcurrentHashMap<String, List<String>> map){
    this.id = i;
    threadsToVerticleMap = map;
  }

  public void start() {

    vertx.eventBus().consumer("my-address", message -> {
      threadsToVerticleMap.computeIfAbsent(Thread.currentThread().getName(), (a) -> new ArrayList<>()).add("V["+id+" - 1] msg:" + message.body());
    });

    vertx.eventBus().consumer("my-address", message -> {
      threadsToVerticleMap.computeIfAbsent(Thread.currentThread().getName(),  (a) -> new ArrayList<>()).add("V["+id+" - 2] msg:" + message.body());
    });

  }
}
