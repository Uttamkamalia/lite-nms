package com.motadata.nms.polling;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.motadata.nms.utils.EventBusChannels.*;

/**
 * PollingSchedulerVerticle schedules periodic polling tasks based on metric group configurations.
 * It retrieves polling jobs from shared data and sends them to the MetricPollerVerticle.
 */
public class PollingSchedulerVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(PollingSchedulerVerticle.class);
    private static final String POLLING_JOBS_MAP = "polling-jobs";

    // Map to store timer IDs for each metric group
    private final Map<String, Long> scheduledTimers = new ConcurrentHashMap<>();

    @Override
    public void start(Promise<Void> startPromise) {
        // Register consumer for scheduling polling jobs
        vertx.eventBus().consumer(METRIC_GROUP_POLLING_SCHEDULE.name(), message -> {
            JsonObject request = (JsonObject) message.body();
            Integer metricGroupId = request.getInteger("metricGroupId");
            Integer jobId = request.getInteger("jobId");
            Integer deviceTypeId = request.getInteger("deviceTypeId");
            Integer pollingInterval = request.getInteger("pollingInterval", 60); // TODO refactor to fetch default from config()

            if (metricGroupId == null || jobId == null || deviceTypeId == null) {
                message.fail(400, "Missing required parameters: metricGroupId, jobId, or deviceTypeId");
                return;
            }

            // Create a unique key for this metric group and device type
            String schedulerKey = metricGroupId + "-" + deviceTypeId+"-"+jobId;

            // Cancel existing timer if it exists
            if (scheduledTimers.containsKey(schedulerKey)) {
                vertx.cancelTimer(scheduledTimers.get(schedulerKey));
                logger.info("Cancelled existing polling timer for metric group " + metricGroupId);
            }

            // Schedule new periodic task
            long timerId = schedulePollingTask(metricGroupId, deviceTypeId, jobId, pollingInterval);
            scheduledTimers.put(schedulerKey, timerId);

            logger.info("Scheduled polling for metric group " + metricGroupId + " and jobId " + jobId + " for device type " + deviceTypeId +
                       " with interval " + pollingInterval + " seconds");

            message.reply(new JsonObject()
                .put("status", "success")
                .put("message", "Polling scheduled")
                .put("timerId", timerId));
        });

        logger.info("PollingSchedulerVerticle started");
        startPromise.complete();
    }

    // TODO need to refactor this to schedule just one periodic job for each metric group
    private long schedulePollingTask(Integer metricGroupId, Integer deviceTypeId, Integer jobId, Integer pollingInterval) {
        // Convert polling interval from seconds to milliseconds
        long intervalMs = pollingInterval * 1000L;

        // Schedule periodic task
        return vertx.setPeriodic(intervalMs, timerId -> {
            // Get the shared map containing polling jobs
            LocalMap<String, JsonObject> pollingJobsMap = PollingOrchestratorVerticle.getPollingJobsMap(metricGroupId, deviceTypeId);

            if (pollingJobsMap.isEmpty() ) {
                logger.warn("No polling jobs found for metric group " + metricGroupId);
                return;
            }

            logger.info("Executing polling for metric group " + metricGroupId +
                       " with " + pollingJobsMap.size() + " jobs");

            JsonObject job = JsonObject.mapFrom(pollingJobsMap.get(jobId));
            pollingJobsMap.forEach((key, value) -> {
              logger.info("Sending Polling-job: " + key + " - " + value + "for execution");
              vertx.eventBus().send(METRIC_POLLER_EXECUTE.name(), value);
            });
        });
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        // Cancel all scheduled timers
        for (Long timerId : scheduledTimers.values()) {
            vertx.cancelTimer(timerId);
        }
        scheduledTimers.clear();

        logger.info("PollingSchedulerVerticle stopped");
        stopPromise.complete();
    }
}
