package com.motadata.nms.commons;

import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;

public class SharedMapUtils {

  private static final String POLLING_JOBS_MAP = "polling-jobs";
  private static final String SCHEDULED_TIMERS_MAP = "scheduled-polling-timers";

  public static LocalMap<String, JsonObject> getPollingJobsMap(Integer metricGroupId, int deviceTypeId) {
    String mapName = POLLING_JOBS_MAP + "-" + metricGroupId + "-" + deviceTypeId;
    return VertxProvider.getVertx().sharedData().getLocalMap(mapName);
  }

  public static LocalMap<String, Long> getMetricGroupPollingScheduledJobTimersMap(Integer metricGroupId, int deviceTypeId) {
    String mapName = SCHEDULED_TIMERS_MAP + "-" + metricGroupId + "-" + deviceTypeId;
    return VertxProvider.getVertx().sharedData().getLocalMap(mapName);
  }
}
