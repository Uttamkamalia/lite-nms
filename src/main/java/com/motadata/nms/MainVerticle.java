package com.motadata.nms;


import com.motadata.nms.commons.VertxProvider;
import com.motadata.nms.datastore.DatabaseVerticle;
import com.motadata.nms.polling.PollingVerticle;
import com.motadata.nms.rest.ApiVerticle;
import com.motadata.nms.discovery.DiscoveryVerticle;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MainVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  public void start(Promise<Void> startPromise) {
    VertxProvider.initialize(vertx);

    loadConfig()
      .compose(this::deployVerticles)
      .onSuccess(id -> startPromise.complete())
      .onFailure(err -> {
        logger.error("Error while bootstrapping:",err);
          startPromise.fail(err);
      });
  }

  private Future<JsonObject> loadConfig(){
    ConfigStoreOptions fileStore = new ConfigStoreOptions()
      .setType("file")
      .setFormat("json")
      .setConfig(new JsonObject().put("path", "src/main/resources/config.json"));

    ConfigRetriever retriever = ConfigRetriever.create(vertx,
      new ConfigRetrieverOptions().addStore(fileStore));

    return retriever.getConfig();
  }

  private Future<String> deployVerticles(JsonObject config){
    logger.info("Configuration" + config.getFloat("http.port"));
    logger.info("Configuration via json-obj" + config.getJsonObject("http").getInteger("port"));
    return Future.succeededFuture()
        .compose(v-> vertx.deployVerticle(new DatabaseVerticle(), new DeploymentOptions().setConfig(config).setThreadingModel(ThreadingModel.WORKER)))
//        .compose(depId -> vertx.deployVerticle(new DiscoveryVerticle(), new DeploymentOptions().setConfig(config)))
//        .compose(depId -> vertx.deployVerticle(new PollingVerticle(), new DeploymentOptions().setConfig(config)))
        .compose(depId -> vertx.deployVerticle(new ApiVerticle(), new DeploymentOptions().setConfig(config)));
  }
}

