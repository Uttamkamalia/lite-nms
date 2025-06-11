package com.motadata.nms.rest;

import com.motadata.nms.commons.RequestIdHandler;
import com.motadata.nms.commons.VertxProvider;
import com.motadata.nms.datastore.utils.ConfigKeys;
import com.motadata.nms.rest.handlers.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class ApiVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(ApiVerticle.class);

  public void start(Promise<Void> startPromise) {
    registerExceptionHandler();

    HttpServer httpServer = vertx.createHttpServer();

    Router router = Router.router(vertx);

    RequestIdHandler requestIdHandler = new RequestIdHandler();
    router.route().handler(requestIdHandler);
    router.route().handler(BodyHandler.create());

    router.errorHandler(500, ctx -> {
      logger.error("HTTP error: ", ctx.failure());
      ctx.response().setStatusCode(500).end("Internal server error");
    });

    new DeviceTypeApiHandler().registerRoutes(router);
    new CredentialProfileApiHandler().registerRoutes(router);
    new DiscoveryProfileApiHandler().registerRoutes(router);
    new MetricApiHandler().registerRoutes(router);
    new MetricGroupApiHandler().registerRoutes(router);

    httpServer.requestHandler(router)
      .listen(config().getJsonObject(ConfigKeys.HTTP).getInteger(ConfigKeys.HTTP_PORT));
    startPromise.complete();
  }

  private void registerExceptionHandler() {
    VertxProvider.getVertx().getOrCreateContext().exceptionHandler(cause -> {
      logger.error("Uncaught exception in API Verticle context : " + cause.getMessage(), cause);
    });
  }
}
