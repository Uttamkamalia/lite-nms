package com.motadata.nms.models;

import com.motadata.nms.commons.NMSException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.motadata.nms.rest.utils.ErrorCodes.BAD_REQUEST;

public class MetricGroup {
  private Integer id;
  private String name;
  private List<Integer> metrics;
  private Integer deviceTypeId;
  private Integer pollingIntervalSeconds;
  private Instant lastPolledAt;
  private String status;
  private Instant createdAt;

  // Default constructor
  public MetricGroup() {
    this.metrics = new ArrayList<>();
    this.pollingIntervalSeconds = 10; // Default polling interval
    this.status = "STOPPED"; // Default status
  }

  // Constructor without ID
  public MetricGroup(String name, List<Integer> metrics, Integer deviceTypeId) {
    this();
    this.name = name;
    this.metrics = metrics;
    this.deviceTypeId = deviceTypeId;
  }

  // Full constructor
  public MetricGroup(Integer id, String name, List<Integer> metrics, Integer deviceTypeId,
                    Integer pollingIntervalSeconds, Instant lastPolledAt, String status, Instant createdAt) {
    this.id = id;
    this.name = name;
    this.metrics = metrics;
    this.deviceTypeId = deviceTypeId;
    this.pollingIntervalSeconds = pollingIntervalSeconds;
    this.lastPolledAt = lastPolledAt;
    this.status = status;
    this.createdAt = createdAt;
  }

  // Getters and setters
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Integer> getMetrics() {
    return metrics;
  }

  public void setMetrics(List<Integer> metrics) {
    this.metrics = metrics;
  }

  public Integer getDeviceTypeId() {
    return deviceTypeId;
  }

  public void setDeviceTypeId(Integer deviceTypeId) {
    this.deviceTypeId = deviceTypeId;
  }

  public Integer getPollingIntervalSeconds() {
    return pollingIntervalSeconds;
  }

  public void setPollingIntervalSeconds(Integer pollingIntervalSeconds) {
    this.pollingIntervalSeconds = pollingIntervalSeconds;
  }

  public Instant getLastPolledAt() {
    return lastPolledAt;
  }

  public void setLastPolledAt(Instant lastPolledAt) {
    this.lastPolledAt = lastPolledAt;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  // Convert to JSON
  public JsonObject toJson() {
    JsonObject json = new JsonObject()
      .put("name", name)
      .put("metrics", new JsonArray(metrics))
      .put("device_type_id", deviceTypeId)
      .put("polling_interval_seconds", pollingIntervalSeconds)
      .put("status", status);

    if (id != null) {
      json.put("id", id);
    }

    if (lastPolledAt != null) {
      json.put("last_polled_at", lastPolledAt.toString());
    }

    if (createdAt != null) {
      json.put("created_at", createdAt.toString());
    }

    return json;
  }

  // Create from JSON
  public static MetricGroup fromJson(JsonObject json) {
    if (json == null) {
      throw new NMSException(BAD_REQUEST, "MetricGroup JSON cannot be null");
    }

    try {
      MetricGroup metricGroup = new MetricGroup();

      metricGroup.setId(json.getInteger("id"));

      String name = json.getString("name");
      if (name == null || name.isEmpty()) {
        throw new NMSException(BAD_REQUEST, "Metric group name is required");
      }
      metricGroup.setName(name);

      JsonArray metricsArray = json.getJsonArray("metrics");
      if (metricsArray == null || metricsArray.isEmpty()) {
        throw new NMSException(BAD_REQUEST, "Metrics list cannot be empty");
      }

      List<Integer> metrics = new ArrayList<>();
      for (int i = 0; i < metricsArray.size(); i++) {
        metrics.add(metricsArray.getInteger(i));
      }
      metricGroup.setMetrics(metrics);

      Integer deviceTypeId = json.getInteger("device_type_id");
      if (deviceTypeId == null) {
        throw new NMSException(BAD_REQUEST, "Device type ID is required");
      }
      metricGroup.setDeviceTypeId(deviceTypeId);

      Integer pollingInterval = json.getInteger("polling_interval_seconds");
      if (pollingInterval != null) {
        metricGroup.setPollingIntervalSeconds(pollingInterval);
      }

      String status = json.getString("status");
      if (status != null) {
        metricGroup.setStatus(status);
      }

      String lastPolledAtStr = json.getString("last_polled_at");
      if (lastPolledAtStr != null && !lastPolledAtStr.isEmpty()) {
        metricGroup.setLastPolledAt(Instant.parse(lastPolledAtStr));
      }

      String createdAtStr = json.getString("created_at");
      if (createdAtStr != null && !createdAtStr.isEmpty()) {
        metricGroup.setCreatedAt(Instant.parse(createdAtStr));
      }

      return metricGroup;
    } catch (NMSException e) {
      throw e;
    } catch (Exception e) {
      throw new NMSException(BAD_REQUEST, "Invalid metric group data: " + e.getMessage(), e);
    }
  }

  @Override
  public String toString() {
    return "MetricGroup{" +
      "id=" + id +
      ", name='" + name + '\'' +
      ", metrics=" + metrics +
      ", deviceTypeId=" + deviceTypeId +
      ", pollingIntervalSeconds=" + pollingIntervalSeconds +
      ", status='" + status + '\'' +
      '}';
  }
}

