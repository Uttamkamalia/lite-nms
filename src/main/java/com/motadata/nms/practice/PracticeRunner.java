package com.motadata.nms.practice;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PracticeRunner {
  public static void main(String[] args) throws InterruptedException {

    ConcurrentHashMap<String, List<String>> map = new ConcurrentHashMap<>();

    Vertx vertx = Vertx.vertx();
    for(int i=0;i<2;i++){
      vertx.deployVerticle(new EventThreadsDemo(1+i, map),new DeploymentOptions().setWorker(false));
    }

    vertx.deployVerticle(new EventThreadDeo2(10, map),new DeploymentOptions().setWorker(false).setWorkerPoolName("local-worker-pool-for-standard-verticle").setWorkerPoolSize(3));

    vertx.createSharedWorkerExecutor("prac-worker-pool", 3);

    for(int i=0;i<5;i++){
      vertx.eventBus().send("my-address",  i);
      try {
        Thread.sleep(1000);
        System.out.println("::::::::::::::::::::::::::::::::::::::::::::::::::::::  "+i);
        map.forEach((key, value) -> {
          System.out.println(key + " : " + value);
        });

      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    vertx.eventBus().send("my-address-execute-blocking" , "{{blocking msg}}");
    System.out.println("::::::::::::::::::::::::::::::::::::::::::::::::::::::   check worker pool  ");
    Thread.sleep(5000);
    map.forEach((key, value) -> {
      System.out.println(key + " : " + value);
    });


  }
}
