package com.motadata.nms.discovery;



import io.vertx.core.AbstractVerticle;

public class DiscoveryVerticle extends AbstractVerticle {
  @Override
  public void start() {
    DiscoveryService discoveryService = new DiscoveryService(vertx);
    vertx.eventBus().consumer("discovery.trigger", message -> {
      discoveryService.discoverDevices(message.body().toString()).onComplete(ar->{
        if(ar.succeeded()){
          message.reply(ar.result());
        } else {
          message.fail(1,ar.cause().toString());
        }
      });
    });
  }
}
