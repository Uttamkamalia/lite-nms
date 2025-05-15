package com.motatadata.training.datastore;

import com.motadata.nms.datastore.DatabaseVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.junit5.Checkpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class DatabaseVerticleTest {

  @BeforeEach
  void deployVerticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new DatabaseVerticle(), testContext.succeedingThenComplete());
  }

  @Test
  void testSaveAndRetrieveDevice(Vertx vertx, VertxTestContext testContext) {
    JsonObject device = new JsonObject()
      .put("ip_address", "10.0.0.1")
      .put("hostname", "test-device")
      .put("os", "Linux")
      .put("device_type", "SSH")
      .put("discovery_id", 42);

    Checkpoint checkpoint = testContext.checkpoint(2);

    vertx.eventBus().request("db.saveDiscovery", device, saveRes -> {
      testContext.verify(() -> {
        assertTrue(saveRes.succeeded());
        assertEquals("Device saved", saveRes.result().body());
        checkpoint.flag();
      });

      // Fetch devices
      vertx.eventBus().request("db.getDiscoveredDevices", "", fetchRes -> {
        testContext.verify(() -> {
          assertTrue(fetchRes.succeeded());
          JsonArray resultArray = (JsonArray) fetchRes.result().body();
          assertFalse(resultArray.isEmpty());
          JsonObject savedDevice = resultArray.stream()
            .map(obj -> (JsonObject) obj)
            .filter(obj -> "10.0.0.1".equals(obj.getString("ip_address")))
            .findFirst().orElse(null);
          assertNotNull(savedDevice);
          assertEquals("test-device", savedDevice.getString("hostname"));
          checkpoint.flag();
        });
      });
    });
  }
}
