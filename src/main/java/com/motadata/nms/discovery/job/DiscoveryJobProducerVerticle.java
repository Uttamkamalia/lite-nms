package com.motadata.nms.discovery.job;

import com.motadata.nms.discovery.context.DiscoveryContext;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class DiscoveryJobProducerVerticle extends AbstractVerticle {

  private static final String DISCOVERY_CONTEXT_CHANNEL = "discovery.context.channel";
  private static final String DISCOVERY_JOB_CHANNEL = "discovery.job.channel";

  private int batchSize;

  @Override
  public void start(Promise<Void> startPromise) {
    batchSize = config().getInteger("discovery.batch.size", 10);

    vertx.eventBus().consumer(DISCOVERY_CONTEXT_CHANNEL, msg -> {
      DiscoveryContext context = DiscoveryContext.fromJson((JsonObject) msg.body());

      List<String> ips = context.getTargetIps();
      for (int i = 0; i < ips.size(); i += batchSize) {
        int end = Math.min(i + batchSize, ips.size());
        List<String> batch = ips.subList(i, end);

        batch.forEach(ip -> {
          DiscoveryJob job = new DiscoveryJob(ip, context.getPort(), context.getCredentialProfile(), context.getCredentialProfile());
          vertx.eventBus().publish(DISCOVERY_JOB_CHANNEL, job.toJson());
        });
      }

      msg.reply("Jobs submitted");
    });

    startPromise.complete();
  }
}
