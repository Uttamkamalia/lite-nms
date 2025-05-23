package com.motadata.nms;


import com.motadata.nms.commons.VertxProvider;
import com.motadata.nms.datastore.DatabaseVerticle;
import com.motadata.nms.discovery.DiscoveryResultCollectorVerticle;
import com.motadata.nms.discovery.DiscoveryVerticle;
import com.motadata.nms.discovery.context.DiscoveryContextBuilderVerticle;
import com.motadata.nms.discovery.job.DiscoveryWorkerVerticle;
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

    return retriever.getConfig().onSuccess(config -> {
      // Store config in shared data for access by other components
      vertx.sharedData().getLocalMap("config").put("config", config);

      // TODO initial security- ecryption servixex
    });
  }

  private Future<String> deployVerticles(JsonObject config) {
    logger.info("Configuration" + config.getFloat("http.port"));
    logger.info("Configuration via json-obj" + config.getJsonObject("http").getInteger("port"));

    // Deploy options for worker verticles
    // TODO worker froup: db, discovery-context-builder, discovery-job-worker
    DeploymentOptions workerOptions = new DeploymentOptions()
      .setConfig(config)
      .setWorker(true)
      .setWorkerPoolSize(5)
      .setWorkerPoolName("nms-worker-pool");

    // Deploy options for standard verticles
    DeploymentOptions standardOptions = new DeploymentOptions()
      .setConfig(config);

    return Future.succeededFuture()
      .compose(v -> vertx.deployVerticle(new DatabaseVerticle(), workerOptions))
      // Deploy the context builder as a worker verticle
      .compose(depId -> vertx.deployVerticle(new DiscoveryContextBuilderVerticle(), workerOptions))
      // Deploy the discovery verticle as a standard verticle
      .compose(depId -> vertx.deployVerticle(new DiscoveryVerticle(), standardOptions))
      // Deploy the job worker as a worker verticle
      .compose(depId -> vertx.deployVerticle(new DiscoveryJobWorkerVerticle(), workerOptions))
      // Deploy our new discovery worker verticle
      .compose(depId -> vertx.deployVerticle(new DiscoveryWorkerVerticle(), workerOptions))
      // Deploy the result collector as a standard verticle
      .compose(depId -> vertx.deployVerticle(new DiscoveryResultCollectorVerticle(), standardOptions))
      .compose(depId -> vertx.deployVerticle(new ApiVerticle(), standardOptions));
  }
}

