package com.motadata.nms.datastore;

import com.motadata.nms.commons.JsonPojoMapper;
import com.motadata.nms.datastore.dao.*;
import com.motadata.nms.datastore.utils.ErrorHandler;
import com.motadata.nms.models.credential.CredentialProfile;
import com.motadata.nms.models.DeviceType;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.*;


import static com.motadata.nms.datastore.utils.Constants.*;
import static com.motadata.nms.utils.EventBusChannels.*;


public class DatabaseVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseVerticle.class);

  private PgPool pool;

  private  DeviceTypeDAO deviceTypeDAO;
  private  CredentialProfileDAO credentialProfileDAO;
  private  DiscoveryProfileDAO discoveryProfileDAO;
  private  ProvisionedDeviceDAO provisionedDeviceDAO;
  private MetricGroupDAO metricGroupDAO;


  private void initPool(){
    JsonObject dbConfig = config().getJsonObject(CONFIG_DATASTORE);
    PgConnectOptions connectOptions = new PgConnectOptions()
      .setPort(dbConfig.getInteger(CONFIG_DATASTORE_PORT))
      .setHost(dbConfig.getString(CONFIG_DATASTORE_HOST))
      .setDatabase(dbConfig.getString(CONFIG_DATASTORE_NAME))
      .setUser(dbConfig.getString(CONFIG_DATASTORE_USER))
      .setPassword(dbConfig.getString(CONFIG_DATASTORE_PASSWORD));

    PoolOptions poolOptions = new PoolOptions().setMaxSize(5);
    this.pool = PgPool.pool(vertx,  connectOptions, poolOptions);
    this.pool
      .getConnection()
      .onSuccess(conn -> logger.info("Postgres db connection successful"))
      .onFailure(err-> logger.error("Postgres db connection failed", err));
  }

  private void initDao(){
    deviceTypeDAO = new DeviceTypeDAO(pool);
    credentialProfileDAO = new CredentialProfileDAO(pool);
    discoveryProfileDAO = new DiscoveryProfileDAO(pool);
    provisionedDeviceDAO = new ProvisionedDeviceDAO(pool);
    metricGroupDAO = new MetricGroupDAO(pool);
  }

  @Override
  public void start() {
    initPool();
    initDao();
    registerDeviceTypeEventConsumers();
    registerCredentialProfileEventConsumers();
    registerDiscoveryProfileEventConsumers();
  }

  private void registerDeviceTypeEventConsumers(){

    vertx.eventBus().consumer(DEVICE_TYPE_SAVE.name())
      .handler(deviceTypeMsg -> {

        DeviceType deviceType = JsonPojoMapper.map((JsonObject)deviceTypeMsg.body(), DeviceType.class);
        deviceTypeDAO.save(deviceType)
          .onSuccess(deviceTypeMsg::reply)
          .onFailure(err -> ErrorHandler.replyFailure(deviceTypeMsg, err));
      });

    vertx.eventBus().consumer(DEVICE_TYPE_GET.name())
      .handler(id -> {

        deviceTypeDAO.get((Integer) id.body())
          .onSuccess(id::reply)
          .onFailure(err -> ErrorHandler.replyFailure(id, err));
      });

    vertx.eventBus().consumer(DEVICE_TYPE_GET_ALL.name())
      .handler(blankMsg -> {

        deviceTypeDAO.getAll()
          .onSuccess(blankMsg::reply)
          .onFailure(err -> ErrorHandler.replyFailure(blankMsg, err));
      });

    vertx.eventBus().consumer(DEVICE_TYPE_DELETE.name())
      .handler(idMsg -> {

        Integer id = (Integer) idMsg.body();
        deviceTypeDAO.delete(id)
          .onSuccess(idMsg::reply)
          .onFailure(err -> ErrorHandler.replyFailure(idMsg, err));
      });
  }

  private void registerCredentialProfileEventConsumers() {

    vertx.eventBus().consumer(CREDENTIAL_PROFILE_SAVE.name())
      .handler(msg -> {
        CredentialProfile profile = CredentialProfile.fromJson((JsonObject) msg.body());
        credentialProfileDAO.save(profile)
          .onSuccess(savedId -> msg.reply(new JsonObject().put("id", savedId)))
          .onFailure(err -> ErrorHandler.replyFailure(msg, err));
      });

    vertx.eventBus().consumer(CREDENTIAL_PROFILE_GET.name())
      .handler(msg -> {
        Integer id = (Integer) msg.body();
        credentialProfileDAO.get(id)
          .onSuccess(msg::reply)
          .onFailure(err -> ErrorHandler.replyFailure(msg, err));
      });

    vertx.eventBus().consumer(CREDENTIAL_PROFILE_GET_ALL.name())
      .handler(msg -> {
        credentialProfileDAO.getAll()
          .onSuccess(msg::reply)
          .onFailure(err -> ErrorHandler.replyFailure(msg, err));
      });

    vertx.eventBus().consumer(CREDENTIAL_PROFILE_UPDATE.name())
      .handler(msg -> {
        CredentialProfile profile = CredentialProfile.fromJson((JsonObject) msg.body());
        credentialProfileDAO.update(profile)
          .onSuccess(updatedId -> msg.reply(new JsonObject().put("id", updatedId)))
          .onFailure(err -> ErrorHandler.replyFailure(msg, err));
      });

    vertx.eventBus().consumer(CREDENTIAL_PROFILE_DELETE.name())
      .handler(msg -> {
        Integer id = (Integer) msg.body();
        credentialProfileDAO.delete(id)
          .onSuccess(deletedId -> msg.reply(new JsonObject().put("id", deletedId)))
          .onFailure(err -> ErrorHandler.replyFailure(msg, err));
      });
  }

  private void registerDiscoveryProfileEventConsumers() {
    vertx.eventBus().consumer(DISCOVERY_PROFILE_SAVE.name())
      .handler(msg -> {
        DiscoveryProfile profile = DiscoveryProfile.fromJson((JsonObject) msg.body());
        discoveryProfileDAO.save(profile)
          .onSuccess(savedId -> msg.reply(new JsonObject().put("id", savedId)))
          .onFailure(err -> ErrorHandler.replyFailure(msg, err));
      });

    vertx.eventBus().consumer(DISCOVERY_PROFILE_GET.name())
      .handler(msg -> {
        Integer id = (Integer) msg.body();
        discoveryProfileDAO.get(id)
          .onSuccess(msg::reply)
          .onFailure(err -> ErrorHandler.replyFailure(msg, err));
      });

    vertx.eventBus().consumer(DISCOVERY_PROFILE_GET_ALL.name())
      .handler(msg -> {
        discoveryProfileDAO.getAll()
          .onSuccess(msg::reply)
          .onFailure(err -> ErrorHandler.replyFailure(msg, err));
      });

    vertx.eventBus().consumer(DISCOVERY_PROFILE_DELETE.name())
      .handler(msg -> {
        Integer id = (Integer) msg.body();
        discoveryProfileDAO.delete(id)
          .onSuccess(deletedId -> msg.reply(new JsonObject().put("id", deletedId)))
          .onFailure(err -> ErrorHandler.replyFailure(msg, err));
      });
  }
}



