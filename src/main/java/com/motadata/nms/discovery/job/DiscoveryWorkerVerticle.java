package com.motadata.nms.discovery.job;

import com.motadata.nms.models.ProvisionedDevice;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static com.motadata.nms.utils.EventBusChannels.*;

public class DiscoveryWorkerVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(DiscoveryWorkerVerticle.class);

  // TODO to use config()
  private static final int PING_TIMEOUT = 3000; // ms
  private static final int PORT_CHECK_TIMEOUT = 3000; // ms
  private static final int GO_PROCESS_TIMEOUT = 30000; // ms

  // TODO to use config()
  private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
  private static final String JOB_FILES_DIR = TEMP_DIR + File.separator + "discovery_jobs";

  // TODO to get this plugin name from enum Protocol
  private static final String GO_PLUGIN_PATH = "discovery_plugin"; // Path to the Go executable

  private NetClient netClient;

  @Override
  public void start(Promise<Void> startPromise) {
    // Initialize the NetClient for port checks
    NetClientOptions options = new NetClientOptions()
      .setConnectTimeout(PORT_CHECK_TIMEOUT)
      .setReconnectAttempts(0);
    netClient = vertx.createNetClient(options);

    vertx.fileSystem().mkdirs(JOB_FILES_DIR)
      .onSuccess(v -> {
        // Register consumer for discovery jobs
        vertx.eventBus().consumer(DISCOVERY_JOBS_SSH.name(), message -> {
          JsonObject jobJson = (JsonObject) message.body();

          // Determine job type and create appropriate job object
          String jobType = jobJson.getString("type", "UNKNOWN");
          DiscoveryJob job;

          // TODO refactor this
          try {
            if ("SNMP".equals(jobType)) {
              job = SnmpDiscoveryJob.fromJson(jobJson);
            } else if ("SSH".equals(jobType)) {
              job = SshDiscoveryJob.fromJson(jobJson);
            } else {
              throw new IllegalArgumentException("Unsupported job type: " + jobType);
            }

            // Process the job
            processDiscoveryJob(job)
              .onSuccess(result -> message.reply(result))
              .onFailure(err -> message.fail(500, err.getMessage()));

          } catch (Exception e) {
            logger.error("Failed to process discovery job", e);
            message.fail(400, "Failed to process discovery job: " + e.getMessage());
          }
        });

        logger.info("DiscoveryWorkerVerticle started");
        startPromise.complete();
      })
      .onFailure(err -> {
        logger.error("Failed to create job files directory", err);
        startPromise.fail(err);
      });
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    if (netClient != null) {
      netClient.close();
    }
    stopPromise.complete();
  }

  /**
   * Process a discovery job
   * @param job The discovery job to process
   * @return A Future containing the result of the discovery
   */
  private Future<JsonObject> processDiscoveryJob(DiscoveryJob job) {
    String ip = job.getIpBatch().get(0); // Get the first IP from the batch
    logger.info("Processing discovery job for IP {} with protocol {}",
                ip, job.getClass().getSimpleName());

    // Step 1: Ping check
    return pingHost(ip)
      .compose(pingSuccess -> {
        logger.debug("Ping successful for IP {}", ip);

        // Step 2: Port check
        return checkPort(ip, job.getPort());
      })
      .compose(portSuccess -> {
          logger.debug("Port {} is open on IP {}", job.getPort(), ip);

        // Step 3: Serialize job to JSON file
        return serializeJobToFile(job);
      })
      .compose(jobFilePath -> {
        logger.debug("Job serialized to file: {}", jobFilePath);

        // Step 4: Execute Go plugin
        return executeGoPlugin(jobFilePath, job);
      })
      .compose(result -> {
        logger.debug("Go plugin execution result: {}", result.encodePrettily());

        // Step 5: Process the result
        if ("SUCCESS".equals(result.getString("status"))) {
          // Create a provisioned device
          ProvisionedDevice device = new ProvisionedDevice(
            ip,
            job.getPort(),
            job.getDiscoveryProfileId(),
            job.getCredentialProfileId(),
            result.getString("hostname", "unknown-" + ip),
            result.getString("os", "Unknown"),
            result.getString("protocol", job instanceof SnmpDiscoveryJob ? "SNMP" : "SSH"),
            "SUCCESS",
            Instant.now().toString()
          );

          // Save the device to the database
          return saveProvisionedDevice(device)
            .map(savedDevice -> {
              // Return success result
              return new JsonObject()
                .put("status", "SUCCESS")
                .put("message", "Device discovered successfully")
                .put("device", savedDevice.toJson());
            });
        } else {
          // Return failure result
          return Future.succeededFuture(new JsonObject()
            .put("status", "FAILED")
            .put("message", result.getString("reason", "Unknown error"))
            .put("ip", ip)
            .put("port", job.getPort()));
        }
      })
      .recover(err -> {
        logger.error("Discovery failed for IP {}: {}", ip, err.getMessage());

        // Return failure result
        return Future.succeededFuture(new JsonObject()
          .put("status", "FAILED")
          .put("message", err.getMessage())
          .put("ip", ip)
          .put("port", job.getPort()));
      });
  }

  /**
   * Ping a host to check if it's reachable
   * @param ip The IP address to ping
   * @return A Future that succeeds if the host is reachable
   */
  private Future<Void> pingHost(String ip) {
    Promise<Void> promise = Promise.promise();

    // Use the vertx executeBlocking method to run the ping command
    vertx.executeBlocking(blocking -> {
      try {
        // Use Java's ProcessBuilder to execute the ping command
        ProcessBuilder processBuilder = new ProcessBuilder();

        // Adjust the command based on the operating system
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
          // Windows ping command
          processBuilder.command("ping", "-n", "1", "-w", String.valueOf(PING_TIMEOUT), ip);
        } else {
          // Unix/Linux ping command
          processBuilder.command("ping", "-c", "1", "-W", String.valueOf(PING_TIMEOUT / 1000), ip);
        }

        Process process = processBuilder.start();
        boolean completed = process.waitFor(PING_TIMEOUT, TimeUnit.MILLISECONDS);

        if (!completed) {
          process.destroyForcibly();
          blocking.fail("Ping timed out for " + ip);
          return;
        }

        int exitCode = process.exitValue();
        if (exitCode == 0) {
          blocking.complete();
        } else {
          blocking.fail("Host unreachable: " + ip);
        }
      } catch (Exception e) {
        blocking.fail("Ping failed: " + e.getMessage());
      }
    }, false) // Run in worker thread pool
      .onSuccess(result -> promise.complete())
      .onFailure(promise::fail);

    return promise.future();
  }

  /**
   * Check if a port is open on a host
   * @param ip The IP address to check
   * @param port The port to check
   * @return A Future that succeeds if the port is open
   */
  private Future<Void> checkPort(String ip, int port) {
    Promise<Void> promise = Promise.promise();

    netClient.connect(port, ip, result -> {
      if (result.succeeded()) {
        // Close the connection immediately
        result.result().close();
        promise.complete();
      } else {
        promise.fail("Port " + port + " is closed on " + ip + ": " + result.cause().getMessage());
      }
    });

    return promise.future();
  }

  /**
   * Serialize a discovery job to a JSON file
   * @param job The discovery job to serialize
   * @return A Future containing the path to the job file
   */
  private Future<String> serializeJobToFile(DiscoveryJob job) {
    Promise<String> promise = Promise.promise();

    // Create the job file path
    String jobFilePath = JOB_FILES_DIR + File.separator + job.getId() + ".json";

    // Serialize the job to JSON
    JsonObject jobJson = job.toJson();

    // Write the JSON to the file
    vertx.fileSystem().writeFile(jobFilePath, Buffer.buffer(jobJson.encode()))
      .onSuccess(v -> promise.complete(jobFilePath))
      .onFailure(err -> {
        logger.error("Failed to write job file", err);
        promise.fail(err);
      });

    return promise.future();
  }

  /**
   * Execute the Go plugin with the job file
   * @param jobFilePath The path to the job file
   * @param job The discovery job
   * @return A Future containing the result of the Go plugin execution
   */
  private Future<JsonObject> executeGoPlugin(String jobFilePath, DiscoveryJob job) {
    Promise<JsonObject> promise = Promise.promise();

    // Use vertx.executeBlocking to run the Go plugin
    vertx.executeBlocking(blocking -> {
      try {
        // Build the command to run the Go plugin
        ProcessBuilder processBuilder = new ProcessBuilder();

        // Set the command with the job file path
        processBuilder.command(
          GO_PLUGIN_PATH,
          "--job-file=" + jobFilePath
        );

        // Redirect error stream to output stream
        processBuilder.redirectErrorStream(true);

        logger.debug("Executing Go plugin: {}", String.join(" ", processBuilder.command()));

        // Start the process
        Process process = processBuilder.start();

        // Read the process output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
          String line;
          while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
          }
        }

        // Wait for the process to complete with timeout
        boolean completed = process.waitFor(GO_PROCESS_TIMEOUT, TimeUnit.MILLISECONDS);

        if (!completed) {
          process.destroyForcibly();
          blocking.fail("Go plugin timed out for job: " + job.getId());
          return;
        }

        int exitCode = process.exitValue();
        if (exitCode == 0) {
          // Parse the output as JSON
          try {
            JsonObject result = new JsonObject(output.toString());
            blocking.complete(result);
          } catch (Exception e) {
            blocking.fail("Failed to parse Go plugin output as JSON: " + e.getMessage());
          }
        } else {
          blocking.fail("Go plugin failed with exit code " + exitCode + ": " + output.toString());
        }
      } catch (Exception e) {
        blocking.fail("Failed to execute Go plugin: " + e.getMessage());
      }
    }, false) // Run in worker thread pool
      .onSuccess(result -> promise.complete((JsonObject) result))
      .onFailure(promise::fail);

    return promise.future();
  }

  /**
   * Save a provisioned device to the database
   * @param device The provisioned device to save
   * @return A Future containing the saved device
   */
  private Future<ProvisionedDevice> saveProvisionedDevice(ProvisionedDevice device) {
    Promise<ProvisionedDevice> promise = Promise.promise();

    // SQL to insert a provisioned device
    String sql = "INSERT INTO provisioned_devices " +
                 "(ip_address, port, discovery_profile_id, credential_profile_id, hostname, os, protocol, status, discovery_time) " +
                 "VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9) " +
                 "RETURNING id";

    // Create a tuple with the device data
    Tuple params = Tuple.of(
      device.getIpAddress(),
      device.getPort(),
      device.getDiscoveryProfileId(),
      device.getCredentialProfileId(),
      device.getHostname(),
      device.getOs(),
      device.getProtocol(),
      device.getStatus(),
      device.getDiscoveryTime()
    );

    // Execute the query
    pgPool.preparedQuery(sql)
      .execute(params)
      .onSuccess(rows -> {
        // Get the generated ID
        RowSet<Row> rowSet = rows;
        Row row = rowSet.iterator().next();
        int id = row.getInteger(0);

        // Set the ID on the device
        device.setId(id);

        // Send the device to the event bus
        vertx.eventBus().send(DISCOVERY_RESULT.name(), device.toJson());

        promise.complete(device);
      })
      .onFailure(err -> {
        logger.error("Failed to save provisioned device", err);
        promise.fail(err);
      });

    return promise.future();
  }
}
