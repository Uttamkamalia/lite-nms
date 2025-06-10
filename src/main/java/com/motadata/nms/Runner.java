package com.motadata.nms;

import com.motadata.nms.practice.EventThreadsDemo;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgBuilder;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlConnectOptions;

public class Runner {
  public static void main(String[] args) {
    System.out.println("Runner");
    Vertx.vertx().deployVerticle(new MainVerticle());
  }
}
