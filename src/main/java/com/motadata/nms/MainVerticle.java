package com.motadata.nms;


import com.motadata.nms.commons.GenericJacksonCodec;
import com.motadata.nms.commons.VertxProvider;
import com.motadata.nms.datastore.DatabaseVerticle;
import com.motadata.nms.discovery.DiscoveryVerticle;
import com.motadata.nms.discovery.context.DiscoveryContextBuilderVerticle;
import com.motadata.nms.discovery.job.SnmpDiscoveryJob;
import com.motadata.nms.discovery.job.SshDiscoveryJob;
import com.motadata.nms.models.*;
import com.motadata.nms.models.credential.Credential;
import com.motadata.nms.models.credential.CredentialProfile;
import com.motadata.nms.models.credential.SnmpCredential;
import com.motadata.nms.models.credential.SshCredential;
import com.motadata.nms.polling.PollingOrchestratorVerticle;
import com.motadata.nms.polling.PollingSchedulerVerticle;
import com.motadata.nms.rest.ApiVerticle;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import static com.motadata.nms.datastore.utils.ConfigKeys.*;

public class MainVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);
  public static final String CONFIG_FILE_PATH = "src/main/resources/config.json";

  public void start(Promise<Void> startPromise) {
    VertxProvider.initialize(vertx);

    loadConfig()
      .compose(this::deployVerticles)
      .compose(v -> registerEventBusCodecs())
      .compose(v -> registerVertxGlobalExceptionHandler())
      .onSuccess(v -> startPromise.complete())
      .onFailure(err -> {
        logger.error("Error while bootstrapping:", err);
        startPromise.fail(err);
      });
  }

  private Future<Void> registerVertxGlobalExceptionHandler() {
    vertx.exceptionHandler(cause -> {
      logger.error("Global exception handler: " + cause.getMessage(), cause);
    });
    return Future.succeededFuture();
  }

  private Future<JsonObject> loadConfig() {
    ConfigStoreOptions fileStore = new ConfigStoreOptions()
      .setType("file")
      .setFormat("json")
      .setConfig(new JsonObject().put("path", CONFIG_FILE_PATH));

    ConfigRetriever retriever = ConfigRetriever.create(vertx,
      new ConfigRetrieverOptions().addStore(fileStore));

    return retriever.getConfig().onSuccess(config -> {
      // Store config in shared data for access by other components
      logger.info("Configuration loaded successfully.");
      vertx.sharedData().getLocalMap("config").put("config", config);
    });
  }

  private Future<Void> registerEventBusCodecs() {
    vertx.eventBus().registerDefaultCodec(DeviceType.class, new GenericJacksonCodec<>(DeviceType.class));
    vertx.eventBus().registerDefaultCodec(Credential.class, new GenericJacksonCodec<>(Credential.class));
    vertx.eventBus().registerDefaultCodec(CredentialProfile.class, new GenericJacksonCodec<>(CredentialProfile.class));
    vertx.eventBus().registerDefaultCodec(SnmpCredential.class, new GenericJacksonCodec<>(SnmpCredential.class));
    vertx.eventBus().registerDefaultCodec(SshCredential.class, new GenericJacksonCodec<>(SshCredential.class));
    vertx.eventBus().registerDefaultCodec(DiscoveryProfile.class, new GenericJacksonCodec<>(DiscoveryProfile.class));
    vertx.eventBus().registerDefaultCodec(SshDiscoveryJob.class, new GenericJacksonCodec<>(SshDiscoveryJob.class));
    vertx.eventBus().registerDefaultCodec(SnmpDiscoveryJob.class, new GenericJacksonCodec<>(SnmpDiscoveryJob.class));
    vertx.eventBus().registerDefaultCodec(ProvisionedDevice.class, new GenericJacksonCodec<>(ProvisionedDevice.class));
    vertx.eventBus().registerDefaultCodec(Metric.class, new GenericJacksonCodec<>(Metric.class));
    vertx.eventBus().registerDefaultCodec(MetricGroup.class, new GenericJacksonCodec<>(MetricGroup.class));
    logger.info("Event bus codecs registered.");
    return Future.succeededFuture();
  }

  private Future<Void> deployVerticles(JsonObject config) {

    DeploymentOptions dbOptions = new DeploymentOptions()
      .setConfig(config)
      .setInstances(1);

    Integer pollingSchedularInstanceCount = config.getJsonObject(POLLING).getInteger(POLLING_SCHEDULER_INSTANCES, 1);
    Integer pollingWorkerPoolSize = config.getJsonObject(POLLING).getInteger(POLLING_WORKER_POOL_SIZE, 2);
    DeploymentOptions pollingSchedularDeploymentOptions = new DeploymentOptions()
      .setConfig(config)
      .setInstances(pollingSchedularInstanceCount) // polling scheduler threads required to handle vertx.setPeriodic()
      .setWorkerPoolName("polling-worker-pool")
      .setWorkerPoolSize(pollingWorkerPoolSize); // worker threads required to handle polling-executions via executeBlocking()

    DeploymentOptions standardOptions = new DeploymentOptions()
      .setConfig(config);

    return Future.succeededFuture()
      .compose(v -> vertx.deployVerticle( DatabaseVerticle.class.getName(), dbOptions))

      .compose(depId -> vertx.deployVerticle( DiscoveryVerticle.class.getName(), standardOptions))
      .compose(depId -> vertx.deployVerticle( DiscoveryContextBuilderVerticle.class.getName(), standardOptions))

      .compose(depId -> vertx.deployVerticle( PollingOrchestratorVerticle.class.getName(), standardOptions))
      .compose(depId -> vertx.deployVerticle( PollingSchedulerVerticle.class.getName(), pollingSchedularDeploymentOptions)) // set a private worker thread pool

      .compose(depId -> vertx.deployVerticle( ApiVerticle.class.getName(), standardOptions))
      .compose(depId -> {
        logger.info("All Verticles deployed successfully.");
        return Future.succeededFuture();
      });
  }
}

