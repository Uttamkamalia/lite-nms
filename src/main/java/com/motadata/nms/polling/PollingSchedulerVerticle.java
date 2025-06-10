package com.motadata.nms.polling;

import com.motadata.nms.commons.VertxProvider;
import com.motadata.nms.datastore.utils.ConfigKeys;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;


import static com.motadata.nms.commons.SharedMapUtils.getPollingJobsMap;
import static com.motadata.nms.commons.SharedMapUtils.getMetricGroupPollingScheduledJobTimersMap;
import static com.motadata.nms.datastore.utils.ConfigKeys.*;
import static com.motadata.nms.datastore.utils.ConfigKeys.POLLING_PLUGIN_EXECUTABLE;
import static com.motadata.nms.utils.EventBusChannels.*;


public class PollingSchedulerVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(PollingSchedulerVerticle.class);
    private  Integer defaultPollingIntervalMs ;
    private Integer pollingJobTimeoutMs;
    private String pluginExecutable;

    @Override
    public void start(Promise<Void> startPromise) {
      defaultPollingIntervalMs = config().getJsonObject(POLLING).getInteger(POLLING_DEFAULT_INTERVAL_MS, 60000);
      pollingJobTimeoutMs = config().getJsonObject(ConfigKeys.POLLING).getInteger(POLLING_JOB_TIMEOUT_MS, 10000);
      pluginExecutable = config().getJsonObject(ConfigKeys.POLLING).getString(POLLING_PLUGIN_EXECUTABLE);

        vertx.eventBus().consumer(METRIC_GROUP_POLLING_SCHEDULE.name(), message -> {
            JsonObject request = (JsonObject) message.body();
            Integer metricGroupId = request.getInteger("metric_group_id");
            Integer deviceTypeId = request.getInteger("device_type_id");
            Integer pollingInterval = request.getInteger("polling_interval_seconds", defaultPollingIntervalMs);

            if (metricGroupId == null || deviceTypeId == null) {
                message.fail(400, "Missing required parameters: metricGroupId, jobId, or deviceTypeId");
                return;
            }

            String metricGroupScheduledTimerKey = metricGroupId + "-" + deviceTypeId;

            LocalMap<String, Long> scheduledTimers = getMetricGroupPollingScheduledJobTimersMap(metricGroupId, deviceTypeId);

            if (scheduledTimers.containsKey(metricGroupScheduledTimerKey)) {
                vertx.cancelTimer(scheduledTimers.get(metricGroupScheduledTimerKey));
                logger.info("Cancelled existing polling timer for metric group " + metricGroupId);
            }

            long timerId = schedulePollingTask(metricGroupId, deviceTypeId, pollingInterval);
            scheduledTimers.put(metricGroupScheduledTimerKey, timerId);

            logger.info("Scheduled polling for metric group " + metricGroupId  + " for device type " + deviceTypeId +
                       " with interval " + pollingInterval + " seconds");
        });

        logger.info("PollingSchedulerVerticle started");
        startPromise.complete();
    }

    private long schedulePollingTask(Integer metricGroupId, Integer deviceTypeId, Integer pollingInterval) {
        long intervalMs = pollingInterval * 1000L;

        return vertx.setPeriodic(intervalMs, timerId -> {
            LocalMap<String, JsonObject> pollingJobsMap = getPollingJobsMap(metricGroupId, deviceTypeId);

            if (pollingJobsMap.isEmpty() ) {
                logger.warn("No polling jobs found for metric group " + metricGroupId);
                return;
            }
            logger.debug("Executing polling for metric group " + metricGroupId + " with " + pollingJobsMap.size() + " jobs");

            pollingJobsMap.forEach((key, value) -> {
              logger.debug("Sending Polling-job execution: " + key + " - " + value + "for execution");
              PollingExecution pollingExecution = new PollingExecution(value, pluginExecutable, pollingJobTimeoutMs);
              VertxProvider.getVertx().executeBlocking(pollingExecution::execute, false, res -> {
                if (res.failed()) {
                  logger.error("Failed to execute polling job: " + key, res.cause());
                }
              });
            });
        });
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        // Cancel all scheduled timers TODO

        logger.info("PollingSchedulerVerticle stopped");
        stopPromise.complete();
    }
}
