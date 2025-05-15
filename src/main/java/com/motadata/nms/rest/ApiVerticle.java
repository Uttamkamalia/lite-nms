package com.motadata.nms.rest;

import com.motadata.nms.rest.handlers.CredentialProfileApiHandler;
import com.motadata.nms.rest.handlers.DeviceTypeApiHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class ApiVerticle extends AbstractVerticle {
  private static final Integer DEFAULT_HTTP_PORT = 8888;


  @Override
  public void start() {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    new DeviceTypeApiHandler().registerRoutes(router);
    new CredentialProfileApiHandler().registerRoutes(router);


    vertx.createHttpServer()
      .requestHandler(router)
      .listen(fetchHttpPort())
      .onSuccess(server -> System.out.println("HTTP server running on port " + server.actualPort()));
  }

  private Integer fetchHttpPort(){
    return config().getJsonObject("http").getInteger("port", DEFAULT_HTTP_PORT);
  }
}
