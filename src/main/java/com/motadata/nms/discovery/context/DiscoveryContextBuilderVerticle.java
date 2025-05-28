package com.motadata.nms.discovery.context;

import com.motadata.nms.commons.NMSException;
import com.motadata.nms.rest.utils.ErrorCodes;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import static com.motadata.nms.utils.EventBusChannels.*;

/**
 * Worker verticle for building DiscoveryContext objects
 * This verticle handles the potentially blocking operations involved in building a DiscoveryContext
 */
public class DiscoveryContextBuilderVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(DiscoveryContextBuilderVerticle.class);

  private DiscoveryContextBuilder contextBuilder;

  @Override
  public void start(Promise<Void> startPromise) {
    contextBuilder = new DiscoveryContextBuilder();

    registerDiscoveryContextBuildConsumer();

    startPromise.complete();
  }

  private void registerDiscoveryContextBuildConsumer() {
    vertx.eventBus().consumer(DISCOVERY_CONTEXT_BUILD.name(), discoveryProfileIdMsg -> {
      JsonObject request = (JsonObject) discoveryProfileIdMsg.body();
      Integer discoveryProfileId = request.getInteger(DiscoveryContext.DISCOVERY_PROFILE_ID);

      contextBuilder
        .buildFromProfileId(discoveryProfileId)
        .onSuccess(context -> {
          logger.info("Successfully built discovery context for discovery-profile-id:" + discoveryProfileId);
          discoveryProfileIdMsg.reply(context.toJson());
        })
        .onFailure(err -> {
          logger.error("Failed to build discovery context for discovery-profile-id:"+discoveryProfileId+ " with error :"+ err.getMessage(), err);
          if (err instanceof NMSException) {
            NMSException nmsErr = (NMSException) err;
            discoveryProfileIdMsg.fail(nmsErr.getStatusCode(), nmsErr.getMessage()+nmsErr.getCause());
          } else {
            discoveryProfileIdMsg.fail(ErrorCodes.INTERNAL_ERROR, "Failed to build discovery context: " + err.getMessage()+err.getCause());
          }
        });
    });
  }
}
