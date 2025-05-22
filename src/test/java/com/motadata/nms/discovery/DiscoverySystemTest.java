package com.motadata.nms.discovery;

import com.motadata.nms.discovery.context.DiscoveryContextBuilderVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.motadata.nms.utils.EventBusChannels.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class DiscoverySystemTest {

  private static final String DISCOVERY_JOB_CHANNEL = "discovery.job.channel";

  @BeforeEach
  void setup(Vertx vertx, VertxTestContext testContext) {
    // Mock the database responses for the context builder
    vertx.eventBus().consumer(DISCOVERY_PROFILE_GET.name(), msg -> {
      Integer profileId = (Integer) msg.body();
      if (profileId.equals(1)) {
        JsonObject profile = new JsonObject()
          .put("id", 1)
          .put("target", "192.168.1.1-192.168.1.10")
          .put("credentials_profile_id", 2);
        msg.reply(profile);
      } else {
        msg.fail(404, "Profile not found");
      }
    });

    vertx.eventBus().consumer(CREDENTIAL_PROFILE_GET.name(), msg -> {
      Integer profileId = (Integer) msg.body();
      if (profileId.equals(2)) {
        JsonObject credential = new JsonObject()
          .put("id", 2)
          .put("name", "Test SNMP")
          .put("credential", new JsonObject()
            .put("type", "SNMP")
            .put("version", "v2c")
            .put("community", "public"));
        msg.reply(credential);
      } else {
        msg.fail(404, "Credential not found");
      }
    });

    // Deploy the worker verticle
    DeploymentOptions workerOptions = new DeploymentOptions()
      .setWorker(true);

    // Deploy the standard verticle
    DeploymentOptions standardOptions = new DeploymentOptions()
      .putHeader("discovery.batch.size", "5");

    // Deploy both verticles
    vertx.deployVerticle(new DiscoveryContextBuilderVerticle(), workerOptions)
      .compose(id -> vertx.deployVerticle(new DiscoveryVerticle(), standardOptions))
      .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void testEndToEndDiscoveryFlow(Vertx vertx, VertxTestContext testContext) {
    // Count the number of jobs created
    AtomicInteger jobCount = new AtomicInteger(0);
    List<JsonObject> jobs = new ArrayList<>();

    // Register a consumer for the job channel
    vertx.eventBus().consumer(DISCOVERY_JOB_CHANNEL, msg -> {
      JsonObject job = (JsonObject) msg.body();
      jobs.add(job);

      // If we've received all expected jobs, complete the test
      if (jobCount.incrementAndGet() == 10) {
        testContext.verify(() -> {
          // Verify we have jobs for all IPs
          assertEquals(10, jobs.size());

          // Verify job properties
          JsonObject firstJob = jobs.get(0);
          assertTrue(firstJob.getString("ip").startsWith("192.168.1."));
          assertEquals(161, firstJob.getInteger("port"));
          assertEquals(2, firstJob.getInteger("credentialsProfileId"));
          assertEquals(1, firstJob.getInteger("discoveryProfileId"));

          testContext.completeNow();
        });
      }
    });

    // Trigger discovery
    vertx.eventBus().request(DISCOVERY_TRIGGER.name(), 1, testContext.succeeding(reply -> {
      JsonObject response = (JsonObject) reply.body();
      testContext.verify(() -> {
        assertEquals("success", response.getString("status"));
        assertEquals(10, response.getInteger("jobsCreated"));
      });
    }));
  }

  @Test
  void testDiscoveryWithInvalidProfileId(Vertx vertx, VertxTestContext testContext) {
    vertx.eventBus().request(DISCOVERY_TRIGGER.name(), 999, testContext.failing(error -> {
      testContext.verify(() -> {
        assertEquals(404, error.failureCode());
        assertTrue(error.getMessage().contains("not found"));
        testContext.completeNow();
      });
    }));
  }
}
