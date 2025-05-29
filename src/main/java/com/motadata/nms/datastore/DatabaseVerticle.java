package com.motadata.nms.datastore;

import com.motadata.nms.commons.JsonPojoMapper;
import com.motadata.nms.datastore.dao.*;
import com.motadata.nms.datastore.utils.ErrorHandler;
import com.motadata.nms.models.*;
import com.motadata.nms.models.credential.CredentialProfile;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.impl.logging.Logger;;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.*;


import static com.motadata.nms.datastore.utils.ConfigKeys.*;
import static com.motadata.nms.utils.EventBusChannels.*;


public class DatabaseVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseVerticle.class);

  private PgPool pool;

  private  DeviceTypeDAO deviceTypeDAO;
  private  CredentialProfileDAO credentialProfileDAO;
  private  DiscoveryProfileDAO discoveryProfileDAO;
  private  MetricDAO metricDAO;
  private  MetricGroupDAO metricGroupDAO;
  private  ProvisionedDeviceDAO provisionedDeviceDAO;

  private void initPool(){
    JsonObject dbConfig = config().getJsonObject(DATASTORE);
    PgConnectOptions connectOptions = new PgConnectOptions()
      .setPort(dbConfig.getInteger(DATASTORE_PORT))
      .setHost(dbConfig.getString(DATASTORE_HOST))
      .setDatabase(dbConfig.getString(DATASTORE_NAME))
      .setUser(dbConfig.getString(DATASTORE_USER))
      .setPassword(dbConfig.getString(DATASTORE_PASSWORD));

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
    metricDAO = new MetricDAO(pool);
    metricGroupDAO = new MetricGroupDAO(pool);
    provisionedDeviceDAO = new ProvisionedDeviceDAO(pool);
  }

  @Override
  public void start() {
    initPool();
    initDao();
    registerDeviceTypeEventConsumers();
    registerCredentialProfileEventConsumers();
    registerDiscoveryProfileEventConsumers();
    registerProvisionedDeviceEventConsumers();
    registerMetricEventConsumers();
    registerMetricGroupEventConsumers();
  }

  private void registerDeviceTypeEventConsumers(){

    vertx.eventBus().consumer(DEVICE_TYPE_SAVE.name())
      .handler(deviceTypeMsg -> {
        if(deviceTypeMsg.body() instanceof DeviceType deviceType){
          deviceTypeDAO.save(deviceType)
            .onSuccess(deviceTypeMsg::reply)
            .onFailure(err -> ErrorHandler.replyFailure(deviceTypeMsg, logger, err));
        }
      });

    vertx.eventBus().consumer(DEVICE_TYPE_GET.name())
      .handler(id -> {

        deviceTypeDAO.get((Integer) id.body())
          .onSuccess(id::reply)
          .onFailure(err -> ErrorHandler.replyFailure(id, logger, err));
      });

    vertx.eventBus().consumer(DEVICE_TYPE_GET_ALL.name())
      .handler(blankMsg -> {

        deviceTypeDAO.getAll()
          .onSuccess(blankMsg::reply)
          .onFailure(err -> ErrorHandler.replyFailure(blankMsg, logger, err));
      });

    vertx.eventBus().consumer(DEVICE_TYPE_DELETE.name())
      .handler(idMsg -> {

        Integer id = (Integer) idMsg.body();
        deviceTypeDAO.delete(id)
          .onSuccess(idMsg::reply)
          .onFailure(err -> ErrorHandler.replyFailure(idMsg,  logger, err));
      });
  }

  private void registerCredentialProfileEventConsumers() {

    vertx.eventBus().consumer(CREDENTIAL_PROFILE_SAVE.name())
      .handler(msg -> {
        if(msg.body() instanceof CredentialProfile credentialProfile){
          credentialProfileDAO.save(credentialProfile)
            .onSuccess(savedId -> msg.reply(new JsonObject().put("id", savedId)))
            .onFailure(err -> ErrorHandler.replyFailure(msg,  logger, err));
        }
      });

    vertx.eventBus().consumer(CREDENTIAL_PROFILE_GET.name())
      .handler(msg -> {
        Integer id = (Integer) msg.body();
        credentialProfileDAO.get(id)
          .onSuccess(msg::reply)
          .onFailure(err -> ErrorHandler.replyFailure(msg,  logger, err));
      });

    vertx.eventBus().consumer(CREDENTIAL_PROFILE_GET_ALL.name())
      .handler(msg -> {
        credentialProfileDAO.getAll()
          .onSuccess(msg::reply)
          .onFailure(err -> ErrorHandler.replyFailure(msg,  logger, err));
      });

    vertx.eventBus().consumer(CREDENTIAL_PROFILE_UPDATE.name())
      .handler(msg -> {
        CredentialProfile profile = CredentialProfile.fromJson((JsonObject) msg.body());
        credentialProfileDAO.update(profile)
          .onSuccess(updatedId -> msg.reply(new JsonObject().put("id", updatedId)))
          .onFailure(err -> ErrorHandler.replyFailure(msg,  logger, err));
      });

    vertx.eventBus().consumer(CREDENTIAL_PROFILE_DELETE.name())
      .handler(msg -> {
        Integer id = (Integer) msg.body();
        credentialProfileDAO.delete(id)
          .onSuccess(deletedId -> msg.reply(new JsonObject().put("id", deletedId)))
          .onFailure(err -> ErrorHandler.replyFailure(msg,  logger, err));
      });
  }

  private void registerDiscoveryProfileEventConsumers() {
    vertx.eventBus().consumer(DISCOVERY_PROFILE_SAVE.name())
      .handler(msg -> {
        if(msg.body() instanceof DiscoveryProfile discoveryProfile){
          discoveryProfileDAO.save(discoveryProfile)
            .onSuccess(savedId -> msg.reply(new JsonObject().put("id", savedId)))
            .onFailure(err -> ErrorHandler.replyFailure(msg,  logger, err));
        }
      });

    vertx.eventBus().consumer(DISCOVERY_PROFILE_GET.name())
      .handler(msg -> {
        Integer id = (Integer) msg.body();
        discoveryProfileDAO.get(id)
          .onSuccess(msg::reply)
          .onFailure(err -> ErrorHandler.replyFailure(msg,  logger, err));
      });

    vertx.eventBus().consumer(DISCOVERY_PROFILE_GET_ALL.name())
      .handler(msg -> {
        discoveryProfileDAO.getAll()
          .onSuccess(msg::reply)
          .onFailure(err -> ErrorHandler.replyFailure(msg,  logger, err));
      });

    vertx.eventBus().consumer(DISCOVERY_PROFILE_DELETE.name())
      .handler(msg -> {
        Integer id = (Integer) msg.body();
        discoveryProfileDAO.delete(id)
          .onSuccess(deletedId -> msg.reply(new JsonObject().put("id", deletedId)))
          .onFailure(err -> ErrorHandler.replyFailure(msg,  logger, err));
      });
  }

  private void registerMetricEventConsumers() {
    vertx.eventBus().consumer(METRIC_SAVE.name())
      .handler(msg -> {
        if(msg.body() instanceof Metric metric){
          metricDAO.save(metric)
            .onSuccess(savedId -> msg.reply(new JsonObject().put("id", savedId)))
            .onFailure(err -> ErrorHandler.replyFailure(msg, logger, err));
        }
      });

    vertx.eventBus().consumer(METRIC_GET.name())
      .handler(msg -> {
        Integer id = (Integer) msg.body();
        metricDAO.get(id)
          .onSuccess(msg::reply)
          .onFailure(err -> ErrorHandler.replyFailure(msg, logger, err));
      });

    vertx.eventBus().consumer(METRIC_GET_ALL.name())
      .handler(msg -> {
        metricDAO.getAll()
          .onSuccess(msg::reply)
          .onFailure(err -> ErrorHandler.replyFailure(msg, logger, err));
      });

    vertx.eventBus().consumer(METRIC_GET_BY_DEVICE_TYPE.name())
      .handler(msg -> {
        Integer deviceTypeId = (Integer) msg.body();
        metricDAO.getByDeviceType(deviceTypeId)
          .onSuccess(msg::reply)
          .onFailure(err -> ErrorHandler.replyFailure(msg, logger, err));
      });

    vertx.eventBus().consumer(METRIC_UPDATE.name())
      .handler(msg -> {
        if(msg.body() instanceof Metric metric){
          metricDAO.update(metric)
            .onSuccess(updatedId -> msg.reply(new JsonObject().put("id", updatedId)))
            .onFailure(err -> ErrorHandler.replyFailure(msg, logger, err));
        }
      });

    vertx.eventBus().consumer(METRIC_DELETE.name())
      .handler(msg -> {
        Integer id = (Integer) msg.body();
        metricDAO.delete(id)
          .onSuccess(deletedId -> msg.reply(new JsonObject().put("id", deletedId)))
          .onFailure(err -> ErrorHandler.replyFailure(msg, logger, err));
      });
  }

  private void registerMetricGroupEventConsumers() {
    vertx.eventBus().consumer(METRIC_GROUP_SAVE.name())
      .handler(msg -> {
        if(msg.body() instanceof MetricGroup metricGroup){
          metricGroupDAO.save(metricGroup)
            .onSuccess(savedId -> msg.reply(new JsonObject().put("id", savedId)))
            .onFailure(err -> ErrorHandler.replyFailure(msg, logger, err));
        }
      });

    vertx.eventBus().consumer(METRIC_GROUP_GET.name())
      .handler(msg -> {
        Integer id = (Integer) msg.body();
        metricGroupDAO.get(id)
          .onSuccess(metricGroup -> msg.reply(metricGroup))
          .onFailure(err -> ErrorHandler.replyFailure(msg, logger, err));
      });

    vertx.eventBus().consumer(METRIC_GROUP_GET_ALL.name())
      .handler(msg -> {
        metricGroupDAO.getAll()
          .onSuccess(metricGroups -> {
            JsonArray result = new JsonArray();
            metricGroups.forEach(mg -> result.add(mg));
            msg.reply(result);
          })
          .onFailure(err -> ErrorHandler.replyFailure(msg, logger, err));
      });


    vertx.eventBus().consumer(METRIC_GROUP_DELETE.name())
      .handler(msg -> {
        Integer id = (Integer) msg.body();
        metricGroupDAO.delete(id)
          .onSuccess(v -> msg.reply("deleted"))
          .onFailure(err -> ErrorHandler.replyFailure(msg, logger, err));
      });
  }

  private void registerProvisionedDeviceEventConsumers() {
    vertx.eventBus().consumer(PROVISIONED_DEVICE_SAVE.name())
      .handler(msg -> {
        if(msg.body() instanceof ProvisionedDevice provisionedDevice){
          provisionedDeviceDAO.save(provisionedDevice)
            .onSuccess(savedId -> msg.reply(""))
            .onFailure(err -> ErrorHandler.replyFailure(msg, logger, err));
        }
      });

    vertx.eventBus().consumer(PROVISIONED_DEVICE_GET.name())
      .handler(msg -> {
        Integer id = (Integer) msg.body();
        provisionedDeviceDAO.get(id)
          .onSuccess(device -> msg.reply(device))
          .onFailure(err -> ErrorHandler.replyFailure(msg, logger, err));
      });

    vertx.eventBus().consumer(PROVISIONED_DEVICE_GET_ALL.name())
      .handler(msg -> {
        provisionedDeviceDAO.getAll()
          .onSuccess(devices -> {
            JsonArray result = new JsonArray();
            devices.forEach(device -> result.add(device.toJson()));
            msg.reply(result);
          })
          .onFailure(err -> ErrorHandler.replyFailure(msg, logger, err));
      });

    vertx.eventBus().consumer(PROVISIONED_DEVICE_GET_BY_IP.name())
      .handler(msg -> {
        String ip = (String) msg.body();
        provisionedDeviceDAO.findByIp(ip)
          .onSuccess(device -> msg.reply(device.toJson()))
          .onFailure(err -> ErrorHandler.replyFailure(msg, logger, err));
      });

    vertx.eventBus().consumer(PROVISIONED_DEVICE_GET_BY_DISCOVERY_PROFILE.name())
      .handler(msg -> {
        Integer discoveryProfileId = (Integer) msg.body();
        provisionedDeviceDAO.findByDiscoveryProfileId(discoveryProfileId)
          .onSuccess(devices -> {
            JsonArray result = new JsonArray();
            devices.forEach(device -> result.add(device.toJson()));
            msg.reply(result);
          })
          .onFailure(err -> ErrorHandler.replyFailure(msg, logger, err));
      });

    vertx.eventBus().consumer(PROVISIONED_DEVICE_UPDATE.name())
      .handler(msg -> {
        if(msg.body() instanceof ProvisionedDevice provisionedDevice){
          provisionedDeviceDAO.update(provisionedDevice)
            .onSuccess(updatedDevice -> msg.reply(updatedDevice.toJson()))
            .onFailure(err -> ErrorHandler.replyFailure(msg, logger, err));
        }
      });

    vertx.eventBus().consumer(PROVISIONED_DEVICE_DELETE.name())
      .handler(msg -> {
        Integer id = (Integer) msg.body();
        provisionedDeviceDAO.delete(id)
          .onSuccess(v -> msg.reply("deleted"))
          .onFailure(err -> ErrorHandler.replyFailure(msg, logger, err));
      });
  }
}
