package com.motadata.nms.polling;

import com.motadata.nms.datastore.utils.ConfigKeys;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static com.motadata.nms.datastore.utils.ConfigKeys.*;
import static com.motadata.nms.utils.EventBusChannels.*;

public class PollingJobExecutorVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(PollingJobExecutorVerticle.class);

    @Override
    public void start(Promise<Void> startPromise) {
        String pluginExecutable = config().getJsonObject(ConfigKeys.POLLING).getString(POLLING_PLUGIN_EXECUTABLE);

        vertx.eventBus().consumer(METRIC_POLLER_EXECUTE.name(), message -> {
            JsonObject pollingJob = (JsonObject) message.body();
            String jobId = pollingJob.getString("id");

            logger.info("Executing polling job: {}", jobId);

            Process process = null;
            try {
                // Encode the job as Base64 to avoid command-line escaping issues
                String encodedJob = pollingJob.encode();
                ProcessBuilder processBuilder = new ProcessBuilder(
                    pluginExecutable,
                    "POLLING" ,  encodedJob
                );

                // Redirect error stream to output stream
                processBuilder.redirectErrorStream(true);

                logger.debug("Executing polling plugin: {}", String.join(" ", processBuilder.command()));

                // Start the process
                process = processBuilder.start();

                // Read the process output
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                // Wait for the process to complete with timeout
                long pollingJobTimeout = config()
                    .getJsonObject(ConfigKeys.POLLING)
                    .getInteger(POLLING_JOB_TIMEOUT_MS, 3000);

                boolean finished = process.waitFor(pollingJobTimeout, TimeUnit.MILLISECONDS);

                if (!finished) {
                    logger.error("Timeout occurred while waiting for polling job (jobId={})", jobId);

                    process.destroyForcibly(); // Kill the hung process
                } else {
                    int exitCode = process.exitValue();

                    if (exitCode == 0) {
                        // Try to parse the output as JSON
                        try {
                            JsonObject result = new JsonObject(output.toString().trim());
                            result.put("status", "success");
                            result.put("jobId", jobId);

                            logger.info("Polling job completed successfully: {} {} ",jobId, output);
//                            processPollingResults(pollingJob, result);
                            message.reply(result);
                        } catch (Exception e) {
                            logger.error("Failed to parse plugin output as JSON: {}", e.getMessage());

                            JsonObject result = new JsonObject()
                                .put("status", "error")
                                .put("jobId", jobId)
                                .put("error", "Failed to parse plugin output: " + e.getMessage())
                                .put("output", output.toString());

//                            handlePollingError(pollingJob, result);
//                            message.reply(result);
                        }
                    } else {
                        logger.error("Polling plugin failed with exit code {}: {}", exitCode, output.toString());

                        JsonObject result = new JsonObject()
                            .put("status", "error")
                            .put("jobId", jobId)
                            .put("exitCode", exitCode)
                            .put("error", "Plugin failed with exit code " + exitCode)
                            .put("output", output.toString());

//                        handlePollingError(pollingJob, result);
//                        message.reply(result);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to execute polling plugin: {}", e.getMessage(), e);

                JsonObject result = new JsonObject()
                    .put("status", "error")
                    .put("jobId", jobId)
                    .put("error", "Failed to execute plugin: " + e.getMessage());

//                handlePollingError(pollingJob, result);
//                message.reply(result);
            }
        });

        logger.info("PollingJobExecutorVerticle started");
        startPromise.complete();
    }

    /**
     * Process successful polling results
     * @param job The original polling job
     * @param result The execution result
     */
    private void processPollingResults(JsonObject job, JsonObject result) {
        String jobId = job.getString("id");
        Integer metricGroupId = job.getInteger("metric_group_id");

        logger.info("Processing results for polling job: {}", jobId);

        // Extract metrics data from the result
        JsonArray metricsData = result.getJsonArray("metrics", new JsonArray());

        if (metricsData.isEmpty()) {
            logger.warn("No metrics data returned for job: {}", jobId);
            return;
        }

        // Store metrics data
        JsonObject metricsStorage = new JsonObject()
            .put("jobId", jobId)
            .put("metricGroupId", metricGroupId)
            .put("timestamp", System.currentTimeMillis())
            .put("metrics", metricsData);

        vertx.eventBus().request(POLLING_RESULTS_STORE.name(), metricsStorage, reply -> {
            if (reply.failed()) {
                logger.error("Failed to store polling results: {}", reply.cause().getMessage());
            }
        });
    }

    /**
     * Handle polling errors
     * @param job The original polling job
     * @param error The error result
     */
    private void handlePollingError(JsonObject job, JsonObject error) {
        String jobId = job.getString("id");
        Integer metricGroupId = job.getInteger("metric_group_id");

        logger.error("Handling error for polling job: {}", jobId);

        // Create error record
        JsonObject errorRecord = new JsonObject()
            .put("jobId", jobId)
            .put("metricGroupId", metricGroupId)
            .put("timestamp", System.currentTimeMillis())
            .put("error", error.getString("error"))
            .put("details", error);

        // Store error record
        vertx.eventBus().request(POLLING_ERRORS_STORE.name(), errorRecord, reply -> {
            if (reply.failed()) {
                logger.error("Failed to store polling error: {}", reply.cause().getMessage());
            }
        });
    }
}
