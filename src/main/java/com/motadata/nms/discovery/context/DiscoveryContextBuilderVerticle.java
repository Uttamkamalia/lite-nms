package com.motadata.nms.discovery.context;

import com.motadata.nms.commons.NMSException;
import com.motadata.nms.rest.utils.ErrorCodes;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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

    // Register consumer for building discovery contexts
    vertx.eventBus().consumer(DISCOVERY_CONTEXT_BUILD.name(), discoveryProfileIdMsg -> {
      JsonObject request = (JsonObject) discoveryProfileIdMsg.body();
      Integer discoveryProfileId = request.getInteger(DiscoveryContext.DISCOVERY_PROFILE_ID);

      if (discoveryProfileId == null) {
        logger.error("Profile ID is required for context building");
        discoveryProfileIdMsg.fail(ErrorCodes.INTERNAL_ERROR, "Discovery Profile ID is required");
        return;
      }

      logger.info("Building discovery context for profile ID: " + discoveryProfileId);

      // Build the context - this is potentially blocking but safe in a worker verticle
      contextBuilder.buildFromProfileId(discoveryProfileId)
        .onSuccess(context -> {

          logger.info("Successfully built discovery context for profile ID: " + discoveryProfileId);
          discoveryProfileIdMsg.reply(context.toJson());

        })
        .onFailure(err -> {

          logger.error("Failed to build discovery context: " + err.getMessage(), err);
          if (err instanceof NMSException) {
            NMSException nmsErr = (NMSException) err;
            discoveryProfileIdMsg.fail(nmsErr.getStatusCode(), nmsErr.getMessage());
          } else {
            discoveryProfileIdMsg.fail(ErrorCodes.INTERNAL_ERROR, "Failed to build discovery context: " + err.getMessage());
          }
        });
    });

    startPromise.complete();
  }
}
